package net.minecraft.world.item.component;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public record ItemAttributeModifiers(List<ItemAttributeModifiers.Entry> modifiers, boolean showInTooltip) {
   public static final ItemAttributeModifiers EMPTY = new ItemAttributeModifiers(List.of(), true);
   private static final Codec<ItemAttributeModifiers> FULL_CODEC = RecordCodecBuilder.create(
      p_337947_ -> p_337947_.group(
            ItemAttributeModifiers.Entry.CODEC.listOf().fieldOf("modifiers").forGetter(ItemAttributeModifiers::modifiers),
            Codec.BOOL.optionalFieldOf("show_in_tooltip", true).forGetter(ItemAttributeModifiers::showInTooltip)
         )
         .apply(p_337947_, ItemAttributeModifiers::new)
   );
   public static final Codec<ItemAttributeModifiers> CODEC = Codec.withAlternative(
      FULL_CODEC, ItemAttributeModifiers.Entry.CODEC.listOf(), p_332621_ -> new ItemAttributeModifiers(p_332621_, true)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers> STREAM_CODEC = StreamCodec.composite(
      ItemAttributeModifiers.Entry.STREAM_CODEC.apply(ByteBufCodecs.list()),
      ItemAttributeModifiers::modifiers,
      ByteBufCodecs.BOOL,
      ItemAttributeModifiers::showInTooltip,
      ItemAttributeModifiers::new
   );
   public static final DecimalFormat ATTRIBUTE_MODIFIER_FORMAT = Util.make(
      new DecimalFormat("#.##"), p_331600_ -> p_331600_.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT))
   );

   public ItemAttributeModifiers withTooltip(boolean p_335777_) {
      return new ItemAttributeModifiers(this.modifiers, p_335777_);
   }

   public static ItemAttributeModifiers.Builder builder() {
      return new ItemAttributeModifiers.Builder();
   }

   public ItemAttributeModifiers withModifierAdded(Holder<Attribute> p_330266_, AttributeModifier p_331954_, EquipmentSlotGroup p_332175_) {
      com.google.common.collect.ImmutableList.Builder<ItemAttributeModifiers.Entry> $$3 = ImmutableList.builderWithExpectedSize(this.modifiers.size() + 1);

      for (ItemAttributeModifiers.Entry $$4 : this.modifiers) {
         if (!$$4.matches(p_330266_, p_331954_.id())) {
            $$3.add($$4);
         }
      }

      $$3.add(new ItemAttributeModifiers.Entry(p_330266_, p_331954_, p_332175_));
      return new ItemAttributeModifiers($$3.build(), this.showInTooltip);
   }

   public void forEach(EquipmentSlotGroup p_348576_, BiConsumer<Holder<Attribute>, AttributeModifier> p_348660_) {
      for (ItemAttributeModifiers.Entry $$2 : this.modifiers) {
         if ($$2.slot.equals(p_348576_)) {
            p_348660_.accept($$2.attribute, $$2.modifier);
         }
      }
   }

   public void forEach(EquipmentSlot p_332158_, BiConsumer<Holder<Attribute>, AttributeModifier> p_331684_) {
      for (ItemAttributeModifiers.Entry $$2 : this.modifiers) {
         if ($$2.slot.test(p_332158_)) {
            p_331684_.accept($$2.attribute, $$2.modifier);
         }
      }
   }

   public double compute(double p_330928_, EquipmentSlot p_330945_) {
      double $$2 = p_330928_;

      for (ItemAttributeModifiers.Entry $$3 : this.modifiers) {
         if ($$3.slot.test(p_330945_)) {
            double $$4 = $$3.modifier.amount();

            $$2 += switch ($$3.modifier.operation()) {
               case ADD_VALUE -> $$4;
               case ADD_MULTIPLIED_BASE -> $$4 * p_330928_;
               case ADD_MULTIPLIED_TOTAL -> $$4 * $$2;
            };
         }
      }

      return $$2;
   }

   public static class Builder {
      private final com.google.common.collect.ImmutableList.Builder<ItemAttributeModifiers.Entry> entries = ImmutableList.builder();

      Builder() {
      }

      public ItemAttributeModifiers.Builder add(Holder<Attribute> p_330324_, AttributeModifier p_331766_, EquipmentSlotGroup p_331205_) {
         this.entries.add(new ItemAttributeModifiers.Entry(p_330324_, p_331766_, p_331205_));
         return this;
      }

      public ItemAttributeModifiers build() {
         return new ItemAttributeModifiers(this.entries.build(), true);
      }
   }

   public record Entry(Holder<Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup slot) {
      public static final Codec<ItemAttributeModifiers.Entry> CODEC = RecordCodecBuilder.create(
         p_348388_ -> p_348388_.group(
               Attribute.CODEC.fieldOf("type").forGetter(ItemAttributeModifiers.Entry::attribute),
               AttributeModifier.MAP_CODEC.forGetter(ItemAttributeModifiers.Entry::modifier),
               EquipmentSlotGroup.CODEC.optionalFieldOf("slot", EquipmentSlotGroup.ANY).forGetter(ItemAttributeModifiers.Entry::slot)
            )
            .apply(p_348388_, ItemAttributeModifiers.Entry::new)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers.Entry> STREAM_CODEC = StreamCodec.composite(
         Attribute.STREAM_CODEC,
         ItemAttributeModifiers.Entry::attribute,
         AttributeModifier.STREAM_CODEC,
         ItemAttributeModifiers.Entry::modifier,
         EquipmentSlotGroup.STREAM_CODEC,
         ItemAttributeModifiers.Entry::slot,
         ItemAttributeModifiers.Entry::new
      );

      public boolean matches(Holder<Attribute> p_350582_, ResourceLocation p_350416_) {
         return p_350582_.equals(this.attribute) && this.modifier.is(p_350416_);
      }
   }
}
