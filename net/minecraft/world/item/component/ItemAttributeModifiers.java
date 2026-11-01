package net.minecraft.world.item.component;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.apache.commons.lang3.function.TriConsumer;
import org.jspecify.annotations.Nullable;

public record ItemAttributeModifiers(List<ItemAttributeModifiers.Entry> modifiers) {
   public static final ItemAttributeModifiers EMPTY = new ItemAttributeModifiers(List.of());
   public static final Codec<ItemAttributeModifiers> CODEC = ItemAttributeModifiers.Entry.CODEC
      .listOf()
      .xmap(ItemAttributeModifiers::new, ItemAttributeModifiers::modifiers);
   public static final StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers> STREAM_CODEC = StreamCodec.composite(
      ItemAttributeModifiers.Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), ItemAttributeModifiers::modifiers, ItemAttributeModifiers::new
   );
   public static final DecimalFormat ATTRIBUTE_MODIFIER_FORMAT = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT));

   public static ItemAttributeModifiers.Builder builder() {
      return new ItemAttributeModifiers.Builder();
   }

   public ItemAttributeModifiers withModifierAdded(Holder<Attribute> p_330266_, AttributeModifier p_331954_, EquipmentSlotGroup p_332175_) {
      com.google.common.collect.ImmutableList.Builder<ItemAttributeModifiers.Entry> builder = ImmutableList.builderWithExpectedSize(this.modifiers.size() + 1);

      for (ItemAttributeModifiers.Entry itemattributemodifiers$entry : this.modifiers) {
         if (!itemattributemodifiers$entry.matches(p_330266_, p_331954_.id())) {
            builder.add(itemattributemodifiers$entry);
         }
      }

      builder.add(new ItemAttributeModifiers.Entry(p_330266_, p_331954_, p_332175_));
      return new ItemAttributeModifiers(builder.build());
   }

   public void forEach(EquipmentSlotGroup p_416672_, TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> p_416076_) {
      for (ItemAttributeModifiers.Entry itemattributemodifiers$entry : this.modifiers) {
         if (itemattributemodifiers$entry.slot.equals(p_416672_)) {
            p_416076_.accept(itemattributemodifiers$entry.attribute, itemattributemodifiers$entry.modifier, itemattributemodifiers$entry.display);
         }
      }
   }

   public void forEach(EquipmentSlotGroup p_348576_, BiConsumer<Holder<Attribute>, AttributeModifier> p_348660_) {
      for (ItemAttributeModifiers.Entry itemattributemodifiers$entry : this.modifiers) {
         if (itemattributemodifiers$entry.slot.equals(p_348576_)) {
            p_348660_.accept(itemattributemodifiers$entry.attribute, itemattributemodifiers$entry.modifier);
         }
      }
   }

   public void forEach(EquipmentSlot p_332158_, BiConsumer<Holder<Attribute>, AttributeModifier> p_331684_) {
      for (ItemAttributeModifiers.Entry itemattributemodifiers$entry : this.modifiers) {
         if (itemattributemodifiers$entry.slot.test(p_332158_)) {
            p_331684_.accept(itemattributemodifiers$entry.attribute, itemattributemodifiers$entry.modifier);
         }
      }
   }

   public double compute(Holder<Attribute> p_481256_, double p_330928_, EquipmentSlot p_330945_) {
      double d0 = p_330928_;

      for (ItemAttributeModifiers.Entry itemattributemodifiers$entry : this.modifiers) {
         if (itemattributemodifiers$entry.slot.test(p_330945_) && itemattributemodifiers$entry.attribute == p_481256_) {
            double d1 = itemattributemodifiers$entry.modifier.amount();

            d0 += switch (itemattributemodifiers$entry.modifier.operation()) {
               case ADD_VALUE -> d1;
               case ADD_MULTIPLIED_BASE -> d1 * p_330928_;
               case ADD_MULTIPLIED_TOTAL -> d1 * d0;
            };
         }
      }

      return d0;
   }

   public static class Builder {
      private final com.google.common.collect.ImmutableList.Builder<ItemAttributeModifiers.Entry> entries = ImmutableList.builder();

      Builder() {
      }

      public ItemAttributeModifiers.Builder add(Holder<Attribute> p_330324_, AttributeModifier p_331766_, EquipmentSlotGroup p_331205_) {
         this.entries.add(new ItemAttributeModifiers.Entry(p_330324_, p_331766_, p_331205_));
         return this;
      }

      public ItemAttributeModifiers.Builder add(
         Holder<Attribute> p_416496_, AttributeModifier p_416176_, EquipmentSlotGroup p_415827_, ItemAttributeModifiers.Display p_416422_
      ) {
         this.entries.add(new ItemAttributeModifiers.Entry(p_416496_, p_416176_, p_415827_, p_416422_));
         return this;
      }

      public ItemAttributeModifiers build() {
         return new ItemAttributeModifiers(this.entries.build());
      }
   }

   public interface Display {
      Codec<ItemAttributeModifiers.Display> CODEC = ItemAttributeModifiers.Display.Type.CODEC
         .dispatch("type", ItemAttributeModifiers.Display::type, p_416469_ -> p_416469_.codec);
      StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers.Display> STREAM_CODEC = ItemAttributeModifiers.Display.Type.STREAM_CODEC
         .<RegistryFriendlyByteBuf>cast()
         .dispatch(ItemAttributeModifiers.Display::type, ItemAttributeModifiers.Display.Type::streamCodec);

      static ItemAttributeModifiers.Display attributeModifiers() {
         return ItemAttributeModifiers.Display.Default.INSTANCE;
      }

      static ItemAttributeModifiers.Display hidden() {
         return ItemAttributeModifiers.Display.Hidden.INSTANCE;
      }

      static ItemAttributeModifiers.Display override(Component p_415903_) {
         return new ItemAttributeModifiers.Display.OverrideText(p_415903_);
      }

      ItemAttributeModifiers.Display.Type type();

      void apply(Consumer<Component> var1, @Nullable Player var2, Holder<Attribute> var3, AttributeModifier var4);

      public record Default() implements ItemAttributeModifiers.Display {
         static final ItemAttributeModifiers.Display.Default INSTANCE = new ItemAttributeModifiers.Display.Default();
         static final MapCodec<ItemAttributeModifiers.Display.Default> CODEC = MapCodec.unit(INSTANCE);
         static final StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers.Display.Default> STREAM_CODEC = StreamCodec.unit(INSTANCE);

         @Override
         public ItemAttributeModifiers.Display.Type type() {
            return ItemAttributeModifiers.Display.Type.DEFAULT;
         }

         @Override
         public void apply(Consumer<Component> p_415634_, @Nullable Player p_415547_, Holder<Attribute> p_416596_, AttributeModifier p_415942_) {
            double d0 = p_415942_.amount();
            boolean flag = false;
            if (p_415547_ != null) {
               if (p_415942_.is(Item.BASE_ATTACK_DAMAGE_ID)) {
                  d0 += p_415547_.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
                  flag = true;
               } else if (p_415942_.is(Item.BASE_ATTACK_SPEED_ID)) {
                  d0 += p_415547_.getAttributeBaseValue(Attributes.ATTACK_SPEED);
                  flag = true;
               }
            }

            double d1;
            if (p_415942_.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE
               || p_415942_.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
               d1 = d0 * 100.0;
            } else if (p_416596_.is(Attributes.KNOCKBACK_RESISTANCE)) {
               d1 = d0 * 10.0;
            } else {
               d1 = d0;
            }

            if (flag) {
               p_415634_.accept(
                  CommonComponents.space()
                     .append(
                        Component.translatable(
                           "attribute.modifier.equals." + p_415942_.operation().id(),
                           ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(d1),
                           Component.translatable(p_416596_.value().getDescriptionId())
                        )
                     )
                     .withStyle(ChatFormatting.DARK_GREEN)
               );
            } else if (d0 > 0.0) {
               p_415634_.accept(
                  Component.translatable(
                        "attribute.modifier.plus." + p_415942_.operation().id(),
                        ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(d1),
                        Component.translatable(p_416596_.value().getDescriptionId())
                     )
                     .withStyle(p_416596_.value().getStyle(true))
               );
            } else if (d0 < 0.0) {
               p_415634_.accept(
                  Component.translatable(
                        "attribute.modifier.take." + p_415942_.operation().id(),
                        ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(-d1),
                        Component.translatable(p_416596_.value().getDescriptionId())
                     )
                     .withStyle(p_416596_.value().getStyle(false))
               );
            }
         }
      }

      public record Hidden() implements ItemAttributeModifiers.Display {
         static final ItemAttributeModifiers.Display.Hidden INSTANCE = new ItemAttributeModifiers.Display.Hidden();
         static final MapCodec<ItemAttributeModifiers.Display.Hidden> CODEC = MapCodec.unit(INSTANCE);
         static final StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers.Display.Hidden> STREAM_CODEC = StreamCodec.unit(INSTANCE);

         @Override
         public ItemAttributeModifiers.Display.Type type() {
            return ItemAttributeModifiers.Display.Type.HIDDEN;
         }

         @Override
         public void apply(Consumer<Component> p_416397_, @Nullable Player p_415921_, Holder<Attribute> p_415720_, AttributeModifier p_416314_) {
         }
      }

      public record OverrideText(Component component) implements ItemAttributeModifiers.Display {
         static final MapCodec<ItemAttributeModifiers.Display.OverrideText> CODEC = RecordCodecBuilder.mapCodec(
            p_416311_ -> p_416311_.group(ComponentSerialization.CODEC.fieldOf("value").forGetter(ItemAttributeModifiers.Display.OverrideText::component))
               .apply(p_416311_, ItemAttributeModifiers.Display.OverrideText::new)
         );
         static final StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers.Display.OverrideText> STREAM_CODEC = StreamCodec.composite(
            ComponentSerialization.STREAM_CODEC, ItemAttributeModifiers.Display.OverrideText::component, ItemAttributeModifiers.Display.OverrideText::new
         );

         @Override
         public ItemAttributeModifiers.Display.Type type() {
            return ItemAttributeModifiers.Display.Type.OVERRIDE;
         }

         @Override
         public void apply(Consumer<Component> p_416508_, @Nullable Player p_416569_, Holder<Attribute> p_416290_, AttributeModifier p_416417_) {
            p_416508_.accept(this.component);
         }
      }

      public static enum Type implements StringRepresentable {
         DEFAULT("default", 0, ItemAttributeModifiers.Display.Default.CODEC, ItemAttributeModifiers.Display.Default.STREAM_CODEC),
         HIDDEN("hidden", 1, ItemAttributeModifiers.Display.Hidden.CODEC, ItemAttributeModifiers.Display.Hidden.STREAM_CODEC),
         OVERRIDE("override", 2, ItemAttributeModifiers.Display.OverrideText.CODEC, ItemAttributeModifiers.Display.OverrideText.STREAM_CODEC);

         static final Codec<ItemAttributeModifiers.Display.Type> CODEC = StringRepresentable.fromEnum(ItemAttributeModifiers.Display.Type::values);
         private static final IntFunction<ItemAttributeModifiers.Display.Type> BY_ID = ByIdMap.continuous(
            ItemAttributeModifiers.Display.Type::id, values(), ByIdMap.OutOfBoundsStrategy.ZERO
         );
         static final StreamCodec<ByteBuf, ItemAttributeModifiers.Display.Type> STREAM_CODEC = ByteBufCodecs.idMapper(
            BY_ID, ItemAttributeModifiers.Display.Type::id
         );
         private final String name;
         private final int id;
         final MapCodec<? extends ItemAttributeModifiers.Display> codec;
         private final StreamCodec<RegistryFriendlyByteBuf, ? extends ItemAttributeModifiers.Display> streamCodec;

         private Type(
            String p_415852_,
            int p_416049_,
            MapCodec<? extends ItemAttributeModifiers.Display> p_416438_,
            StreamCodec<RegistryFriendlyByteBuf, ? extends ItemAttributeModifiers.Display> p_416142_
         ) {
            this.name = p_415852_;
            this.id = p_416049_;
            this.codec = p_416438_;
            this.streamCodec = p_416142_;
         }

         @Override
         public String getSerializedName() {
            return this.name;
         }

         private int id() {
            return this.id;
         }

         private StreamCodec<RegistryFriendlyByteBuf, ? extends ItemAttributeModifiers.Display> streamCodec() {
            return this.streamCodec;
         }
      }
   }

   public record Entry(Holder<Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup slot, ItemAttributeModifiers.Display display) {
      public static final Codec<ItemAttributeModifiers.Entry> CODEC = RecordCodecBuilder.create(
         p_419448_ -> p_419448_.group(
               Attribute.CODEC.fieldOf("type").forGetter(ItemAttributeModifiers.Entry::attribute),
               AttributeModifier.MAP_CODEC.forGetter(ItemAttributeModifiers.Entry::modifier),
               EquipmentSlotGroup.CODEC.optionalFieldOf("slot", EquipmentSlotGroup.ANY).forGetter(ItemAttributeModifiers.Entry::slot),
               ItemAttributeModifiers.Display.CODEC
                  .optionalFieldOf("display", ItemAttributeModifiers.Display.Default.INSTANCE)
                  .forGetter(ItemAttributeModifiers.Entry::display)
            )
            .apply(p_419448_, ItemAttributeModifiers.Entry::new)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers.Entry> STREAM_CODEC = StreamCodec.composite(
         Attribute.STREAM_CODEC,
         ItemAttributeModifiers.Entry::attribute,
         AttributeModifier.STREAM_CODEC,
         ItemAttributeModifiers.Entry::modifier,
         EquipmentSlotGroup.STREAM_CODEC,
         ItemAttributeModifiers.Entry::slot,
         ItemAttributeModifiers.Display.STREAM_CODEC,
         ItemAttributeModifiers.Entry::display,
         ItemAttributeModifiers.Entry::new
      );

      public Entry(Holder<Attribute> p_330366_, AttributeModifier p_332188_, EquipmentSlotGroup p_331685_) {
         this(p_330366_, p_332188_, p_331685_, ItemAttributeModifiers.Display.attributeModifiers());
      }

      public boolean matches(Holder<Attribute> p_350582_, Identifier p_469293_) {
         return p_350582_.equals(this.attribute) && this.modifier.is(p_469293_);
      }
   }
}
