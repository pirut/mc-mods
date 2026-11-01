package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractSkeletonRenderer<T extends AbstractSkeleton, S extends SkeletonRenderState> extends HumanoidMobRenderer<T, S, SkeletonModel<S>> {
   public AbstractSkeletonRenderer(EntityRendererProvider.Context p_365063_, ModelLayerLocation p_364353_, ArmorModelSet<ModelLayerLocation> p_433992_) {
      this(p_365063_, p_433992_, new SkeletonModel<>(p_365063_.bakeLayer(p_364353_)));
   }

   public AbstractSkeletonRenderer(EntityRendererProvider.Context p_360948_, ArmorModelSet<ModelLayerLocation> p_433462_, SkeletonModel<S> p_478827_) {
      super(p_360948_, p_478827_, 0.5F);
      this.addLayer(
         new HumanoidArmorLayer<>(this, ArmorModelSet.bake(p_433462_, p_360948_.getModelSet(), SkeletonModel::new), p_360948_.getEquipmentRenderer())
      );
   }

   public void extractRenderState(T p_478693_, S p_364836_, float p_362389_) {
      super.extractRenderState(p_478693_, p_364836_, p_362389_);
      p_364836_.isAggressive = p_478693_.isAggressive();
      p_364836_.isShaking = p_478693_.isShaking();
      p_364836_.isHoldingBow = p_478693_.getMainHandItem().is(Items.BOW);
   }

   protected boolean isShaking(S p_364410_) {
      return p_364410_.isShaking;
   }

   protected HumanoidModel.ArmPose getArmPose(T p_477991_, HumanoidArm p_389423_) {
      return p_477991_.getMainArm() == p_389423_ && p_477991_.isAggressive() && p_477991_.getMainHandItem().is(Items.BOW)
         ? HumanoidModel.ArmPose.BOW_AND_ARROW
         : super.getArmPose(p_477991_, p_389423_);
   }
}
