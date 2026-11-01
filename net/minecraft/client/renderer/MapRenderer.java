package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MapTextureManager;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.data.AtlasIds;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.gui.map.MapDecorationRendererManager;
import net.neoforged.neoforge.client.renderstate.RenderStateExtensions;

@OnlyIn(Dist.CLIENT)
public class MapRenderer {
   private static final float MAP_Z_OFFSET = -0.01F;
   private static final float DECORATION_Z_OFFSET = -0.001F;
   public static final int WIDTH = 128;
   public static final int HEIGHT = 128;
   private final TextureAtlas decorationSprites;
   private final MapTextureManager mapTextureManager;

   public MapRenderer(AtlasManager p_435903_, MapTextureManager p_360823_) {
      this.decorationSprites = p_435903_.getAtlasOrThrow(AtlasIds.MAP_DECORATIONS);
      this.mapTextureManager = p_360823_;
   }

   public void render(MapRenderState p_360365_, PoseStack p_362483_, SubmitNodeCollector p_434609_, boolean p_362262_, int p_364872_) {
      p_434609_.submitCustomGeometry(p_362483_, RenderTypes.text(p_360365_.texture), (p_433720_, p_435645_) -> {
         p_435645_.addVertex(p_433720_, 0.0F, 128.0F, -0.01F).setColor(-1).setUv(0.0F, 1.0F).setLight(p_364872_);
         p_435645_.addVertex(p_433720_, 128.0F, 128.0F, -0.01F).setColor(-1).setUv(1.0F, 1.0F).setLight(p_364872_);
         p_435645_.addVertex(p_433720_, 128.0F, 0.0F, -0.01F).setColor(-1).setUv(1.0F, 0.0F).setLight(p_364872_);
         p_435645_.addVertex(p_433720_, 0.0F, 0.0F, -0.01F).setColor(-1).setUv(0.0F, 0.0F).setLight(p_364872_);
      });
      int i = 0;

      for (MapRenderState.MapDecorationRenderState maprenderstate$mapdecorationrenderstate : p_360365_.decorations) {
         if (!p_362262_ || maprenderstate$mapdecorationrenderstate.renderOnFrame) {
            if (MapDecorationRendererManager.render(
               maprenderstate$mapdecorationrenderstate, p_362483_, p_434609_, p_360365_, this.decorationSprites, p_362262_, p_364872_, i
            )) {
               i++;
            } else {
               p_362483_.pushPose();
               p_362483_.translate(maprenderstate$mapdecorationrenderstate.x / 2.0F + 64.0F, maprenderstate$mapdecorationrenderstate.y / 2.0F + 64.0F, -0.02F);
               p_362483_.mulPose(Axis.ZP.rotationDegrees(maprenderstate$mapdecorationrenderstate.rot * 360 / 16.0F));
               p_362483_.scale(4.0F, 4.0F, 3.0F);
               p_362483_.translate(-0.125F, 0.125F, 0.0F);
               TextureAtlasSprite textureatlassprite = maprenderstate$mapdecorationrenderstate.atlasSprite;
               if (textureatlassprite != null) {
                  float f = i * -0.001F;
                  p_434609_.submitCustomGeometry(
                     p_362483_,
                     RenderTypes.text(textureatlassprite.atlasLocation()),
                     (p_433913_, p_434099_) -> {
                        p_434099_.addVertex(p_433913_, -1.0F, 1.0F, f)
                           .setColor(-1)
                           .setUv(textureatlassprite.getU0(), textureatlassprite.getV0())
                           .setLight(p_364872_);
                        p_434099_.addVertex(p_433913_, 1.0F, 1.0F, f)
                           .setColor(-1)
                           .setUv(textureatlassprite.getU1(), textureatlassprite.getV0())
                           .setLight(p_364872_);
                        p_434099_.addVertex(p_433913_, 1.0F, -1.0F, f)
                           .setColor(-1)
                           .setUv(textureatlassprite.getU1(), textureatlassprite.getV1())
                           .setLight(p_364872_);
                        p_434099_.addVertex(p_433913_, -1.0F, -1.0F, f)
                           .setColor(-1)
                           .setUv(textureatlassprite.getU0(), textureatlassprite.getV1())
                           .setLight(p_364872_);
                     }
                  );
                  p_362483_.popPose();
               }

               if (maprenderstate$mapdecorationrenderstate.name != null) {
                  Font font = Minecraft.getInstance().font;
                  float f1 = font.width(maprenderstate$mapdecorationrenderstate.name);
                  float f2 = Mth.clamp(25.0F / f1, 0.0F, 0.6666667F);
                  p_362483_.pushPose();
                  p_362483_.translate(
                     maprenderstate$mapdecorationrenderstate.x / 2.0F + 64.0F - f1 * f2 / 2.0F,
                     maprenderstate$mapdecorationrenderstate.y / 2.0F + 64.0F + 4.0F,
                     -0.025F
                  );
                  p_362483_.scale(f2, f2, -1.0F);
                  p_362483_.translate(0.0F, 0.0F, 0.1F);
                  p_434609_.order(1)
                     .submitText(
                        p_362483_,
                        0.0F,
                        0.0F,
                        maprenderstate$mapdecorationrenderstate.name.getVisualOrderText(),
                        false,
                        Font.DisplayMode.NORMAL,
                        p_364872_,
                        -1,
                        Integer.MIN_VALUE,
                        0
                     );
                  p_362483_.popPose();
               }

               i++;
            }
         }
      }
   }

   public void extractRenderState(MapId p_361383_, MapItemSavedData p_363500_, MapRenderState p_364922_) {
      p_364922_.texture = this.mapTextureManager.prepareMapTexture(p_361383_, p_363500_);
      p_364922_.decorations.clear();
      RenderStateExtensions.onUpdateMapRenderState(p_363500_, p_364922_);

      for (MapDecoration mapdecoration : p_363500_.getDecorations()) {
         p_364922_.decorations
            .add(
               RenderStateExtensions.onUpdateMapDecorationRenderState(
                  mapdecoration.type(), p_363500_, p_364922_, this.extractDecorationRenderState(mapdecoration)
               )
            );
      }
   }

   private MapRenderState.MapDecorationRenderState extractDecorationRenderState(MapDecoration p_364175_) {
      MapRenderState.MapDecorationRenderState maprenderstate$mapdecorationrenderstate = new MapRenderState.MapDecorationRenderState();
      maprenderstate$mapdecorationrenderstate.type = p_364175_.type();
      maprenderstate$mapdecorationrenderstate.atlasSprite = this.decorationSprites.getSprite(p_364175_.getSpriteLocation());
      maprenderstate$mapdecorationrenderstate.x = p_364175_.x();
      maprenderstate$mapdecorationrenderstate.y = p_364175_.y();
      maprenderstate$mapdecorationrenderstate.rot = p_364175_.rot();
      maprenderstate$mapdecorationrenderstate.name = p_364175_.name().orElse(null);
      maprenderstate$mapdecorationrenderstate.renderOnFrame = p_364175_.renderOnFrame();
      return maprenderstate$mapdecorationrenderstate;
   }
}
