package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.player.Player;

public class FollowTemptation extends Behavior<PathfinderMob> {
   public static final int TEMPTATION_COOLDOWN = 100;
   public static final double DEFAULT_CLOSE_ENOUGH_DIST = 2.5;
   public static final double BACKED_UP_CLOSE_ENOUGH_DIST = 3.5;
   private final Function<LivingEntity, Float> speedModifier;
   private final Function<LivingEntity, Double> closeEnoughDistance;

   public FollowTemptation(Function<LivingEntity, Float> p_147486_) {
      this(p_147486_, p_288784_ -> 2.5);
   }

   public FollowTemptation(Function<LivingEntity, Float> p_288997_, Function<LivingEntity, Double> p_288972_) {
      super(Util.make(() -> {
         Builder<MemoryModuleType<?>, MemoryStatus> $$0 = ImmutableMap.builder();
         $$0.put(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED);
         $$0.put(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED);
         $$0.put(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT);
         $$0.put(MemoryModuleType.IS_TEMPTED, MemoryStatus.REGISTERED);
         $$0.put(MemoryModuleType.TEMPTING_PLAYER, MemoryStatus.VALUE_PRESENT);
         $$0.put(MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT);
         $$0.put(MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT);
         return $$0.build();
      }));
      this.speedModifier = p_288997_;
      this.closeEnoughDistance = p_288972_;
   }

   protected float getSpeedModifier(PathfinderMob p_147498_) {
      return this.speedModifier.apply(p_147498_);
   }

   private Optional<Player> getTemptingPlayer(PathfinderMob p_147509_) {
      return p_147509_.getBrain().getMemory(MemoryModuleType.TEMPTING_PLAYER);
   }

   @Override
   protected boolean timedOut(long p_147488_) {
      return false;
   }

   protected boolean canStillUse(ServerLevel p_147494_, PathfinderMob p_147495_, long p_147496_) {
      return this.getTemptingPlayer(p_147495_).isPresent()
         && !p_147495_.getBrain().hasMemoryValue(MemoryModuleType.BREED_TARGET)
         && !p_147495_.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING);
   }

   protected void start(ServerLevel p_147505_, PathfinderMob p_147506_, long p_147507_) {
      p_147506_.getBrain().setMemory(MemoryModuleType.IS_TEMPTED, true);
   }

   protected void stop(ServerLevel p_147515_, PathfinderMob p_147516_, long p_147517_) {
      Brain<?> $$3 = p_147516_.getBrain();
      $$3.setMemory(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS, 100);
      $$3.setMemory(MemoryModuleType.IS_TEMPTED, false);
      $$3.eraseMemory(MemoryModuleType.WALK_TARGET);
      $$3.eraseMemory(MemoryModuleType.LOOK_TARGET);
   }

   protected void tick(ServerLevel p_147523_, PathfinderMob p_147524_, long p_147525_) {
      Player $$3 = this.getTemptingPlayer(p_147524_).get();
      Brain<?> $$4 = p_147524_.getBrain();
      $$4.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker($$3, true));
      double $$5 = this.closeEnoughDistance.apply(p_147524_);
      if (p_147524_.distanceToSqr($$3) < Mth.square($$5)) {
         $$4.eraseMemory(MemoryModuleType.WALK_TARGET);
      } else {
         $$4.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new EntityTracker($$3, false), this.getSpeedModifier(p_147524_), 2));
      }
   }
}
