package net.minecraft.world.item;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public class InstrumentItem extends Item {
   private final TagKey<Instrument> instruments;

   public InstrumentItem(Item.Properties p_220099_, TagKey<Instrument> p_220100_) {
      super(p_220099_);
      this.instruments = p_220100_;
   }

   @Override
   public void appendHoverText(ItemStack p_220115_, Item.TooltipContext p_339630_, List<Component> p_220117_, TooltipFlag p_220118_) {
      super.appendHoverText(p_220115_, p_339630_, p_220117_, p_220118_);
      Optional<ResourceKey<Instrument>> $$4 = this.getInstrument(p_220115_).flatMap(Holder::unwrapKey);
      if ($$4.isPresent()) {
         MutableComponent $$5 = Component.translatable(Util.makeDescriptionId("instrument", $$4.get().location()));
         p_220117_.add($$5.withStyle(ChatFormatting.GRAY));
      }
   }

   public static ItemStack create(Item p_220108_, Holder<Instrument> p_220109_) {
      ItemStack $$2 = new ItemStack(p_220108_);
      $$2.set(DataComponents.INSTRUMENT, p_220109_);
      return $$2;
   }

   public static void setRandom(ItemStack p_220111_, TagKey<Instrument> p_220112_, RandomSource p_220113_) {
      Optional<Holder<Instrument>> $$3 = BuiltInRegistries.INSTRUMENT.getRandomElementOf(p_220112_, p_220113_);
      $$3.ifPresent(p_330088_ -> p_220111_.set(DataComponents.INSTRUMENT, (Holder<Instrument>)p_330088_));
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level p_220123_, Player p_220124_, InteractionHand p_220125_) {
      ItemStack $$3 = p_220124_.getItemInHand(p_220125_);
      Optional<? extends Holder<Instrument>> $$4 = this.getInstrument($$3);
      if ($$4.isPresent()) {
         Instrument $$5 = $$4.get().value();
         p_220124_.startUsingItem(p_220125_);
         play(p_220123_, p_220124_, $$5);
         p_220124_.getCooldowns().addCooldown(this, $$5.useDuration());
         p_220124_.awardStat(Stats.ITEM_USED.get(this));
         return InteractionResultHolder.consume($$3);
      } else {
         return InteractionResultHolder.fail($$3);
      }
   }

   @Override
   public int getUseDuration(ItemStack p_220131_, LivingEntity p_345916_) {
      Optional<Holder<Instrument>> $$2 = this.getInstrument(p_220131_);
      return $$2.<Integer>map(p_248418_ -> p_248418_.value().useDuration()).orElse(0);
   }

   private Optional<Holder<Instrument>> getInstrument(ItemStack p_220135_) {
      Holder<Instrument> $$1 = p_220135_.get(DataComponents.INSTRUMENT);
      if ($$1 != null) {
         return Optional.of($$1);
      } else {
         Iterator<Holder<Instrument>> $$2 = BuiltInRegistries.INSTRUMENT.getTagOrEmpty(this.instruments).iterator();
         return $$2.hasNext() ? Optional.of($$2.next()) : Optional.empty();
      }
   }

   @Override
   public UseAnim getUseAnimation(ItemStack p_220133_) {
      return UseAnim.TOOT_HORN;
   }

   private static void play(Level p_220127_, Player p_220128_, Instrument p_220129_) {
      SoundEvent $$3 = p_220129_.soundEvent().value();
      float $$4 = p_220129_.range() / 16.0F;
      p_220127_.playSound(p_220128_, p_220128_, $$3, SoundSource.RECORDS, $$4, 1.0F);
      p_220127_.gameEvent(GameEvent.INSTRUMENT_PLAY, p_220128_.position(), GameEvent.Context.of(p_220128_));
   }
}
