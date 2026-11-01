package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LivingEntityEmissiveLayer<S extends LivingEntityRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
   private final Function<S, Identifier> textureProvider;
   private final LivingEntityEmissiveLayer.AlphaFunction<S> alphaFunction;
   private final M model;
   private final Function<Identifier, RenderType> bufferProvider;
   private final boolean alwaysVisible;

   public LivingEntityEmissiveLayer(
      RenderLayerParent<S, M> p_379642_,
      Function<S, Identifier> p_380046_,
      LivingEntityEmissiveLayer.AlphaFunction<S> p_380124_,
      M p_435959_,
      Function<Identifier, RenderType> p_433975_,
      boolean p_388482_
   ) {
      super(p_379642_);
      this.textureProvider = p_380046_;
      this.alphaFunction = p_380124_;
      this.model = p_435959_;
      this.bufferProvider = p_433975_;
      this.alwaysVisible = p_388482_;
   }

   public void submit(PoseStack p_435476_, SubmitNodeCollector p_435488_, int p_433546_, S p_434369_, float p_433568_, float p_435527_) {
      if (!p_434369_.isInvisible || this.alwaysVisible) {
         float f = this.alphaFunction.apply(p_434369_, p_434369_.ageInTicks);
         if (!(f <= 1.0E-5F)) {
            int i = ARGB.white(f);
            RenderType rendertype = this.bufferProvider.apply(this.textureProvider.apply(p_434369_));
            p_435488_.order(1)
               .submitModel(
                  this.model,
                  p_434369_,
                  p_435476_,
                  rendertype,
                  p_433546_,
                  LivingEntityRenderer.getOverlayCoords(p_434369_, 0.0F),
                  i,
                  null,
                  p_434369_.outlineColor,
                  null
               );
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public interface AlphaFunction<S extends LivingEntityRenderState> {
      float apply(S var1, float var2);
   }
}
