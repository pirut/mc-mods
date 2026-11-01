package net.minecraft.world.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

public class GroundPathNavigation extends PathNavigation {
   private boolean avoidSun;

   public GroundPathNavigation(Mob p_26448_, Level p_26449_) {
      super(p_26448_, p_26449_);
   }

   @Override
   protected PathFinder createPathFinder(int p_26453_) {
      this.nodeEvaluator = new WalkNodeEvaluator();
      this.nodeEvaluator.setCanPassDoors(true);
      return new PathFinder(this.nodeEvaluator, p_26453_);
   }

   @Override
   protected boolean canUpdatePath() {
      return this.mob.onGround() || this.mob.isInLiquid() || this.mob.isPassenger();
   }

   @Override
   protected Vec3 getTempMobPos() {
      return new Vec3(this.mob.getX(), this.getSurfaceY(), this.mob.getZ());
   }

   @Override
   public Path createPath(BlockPos p_26475_, int p_26476_) {
      LevelChunk $$2 = this.level
         .getChunkSource()
         .getChunkNow(SectionPos.blockToSectionCoord(p_26475_.getX()), SectionPos.blockToSectionCoord(p_26475_.getZ()));
      if ($$2 == null) {
         return null;
      } else {
         if ($$2.getBlockState(p_26475_).isAir()) {
            BlockPos $$3 = p_26475_.below();

            while ($$3.getY() > this.level.getMinBuildHeight() && $$2.getBlockState($$3).isAir()) {
               $$3 = $$3.below();
            }

            if ($$3.getY() > this.level.getMinBuildHeight()) {
               return super.createPath($$3.above(), p_26476_);
            }

            while ($$3.getY() < this.level.getMaxBuildHeight() && $$2.getBlockState($$3).isAir()) {
               $$3 = $$3.above();
            }

            p_26475_ = $$3;
         }

         if (!$$2.getBlockState(p_26475_).isSolid()) {
            return super.createPath(p_26475_, p_26476_);
         } else {
            BlockPos $$4 = p_26475_.above();

            while ($$4.getY() < this.level.getMaxBuildHeight() && $$2.getBlockState($$4).isSolid()) {
               $$4 = $$4.above();
            }

            return super.createPath($$4, p_26476_);
         }
      }
   }

   @Override
   public Path createPath(Entity p_26465_, int p_26466_) {
      return this.createPath(p_26465_.blockPosition(), p_26466_);
   }

   private int getSurfaceY() {
      if (this.mob.isInWater() && this.canFloat()) {
         int $$0 = this.mob.getBlockY();
         BlockState $$1 = this.level.getBlockState(BlockPos.containing(this.mob.getX(), $$0, this.mob.getZ()));
         int $$2 = 0;

         while ($$1.is(Blocks.WATER)) {
            $$1 = this.level.getBlockState(BlockPos.containing(this.mob.getX(), ++$$0, this.mob.getZ()));
            if (++$$2 > 16) {
               return this.mob.getBlockY();
            }
         }

         return $$0;
      } else {
         return Mth.floor(this.mob.getY() + 0.5);
      }
   }

   @Override
   protected void trimPath() {
      super.trimPath();
      if (this.avoidSun) {
         if (this.level.canSeeSky(BlockPos.containing(this.mob.getX(), this.mob.getY() + 0.5, this.mob.getZ()))) {
            return;
         }

         for (int $$0 = 0; $$0 < this.path.getNodeCount(); $$0++) {
            Node $$1 = this.path.getNode($$0);
            if (this.level.canSeeSky(new BlockPos($$1.x, $$1.y, $$1.z))) {
               this.path.truncateNodes($$0);
               return;
            }
         }
      }
   }

   protected boolean hasValidPathType(PathType p_326937_) {
      if (p_326937_ == PathType.WATER) {
         return false;
      } else {
         return p_326937_ == PathType.LAVA ? false : p_326937_ != PathType.OPEN;
      }
   }

   public void setCanOpenDoors(boolean p_26478_) {
      this.nodeEvaluator.setCanOpenDoors(p_26478_);
   }

   public boolean canPassDoors() {
      return this.nodeEvaluator.canPassDoors();
   }

   public void setCanPassDoors(boolean p_148215_) {
      this.nodeEvaluator.setCanPassDoors(p_148215_);
   }

   public boolean canOpenDoors() {
      return this.nodeEvaluator.canPassDoors();
   }

   public void setAvoidSun(boolean p_26491_) {
      this.avoidSun = p_26491_;
   }

   public void setCanWalkOverFences(boolean p_255877_) {
      this.nodeEvaluator.setCanWalkOverFences(p_255877_);
   }
}
