package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;

@Environment(EnvType.CLIENT)
public class BoatRenderer extends AbstractBoatRenderer {
   private final Model.Simple waterPatchModel;
   private final Identifier texture;
   private final EntityModel<BoatRenderState> model;

   public BoatRenderer(EntityRendererProvider.Context context, ModelLayerLocation modelLayer) {
      super(context);
      this.texture = modelLayer.model().withPath(string -> "textures/entity/" + string + ".png");
      this.waterPatchModel = new Model.Simple(context.bakeLayer(ModelLayers.BOAT_WATER_PATCH), identifier -> RenderTypes.waterMask());
      this.model = new BoatModel(context.bakeLayer(modelLayer));
   }

   @Override
   protected EntityModel<BoatRenderState> model() {
      return this.model;
   }

   @Override
   protected RenderType renderType() {
      return this.model.renderType(this.texture);
   }

   @Override
   protected void submitTypeAdditions(BoatRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, int lightCoords) {
      if (!renderState.isUnderWater) {
         nodeCollector.submitModel(
            this.waterPatchModel,
            Unit.INSTANCE,
            poseStack,
            this.waterPatchModel.renderType(this.texture),
            lightCoords,
            OverlayTexture.NO_OVERLAY,
            renderState.outlineColor,
            null
         );
      }
   }
}
