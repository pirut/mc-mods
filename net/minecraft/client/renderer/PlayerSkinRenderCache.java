package net.minecraft.client.renderer;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class PlayerSkinRenderCache {
   public static final RenderType DEFAULT_PLAYER_SKIN_RENDER_TYPE = playerSkinRenderType(DefaultPlayerSkin.getDefaultSkin());
   public static final Duration CACHE_DURATION = Duration.ofMinutes(5L);
   private final LoadingCache<ResolvableProfile, CompletableFuture<Optional<PlayerSkinRenderCache.RenderInfo>>> renderInfoCache = CacheBuilder.newBuilder()
      .expireAfterAccess(CACHE_DURATION)
      .build(
         new CacheLoader<ResolvableProfile, CompletableFuture<Optional<PlayerSkinRenderCache.RenderInfo>>>() {
            public CompletableFuture<Optional<PlayerSkinRenderCache.RenderInfo>> load(ResolvableProfile p_440714_) {
               return p_440714_.resolveProfile(PlayerSkinRenderCache.this.profileResolver)
                  .thenCompose(
                     p_450739_ -> PlayerSkinRenderCache.this.skinManager
                        .get(p_450739_)
                        .thenApply(
                           p_450734_ -> p_450734_.map(p_450737_ -> PlayerSkinRenderCache.this.new RenderInfo(p_450739_, p_450737_, p_440714_.skinPatch()))
                        )
                  );
            }
         }
      );
   private final LoadingCache<ResolvableProfile, PlayerSkinRenderCache.RenderInfo> defaultSkinCache = CacheBuilder.newBuilder()
      .expireAfterAccess(CACHE_DURATION)
      .build(new CacheLoader<ResolvableProfile, PlayerSkinRenderCache.RenderInfo>() {
         public PlayerSkinRenderCache.RenderInfo load(ResolvableProfile p_439888_) {
            GameProfile gameprofile = p_439888_.partialProfile();
            return PlayerSkinRenderCache.this.new RenderInfo(gameprofile, DefaultPlayerSkin.get(gameprofile), p_439888_.skinPatch());
         }
      });
   final TextureManager textureManager;
   final SkinManager skinManager;
   final ProfileResolver profileResolver;

   public PlayerSkinRenderCache(TextureManager p_442728_, SkinManager p_440573_, ProfileResolver p_440192_) {
      this.textureManager = p_442728_;
      this.skinManager = p_440573_;
      this.profileResolver = p_440192_;
   }

   public PlayerSkinRenderCache.RenderInfo getOrDefault(ResolvableProfile p_438888_) {
      PlayerSkinRenderCache.RenderInfo playerskinrendercache$renderinfo = this.lookup(p_438888_).getNow(Optional.empty()).orElse(null);
      return playerskinrendercache$renderinfo != null
         ? playerskinrendercache$renderinfo
         : (PlayerSkinRenderCache.RenderInfo)this.defaultSkinCache.getUnchecked(p_438888_);
   }

   public Supplier<PlayerSkinRenderCache.RenderInfo> createLookup(ResolvableProfile p_443503_) {
      PlayerSkinRenderCache.RenderInfo playerskinrendercache$renderinfo = (PlayerSkinRenderCache.RenderInfo)this.defaultSkinCache.getUnchecked(p_443503_);
      CompletableFuture<Optional<PlayerSkinRenderCache.RenderInfo>> completablefuture = (CompletableFuture<Optional<PlayerSkinRenderCache.RenderInfo>>)this.renderInfoCache
         .getUnchecked(p_443503_);
      Optional<PlayerSkinRenderCache.RenderInfo> optional = completablefuture.getNow(null);
      if (optional != null) {
         PlayerSkinRenderCache.RenderInfo playerskinrendercache$renderinfo1 = optional.orElse(playerskinrendercache$renderinfo);
         return () -> playerskinrendercache$renderinfo1;
      } else {
         return () -> completablefuture.getNow(Optional.empty()).orElse(playerskinrendercache$renderinfo);
      }
   }

   public CompletableFuture<Optional<PlayerSkinRenderCache.RenderInfo>> lookup(ResolvableProfile p_446256_) {
      return (CompletableFuture<Optional<PlayerSkinRenderCache.RenderInfo>>)this.renderInfoCache.getUnchecked(p_446256_);
   }

   static RenderType playerSkinRenderType(PlayerSkin p_451433_) {
      return SkullBlockRenderer.getPlayerSkinRenderType(p_451433_.body().texturePath());
   }

   @OnlyIn(Dist.CLIENT)
   public final class RenderInfo {
      private final GameProfile gameProfile;
      private final PlayerSkin playerSkin;
      @Nullable
      private RenderType itemRenderType;
      @Nullable
      private GpuTextureView textureView;
      @Nullable
      private GlyphRenderTypes glyphRenderTypes;

      public RenderInfo(GameProfile p_440508_, PlayerSkin p_451437_, PlayerSkin.Patch p_451136_) {
         this.gameProfile = p_440508_;
         this.playerSkin = p_451437_.with(p_451136_);
      }

      public GameProfile gameProfile() {
         return this.gameProfile;
      }

      public PlayerSkin playerSkin() {
         return this.playerSkin;
      }

      public RenderType renderType() {
         if (this.itemRenderType == null) {
            this.itemRenderType = PlayerSkinRenderCache.playerSkinRenderType(this.playerSkin);
         }

         return this.itemRenderType;
      }

      public GpuTextureView textureView() {
         if (this.textureView == null) {
            this.textureView = PlayerSkinRenderCache.this.textureManager.getTexture(this.playerSkin.body().texturePath()).getTextureView();
         }

         return this.textureView;
      }

      public GlyphRenderTypes glyphRenderTypes() {
         if (this.glyphRenderTypes == null) {
            this.glyphRenderTypes = GlyphRenderTypes.createForColorTexture(this.playerSkin.body().texturePath());
         }

         return this.glyphRenderTypes;
      }

      @Override
      public boolean equals(Object p_439418_) {
         return this == p_439418_
            || p_439418_ instanceof PlayerSkinRenderCache.RenderInfo playerskinrendercache$renderinfo
               && this.gameProfile.equals(playerskinrendercache$renderinfo.gameProfile)
               && this.playerSkin.equals(playerskinrendercache$renderinfo.playerSkin);
      }

      @Override
      public int hashCode() {
         int i = 1;
         i = 31 * i + this.gameProfile.hashCode();
         return 31 * i + this.playerSkin.hashCode();
      }
   }
}
