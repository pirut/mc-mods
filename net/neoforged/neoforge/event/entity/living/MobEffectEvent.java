package net.neoforged.neoforge.event.entity.living;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.EffectCure;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

public abstract class MobEffectEvent extends LivingEvent {
   @Nullable
   protected final MobEffectInstance effectInstance;

   protected MobEffectEvent(LivingEntity living, MobEffectInstance effectInstance) {
      super(living);
      this.effectInstance = effectInstance;
   }

   @Nullable
   public MobEffectInstance getEffectInstance() {
      return this.effectInstance;
   }

   public static class Added extends MobEffectEvent {
      private final MobEffectInstance oldEffectInstance;
      private final Entity source;

      @Internal
      public Added(LivingEntity living, MobEffectInstance oldEffectInstance, MobEffectInstance newEffectInstance, Entity source) {
         super(living, newEffectInstance);
         this.oldEffectInstance = oldEffectInstance;
         this.source = source;
      }

      @Override
      public MobEffectInstance getEffectInstance() {
         return super.getEffectInstance();
      }

      @Nullable
      public MobEffectInstance getOldEffectInstance() {
         return this.oldEffectInstance;
      }

      @Nullable
      public Entity getEffectSource() {
         return this.source;
      }
   }

   public static class Applicable extends MobEffectEvent {
      protected MobEffectEvent.Applicable.Result result = MobEffectEvent.Applicable.Result.DEFAULT;
      @Nullable
      private final Entity source;

      @Internal
      public Applicable(LivingEntity living, MobEffectInstance effectInstance, @Nullable Entity source) {
         super(living, effectInstance);
         this.source = source;
      }

      @Override
      public MobEffectInstance getEffectInstance() {
         return super.getEffectInstance();
      }

      public void setResult(MobEffectEvent.Applicable.Result result) {
         this.result = result;
      }

      public MobEffectEvent.Applicable.Result getResult() {
         return this.result;
      }

      @Nullable
      public Entity getEffectSource() {
         return this.source;
      }

      public boolean getApplicationResult() {
         return this.result == MobEffectEvent.Applicable.Result.APPLY
            ? true
            : this.result == MobEffectEvent.Applicable.Result.DEFAULT && this.getEntity().canBeAffected(this.getEffectInstance());
      }

      public static enum Result {
         APPLY,
         DEFAULT,
         DO_NOT_APPLY;
      }
   }

   public static class Expired extends MobEffectEvent implements ICancellableEvent {
      @Internal
      public Expired(LivingEntity living, MobEffectInstance effectInstance) {
         super(living, effectInstance);
      }
   }

   public static class Remove extends MobEffectEvent implements ICancellableEvent {
      private final Holder<MobEffect> effect;
      @Nullable
      private final EffectCure cure;

      @Internal
      public Remove(LivingEntity living, Holder<MobEffect> effect, @Nullable EffectCure cure) {
         super(living, living.getEffect(effect));
         this.effect = effect;
         this.cure = cure;
      }

      @Internal
      public Remove(LivingEntity living, MobEffectInstance effectInstance, @Nullable EffectCure cure) {
         super(living, effectInstance);
         this.effect = effectInstance.getEffect();
         this.cure = cure;
      }

      public Holder<MobEffect> getEffect() {
         return this.effect;
      }

      @Nullable
      public EffectCure getCure() {
         return this.cure;
      }

      @Nullable
      @Override
      public MobEffectInstance getEffectInstance() {
         return super.getEffectInstance();
      }
   }
}
