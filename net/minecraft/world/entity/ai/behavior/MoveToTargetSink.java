package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class MoveToTargetSink extends Behavior<Mob> {
   private static final int MAX_COOLDOWN_BEFORE_RETRYING = 40;
   private int remainingCooldown;
   @Nullable
   private Path path;
   @Nullable
   private BlockPos lastTargetPos;
   private float speedModifier;

   public MoveToTargetSink() {
      this(150, 250);
   }

   public MoveToTargetSink(int p_23573_, int p_23574_) {
      super(
         ImmutableMap.of(
            MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
            MemoryStatus.REGISTERED,
            MemoryModuleType.PATH,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.WALK_TARGET,
            MemoryStatus.VALUE_PRESENT
         ),
         p_23573_,
         p_23574_
      );
   }

   protected boolean checkExtraStartConditions(ServerLevel p_23583_, Mob p_23584_) {
      if (this.remainingCooldown > 0) {
         this.remainingCooldown--;
         return false;
      } else {
         Brain<?> $$2 = p_23584_.getBrain();
         WalkTarget $$3 = $$2.getMemory(MemoryModuleType.WALK_TARGET).get();
         boolean $$4 = this.reachedTarget(p_23584_, $$3);
         if (!$$4 && this.tryComputePath(p_23584_, $$3, p_23583_.getGameTime())) {
            this.lastTargetPos = $$3.getTarget().currentBlockPosition();
            return true;
         } else {
            $$2.eraseMemory(MemoryModuleType.WALK_TARGET);
            if ($$4) {
               $$2.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
            }

            return false;
         }
      }
   }

   protected boolean canStillUse(ServerLevel p_23586_, Mob p_23587_, long p_23588_) {
      if (this.path != null && this.lastTargetPos != null) {
         Optional<WalkTarget> $$3 = p_23587_.getBrain().getMemory(MemoryModuleType.WALK_TARGET);
         boolean $$4 = $$3.map(MoveToTargetSink::isWalkTargetSpectator).orElse(false);
         PathNavigation $$5 = p_23587_.getNavigation();
         return !$$5.isDone() && $$3.isPresent() && !this.reachedTarget(p_23587_, $$3.get()) && !$$4;
      } else {
         return false;
      }
   }

   protected void stop(ServerLevel p_23601_, Mob p_23602_, long p_23603_) {
      if (p_23602_.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)
         && !this.reachedTarget(p_23602_, p_23602_.getBrain().getMemory(MemoryModuleType.WALK_TARGET).get())
         && p_23602_.getNavigation().isStuck()) {
         this.remainingCooldown = p_23601_.getRandom().nextInt(40);
      }

      p_23602_.getNavigation().stop();
      p_23602_.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      p_23602_.getBrain().eraseMemory(MemoryModuleType.PATH);
      this.path = null;
   }

   protected void start(ServerLevel p_23609_, Mob p_23610_, long p_23611_) {
      p_23610_.getBrain().setMemory(MemoryModuleType.PATH, this.path);
      p_23610_.getNavigation().moveTo(this.path, (double)this.speedModifier);
   }

   protected void tick(ServerLevel p_23617_, Mob p_23618_, long p_23619_) {
      Path $$3 = p_23618_.getNavigation().getPath();
      Brain<?> $$4 = p_23618_.getBrain();
      if (this.path != $$3) {
         this.path = $$3;
         $$4.setMemory(MemoryModuleType.PATH, $$3);
      }

      if ($$3 != null && this.lastTargetPos != null) {
         WalkTarget $$5 = $$4.getMemory(MemoryModuleType.WALK_TARGET).get();
         if ($$5.getTarget().currentBlockPosition().distSqr(this.lastTargetPos) > 4.0 && this.tryComputePath(p_23618_, $$5, p_23617_.getGameTime())) {
            this.lastTargetPos = $$5.getTarget().currentBlockPosition();
            this.start(p_23617_, p_23618_, p_23619_);
         }
      }
   }

   private boolean tryComputePath(Mob p_23593_, WalkTarget p_23594_, long p_23595_) {
      BlockPos $$3 = p_23594_.getTarget().currentBlockPosition();
      this.path = p_23593_.getNavigation().createPath($$3, 0);
      this.speedModifier = p_23594_.getSpeedModifier();
      Brain<?> $$4 = p_23593_.getBrain();
      if (this.reachedTarget(p_23593_, p_23594_)) {
         $$4.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
      } else {
         boolean $$5 = this.path != null && this.path.canReach();
         if ($$5) {
            $$4.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
         } else if (!$$4.hasMemoryValue(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)) {
            $$4.setMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, p_23595_);
         }

         if (this.path != null) {
            return true;
         }

         Vec3 $$6 = DefaultRandomPos.getPosTowards((PathfinderMob)p_23593_, 10, 7, Vec3.atBottomCenterOf($$3), (float) (Math.PI / 2));
         if ($$6 != null) {
            this.path = p_23593_.getNavigation().createPath($$6.x, $$6.y, $$6.z, 0);
            return this.path != null;
         }
      }

      return false;
   }

   private boolean reachedTarget(Mob p_23590_, WalkTarget p_23591_) {
      return p_23591_.getTarget().currentBlockPosition().distManhattan(p_23590_.blockPosition()) <= p_23591_.getCloseEnoughDist();
   }

   private static boolean isWalkTargetSpectator(WalkTarget p_277420_) {
      return p_277420_.getTarget() instanceof EntityTracker $$2 ? $$2.getEntity().isSpectator() : false;
   }
}
