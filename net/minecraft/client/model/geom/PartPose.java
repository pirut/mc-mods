package net.minecraft.client.model.geom;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record PartPose(float x, float y, float z, float xRot, float yRot, float zRot, float xScale, float yScale, float zScale) {
   public static final PartPose ZERO = offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

   public static PartPose offset(float p_171420_, float p_171421_, float p_171422_) {
      return offsetAndRotation(p_171420_, p_171421_, p_171422_, 0.0F, 0.0F, 0.0F);
   }

   public static PartPose rotation(float p_171431_, float p_171432_, float p_171433_) {
      return offsetAndRotation(0.0F, 0.0F, 0.0F, p_171431_, p_171432_, p_171433_);
   }

   public static PartPose offsetAndRotation(float p_171424_, float p_171425_, float p_171426_, float p_171427_, float p_171428_, float p_171429_) {
      return new PartPose(p_171424_, p_171425_, p_171426_, p_171427_, p_171428_, p_171429_, 1.0F, 1.0F, 1.0F);
   }

   public PartPose translated(float p_365091_, float p_362471_, float p_362753_) {
      return new PartPose(this.x + p_365091_, this.y + p_362471_, this.z + p_362753_, this.xRot, this.yRot, this.zRot, this.xScale, this.yScale, this.zScale);
   }

   public PartPose withScale(float p_361213_) {
      return new PartPose(this.x, this.y, this.z, this.xRot, this.yRot, this.zRot, p_361213_, p_361213_, p_361213_);
   }

   public PartPose scaled(float p_363407_) {
      return p_363407_ == 1.0F ? this : this.scaled(p_363407_, p_363407_, p_363407_);
   }

   public PartPose scaled(float p_362706_, float p_362631_, float p_361375_) {
      return new PartPose(
         this.x * p_362706_,
         this.y * p_362631_,
         this.z * p_361375_,
         this.xRot,
         this.yRot,
         this.zRot,
         this.xScale * p_362706_,
         this.yScale * p_362631_,
         this.zScale * p_361375_
      );
   }
}
