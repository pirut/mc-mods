package net.neoforged.neoforge.event.entity.living;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.Nullable;

public class LivingChangeTargetEvent extends LivingEvent implements ICancellableEvent {
   private final LivingChangeTargetEvent.ILivingTargetType targetType;
   @Nullable
   private final LivingEntity originalAboutToBeSetTarget;
   @Nullable
   private LivingEntity newAboutToBeSetTarget;

   public LivingChangeTargetEvent(LivingEntity entity, @Nullable LivingEntity aboutToBeSetTarget, LivingChangeTargetEvent.ILivingTargetType targetType) {
      super(entity);
      this.originalAboutToBeSetTarget = aboutToBeSetTarget;
      this.newAboutToBeSetTarget = aboutToBeSetTarget;
      this.targetType = targetType;
   }

   @Nullable
   public LivingEntity getNewAboutToBeSetTarget() {
      return this.newAboutToBeSetTarget;
   }

   public void setNewAboutToBeSetTarget(@Nullable LivingEntity newAboutToBeSetTarget) {
      this.newAboutToBeSetTarget = newAboutToBeSetTarget;
   }

   public LivingChangeTargetEvent.ILivingTargetType getTargetType() {
      return this.targetType;
   }

   @Nullable
   public LivingEntity getOriginalAboutToBeSetTarget() {
      return this.originalAboutToBeSetTarget;
   }

   public interface ILivingTargetType {
   }

   public static enum LivingTargetType implements LivingChangeTargetEvent.ILivingTargetType {
      MOB_TARGET,
      BEHAVIOR_TARGET;
   }
}
