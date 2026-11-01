package net.minecraft.util;

import net.minecraft.core.Direction;

public class SegmentedAnglePrecision {
   private final int mask;
   private final int precision;
   private final float degreeToAngle;
   private final float angleToDegree;

   public SegmentedAnglePrecision(int p_265275_) {
      if (p_265275_ < 2) {
         throw new IllegalArgumentException("Precision cannot be less than 2 bits");
      } else if (p_265275_ > 30) {
         throw new IllegalArgumentException("Precision cannot be greater than 30 bits");
      } else {
         int $$1 = 1 << p_265275_;
         this.mask = $$1 - 1;
         this.precision = p_265275_;
         this.degreeToAngle = $$1 / 360.0F;
         this.angleToDegree = 360.0F / $$1;
      }
   }

   public boolean isSameAxis(int p_265505_, int p_265708_) {
      int $$2 = this.getMask() >> 1;
      return (p_265505_ & $$2) == (p_265708_ & $$2);
   }

   public int fromDirection(Direction p_265731_) {
      if (p_265731_.getAxis().isVertical()) {
         return 0;
      } else {
         int $$1 = p_265731_.get2DDataValue();
         return $$1 << this.precision - 2;
      }
   }

   public int fromDegreesWithTurns(float p_265346_) {
      return Math.round(p_265346_ * this.degreeToAngle);
   }

   public int fromDegrees(float p_265688_) {
      return this.normalize(this.fromDegreesWithTurns(p_265688_));
   }

   public float toDegreesWithTurns(int p_265278_) {
      return p_265278_ * this.angleToDegree;
   }

   public float toDegrees(int p_265623_) {
      float $$1 = this.toDegreesWithTurns(this.normalize(p_265623_));
      return $$1 >= 180.0F ? $$1 - 360.0F : $$1;
   }

   public int normalize(int p_265542_) {
      return p_265542_ & this.mask;
   }

   public int getMask() {
      return this.mask;
   }
}
