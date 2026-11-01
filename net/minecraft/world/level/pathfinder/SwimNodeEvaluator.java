package net.minecraft.world.level.pathfinder;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class SwimNodeEvaluator extends NodeEvaluator {
   private final boolean allowBreaching;
   private final Long2ObjectMap<PathType> pathTypesByPosCache = new Long2ObjectOpenHashMap();

   public SwimNodeEvaluator(boolean p_77457_) {
      this.allowBreaching = p_77457_;
   }

   @Override
   public void prepare(PathNavigationRegion p_192959_, Mob p_192960_) {
      super.prepare(p_192959_, p_192960_);
      this.pathTypesByPosCache.clear();
   }

   @Override
   public void done() {
      super.done();
      this.pathTypesByPosCache.clear();
   }

   @Override
   public Node getStart() {
      return this.getNode(Mth.floor(this.mob.getBoundingBox().minX), Mth.floor(this.mob.getBoundingBox().minY + 0.5), Mth.floor(this.mob.getBoundingBox().minZ));
   }

   @Override
   public Target getTarget(double p_326917_, double p_326806_, double p_326896_) {
      return this.getTargetNodeAt(p_326917_, p_326806_, p_326896_);
   }

   @Override
   public int getNeighbors(Node[] p_77483_, Node p_77484_) {
      int $$2 = 0;
      Map<Direction, Node> $$3 = Maps.newEnumMap(Direction.class);

      for (Direction $$4 : Direction.values()) {
         Node $$5 = this.findAcceptedNode(p_77484_.x + $$4.getStepX(), p_77484_.y + $$4.getStepY(), p_77484_.z + $$4.getStepZ());
         $$3.put($$4, $$5);
         if (this.isNodeValid($$5)) {
            p_77483_[$$2++] = $$5;
         }
      }

      for (Direction $$6 : Direction.Plane.HORIZONTAL) {
         Direction $$7 = $$6.getClockWise();
         if (hasMalus($$3.get($$6)) && hasMalus($$3.get($$7))) {
            Node $$8 = this.findAcceptedNode(p_77484_.x + $$6.getStepX() + $$7.getStepX(), p_77484_.y, p_77484_.z + $$6.getStepZ() + $$7.getStepZ());
            if (this.isNodeValid($$8)) {
               p_77483_[$$2++] = $$8;
            }
         }
      }

      return $$2;
   }

   protected boolean isNodeValid(@Nullable Node p_192962_) {
      return p_192962_ != null && !p_192962_.closed;
   }

   private static boolean hasMalus(@Nullable Node p_326813_) {
      return p_326813_ != null && p_326813_.costMalus >= 0.0F;
   }

   @Nullable
   protected Node findAcceptedNode(int p_263032_, int p_263066_, int p_263105_) {
      Node $$3 = null;
      PathType $$4 = this.getCachedBlockType(p_263032_, p_263066_, p_263105_);
      if (this.allowBreaching && $$4 == PathType.BREACH || $$4 == PathType.WATER) {
         float $$5 = this.mob.getPathfindingMalus($$4);
         if ($$5 >= 0.0F) {
            $$3 = this.getNode(p_263032_, p_263066_, p_263105_);
            $$3.type = $$4;
            $$3.costMalus = Math.max($$3.costMalus, $$5);
            if (this.currentContext.level().getFluidState(new BlockPos(p_263032_, p_263066_, p_263105_)).isEmpty()) {
               $$3.costMalus += 8.0F;
            }
         }
      }

      return $$3;
   }

   protected PathType getCachedBlockType(int p_192968_, int p_192969_, int p_192970_) {
      return (PathType)this.pathTypesByPosCache
         .computeIfAbsent(BlockPos.asLong(p_192968_, p_192969_, p_192970_), p_330157_ -> this.getPathType(this.currentContext, p_192968_, p_192969_, p_192970_));
   }

   @Override
   public PathType getPathType(PathfindingContext p_330490_, int p_326812_, int p_326835_, int p_326945_) {
      return this.getPathTypeOfMob(p_330490_, p_326812_, p_326835_, p_326945_, this.mob);
   }

   @Override
   public PathType getPathTypeOfMob(PathfindingContext p_330584_, int p_77473_, int p_77474_, int p_77475_, Mob p_77476_) {
      BlockPos.MutableBlockPos $$5 = new BlockPos.MutableBlockPos();

      for (int $$6 = p_77473_; $$6 < p_77473_ + this.entityWidth; $$6++) {
         for (int $$7 = p_77474_; $$7 < p_77474_ + this.entityHeight; $$7++) {
            for (int $$8 = p_77475_; $$8 < p_77475_ + this.entityDepth; $$8++) {
               BlockState $$9 = p_330584_.getBlockState($$5.set($$6, $$7, $$8));
               FluidState $$10 = $$9.getFluidState();
               if ($$10.isEmpty() && $$9.isPathfindable(PathComputationType.WATER) && $$9.isAir()) {
                  return PathType.BREACH;
               }

               if (!$$10.is(FluidTags.WATER)) {
                  return PathType.BLOCKED;
               }
            }
         }
      }

      BlockState $$11 = p_330584_.getBlockState($$5);
      return $$11.isPathfindable(PathComputationType.WATER) ? PathType.WATER : PathType.BLOCKED;
   }
}
