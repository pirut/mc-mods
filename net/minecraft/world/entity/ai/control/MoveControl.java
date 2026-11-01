package net.minecraft.world.entity.ai.control;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MoveControl implements Control {
   public static final float MIN_SPEED = 5.0E-4F;
   public static final float MIN_SPEED_SQR = 2.5000003E-7F;
   protected static final int MAX_TURN = 90;
   protected final Mob mob;
   protected double wantedX;
   protected double wantedY;
   protected double wantedZ;
   protected double speedModifier;
   protected float strafeForwards;
   protected float strafeRight;
   protected MoveControl.Operation operation = MoveControl.Operation.WAIT;

   public MoveControl(Mob p_24983_) {
      this.mob = p_24983_;
   }

   public boolean hasWanted() {
      return this.operation == MoveControl.Operation.MOVE_TO;
   }

   public double getSpeedModifier() {
      return this.speedModifier;
   }

   public void setWantedPosition(double p_24984_, double p_24985_, double p_24986_, double p_24987_) {
      this.wantedX = p_24984_;
      this.wantedY = p_24985_;
      this.wantedZ = p_24986_;
      this.speedModifier = p_24987_;
      if (this.operation != MoveControl.Operation.JUMPING) {
         this.operation = MoveControl.Operation.MOVE_TO;
      }
   }

   public void strafe(float p_24989_, float p_24990_) {
      this.operation = MoveControl.Operation.STRAFE;
      this.strafeForwards = p_24989_;
      this.strafeRight = p_24990_;
      this.speedModifier = 0.25;
   }

   public void tick() {
      if (this.operation == MoveControl.Operation.STRAFE) {
         float $$0 = (float)this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
         float $$1 = (float)this.speedModifier * $$0;
         float $$2 = this.strafeForwards;
         float $$3 = this.strafeRight;
         float $$4 = Mth.sqrt($$2 * $$2 + $$3 * $$3);
         if ($$4 < 1.0F) {
            $$4 = 1.0F;
         }

         $$4 = $$1 / $$4;
         $$2 *= $$4;
         $$3 *= $$4;
         float $$5 = Mth.sin(this.mob.getYRot() * (float) (Math.PI / 180.0));
         float $$6 = Mth.cos(this.mob.getYRot() * (float) (Math.PI / 180.0));
         float $$7 = $$2 * $$6 - $$3 * $$5;
         float $$8 = $$3 * $$6 + $$2 * $$5;
         if (!this.isWalkable($$7, $$8)) {
            this.strafeForwards = 1.0F;
            this.strafeRight = 0.0F;
         }

         this.mob.setSpeed($$1);
         this.mob.setZza(this.strafeForwards);
         this.mob.setXxa(this.strafeRight);
         this.operation = MoveControl.Operation.WAIT;
      } else if (this.operation == MoveControl.Operation.MOVE_TO) {
         this.operation = MoveControl.Operation.WAIT;
         double $$9 = this.wantedX - this.mob.getX();
         double $$10 = this.wantedZ - this.mob.getZ();
         double $$11 = this.wantedY - this.mob.getY();
         double $$12 = $$9 * $$9 + $$11 * $$11 + $$10 * $$10;
         if ($$12 < 2.5000003E-7F) {
            this.mob.setZza(0.0F);
            return;
         }

         float $$13 = (float)(Mth.atan2($$10, $$9) * 180.0F / (float)Math.PI) - 90.0F;
         this.mob.setYRot(this.rotlerp(this.mob.getYRot(), $$13, 90.0F));
         this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
         BlockPos $$14 = this.mob.blockPosition();
         BlockState $$15 = this.mob.level().getBlockState($$14);
         VoxelShape $$16 = $$15.getCollisionShape(this.mob.level(), $$14);
         if ($$11 > this.mob.maxUpStep() && $$9 * $$9 + $$10 * $$10 < Math.max(1.0F, this.mob.getBbWidth())
            || !$$16.isEmpty() && this.mob.getY() < $$16.max(Direction.Axis.Y) + $$14.getY() && !$$15.is(BlockTags.DOORS) && !$$15.is(BlockTags.FENCES)) {
            this.mob.getJumpControl().jump();
            this.operation = MoveControl.Operation.JUMPING;
         }
      } else if (this.operation == MoveControl.Operation.JUMPING) {
         this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
         if (this.mob.onGround()) {
            this.operation = MoveControl.Operation.WAIT;
         }
      } else {
         this.mob.setZza(0.0F);
      }
   }

   private boolean isWalkable(float p_24997_, float p_24998_) {
      PathNavigation $$2 = this.mob.getNavigation();
      if ($$2 != null) {
         NodeEvaluator $$3 = $$2.getNodeEvaluator();
         if ($$3 != null
            && $$3.getPathType(this.mob, BlockPos.containing(this.mob.getX() + p_24997_, this.mob.getBlockY(), this.mob.getZ() + p_24998_))
               != PathType.WALKABLE) {
            return false;
         }
      }

      return true;
   }

   protected float rotlerp(float p_24992_, float p_24993_, float p_24994_) {
      float $$3 = Mth.wrapDegrees(p_24993_ - p_24992_);
      if ($$3 > p_24994_) {
         $$3 = p_24994_;
      }

      if ($$3 < -p_24994_) {
         $$3 = -p_24994_;
      }

      float $$4 = p_24992_ + $$3;
      if ($$4 < 0.0F) {
         $$4 += 360.0F;
      } else if ($$4 > 360.0F) {
         $$4 -= 360.0F;
      }

      return $$4;
   }

   public double getWantedX() {
      return this.wantedX;
   }

   public double getWantedY() {
      return this.wantedY;
   }

   public double getWantedZ() {
      return this.wantedZ;
   }

   protected static enum Operation {
      WAIT,
      MOVE_TO,
      STRAFE,
      JUMPING;
   }
}
