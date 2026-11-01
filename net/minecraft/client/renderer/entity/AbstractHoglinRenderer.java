package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.monster.hoglin.HoglinModel;
import net.minecraft.client.renderer.entity.state.HoglinRenderState;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.hoglin.HoglinBase;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractHoglinRenderer<T extends Mob & HoglinBase> extends AgeableMobRenderer<T, HoglinRenderState, HoglinModel> {
   public AbstractHoglinRenderer(EntityRendererProvider.Context p_363437_, ModelLayerLocation p_362731_, ModelLayerLocation p_361472_, float p_363226_) {
      super(p_363437_, new HoglinModel(p_363437_.bakeLayer(p_362731_)), new HoglinModel(p_363437_.bakeLayer(p_361472_)), p_363226_);
   }

   public HoglinRenderState createRenderState() {
      return new HoglinRenderState();
   }

   public void extractRenderState(T p_361728_, HoglinRenderState p_360522_, float p_363399_) {
      super.extractRenderState(p_361728_, p_360522_, p_363399_);
      p_360522_.attackAnimationRemainingTicks = p_361728_.getAttackAnimationRemainingTicks();
   }
}
