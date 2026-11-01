package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public abstract class EnergySwirlLayer<S extends EntityRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
   public EnergySwirlLayer(RenderLayerParent<S, M> renderLayerParent) {
      super(renderLayerParent);
   }

   @Override
   public void submit(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, S renderState, float yRot, float xRot) {
      if (this.isPowered(renderState)) {
         float f = renderState.ageInTicks;
         M entityModel = this.model();
         nodeCollector.order(1)
            .submitModel(
               entityModel,
               renderState,
               poseStack,
               RenderTypes.energySwirl(this.getTextureLocation(), this.xOffset(f) % 1.0F, f * 0.01F % 1.0F),
               packedLight,
               OverlayTexture.NO_OVERLAY,
               -8355712,
               null,
               renderState.outlineColor,
               null
            );
      }
   }

   protected abstract boolean isPowered(S renderState);

   protected abstract float xOffset(float tickCount);

   protected abstract Identifier getTextureLocation();

   protected abstract M model();
}
