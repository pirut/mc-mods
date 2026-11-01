package net.minecraft.world.entity;

import java.util.Set;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Scoreboard;

public enum ConversionType {
   SINGLE(true) {
      @Override
      void convert(Mob p_371283_, Mob p_371774_, ConversionParams p_371350_) {
         Entity entity = p_371283_.getFirstPassenger();
         p_371774_.copyPosition(p_371283_);
         p_371774_.setDeltaMovement(p_371283_.getDeltaMovement());
         if (entity != null) {
            entity.stopRiding();
            entity.boardingCooldown = 0;

            for (Entity entity1 : p_371774_.getPassengers()) {
               entity1.stopRiding();
               entity1.remove(Entity.RemovalReason.DISCARDED);
            }

            entity.startRiding(p_371774_);
         }

         Entity entity2 = p_371283_.getVehicle();
         if (entity2 != null) {
            p_371283_.stopRiding();
            p_371774_.startRiding(entity2, false, false);
         }

         if (p_371350_.keepEquipment()) {
            for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
               ItemStack itemstack = p_371283_.getItemBySlot(equipmentslot);
               if (!itemstack.isEmpty()) {
                  p_371774_.setItemSlot(equipmentslot, itemstack.copyAndClear());
                  p_371774_.setDropChance(equipmentslot, p_371283_.getDropChances().byEquipment(equipmentslot));
               }
            }
         }

         p_371774_.fallDistance = p_371283_.fallDistance;
         p_371774_.setSharedFlag(7, p_371283_.isFallFlying());
         p_371774_.lastHurtByPlayerMemoryTime = p_371283_.lastHurtByPlayerMemoryTime;
         p_371774_.hurtTime = p_371283_.hurtTime;
         p_371774_.yBodyRot = p_371283_.yBodyRot;
         p_371774_.setOnGround(p_371283_.onGround());
         p_371283_.getSleepingPos().ifPresent(p_371774_::setSleepingPos);
         Entity entity3 = p_371283_.getLeashHolder();
         if (entity3 != null) {
            p_371774_.setLeashedTo(entity3, true);
         }

         this.convertCommon(p_371283_, p_371774_, p_371350_);
      }
   },
   SPLIT_ON_DEATH(false) {
      @Override
      void convert(Mob p_371507_, Mob p_371702_, ConversionParams p_371413_) {
         Entity entity = p_371507_.getFirstPassenger();
         if (entity != null) {
            entity.stopRiding();
         }

         Entity entity1 = p_371507_.getLeashHolder();
         if (entity1 != null) {
            p_371507_.dropLeash();
         }

         this.convertCommon(p_371507_, p_371702_, p_371413_);
      }
   };

   private static final Set<DataComponentType<?>> COMPONENTS_TO_COPY = Set.of(DataComponents.CUSTOM_NAME, DataComponents.CUSTOM_DATA);
   private final boolean discardAfterConversion;

   private ConversionType(boolean p_371905_) {
      this.discardAfterConversion = p_371905_;
   }

   public boolean shouldDiscardAfterConversion() {
      return this.discardAfterConversion;
   }

   abstract void convert(Mob var1, Mob var2, ConversionParams var3);

   void convertCommon(Mob p_371651_, Mob p_371777_, ConversionParams p_371814_) {
      p_371777_.setAbsorptionAmount(p_371651_.getAbsorptionAmount());

      for (MobEffectInstance mobeffectinstance : p_371651_.getActiveEffects()) {
         p_371777_.addEffect(new MobEffectInstance(mobeffectinstance));
      }

      if (p_371651_.isBaby()) {
         p_371777_.setBaby(true);
      }

      if (p_371651_ instanceof AgeableMob ageablemob && p_371777_ instanceof AgeableMob ageablemob1) {
         ageablemob1.setAge(ageablemob.getAge());
         ageablemob1.forcedAge = ageablemob.forcedAge;
         ageablemob1.forcedAgeTimer = ageablemob.forcedAgeTimer;
      }

      Brain<?> brain = p_371651_.getBrain();
      Brain<?> brain1 = p_371777_.getBrain();
      if (brain.checkMemory(MemoryModuleType.ANGRY_AT, MemoryStatus.REGISTERED) && brain.hasMemoryValue(MemoryModuleType.ANGRY_AT)) {
         brain1.setMemory(MemoryModuleType.ANGRY_AT, brain.getMemory(MemoryModuleType.ANGRY_AT));
      }

      if (p_371814_.preserveCanPickUpLoot()) {
         p_371777_.setCanPickUpLoot(p_371651_.canPickUpLoot());
      }

      p_371777_.setLeftHanded(p_371651_.isLeftHanded());
      p_371777_.setNoAi(p_371651_.isNoAi());
      if (p_371651_.isPersistenceRequired()) {
         p_371777_.setPersistenceRequired();
      }

      p_371777_.setCustomNameVisible(p_371651_.isCustomNameVisible());
      p_371777_.setSharedFlagOnFire(p_371651_.isOnFire());
      p_371777_.setInvulnerable(p_371651_.isInvulnerable());
      p_371777_.setNoGravity(p_371651_.isNoGravity());
      p_371777_.setPortalCooldown(p_371651_.getPortalCooldown());
      p_371777_.setSilent(p_371651_.isSilent());
      p_371651_.getTags().forEach(p_371777_::addTag);

      for (DataComponentType<?> datacomponenttype : COMPONENTS_TO_COPY) {
         copyComponent(p_371651_, p_371777_, datacomponenttype);
      }

      if (p_371814_.team() != null) {
         Scoreboard scoreboard = p_371777_.level().getScoreboard();
         scoreboard.addPlayerToTeam(p_371777_.getStringUUID(), p_371814_.team());
         if (p_371651_.getTeam() != null && p_371651_.getTeam() == p_371814_.team()) {
            scoreboard.removePlayerFromTeam(p_371651_.getStringUUID(), p_371651_.getTeam());
         }
      }

      if (p_371651_ instanceof Zombie zombie && zombie.canBreakDoors() && p_371777_ instanceof Zombie zombie1) {
         zombie1.setCanBreakDoors(true);
      }
   }

   private static <T> void copyComponent(Mob p_423567_, Mob p_423503_, DataComponentType<T> p_423504_) {
      T t = p_423567_.get(p_423504_);
      if (t != null) {
         p_423503_.setComponent(p_423504_, t);
      }
   }
}
