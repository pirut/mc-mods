package net.minecraft.world.level.pathfinder;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;

public class AmphibiousNodeEvaluator extends WalkNodeEvaluator {
   private final boolean prefersShallowSwimming;
   private float oldWalkableCost;
   private float oldWaterBorderCost;

   public AmphibiousNodeEvaluator(boolean p_164659_) {
      this.prefersShallowSwimming = p_164659_;
   }

   @Override
   public void prepare(PathNavigationRegion p_164671_, Mob p_164672_) {
      super.prepare(p_164671_, p_164672_);
      p_164672_.setPathfindingMalus(PathType.WATER, 0.0F);
      this.oldWalkableCost = p_164672_.getPathfindingMalus(PathType.WALKABLE);
      p_164672_.setPathfindingMalus(PathType.WALKABLE, 6.0F);
      this.oldWaterBorderCost = p_164672_.getPathfindingMalus(PathType.WATER_BORDER);
      p_164672_.setPathfindingMalus(PathType.WATER_BORDER, 4.0F);
   }

   @Override
   public void done() {
      this.mob.setPathfindingMalus(PathType.WALKABLE, this.oldWalkableCost);
      this.mob.setPathfindingMalus(PathType.WATER_BORDER, this.oldWaterBorderCost);
      super.done();
   }

   @Override
   public Node getStart() {
      return !this.mob.isInWater()
         ? super.getStart()
         : this.getStartNode(
            new BlockPos(Mth.floor(this.mob.getBoundingBox().minX), Mth.floor(this.mob.getBoundingBox().minY + 0.5), Mth.floor(this.mob.getBoundingBox().minZ))
         );
   }

   @Override
   public Target getTarget(double p_326847_, double p_326890_, double p_326832_) {
      return this.getTargetNodeAt(p_326847_, p_326890_ + 0.5, p_326832_);
   }

   @Override
   public int getNeighbors(Node[] p_164676_, Node p_164677_) {
      int $$2 = super.getNeighbors(p_164676_, p_164677_);
      PathType $$3 = this.getCachedPathType(p_164677_.x, p_164677_.y + 1, p_164677_.z);
      PathType $$4 = this.getCachedPathType(p_164677_.x, p_164677_.y, p_164677_.z);
      int $$5;
      if (this.mob.getPathfindingMalus($$3) >= 0.0F && $$4 != PathType.STICKY_HONEY) {
         $$5 = Mth.floor(Math.max(1.0F, this.mob.maxUpStep()));
      } else {
         $$5 = 0;
      }

      double $$7 = this.getFloorLevel(new BlockPos(p_164677_.x, p_164677_.y, p_164677_.z));
      Node $$8 = this.findAcceptedNode(p_164677_.x, p_164677_.y + 1, p_164677_.z, Math.max(0, $$5 - 1), $$7, Direction.UP, $$4);
      Node $$9 = this.findAcceptedNode(p_164677_.x, p_164677_.y - 1, p_164677_.z, $$5, $$7, Direction.DOWN, $$4);
      if (this.isVerticalNeighborValid($$8, p_164677_)) {
         p_164676_[$$2++] = $$8;
      }

      if (this.isVerticalNeighborValid($$9, p_164677_) && $$4 != PathType.TRAPDOOR) {
         p_164676_[$$2++] = $$9;
      }

      for (int $$10 = 0; $$10 < $$2; $$10++) {
         Node $$11 = p_164676_[$$10];
         if ($$11.type == PathType.WATER && this.prefersShallowSwimming && $$11.y < this.mob.level().getSeaLevel() - 10) {
            $$11.costMalus++;
         }
      }

      return $$2;
   }

   private boolean isVerticalNeighborValid(@Nullable Node p_230611_, Node p_230612_) {
      return this.isNeighborValid(p_230611_, p_230612_) && p_230611_.type == PathType.WATER;
   }

   @Override
   protected boolean isAmphibious() {
      return true;
   }

   @Override
   public PathType getPathType(PathfindingContext p_330489_, int p_164667_, int p_164668_, int p_164669_) {
      PathType $$4 = p_330489_.getPathTypeFromState(p_164667_, p_164668_, p_164669_);
      if ($$4 == PathType.WATER) {
         BlockPos.MutableBlockPos $$5 = new BlockPos.MutableBlockPos();

         for (Direction $$6 : Direction.values()) {
            $$5.set(p_164667_, p_164668_, p_164669_).move($$6);
            PathType $$7 = p_330489_.getPathTypeFromState($$5.getX(), $$5.getY(), $$5.getZ());
            if ($$7 == PathType.BLOCKED) {
               return PathType.WATER_BORDER;
            }
         }

         return PathType.WATER;
      } else {
         return super.getPathType(p_330489_, p_164667_, p_164668_, p_164669_);
      }
   }
}
