package net.minecraft.world.entity.monster.creaking;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTargetSometimes;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.behavior.Swim;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;

public class CreakingAi {
   protected static final ImmutableList<? extends SensorType<? extends Sensor<? super Creaking>>> SENSOR_TYPES = ImmutableList.of(
      SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS
   );
   protected static final ImmutableList<? extends MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(
      MemoryModuleType.NEAREST_LIVING_ENTITIES,
      MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
      MemoryModuleType.NEAREST_VISIBLE_PLAYER,
      MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER,
      MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYERS,
      MemoryModuleType.LOOK_TARGET,
      MemoryModuleType.WALK_TARGET,
      MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
      MemoryModuleType.PATH,
      MemoryModuleType.ATTACK_TARGET,
      MemoryModuleType.ATTACK_COOLING_DOWN
   );

   static void initCoreActivity(Brain<Creaking> p_379953_) {
      p_379953_.addActivity(Activity.CORE, 0, ImmutableList.of(new Swim<Creaking>(0.8F) {
         protected boolean checkExtraStartConditions(ServerLevel p_380301_, Creaking p_380034_) {
            return p_380034_.canMove() && super.checkExtraStartConditions(p_380301_, (LivingEntity)p_380034_);
         }
      }, new LookAtTargetSink(45, 90), new MoveToTargetSink()));
   }

   static void initIdleActivity(Brain<Creaking> p_379511_) {
      p_379511_.addActivity(
         Activity.IDLE,
         10,
         ImmutableList.of(
            StartAttacking.create(
               (p_380834_, p_380835_) -> p_380835_.isActive(),
               (p_379832_, p_379817_) -> p_379817_.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER)
            ),
            SetEntityLookTargetSometimes.create(8.0F, UniformInt.of(30, 60)),
            new RunOne(
               ImmutableList.of(
                  Pair.of(RandomStroll.stroll(0.3F), 2), Pair.of(SetWalkTargetFromLookTarget.create(0.3F, 3), 2), Pair.of(new DoNothing(30, 60), 1)
               )
            )
         )
      );
   }

   static void initFightActivity(Creaking p_397545_, Brain<Creaking> p_379731_) {
      p_379731_.addActivityWithConditions(
         Activity.FIGHT,
         10,
         ImmutableList.of(
            SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1.0F),
            MeleeAttack.create(Creaking::canMove, 40),
            StopAttackingIfTargetInvalid.create((p_396838_, p_396839_) -> !isAttackTargetStillReachable(p_397545_, p_396839_))
         ),
         ImmutableSet.of(Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT))
      );
   }

   private static boolean isAttackTargetStillReachable(Creaking p_397141_, LivingEntity p_397218_) {
      Optional<List<Player>> optional = p_397141_.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYERS);
      return optional.<Boolean>map(p_396836_ -> p_397218_ instanceof Player player && p_396836_.contains(player)).orElse(false);
   }

   public static Brain.Provider<Creaking> brainProvider() {
      return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
   }

   public static Brain<Creaking> makeBrain(Creaking p_397026_, Brain<Creaking> p_380229_) {
      initCoreActivity(p_380229_);
      initIdleActivity(p_380229_);
      initFightActivity(p_397026_, p_380229_);
      p_380229_.setCoreActivities(ImmutableSet.of(Activity.CORE));
      p_380229_.setDefaultActivity(Activity.IDLE);
      p_380229_.useDefaultActivity();
      return p_380229_;
   }

   public static void updateActivity(Creaking p_380185_) {
      if (!p_380185_.canMove()) {
         p_380185_.getBrain().useDefaultActivity();
      } else {
         p_380185_.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.FIGHT, Activity.IDLE));
      }
   }
}
