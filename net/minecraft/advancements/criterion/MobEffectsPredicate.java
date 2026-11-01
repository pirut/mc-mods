package net.minecraft.advancements.criterion;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

public record MobEffectsPredicate(Map<Holder<MobEffect>, MobEffectsPredicate.MobEffectInstancePredicate> effectMap) {
   public static final Codec<MobEffectsPredicate> CODEC = Codec.unboundedMap(MobEffect.CODEC, MobEffectsPredicate.MobEffectInstancePredicate.CODEC)
      .xmap(MobEffectsPredicate::new, MobEffectsPredicate::effectMap);

   public boolean matches(Entity p_469410_) {
      return p_469410_ instanceof LivingEntity livingentity && this.matches(livingentity.getActiveEffectsMap());
   }

   public boolean matches(LivingEntity p_469983_) {
      return this.matches(p_469983_.getActiveEffectsMap());
   }

   public boolean matches(Map<Holder<MobEffect>, MobEffectInstance> p_469608_) {
      for (Entry<Holder<MobEffect>, MobEffectsPredicate.MobEffectInstancePredicate> entry : this.effectMap.entrySet()) {
         MobEffectInstance mobeffectinstance = p_469608_.get(entry.getKey());
         if (!entry.getValue().matches(mobeffectinstance)) {
            return false;
         }
      }

      return true;
   }

   public static class Builder {
      private final com.google.common.collect.ImmutableMap.Builder<Holder<MobEffect>, MobEffectsPredicate.MobEffectInstancePredicate> effectMap = ImmutableMap.builder();

      public static MobEffectsPredicate.Builder effects() {
         return new MobEffectsPredicate.Builder();
      }

      public MobEffectsPredicate.Builder and(Holder<MobEffect> p_467485_) {
         this.effectMap.put(p_467485_, new MobEffectsPredicate.MobEffectInstancePredicate());
         return this;
      }

      public MobEffectsPredicate.Builder and(Holder<MobEffect> p_468387_, MobEffectsPredicate.MobEffectInstancePredicate p_467522_) {
         this.effectMap.put(p_468387_, p_467522_);
         return this;
      }

      public Optional<MobEffectsPredicate> build() {
         return Optional.of(new MobEffectsPredicate(this.effectMap.build()));
      }
   }

   public record MobEffectInstancePredicate(MinMaxBounds.Ints amplifier, MinMaxBounds.Ints duration, Optional<Boolean> ambient, Optional<Boolean> visible) {
      public static final Codec<MobEffectsPredicate.MobEffectInstancePredicate> CODEC = RecordCodecBuilder.create(
         p_468458_ -> p_468458_.group(
               MinMaxBounds.Ints.CODEC.optionalFieldOf("amplifier", MinMaxBounds.Ints.ANY).forGetter(MobEffectsPredicate.MobEffectInstancePredicate::amplifier),
               MinMaxBounds.Ints.CODEC.optionalFieldOf("duration", MinMaxBounds.Ints.ANY).forGetter(MobEffectsPredicate.MobEffectInstancePredicate::duration),
               Codec.BOOL.optionalFieldOf("ambient").forGetter(MobEffectsPredicate.MobEffectInstancePredicate::ambient),
               Codec.BOOL.optionalFieldOf("visible").forGetter(MobEffectsPredicate.MobEffectInstancePredicate::visible)
            )
            .apply(p_468458_, MobEffectsPredicate.MobEffectInstancePredicate::new)
      );

      public MobEffectInstancePredicate() {
         this(MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY, Optional.empty(), Optional.empty());
      }

      public boolean matches(@Nullable MobEffectInstance p_468978_) {
         if (p_468978_ == null) {
            return false;
         } else if (!this.amplifier.matches(p_468978_.getAmplifier())) {
            return false;
         } else if (!this.duration.matches(p_468978_.getDuration())) {
            return false;
         } else {
            return this.ambient.isPresent() && this.ambient.get() != p_468978_.isAmbient()
               ? false
               : !this.visible.isPresent() || this.visible.get() == p_468978_.isVisible();
         }
      }
   }
}
