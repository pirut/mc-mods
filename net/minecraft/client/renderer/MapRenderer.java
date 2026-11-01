package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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

@Environment(EnvType.CLIENT)
public class MapRenderer {
   private static final float MAP_Z_OFFSET = -0.01F;
   private static final float DECORATION_Z_OFFSET = -0.001F;
   public static final int WIDTH = 128;
   public static final int HEIGHT = 128;
   private final TextureAtlas decorationSprites;
   private final MapTextureManager mapTextureManager;

   public MapRenderer(AtlasManager atlasManager, MapTextureManager mapTextureManager) {
      this.decorationSprites = atlasManager.getAtlasOrThrow(AtlasIds.MAP_DECORATIONS);
      this.mapTextureManager = mapTextureManager;
   }

   public void render(MapRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, boolean active, int packedLight) {
      nodeCollector.submitCustomGeometry(poseStack, RenderTypes.text(renderState.texture), (pose, vertexConsumer) -> {
         vertexConsumer.addVertex(pose, 0.0F, 128.0F, -0.01F).setColor(-1).setUv(0.0F, 1.0F).setLight(packedLight);
         vertexConsumer.addVertex(pose, 128.0F, 128.0F, -0.01F).setColor(-1).setUv(1.0F, 1.0F).setLight(packedLight);
         vertexConsumer.addVertex(pose, 128.0F, 0.0F, -0.01F).setColor(-1).setUv(1.0F, 0.0F).setLight(packedLight);
         vertexConsumer.addVertex(pose, 0.0F, 0.0F, -0.01F).setColor(-1).setUv(0.0F, 0.0F).setLight(packedLight);
      });
      int i = 0;

      for (MapRenderState.MapDecorationRenderState mapDecorationRenderState : renderState.decorations) {
         if (!active || mapDecorationRenderState.renderOnFrame) {
            poseStack.pushPose();
            poseStack.translate(mapDecorationRenderState.x / 2.0F + 64.0F, mapDecorationRenderState.y / 2.0F + 64.0F, -0.02F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(mapDecorationRenderState.rot * 360 / 16.0F));
            poseStack.scale(4.0F, 4.0F, 3.0F);
            poseStack.translate(-0.125F, 0.125F, 0.0F);
            TextureAtlasSprite textureAtlasSprite = mapDecorationRenderState.atlasSprite;
            if (textureAtlasSprite != null) {
               float f = i * -0.001F;
               nodeCollector.submitCustomGeometry(
                  poseStack,
                  RenderTypes.text(textureAtlasSprite.atlasLocation()),
                  (pose, vertexConsumer) -> {
                     vertexConsumer.addVertex(pose, -1.0F, 1.0F, f)
                        .setColor(-1)
                        .setUv(textureAtlasSprite.getU0(), textureAtlasSprite.getV0())
                        .setLight(packedLight);
                     vertexConsumer.addVertex(pose, 1.0F, 1.0F, f)
                        .setColor(-1)
                        .setUv(textureAtlasSprite.getU1(), textureAtlasSprite.getV0())
                        .setLight(packedLight);
                     vertexConsumer.addVertex(pose, 1.0F, -1.0F, f)
                        .setColor(-1)
                        .setUv(textureAtlasSprite.getU1(), textureAtlasSprite.getV1())
                        .setLight(packedLight);
                     vertexConsumer.addVertex(pose, -1.0F, -1.0F, f)
                        .setColor(-1)
                        .setUv(textureAtlasSprite.getU0(), textureAtlasSprite.getV1())
                        .setLight(packedLight);
                  }
               );
               poseStack.popPose();
            }

            if (mapDecorationRenderState.name != null) {
               Font font = Minecraft.getInstance().font;
               float g = font.width(mapDecorationRenderState.name);
               float h = Mth.clamp(25.0F / g, 0.0F, 6.0F / 9.0F);
               poseStack.pushPose();
               poseStack.translate(mapDecorationRenderState.x / 2.0F + 64.0F - g * h / 2.0F, mapDecorationRenderState.y / 2.0F + 64.0F + 4.0F, -0.025F);
               poseStack.scale(h, h, -1.0F);
               poseStack.translate(0.0F, 0.0F, 0.1F);
               nodeCollector.order(1)
                  .submitText(
                     poseStack,
                     0.0F,
                     0.0F,
                     mapDecorationRenderState.name.getVisualOrderText(),
                     false,
                     Font.DisplayMode.NORMAL,
                     packedLight,
                     -1,
                     Integer.MIN_VALUE,
                     0
                  );
               poseStack.popPose();
            }

            i++;
         }
      }
   }

   public void extractRenderState(MapId id, MapItemSavedData savedData, MapRenderState renderState) {
      renderState.texture = this.mapTextureManager.prepareMapTexture(id, savedData);
      renderState.decorations.clear();

      for (MapDecoration mapDecoration : savedData.getDecorations()) {
         renderState.decorations.add(this.extractDecorationRenderState(mapDecoration));
      }
   }

   private MapRenderState.MapDecorationRenderState extractDecorationRenderState(MapDecoration decoration) {
      MapRenderState.MapDecorationRenderState mapDecorationRenderState = new MapRenderState.MapDecorationRenderState();
      mapDecorationRenderState.atlasSprite = this.decorationSprites.getSprite(decoration.getSpriteLocation());
      mapDecorationRenderState.x = decoration.x();
      mapDecorationRenderState.y = decoration.y();
      mapDecorationRenderState.rot = decoration.rot();
      mapDecorationRenderState.name = decoration.name().orElse(null);
      mapDecorationRenderState.renderOnFrame = decoration.renderOnFrame();
      return mapDecorationRenderState;
   }
}
