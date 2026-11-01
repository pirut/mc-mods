package net.minecraft.world.entity.ai.goal;

import com.google.common.collect.Lists;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class MoveThroughVillageGoal extends Goal {
   protected final PathfinderMob mob;
   private final double speedModifier;
   @Nullable
   private Path path;
   private BlockPos poiPos;
   private final boolean onlyAtNight;
   private final List<BlockPos> visited = Lists.newArrayList();
   private final int distanceToPoi;
   private final BooleanSupplier canDealWithDoors;

   public MoveThroughVillageGoal(PathfinderMob p_25582_, double p_25583_, boolean p_25584_, int p_25585_, BooleanSupplier p_25586_) {
      this.mob = p_25582_;
      this.speedModifier = p_25583_;
      this.onlyAtNight = p_25584_;
      this.distanceToPoi = p_25585_;
      this.canDealWithDoors = p_25586_;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      if (!GoalUtils.hasGroundPathNavigation(p_25582_)) {
         throw new IllegalArgumentException("Unsupported mob for MoveThroughVillageGoal");
      }
   }

   @Override
   public boolean canUse() {
      if (!GoalUtils.hasGroundPathNavigation(this.mob)) {
         return false;
      } else {
         this.updateVisited();
         if (this.onlyAtNight && this.mob.level().isDay()) {
            return false;
         } else {
            ServerLevel $$0 = (ServerLevel)this.mob.level();
            BlockPos $$1 = this.mob.blockPosition();
            if (!$$0.isCloseToVillage($$1, 6)) {
               return false;
            } else {
               Vec3 $$2 = LandRandomPos.getPos(
                  this.mob,
                  15,
                  7,
                  p_217751_ -> {
                     if (!$$0.isVillage(p_217751_)) {
                        return Double.NEGATIVE_INFINITY;
                     } else {
                        Optional<BlockPos> $$3x = $$0.getPoiManager()
                           .find(p_217758_ -> p_217758_.is(PoiTypeTags.VILLAGE), this::hasNotVisited, p_217751_, 10, PoiManager.Occupancy.IS_OCCUPIED);
                        return $$3x.<Double>map(p_217754_ -> -p_217754_.distSqr($$1)).orElse(Double.NEGATIVE_INFINITY);
                     }
                  }
               );
               if ($$2 == null) {
                  return false;
               } else {
                  Optional<BlockPos> $$3 = $$0.getPoiManager()
                     .find(p_217756_ -> p_217756_.is(PoiTypeTags.VILLAGE), this::hasNotVisited, BlockPos.containing($$2), 10, PoiManager.Occupancy.IS_OCCUPIED);
                  if ($$3.isEmpty()) {
                     return false;
                  } else {
                     this.poiPos = $$3.get().immutable();
                     GroundPathNavigation $$4 = (GroundPathNavigation)this.mob.getNavigation();
                     boolean $$5 = $$4.canOpenDoors();
                     $$4.setCanOpenDoors(this.canDealWithDoors.getAsBoolean());
                     this.path = $$4.createPath(this.poiPos, 0);
                     $$4.setCanOpenDoors($$5);
                     if (this.path == null) {
                        Vec3 $$6 = DefaultRandomPos.getPosTowards(this.mob, 10, 7, Vec3.atBottomCenterOf(this.poiPos), (float) (Math.PI / 2));
                        if ($$6 == null) {
                           return false;
                        }

                        $$4.setCanOpenDoors(this.canDealWithDoors.getAsBoolean());
                        this.path = this.mob.getNavigation().createPath($$6.x, $$6.y, $$6.z, 0);
                        $$4.setCanOpenDoors($$5);
                        if (this.path == null) {
                           return false;
                        }
                     }

                     for (int $$7 = 0; $$7 < this.path.getNodeCount(); $$7++) {
                        Node $$8 = this.path.getNode($$7);
                        BlockPos $$9 = new BlockPos($$8.x, $$8.y + 1, $$8.z);
                        if (DoorBlock.isWoodenDoor(this.mob.level(), $$9)) {
                           this.path = this.mob.getNavigation().createPath($$8.x, $$8.y, $$8.z, 0);
                           break;
                        }
                     }

                     return this.path != null;
                  }
               }
            }
         }
      }
   }

   @Override
   public boolean canContinueToUse() {
      return this.mob.getNavigation().isDone() ? false : !this.poiPos.closerToCenterThan(this.mob.position(), this.mob.getBbWidth() + this.distanceToPoi);
   }

   @Override
   public void start() {
      this.mob.getNavigation().moveTo(this.path, this.speedModifier);
   }

   @Override
   public void stop() {
      if (this.mob.getNavigation().isDone() || this.poiPos.closerToCenterThan(this.mob.position(), this.distanceToPoi)) {
         this.visited.add(this.poiPos);
      }
   }

   private boolean hasNotVisited(BlockPos p_25593_) {
      for (BlockPos $$1 : this.visited) {
         if (Objects.equals(p_25593_, $$1)) {
            return false;
         }
      }

      return true;
   }

   private void updateVisited() {
      if (this.visited.size() > 15) {
         this.visited.remove(0);
      }
   }
}
