package net.minecraft.client.resources;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.hash.Hashing;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.SignatureState;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.authlib.properties.Property;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.texture.SkinTextureDownloader;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Services;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SkinManager {
   static final Logger LOGGER = LogUtils.getLogger();
   private final Services services;
   final SkinTextureDownloader skinTextureDownloader;
   private final LoadingCache<SkinManager.CacheKey, CompletableFuture<Optional<PlayerSkin>>> skinCache;
   private final SkinManager.TextureCache skinTextures;
   private final SkinManager.TextureCache capeTextures;
   private final SkinManager.TextureCache elytraTextures;

   public SkinManager(Path p_294690_, final Services p_439051_, SkinTextureDownloader p_442497_, final Executor p_294105_) {
      this.services = p_439051_;
      this.skinTextureDownloader = p_442497_;
      this.skinTextures = new SkinManager.TextureCache(p_294690_, Type.SKIN);
      this.capeTextures = new SkinManager.TextureCache(p_294690_, Type.CAPE);
      this.elytraTextures = new SkinManager.TextureCache(p_294690_, Type.ELYTRA);
      this.skinCache = CacheBuilder.newBuilder()
         .expireAfterAccess(Duration.ofSeconds(15L))
         .build(
            new CacheLoader<SkinManager.CacheKey, CompletableFuture<Optional<PlayerSkin>>>() {
               public CompletableFuture<Optional<PlayerSkin>> load(SkinManager.CacheKey p_296373_) {
                  return CompletableFuture.<MinecraftProfileTextures>supplyAsync(() -> {
                        Property property = p_296373_.packedTextures();
                        if (property == null) {
                           return MinecraftProfileTextures.EMPTY;
                        } else {
                           MinecraftProfileTextures minecraftprofiletextures = p_439051_.sessionService().unpackTextures(property);
                           if (minecraftprofiletextures.signatureState() == SignatureState.INVALID) {
                              SkinManager.LOGGER.warn("Profile contained invalid signature for textures property (profile id: {})", p_296373_.profileId());
                           }

                           return minecraftprofiletextures;
                        }
                     }, Util.backgroundExecutor().forName("unpackSkinTextures"))
                     .thenComposeAsync(p_307130_ -> SkinManager.this.registerTextures(p_296373_.profileId(), p_307130_), p_294105_)
                     .handle((p_451340_, p_389368_) -> {
                        if (p_389368_ != null) {
                           SkinManager.LOGGER.warn("Failed to load texture for profile {}", p_296373_.profileId, p_389368_);
                        }

                        return Optional.ofNullable(p_451340_);
                     });
               }
            }
         );
   }

   public Supplier<PlayerSkin> createLookup(GameProfile p_439880_, boolean p_440599_) {
      CompletableFuture<Optional<PlayerSkin>> completablefuture = this.get(p_439880_);
      PlayerSkin playerskin = DefaultPlayerSkin.get(p_439880_);
      if (SharedConstants.DEBUG_DEFAULT_SKIN_OVERRIDE) {
         return () -> playerskin;
      } else {
         Optional<PlayerSkin> optional = completablefuture.getNow(null);
         if (optional != null) {
            PlayerSkin playerskin1 = optional.filter(p_450748_ -> !p_440599_ || p_450748_.secure()).orElse(playerskin);
            return () -> playerskin1;
         } else {
            return () -> completablefuture.getNow(Optional.empty()).filter(p_450750_ -> !p_440599_ || p_450750_.secure()).orElse(playerskin);
         }
      }
   }

   public CompletableFuture<Optional<PlayerSkin>> get(GameProfile p_439848_) {
      if (SharedConstants.DEBUG_DEFAULT_SKIN_OVERRIDE) {
         PlayerSkin playerskin = DefaultPlayerSkin.get(p_439848_);
         return CompletableFuture.completedFuture(Optional.of(playerskin));
      } else {
         Property property = this.services.sessionService().getPackedTextures(p_439848_);
         return (CompletableFuture<Optional<PlayerSkin>>)this.skinCache.getUnchecked(new SkinManager.CacheKey(p_439848_.id(), property));
      }
   }

   CompletableFuture<PlayerSkin> registerTextures(UUID p_307544_, MinecraftProfileTextures p_307606_) {
      MinecraftProfileTexture minecraftprofiletexture = p_307606_.skin();
      CompletableFuture<ClientAsset.Texture> completablefuture;
      PlayerModelType playermodeltype;
      if (minecraftprofiletexture != null) {
         completablefuture = this.skinTextures.getOrLoad(minecraftprofiletexture);
         playermodeltype = PlayerModelType.byLegacyServicesName(minecraftprofiletexture.getMetadata("model"));
      } else {
         PlayerSkin playerskin = DefaultPlayerSkin.get(p_307544_);
         completablefuture = CompletableFuture.completedFuture(playerskin.body());
         playermodeltype = playerskin.model();
      }

      MinecraftProfileTexture minecraftprofiletexture2 = p_307606_.cape();
      CompletableFuture<ClientAsset.Texture> completablefuture1 = minecraftprofiletexture2 != null
         ? this.capeTextures.getOrLoad(minecraftprofiletexture2)
         : CompletableFuture.completedFuture(null);
      MinecraftProfileTexture minecraftprofiletexture1 = p_307606_.elytra();
      CompletableFuture<ClientAsset.Texture> completablefuture2 = minecraftprofiletexture1 != null
         ? this.elytraTextures.getOrLoad(minecraftprofiletexture1)
         : CompletableFuture.completedFuture(null);
      return CompletableFuture.allOf(completablefuture, completablefuture1, completablefuture2)
         .thenApply(
            p_450756_ -> new PlayerSkin(
               completablefuture.join(),
               completablefuture1.join(),
               completablefuture2.join(),
               playermodeltype,
               p_307606_.signatureState() == SignatureState.SIGNED
            )
         );
   }

   @OnlyIn(Dist.CLIENT)
   record CacheKey(UUID profileId, @Nullable Property packedTextures) {
   }

   @OnlyIn(Dist.CLIENT)
   class TextureCache {
      private final Path root;
      private final Type type;
      private final Map<String, CompletableFuture<ClientAsset.Texture>> textures = new Object2ObjectOpenHashMap();

      TextureCache(Path p_294453_, Type p_294220_) {
         this.root = p_294453_;
         this.type = p_294220_;
      }

      public CompletableFuture<ClientAsset.Texture> getOrLoad(MinecraftProfileTexture p_294862_) {
         String s = p_294862_.getHash();
         CompletableFuture<ClientAsset.Texture> completablefuture = this.textures.get(s);
         if (completablefuture == null) {
            completablefuture = this.registerTexture(p_294862_);
            this.textures.put(s, completablefuture);
         }

         return completablefuture;
      }

      private CompletableFuture<ClientAsset.Texture> registerTexture(MinecraftProfileTexture p_295647_) {
         String s = Hashing.sha1().hashUnencodedChars(p_295647_.getHash()).toString();
         Identifier identifier = this.getTextureLocation(s);
         Path path = this.root.resolve(s.length() > 2 ? s.substring(0, 2) : "xx").resolve(s);
         return SkinManager.this.skinTextureDownloader.downloadAndRegisterSkin(identifier, path, p_295647_.getUrl(), this.type == Type.SKIN);
      }

      private Identifier getTextureLocation(String p_295006_) {
         String s = switch (this.type) {
            case SKIN -> "skins";
            case CAPE -> "capes";
            case ELYTRA -> "elytra";
            default -> throw new MatchException(null, null);
         };
         return Identifier.withDefaultNamespace(s + "/" + p_295006_);
      }
   }
}
