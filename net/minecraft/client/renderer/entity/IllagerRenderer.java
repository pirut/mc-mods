package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.monster.illager.IllagerModel;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.IllagerRenderState;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.item.CrossbowItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class IllagerRenderer<T extends AbstractIllager, S extends IllagerRenderState> extends MobRenderer<T, S, IllagerModel<S>> {
   protected IllagerRenderer(EntityRendererProvider.Context p_174182_, IllagerModel<S> p_479056_, float p_174184_) {
      super(p_174182_, p_479056_, p_174184_);
      this.addLayer(new CustomHeadLayer<>(this, p_174182_.getModelSet(), p_174182_.getPlayerSkinRenderCache()));
   }

   public void extractRenderState(T p_479421_, S p_364586_, float p_360560_) {
      super.extractRenderState(p_479421_, p_364586_, p_360560_);
      ArmedEntityRenderState.extractArmedEntityRenderState(p_479421_, p_364586_, this.itemModelResolver, p_360560_);
      p_364586_.isRiding = p_479421_.isPassenger();
      p_364586_.mainArm = p_479421_.getMainArm();
      p_364586_.armPose = p_479421_.getArmPose();
      p_364586_.maxCrossbowChargeDuration = p_364586_.armPose == AbstractIllager.IllagerArmPose.CROSSBOW_CHARGE
         ? CrossbowItem.getChargeDuration(p_479421_.getUseItem(), p_479421_)
         : 0;
      p_364586_.ticksUsingItem = p_479421_.getTicksUsingItem(p_360560_);
      p_364586_.attackAnim = p_479421_.getAttackAnim(p_360560_);
      p_364586_.isAggressive = p_479421_.isAggressive();
   }
}
