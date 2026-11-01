package net.minecraft.world.item.alchemy;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.ConsumableListener;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.AttributeUtil;

public record PotionContents(Optional<Holder<Potion>> potion, Optional<Integer> customColor, List<MobEffectInstance> customEffects, Optional<String> customName)
   implements ConsumableListener,
   TooltipProvider {
   public static final PotionContents EMPTY = new PotionContents(Optional.empty(), Optional.empty(), List.of(), Optional.empty());
   private static final Component NO_EFFECT = Component.translatable("effect.none").withStyle(ChatFormatting.GRAY);
   public static final int BASE_POTION_COLOR = -13083194;
   private static final Codec<PotionContents> FULL_CODEC = RecordCodecBuilder.create(
      p_372777_ -> p_372777_.group(
            Potion.CODEC.optionalFieldOf("potion").forGetter(PotionContents::potion),
            Codec.INT.optionalFieldOf("custom_color").forGetter(PotionContents::customColor),
            MobEffectInstance.CODEC.listOf().optionalFieldOf("custom_effects", List.of()).forGetter(PotionContents::customEffects),
            Codec.STRING.optionalFieldOf("custom_name").forGetter(PotionContents::customName)
         )
         .apply(p_372777_, PotionContents::new)
   );
   public static final Codec<PotionContents> CODEC = Codec.withAlternative(FULL_CODEC, Potion.CODEC, PotionContents::new);
   public static final StreamCodec<RegistryFriendlyByteBuf, PotionContents> STREAM_CODEC = StreamCodec.composite(
      Potion.STREAM_CODEC.apply(ByteBufCodecs::optional),
      PotionContents::potion,
      ByteBufCodecs.INT.apply(ByteBufCodecs::optional),
      PotionContents::customColor,
      MobEffectInstance.STREAM_CODEC.apply(ByteBufCodecs.list()),
      PotionContents::customEffects,
      ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs::optional),
      PotionContents::customName,
      PotionContents::new
   );

   public PotionContents(Holder<Potion> p_331208_) {
      this(Optional.of(p_331208_), Optional.empty(), List.of(), Optional.empty());
   }

   public static ItemStack createItemStack(Item p_330388_, Holder<Potion> p_331030_) {
      ItemStack itemstack = new ItemStack(p_330388_);
      itemstack.set(DataComponents.POTION_CONTENTS, new PotionContents(p_331030_));
      return itemstack;
   }

   public boolean is(Holder<Potion> p_331328_) {
      return this.potion.isPresent() && this.potion.get().is(p_331328_) && this.customEffects.isEmpty();
   }

   public Iterable<MobEffectInstance> getAllEffects() {
      if (this.potion.isEmpty()) {
         return this.customEffects;
      } else {
         return (Iterable<MobEffectInstance>)(this.customEffects.isEmpty()
            ? this.potion.get().value().getEffects()
            : Iterables.concat(this.potion.get().value().getEffects(), this.customEffects));
      }
   }

   public void forEachEffect(Consumer<MobEffectInstance> p_331190_, float p_394070_) {
      if (this.potion.isPresent()) {
         for (MobEffectInstance mobeffectinstance : this.potion.get().value().getEffects()) {
            p_331190_.accept(mobeffectinstance.withScaledDuration(p_394070_));
         }
      }

      for (MobEffectInstance mobeffectinstance1 : this.customEffects) {
         p_331190_.accept(mobeffectinstance1.withScaledDuration(p_394070_));
      }
   }

   public PotionContents withPotion(Holder<Potion> p_330288_) {
      return new PotionContents(Optional.of(p_330288_), this.customColor, this.customEffects, this.customName);
   }

   public PotionContents withEffectAdded(MobEffectInstance p_332083_) {
      return new PotionContents(this.potion, this.customColor, Util.copyAndAdd(this.customEffects, p_332083_), this.customName);
   }

   public int getColor() {
      return this.getColorOr(-13083194);
   }

   public int getColorOr(int p_388208_) {
      return this.customColor.isPresent() ? this.customColor.get() : getColorOptional(this.getAllEffects()).orElse(p_388208_);
   }

   public Component getName(String p_372972_) {
      String s = this.customName.or(() -> this.potion.map(p_372776_ -> p_372776_.value().name())).orElse("empty");
      return Component.translatable(p_372972_ + s);
   }

   public static OptionalInt getColorOptional(Iterable<MobEffectInstance> p_332699_) {
      int i = 0;
      int j = 0;
      int k = 0;
      int l = 0;

      for (MobEffectInstance mobeffectinstance : p_332699_) {
         if (mobeffectinstance.isVisible()) {
            int i1 = mobeffectinstance.getEffect().value().getColor();
            int j1 = mobeffectinstance.getAmplifier() + 1;
            i += j1 * ARGB.red(i1);
            j += j1 * ARGB.green(i1);
            k += j1 * ARGB.blue(i1);
            l += j1;
         }
      }

      return l == 0 ? OptionalInt.empty() : OptionalInt.of(ARGB.color(i / l, j / l, k / l));
   }

   public boolean hasEffects() {
      return !this.customEffects.isEmpty() ? true : this.potion.isPresent() && !this.potion.get().value().getEffects().isEmpty();
   }

   public List<MobEffectInstance> customEffects() {
      return Lists.transform(this.customEffects, MobEffectInstance::new);
   }

   public void applyToLivingEntity(LivingEntity p_366471_, float p_393741_) {
      if (p_366471_.level() instanceof ServerLevel serverlevel) {
         Player player1 = p_366471_ instanceof Player player ? player : null;
         this.forEachEffect(p_376018_ -> {
            if (p_376018_.getEffect().value().isInstantenous()) {
               p_376018_.getEffect().value().applyInstantenousEffect(serverlevel, player1, player1, p_366471_, p_376018_.getAmplifier(), 1.0);
            } else {
               p_366471_.addEffect(p_376018_);
            }
         }, p_393741_);
      }
   }

   public static void addPotionTooltip(Iterable<MobEffectInstance> p_331825_, Consumer<Component> p_331296_, float p_332038_, float p_332014_) {
      List<Pair<Holder<Attribute>, AttributeModifier>> list = Lists.newArrayList();
      boolean flag = true;

      for (MobEffectInstance mobeffectinstance : p_331825_) {
         flag = false;
         Holder<MobEffect> holder = mobeffectinstance.getEffect();
         int i = mobeffectinstance.getAmplifier();
         holder.value().createModifiers(i, (p_331556_, p_330860_) -> list.add(new Pair(p_331556_, p_330860_)));
         MutableComponent mutablecomponent = getPotionDescription(holder, i);
         if (!mobeffectinstance.endsWithin(20)) {
            mutablecomponent = Component.translatable(
               "potion.withDuration", mutablecomponent, MobEffectUtil.formatDuration(mobeffectinstance, p_332038_, p_332014_)
            );
         }

         p_331296_.accept(mutablecomponent.withStyle(holder.value().getCategory().getTooltipFormatting()));
      }

      if (flag) {
         p_331296_.accept(NO_EFFECT);
      }

      if (!list.isEmpty()) {
         p_331296_.accept(CommonComponents.EMPTY);
         p_331296_.accept(Component.translatable("potion.whenDrank").withStyle(ChatFormatting.DARK_PURPLE));
         AttributeUtil.addPotionTooltip(list, p_331296_);
      }
   }

   public static MutableComponent getPotionDescription(Holder<MobEffect> p_397215_, int p_397091_) {
      MutableComponent mutablecomponent = Component.translatable(p_397215_.value().getDescriptionId());
      return p_397091_ > 0
         ? Component.translatable("potion.withAmplifier", mutablecomponent, Component.translatable("potion.potency." + p_397091_))
         : mutablecomponent;
   }

   @Override
   public void onConsume(Level p_366836_, LivingEntity p_366750_, ItemStack p_366399_, Consumable p_366541_) {
      this.applyToLivingEntity(p_366750_, p_366399_.getOrDefault(DataComponents.POTION_DURATION_SCALE, 1.0F));
   }

   @Override
   public void addToTooltip(Item.TooltipContext p_399531_, Consumer<Component> p_399764_, TooltipFlag p_400075_, DataComponentGetter p_400022_) {
      addPotionTooltip(this.getAllEffects(), p_399764_, p_400022_.getOrDefault(DataComponents.POTION_DURATION_SCALE, 1.0F), p_399531_.tickRate());
   }
}
