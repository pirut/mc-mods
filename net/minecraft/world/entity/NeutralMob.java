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

   default void setTimeToRemainAngry(long p_456101_) {
      this.setPersistentAngerEndTime(this.level().getGameTime() + p_456101_);
   }

   void setPersistentAngerEndTime(long var1);

   @Nullable
   EntityReference<LivingEntity> getPersistentAngerTarget();

   void setPersistentAngerTarget(@Nullable EntityReference<LivingEntity> var1);

   void startPersistentAngerTimer();

   Level level();

   default void addPersistentAngerSaveData(ValueOutput p_421536_) {
      p_421536_.putLong("anger_end_time", this.getPersistentAngerEndTime());
      p_421536_.storeNullable("angry_at", EntityReference.codec(), this.getPersistentAngerTarget());
   }

   default void readPersistentAngerSaveData(Level p_147286_, ValueInput p_421617_) {
      Optional<Long> optional = p_421617_.getLong("anger_end_time");
      if (optional.isPresent()) {
         this.setPersistentAngerEndTime(optional.get());
      } else {
         Optional<Integer> optional1 = p_421617_.getInt("AngerTime");
         if (optional1.isPresent()) {
            this.setTimeToRemainAngry(optional1.get().intValue());
         } else {
            this.setPersistentAngerEndTime(-1L);
         }
      }

      if (p_147286_ instanceof ServerLevel) {
         this.setPersistentAngerTarget(EntityReference.read(p_421617_, "angry_at"));
         this.setTarget(EntityReference.getLivingEntity(this.getPersistentAngerTarget(), p_147286_));
      }
   }

   default void updatePersistentAnger(ServerLevel p_21667_, boolean p_21668_) {
      LivingEntity livingentity = this.getTarget();
      EntityReference<LivingEntity> entityreference = this.getPersistentAngerTarget();
      if (livingentity != null
         && livingentity.isDeadOrDying()
         && entityreference != null
         && entityreference.matches(livingentity)
         && livingentity instanceof Mob) {
         this.stopBeingAngry();
      } else {
         if (livingentity != null) {
            if (entityreference == null || !entityreference.matches(livingentity)) {
               this.setPersistentAngerTarget(EntityReference.of(livingentity));
            }

            this.startPersistentAngerTimer();
         }

         if (entityreference != null && !this.isAngry() && (livingentity == null || !isValidPlayerTarget(livingentity) || !p_21668_)) {
            this.stopBeingAngry();
         }
      }
   }

   private static boolean isValidPlayerTarget(LivingEntity p_459158_) {
      return p_459158_ instanceof Player player && !player.isCreative() && !player.isSpectator();
   }

   default boolean isAngryAt(LivingEntity p_21675_, ServerLevel p_376199_) {
      if (!this.canAttack(p_21675_)) {
         return false;
      } else if (isValidPlayerTarget(p_21675_) && this.isAngryAtAllPlayers(p_376199_)) {
         return true;
      } else {
         EntityReference<LivingEntity> entityreference = this.getPersistentAngerTarget();
         return entityreference != null && entityreference.matches(p_21675_);
      }
   }

   default boolean isAngryAtAllPlayers(ServerLevel p_376114_) {
      return p_376114_.getGameRules().get(GameRules.UNIVERSAL_ANGER) && this.isAngry() && this.getPersistentAngerTarget() == null;
   }

   default boolean isAngry() {
      long i = this.getPersistentAngerEndTime();
      if (i > 0L) {
         long j = i - this.level().getGameTime();
         return j > 0L;
      } else {
         return false;
      }
   }

   default void playerDied(ServerLevel p_376731_, Player p_21677_) {
      if (p_376731_.getGameRules().get(GameRules.FORGIVE_DEAD_PLAYERS)) {
         EntityReference<LivingEntity> entityreference = this.getPersistentAngerTarget();
         if (entityreference != null && entityreference.matches(p_21677_)) {
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

   void setLastHurtByMob(@Nullable LivingEntity var1);

   void setTarget(@Nullable LivingEntity var1);

   boolean canAttack(LivingEntity var1);

   @Nullable
   LivingEntity getTarget();
}
