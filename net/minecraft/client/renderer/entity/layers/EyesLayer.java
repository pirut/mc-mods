package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

@Environment(EnvType.CLIENT)
public abstract class EyesLayer<S extends EntityRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
   public EyesLayer(RenderLayerParent<S, M> renderLayerParent) {
      super(renderLayerParent);
   }

   @Override
   public void submit(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, S renderState, float yRot, float xRot) {
      nodeCollector.order(1)
         .submitModel(
            this.getParentModel(), renderState, poseStack, this.renderType(), packedLight, OverlayTexture.NO_OVERLAY, -1, null, renderState.outlineColor, null
         );
   }

   public abstract RenderType renderType();
}
