package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public record ItemPredicate(Optional<HolderSet<Item>> items, MinMaxBounds.Ints count, DataComponentMatchers components) implements Predicate<ItemStack> {
   public static final Codec<ItemPredicate> CODEC = RecordCodecBuilder.create(
      p_468806_ -> p_468806_.group(
            RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("items").forGetter(ItemPredicate::items),
            MinMaxBounds.Ints.CODEC.optionalFieldOf("count", MinMaxBounds.Ints.ANY).forGetter(ItemPredicate::count),
            DataComponentMatchers.CODEC.forGetter(ItemPredicate::components)
         )
         .apply(p_468806_, ItemPredicate::new)
   );

   public boolean test(ItemStack p_466911_) {
      if (this.items.isPresent() && !p_466911_.is(this.items.get())) {
         return false;
      } else {
         return !this.count.matches(p_466911_.getCount()) ? false : this.components.test((DataComponentGetter)p_466911_);
      }
   }

   public static class Builder {
      private Optional<HolderSet<Item>> items = Optional.empty();
      private MinMaxBounds.Ints count = MinMaxBounds.Ints.ANY;
      private DataComponentMatchers components = DataComponentMatchers.ANY;

      public static ItemPredicate.Builder item() {
         return new ItemPredicate.Builder();
      }

      public ItemPredicate.Builder of(HolderGetter<Item> p_467524_, ItemLike... p_467813_) {
         this.items = Optional.of(HolderSet.direct(p_467518_ -> p_467518_.asItem().builtInRegistryHolder(), p_467813_));
         return this;
      }

      public ItemPredicate.Builder of(HolderGetter<Item> p_469349_, TagKey<Item> p_467601_) {
         this.items = Optional.of(p_469349_.getOrThrow(p_467601_));
         return this;
      }

      public ItemPredicate.Builder withCount(MinMaxBounds.Ints p_469081_) {
         this.count = p_469081_;
         return this;
      }

      public ItemPredicate.Builder withComponents(DataComponentMatchers p_469310_) {
         this.components = p_469310_;
         return this;
      }

      public ItemPredicate build() {
         return new ItemPredicate(this.items, this.count, this.components);
      }
   }
}
