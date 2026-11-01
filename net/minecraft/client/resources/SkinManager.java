package net.minecraft.client.resources;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.hash.Hashing;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.SignatureState;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.authlib.properties.Property;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.Optionull;
import net.minecraft.Util;
import net.minecraft.client.renderer.texture.HttpTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SkinManager {
   static final Logger LOGGER = LogUtils.getLogger();
   private final MinecraftSessionService sessionService;
   private final LoadingCache<SkinManager.CacheKey, CompletableFuture<PlayerSkin>> skinCache;
   private final SkinManager.TextureCache skinTextures;
   private final SkinManager.TextureCache capeTextures;
   private final SkinManager.TextureCache elytraTextures;

   public SkinManager(TextureManager p_118812_, Path p_294690_, final MinecraftSessionService p_118814_, final Executor p_294105_) {
      this.sessionService = p_118814_;
      this.skinTextures = new SkinManager.TextureCache(p_118812_, p_294690_, Type.SKIN);
      this.capeTextures = new SkinManager.TextureCache(p_118812_, p_294690_, Type.CAPE);
      this.elytraTextures = new SkinManager.TextureCache(p_118812_, p_294690_, Type.ELYTRA);
      this.skinCache = CacheBuilder.newBuilder()
         .expireAfterAccess(Duration.ofSeconds(15L))
         .build(new CacheLoader<SkinManager.CacheKey, CompletableFuture<PlayerSkin>>() {
            public CompletableFuture<PlayerSkin> load(SkinManager.CacheKey p_296373_) {
               return CompletableFuture.<MinecraftProfileTextures>supplyAsync(() -> {
                  Property $$2 = p_296373_.packedTextures();
                  if ($$2 == null) {
                     return MinecraftProfileTextures.EMPTY;
                  } else {
                     MinecraftProfileTextures $$3 = p_118814_.unpackTextures($$2);
                     if ($$3.signatureState() == SignatureState.INVALID) {
                        SkinManager.LOGGER.warn("Profile contained invalid signature for textures property (profile id: {})", p_296373_.profileId());
                     }

                     return $$3;
                  }
               }, Util.backgroundExecutor()).thenComposeAsync(p_307130_ -> SkinManager.this.registerTextures(p_296373_.profileId(), p_307130_), p_294105_);
            }
         });
   }

   public Supplier<PlayerSkin> lookupInsecure(GameProfile p_295432_) {
      CompletableFuture<PlayerSkin> $$1 = this.getOrLoad(p_295432_);
      PlayerSkin $$2 = DefaultPlayerSkin.get(p_295432_);
      return () -> $$1.getNow($$2);
   }

   public PlayerSkin getInsecureSkin(GameProfile p_294261_) {
      PlayerSkin $$1 = this.getOrLoad(p_294261_).getNow(null);
      return $$1 != null ? $$1 : DefaultPlayerSkin.get(p_294261_);
   }

   public CompletableFuture<PlayerSkin> getOrLoad(GameProfile p_294865_) {
      Property $$1 = this.sessionService.getPackedTextures(p_294865_);
      return (CompletableFuture<PlayerSkin>)this.skinCache.getUnchecked(new SkinManager.CacheKey(p_294865_.getId(), $$1));
   }

   CompletableFuture<PlayerSkin> registerTextures(UUID p_307544_, MinecraftProfileTextures p_307606_) {
      MinecraftProfileTexture $$2 = p_307606_.skin();
      CompletableFuture<ResourceLocation> $$3;
      PlayerSkin.Model $$4;
      if ($$2 != null) {
         $$3 = this.skinTextures.getOrLoad($$2);
         $$4 = PlayerSkin.Model.byName($$2.getMetadata("model"));
      } else {
         PlayerSkin $$5 = DefaultPlayerSkin.get(p_307544_);
         $$3 = CompletableFuture.completedFuture($$5.texture());
         $$4 = $$5.model();
      }

      String $$8 = Optionull.map($$2, MinecraftProfileTexture::getUrl);
      MinecraftProfileTexture $$9 = p_307606_.cape();
      CompletableFuture<ResourceLocation> $$10 = $$9 != null ? this.capeTextures.getOrLoad($$9) : CompletableFuture.completedFuture(null);
      MinecraftProfileTexture $$11 = p_307606_.elytra();
      CompletableFuture<ResourceLocation> $$12 = $$11 != null ? this.elytraTextures.getOrLoad($$11) : CompletableFuture.completedFuture(null);
      return CompletableFuture.allOf($$3, $$10, $$12)
         .thenApply(p_307126_ -> new PlayerSkin($$3.join(), $$8, $$10.join(), $$12.join(), $$4, p_307606_.signatureState() == SignatureState.SIGNED));
   }

   @OnlyIn(Dist.CLIENT)
   record CacheKey(UUID profileId, @Nullable Property packedTextures) {
   }

   @OnlyIn(Dist.CLIENT)
   static class TextureCache {
      private final TextureManager textureManager;
      private final Path root;
      private final Type type;
      private final Map<String, CompletableFuture<ResourceLocation>> textures = new Object2ObjectOpenHashMap();

      TextureCache(TextureManager p_295278_, Path p_294453_, Type p_294220_) {
         this.textureManager = p_295278_;
         this.root = p_294453_;
         this.type = p_294220_;
      }

      public CompletableFuture<ResourceLocation> getOrLoad(MinecraftProfileTexture p_294862_) {
         String $$1 = p_294862_.getHash();
         CompletableFuture<ResourceLocation> $$2 = this.textures.get($$1);
         if ($$2 == null) {
            $$2 = this.registerTexture(p_294862_);
            this.textures.put($$1, $$2);
         }

         return $$2;
      }

      private CompletableFuture<ResourceLocation> registerTexture(MinecraftProfileTexture p_295647_) {
         String $$1 = Hashing.sha1().hashUnencodedChars(p_295647_.getHash()).toString();
         ResourceLocation $$2 = this.getTextureLocation($$1);
         Path $$3 = this.root.resolve($$1.length() > 2 ? $$1.substring(0, 2) : "xx").resolve($$1);
         CompletableFuture<ResourceLocation> $$4 = new CompletableFuture<>();
         HttpTexture $$5 = new HttpTexture(
            $$3.toFile(), p_295647_.getUrl(), DefaultPlayerSkin.getDefaultTexture(), this.type == Type.SKIN, () -> $$4.complete($$2)
         );
         this.textureManager.register($$2, $$5);
         return $$4;
      }

      private ResourceLocation getTextureLocation(String p_295006_) {
         String $$1 = switch (this.type) {
            case SKIN -> "skins";
            case CAPE -> "capes";
            case ELYTRA -> "elytra";
            default -> throw new MatchException(null, null);
         };
         return ResourceLocation.withDefaultNamespace($$1 + "/" + p_295006_);
      }
   }
}
