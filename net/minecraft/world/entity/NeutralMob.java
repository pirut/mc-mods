package net.minecraft.world.entity;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public interface NeutralMob {
   String TAG_ANGER_END_TIME = "anger_end_time";
   String TAG_ANGRY_AT = "angry_at";
   long NO_ANGER_END_TIME = -1L;

   long getPersistentAngerEndTime();

   default void setTimeToRemainAngry(long timeToRemainAngry) {
      this.setPersistentAngerEndTime(this.level().getGameTime() + timeToRemainAngry);
   }

   void setPersistentAngerEndTime(long persistentAngerEndTime);

   @Nullable
   EntityReference<LivingEntity> getPersistentAngerTarget();

   void setPersistentAngerTarget(@Nullable EntityReference<LivingEntity> persistentAngerTarget);

   void startPersistentAngerTimer();

   Level level();

   default void addPersistentAngerSaveData(ValueOutput output) {
      output.putLong("anger_end_time", this.getPersistentAngerEndTime());
      output.storeNullable("angry_at", EntityReference.codec(), this.getPersistentAngerTarget());
   }

   default void readPersistentAngerSaveData(Level level, ValueInput input) {
      Optional<Long> optional = input.getLong("anger_end_time");
      if (optional.isPresent()) {
         this.setPersistentAngerEndTime(optional.get());
      } else {
         Optional<Integer> optional2 = input.getInt("AngerTime");
         if (optional2.isPresent()) {
            this.setTimeToRemainAngry(optional2.get().intValue());
         } else {
            this.setPersistentAngerEndTime(-1L);
         }
      }

      if (level instanceof ServerLevel) {
         this.setPersistentAngerTarget(EntityReference.read(input, "angry_at"));
         this.setTarget(EntityReference.getLivingEntity(this.getPersistentAngerTarget(), level));
      }
   }

   default void updatePersistentAnger(ServerLevel serverLevel, boolean updateAnger) {
      LivingEntity livingEntity = this.getTarget();
      EntityReference<LivingEntity> entityReference = this.getPersistentAngerTarget();
      if (livingEntity != null
         && livingEntity.isDeadOrDying()
         && entityReference != null
         && entityReference.matches(livingEntity)
         && livingEntity instanceof Mob) {
         this.stopBeingAngry();
      } else {
         if (livingEntity != null) {
            if (entityReference == null || !entityReference.matches(livingEntity)) {
               this.setPersistentAngerTarget(EntityReference.of(livingEntity));
            }

            this.startPersistentAngerTimer();
         }

         if (entityReference != null && !this.isAngry() && (livingEntity == null || !isValidPlayerTarget(livingEntity) || !updateAnger)) {
            this.stopBeingAngry();
         }
      }
   }

   private static boolean isValidPlayerTarget(LivingEntity target) {
      return target instanceof Player player && !player.isCreative() && !player.isSpectator();
   }

   default boolean isAngryAt(LivingEntity entity, ServerLevel level) {
      if (!this.canAttack(entity)) {
         return false;
      } else if (isValidPlayerTarget(entity) && this.isAngryAtAllPlayers(level)) {
         return true;
      } else {
         EntityReference<LivingEntity> entityReference = this.getPersistentAngerTarget();
         return entityReference != null && entityReference.matches(entity);
      }
   }

   default boolean isAngryAtAllPlayers(ServerLevel level) {
      return level.getGameRules().get(GameRules.UNIVERSAL_ANGER) && this.isAngry() && this.getPersistentAngerTarget() == null;
   }

   default boolean isAngry() {
      long l = this.getPersistentAngerEndTime();
      if (l > 0L) {
         long m = l - this.level().getGameTime();
         return m > 0L;
      } else {
         return false;
      }
   }

   default void playerDied(ServerLevel level, Player player) {
      if (level.getGameRules().get(GameRules.FORGIVE_DEAD_PLAYERS)) {
         EntityReference<LivingEntity> entityReference = this.getPersistentAngerTarget();
         if (entityReference != null && entityReference.matches(player)) {
            this.stopBeingAngry();
         }
      }
   }

   default void forgetCurrentTargetAndRefreshUniversalAnger() {
      this.stopBeingAngry();
      this.startPersistentAngerTimer();
   }

   default void stopBeingAngry() {
      this.setLastHurtByMob(null);
      this.setPersistentAngerTarget(null);
      this.setTarget(null);
      this.setPersistentAngerEndTime(-1L);
   }

   @Nullable
   LivingEntity getLastHurtByMob();

   void setLastHurtByMob(@Nullable LivingEntity livingEntity);

   void setTarget(@Nullable LivingEntity livingEntity);

   boolean canAttack(LivingEntity entity);

   @Nullable
   LivingEntity getTarget();
}
