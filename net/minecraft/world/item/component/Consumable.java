package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.item.consume_effects.PlaySoundConsumeEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public record Consumable(
   float consumeSeconds, ItemUseAnimation animation, Holder<SoundEvent> sound, boolean hasConsumeParticles, List<ConsumeEffect> onConsumeEffects
) {
   public static final float DEFAULT_CONSUME_SECONDS = 1.6F;
   private static final int CONSUME_EFFECTS_INTERVAL = 4;
   private static final float CONSUME_EFFECTS_START_FRACTION = 0.21875F;
   public static final Codec<Consumable> CODEC = RecordCodecBuilder.create(
      p_366542_ -> p_366542_.group(
            ExtraCodecs.NON_NEGATIVE_FLOAT.optionalFieldOf("consume_seconds", 1.6F).forGetter(Consumable::consumeSeconds),
            ItemUseAnimation.CODEC.optionalFieldOf("animation", ItemUseAnimation.EAT).forGetter(Consumable::animation),
            SoundEvent.CODEC.optionalFieldOf("sound", SoundEvents.GENERIC_EAT).forGetter(Consumable::sound),
            Codec.BOOL.optionalFieldOf("has_consume_particles", true).forGetter(Consumable::hasConsumeParticles),
            ConsumeEffect.CODEC.listOf().optionalFieldOf("on_consume_effects", List.of()).forGetter(Consumable::onConsumeEffects)
         )
         .apply(p_366542_, Consumable::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, Consumable> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.FLOAT,
      Consumable::consumeSeconds,
      ItemUseAnimation.STREAM_CODEC,
      Consumable::animation,
      SoundEvent.STREAM_CODEC,
      Consumable::sound,
      ByteBufCodecs.BOOL,
      Consumable::hasConsumeParticles,
      ConsumeEffect.STREAM_CODEC.apply(ByteBufCodecs.list()),
      Consumable::onConsumeEffects,
      Consumable::new
   );

   public InteractionResult startConsuming(LivingEntity p_366412_, ItemStack p_366449_, InteractionHand p_366742_) {
      if (!this.canConsume(p_366412_, p_366449_)) {
         return InteractionResult.FAIL;
      } else {
         boolean flag = this.consumeTicks() > 0;
         if (flag) {
            p_366412_.startUsingItem(p_366742_);
            return InteractionResult.CONSUME;
         } else {
            ItemStack itemstack = this.onConsume(p_366412_.level(), p_366412_, p_366449_);
            return InteractionResult.CONSUME.heldItemTransformedTo(itemstack);
         }
      }
   }

   public ItemStack onConsume(Level p_366638_, LivingEntity p_366573_, ItemStack p_366688_) {
      RandomSource randomsource = p_366573_.getRandom();
      this.emitParticlesAndSounds(randomsource, p_366573_, p_366688_, 16);
      if (p_366573_ instanceof ServerPlayer serverplayer) {
         serverplayer.awardStat(Stats.ITEM_USED.get(p_366688_.getItem()));
         CriteriaTriggers.CONSUME_ITEM.trigger(serverplayer, p_366688_);
      }

      p_366688_.getAllOfType(ConsumableListener.class).forEach(p_366420_ -> p_366420_.onConsume(p_366638_, p_366573_, p_366688_, this));
      if (!p_366638_.isClientSide()) {
         this.onConsumeEffects.forEach(p_366779_ -> p_366779_.apply(p_366638_, p_366688_, p_366573_));
      }

      p_366573_.gameEvent(this.animation == ItemUseAnimation.DRINK ? GameEvent.DRINK : GameEvent.EAT);
      p_366688_.consume(1, p_366573_);
      return p_366688_;
   }

   public boolean canConsume(LivingEntity p_366522_, ItemStack p_366908_) {
      FoodProperties foodproperties = p_366908_.get(DataComponents.FOOD);
      return foodproperties != null && p_366522_ instanceof Player player ? player.canEat(foodproperties.canAlwaysEat()) : true;
   }

   public int consumeTicks() {
      return (int)(this.consumeSeconds * 20.0F);
   }

   public void emitParticlesAndSounds(RandomSource p_366511_, LivingEntity p_366794_, ItemStack p_366479_, int p_366619_) {
      float f = p_366511_.nextBoolean() ? 0.5F : 1.0F;
      float f1 = p_366511_.triangle(1.0F, 0.2F);
      float f2 = 0.5F;
      float f3 = Mth.randomBetween(p_366511_, 0.9F, 1.0F);
      float f4 = this.animation == ItemUseAnimation.DRINK ? 0.5F : f;
      float f5 = this.animation == ItemUseAnimation.DRINK ? f3 : f1;
      if (this.hasConsumeParticles) {
         p_366794_.spawnItemParticles(p_366479_, p_366619_);
      }

      SoundEvent soundevent = p_366794_ instanceof Consumable.OverrideConsumeSound consumable$overrideconsumesound
         ? consumable$overrideconsumesound.getConsumeSound(p_366479_)
         : this.sound.value();
      p_366794_.playSound(soundevent, f4, f5);
   }

   public boolean shouldEmitParticlesAndSounds(int p_366816_) {
      int i = this.consumeTicks() - p_366816_;
      int j = (int)(this.consumeTicks() * 0.21875F);
      boolean flag = i > j;
      return flag && p_366816_ % 4 == 0;
   }

   public static Consumable.Builder builder() {
      return new Consumable.Builder();
   }

   public static class Builder {
      private float consumeSeconds = 1.6F;
      private ItemUseAnimation animation = ItemUseAnimation.EAT;
      private Holder<SoundEvent> sound = SoundEvents.GENERIC_EAT;
      private boolean hasConsumeParticles = true;
      private final List<ConsumeEffect> onConsumeEffects = new ArrayList<>();

      Builder() {
      }

      public Consumable.Builder consumeSeconds(float p_366857_) {
         this.consumeSeconds = p_366857_;
         return this;
      }

      public Consumable.Builder animation(ItemUseAnimation p_366491_) {
         this.animation = p_366491_;
         return this;
      }

      public Consumable.Builder sound(Holder<SoundEvent> p_366867_) {
         this.sound = p_366867_;
         return this;
      }

      public Consumable.Builder soundAfterConsume(Holder<SoundEvent> p_366752_) {
         return this.onConsume(new PlaySoundConsumeEffect(p_366752_));
      }

      public Consumable.Builder hasConsumeParticles(boolean p_366715_) {
         this.hasConsumeParticles = p_366715_;
         return this;
      }

      public Consumable.Builder onConsume(ConsumeEffect p_366518_) {
         this.onConsumeEffects.add(p_366518_);
         return this;
      }

      public Consumable build() {
         return new Consumable(this.consumeSeconds, this.animation, this.sound, this.hasConsumeParticles, this.onConsumeEffects);
      }
   }

   public interface OverrideConsumeSound {
      SoundEvent getConsumeSound(ItemStack var1);
   }
}
