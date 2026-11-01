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
      void convert(Mob oldMob, Mob newMob, ConversionParams conversionParams) {
         Entity entity = oldMob.getFirstPassenger();
         newMob.copyPosition(oldMob);
         newMob.setDeltaMovement(oldMob.getDeltaMovement());
         if (entity != null) {
            entity.stopRiding();
            entity.boardingCooldown = 0;

            for (Entity entity2 : newMob.getPassengers()) {
               entity2.stopRiding();
               entity2.remove(Entity.RemovalReason.DISCARDED);
            }

            entity.startRiding(newMob);
         }

         Entity entity3 = oldMob.getVehicle();
         if (entity3 != null) {
            oldMob.stopRiding();
            newMob.startRiding(entity3, false, false);
         }

         if (conversionParams.keepEquipment()) {
            for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {
               ItemStack itemStack = oldMob.getItemBySlot(equipmentSlot);
               if (!itemStack.isEmpty()) {
                  newMob.setItemSlot(equipmentSlot, itemStack.copyAndClear());
                  newMob.setDropChance(equipmentSlot, oldMob.getDropChances().byEquipment(equipmentSlot));
               }
            }
         }

         newMob.fallDistance = oldMob.fallDistance;
         newMob.setSharedFlag(7, oldMob.isFallFlying());
         newMob.lastHurtByPlayerMemoryTime = oldMob.lastHurtByPlayerMemoryTime;
         newMob.hurtTime = oldMob.hurtTime;
         newMob.yBodyRot = oldMob.yBodyRot;
         newMob.setOnGround(oldMob.onGround());
         oldMob.getSleepingPos().ifPresent(newMob::setSleepingPos);
         Entity entity2 = oldMob.getLeashHolder();
         if (entity2 != null) {
            newMob.setLeashedTo(entity2, true);
         }

         this.convertCommon(oldMob, newMob, conversionParams);
      }
   },
   SPLIT_ON_DEATH(false) {
      @Override
      void convert(Mob oldMob, Mob newMob, ConversionParams conversionParams) {
         Entity entity = oldMob.getFirstPassenger();
         if (entity != null) {
            entity.stopRiding();
         }

         Entity entity2 = oldMob.getLeashHolder();
         if (entity2 != null) {
            oldMob.dropLeash();
         }

         this.convertCommon(oldMob, newMob, conversionParams);
      }
   };

   private static final Set<DataComponentType<?>> COMPONENTS_TO_COPY = Set.of(DataComponents.CUSTOM_NAME, DataComponents.CUSTOM_DATA);
   private final boolean discardAfterConversion;

   ConversionType(final boolean discardAfterConversion) {
      this.discardAfterConversion = discardAfterConversion;
   }

   public boolean shouldDiscardAfterConversion() {
      return this.discardAfterConversion;
   }

   abstract void convert(Mob oldMob, Mob newMob, ConversionParams conversionParams);

   void convertCommon(Mob oldMob, Mob newMob, ConversionParams conversionParams) {
      newMob.setAbsorptionAmount(oldMob.getAbsorptionAmount());

      for (MobEffectInstance mobEffectInstance : oldMob.getActiveEffects()) {
         newMob.addEffect(new MobEffectInstance(mobEffectInstance));
      }

      if (oldMob.isBaby()) {
         newMob.setBaby(true);
      }

      if (oldMob instanceof AgeableMob ageableMob && newMob instanceof AgeableMob ageableMob2) {
         ageableMob2.setAge(ageableMob.getAge());
         ageableMob2.forcedAge = ageableMob.forcedAge;
         ageableMob2.forcedAgeTimer = ageableMob.forcedAgeTimer;
      }

      Brain<?> brain = oldMob.getBrain();
      Brain<?> brain2 = newMob.getBrain();
      if (brain.checkMemory(MemoryModuleType.ANGRY_AT, MemoryStatus.REGISTERED) && brain.hasMemoryValue(MemoryModuleType.ANGRY_AT)) {
         brain2.setMemory(MemoryModuleType.ANGRY_AT, brain.getMemory(MemoryModuleType.ANGRY_AT));
      }

      if (conversionParams.preserveCanPickUpLoot()) {
         newMob.setCanPickUpLoot(oldMob.canPickUpLoot());
      }

      newMob.setLeftHanded(oldMob.isLeftHanded());
      newMob.setNoAi(oldMob.isNoAi());
      if (oldMob.isPersistenceRequired()) {
         newMob.setPersistenceRequired();
      }

      newMob.setCustomNameVisible(oldMob.isCustomNameVisible());
      newMob.setSharedFlagOnFire(oldMob.isOnFire());
      newMob.setInvulnerable(oldMob.isInvulnerable());
      newMob.setNoGravity(oldMob.isNoGravity());
      newMob.setPortalCooldown(oldMob.getPortalCooldown());
      newMob.setSilent(oldMob.isSilent());
      oldMob.getTags().forEach(newMob::addTag);

      for (DataComponentType<?> dataComponentType : COMPONENTS_TO_COPY) {
         copyComponent(oldMob, newMob, dataComponentType);
      }

      if (conversionParams.team() != null) {
         Scoreboard scoreboard = newMob.level().getScoreboard();
         scoreboard.addPlayerToTeam(newMob.getStringUUID(), conversionParams.team());
         if (oldMob.getTeam() != null && oldMob.getTeam() == conversionParams.team()) {
            scoreboard.removePlayerFromTeam(oldMob.getStringUUID(), oldMob.getTeam());
         }
      }

      if (oldMob instanceof Zombie zombie && zombie.canBreakDoors() && newMob instanceof Zombie zombie2) {
         zombie2.setCanBreakDoors(true);
      }
   }

   private static <T> void copyComponent(Mob oldMob, Mob newMob, DataComponentType<T> component) {
      T object = oldMob.get(component);
      if (object != null) {
         newMob.setComponent(component, object);
      }
   }
}
