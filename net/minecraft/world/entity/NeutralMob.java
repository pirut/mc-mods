package net.minecraft.world.entity;

import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

public interface NeutralMob {
   String TAG_ANGER_TIME = "AngerTime";
   String TAG_ANGRY_AT = "AngryAt";

   int getRemainingPersistentAngerTime();

   void setRemainingPersistentAngerTime(int var1);

   @Nullable
   UUID getPersistentAngerTarget();

   void setPersistentAngerTarget(@Nullable UUID var1);

   void startPersistentAngerTimer();

   default void addPersistentAngerSaveData(CompoundTag p_21679_) {
      p_21679_.putInt("AngerTime", this.getRemainingPersistentAngerTime());
      if (this.getPersistentAngerTarget() != null) {
         p_21679_.putUUID("AngryAt", this.getPersistentAngerTarget());
      }
   }

   default void readPersistentAngerSaveData(Level p_147286_, CompoundTag p_147287_) {
      this.setRemainingPersistentAngerTime(p_147287_.getInt("AngerTime"));
      if (p_147286_ instanceof ServerLevel) {
         if (!p_147287_.hasUUID("AngryAt")) {
            this.setPersistentAngerTarget(null);
         } else {
            UUID $$2 = p_147287_.getUUID("AngryAt");
            this.setPersistentAngerTarget($$2);
            Entity $$3 = ((ServerLevel)p_147286_).getEntity($$2);
            if ($$3 != null) {
               if ($$3 instanceof Mob $$4) {
                  this.setTarget($$4);
                  this.setLastHurtByMob($$4);
               }

               if ($$3 instanceof Player $$5) {
                  this.setTarget($$5);
                  this.setLastHurtByPlayer($$5);
               }
            }
         }
      }
   }

   default void updatePersistentAnger(ServerLevel p_21667_, boolean p_21668_) {
      LivingEntity $$2 = this.getTarget();
      UUID $$3 = this.getPersistentAngerTarget();
      if (($$2 == null || $$2.isDeadOrDying()) && $$3 != null && p_21667_.getEntity($$3) instanceof Mob) {
         this.stopBeingAngry();
      } else {
         if ($$2 != null && !Objects.equals($$3, $$2.getUUID())) {
            this.setPersistentAngerTarget($$2.getUUID());
            this.startPersistentAngerTimer();
         }

         if (this.getRemainingPersistentAngerTime() > 0 && ($$2 == null || $$2.getType() != EntityType.PLAYER || !p_21668_)) {
            this.setRemainingPersistentAngerTime(this.getRemainingPersistentAngerTime() - 1);
            if (this.getRemainingPersistentAngerTime() == 0) {
               this.stopBeingAngry();
            }
         }
      }
   }

   default boolean isAngryAt(LivingEntity p_21675_) {
      if (!this.canAttack(p_21675_)) {
         return false;
      } else {
         return p_21675_.getType() == EntityType.PLAYER && this.isAngryAtAllPlayers(p_21675_.level())
            ? true
            : p_21675_.getUUID().equals(this.getPersistentAngerTarget());
      }
   }

   default boolean isAngryAtAllPlayers(Level p_21671_) {
      return p_21671_.getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER) && this.isAngry() && this.getPersistentAngerTarget() == null;
   }

   default boolean isAngry() {
      return this.getRemainingPersistentAngerTime() > 0;
   }

   default void playerDied(Player p_21677_) {
      if (p_21677_.level().getGameRules().getBoolean(GameRules.RULE_FORGIVE_DEAD_PLAYERS)) {
         if (p_21677_.getUUID().equals(this.getPersistentAngerTarget())) {
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
      this.setRemainingPersistentAngerTime(0);
   }

   @Nullable
   LivingEntity getLastHurtByMob();

   void setLastHurtByMob(@Nullable LivingEntity var1);

   void setLastHurtByPlayer(@Nullable Player var1);

   void setTarget(@Nullable LivingEntity var1);

   boolean canAttack(LivingEntity var1);

   @Nullable
   LivingEntity getTarget();
}
