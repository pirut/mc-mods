package net.minecraft.world.item.crafting;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparators;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.crafting.CraftingHelper;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.util.NeoForgeExtraCodecs;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class Ingredient implements Predicate<ItemStack> {
   public static final Ingredient EMPTY = new Ingredient(Stream.empty());
   public static final StreamCodec<RegistryFriendlyByteBuf, Ingredient> CONTENTS_STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, Ingredient>() {
      private static final StreamCodec<RegistryFriendlyByteBuf, ICustomIngredient> CUSTOM_INGREDIENT_CODEC = ByteBufCodecs.registry(
            NeoForgeRegistries.Keys.INGREDIENT_TYPES
         )
         .dispatch(c -> c.getType(), t -> (StreamCodec<? super RegistryFriendlyByteBuf, ? extends ICustomIngredient>)t.streamCodec());

      public void encode(RegistryFriendlyByteBuf buf, Ingredient ingredient) {
         if (ingredient.isSimple()) {
            ItemStack.LIST_STREAM_CODEC.encode(buf, Arrays.asList(ingredient.getItems()));
         } else {
            buf.writeVarInt(-1);
            CUSTOM_INGREDIENT_CODEC.encode(buf, ingredient.customIngredient);
         }
      }

      public Ingredient decode(RegistryFriendlyByteBuf buf) {
         int size = buf.readVarInt();
         return size == -1
            ? new Ingredient(CUSTOM_INGREDIENT_CODEC.decode(buf))
            : Ingredient.fromValues(Stream.<ItemStack>generate(() -> ItemStack.STREAM_CODEC.decode(buf)).limit(size).map(Ingredient.ItemValue::new));
      }
   };
   private final Ingredient.Value[] values;
   @Nullable
   private ItemStack[] itemStacks;
   @Nullable
   private IntList stackingIds;
   @Nullable
   private ICustomIngredient customIngredient = null;
   public static final Codec<Ingredient> CODEC = CraftingHelper.makeIngredientCodec(true);
   public static final Codec<Ingredient> CODEC_NONEMPTY = CraftingHelper.makeIngredientCodec(false);
   public static final MapCodec<Ingredient> MAP_CODEC_NONEMPTY = CraftingHelper.makeIngredientMapCodec();
   public static final Codec<List<Ingredient>> LIST_CODEC = MAP_CODEC_NONEMPTY.codec().listOf();
   public static final Codec<List<Ingredient>> LIST_CODEC_NONEMPTY = LIST_CODEC.validate(
      list -> list.isEmpty() ? DataResult.error(() -> "Item array cannot be empty, at least one item must be defined") : DataResult.success(list)
   );

   private Ingredient(Stream<? extends Ingredient.Value> p_43907_) {
      this.values = p_43907_.toArray(Ingredient.Value[]::new);
   }

   private Ingredient(Ingredient.Value[] p_301044_) {
      this.values = p_301044_;
   }

   public Ingredient(ICustomIngredient customIngredient) {
      this(new Ingredient.Value[0]);
      this.customIngredient = customIngredient;
   }

   public ItemStack[] getItems() {
      if (this.itemStacks == null) {
         Stream<ItemStack> stream = this.customIngredient == null
            ? Arrays.stream(this.values).flatMap(value -> value.getItems().stream())
            : this.customIngredient.getItems();
         this.itemStacks = stream.collect(Collectors.toCollection(ItemStackLinkedSet::createTypeAndComponentsSet)).toArray(ItemStack[]::new);
      }

      return this.itemStacks;
   }

   public boolean test(@Nullable ItemStack p_43914_) {
      if (p_43914_ == null) {
         return false;
      } else if (this.customIngredient != null) {
         return this.customIngredient.test(p_43914_);
      } else if (this.isEmpty()) {
         return p_43914_.isEmpty();
      } else {
         for (ItemStack itemstack : this.getItems()) {
            if (itemstack.is(p_43914_.getItem())) {
               return true;
            }
         }

         return false;
      }
   }

   public IntList getStackingIds() {
      if (this.stackingIds == null) {
         ItemStack[] aitemstack = this.getItems();
         this.stackingIds = new IntArrayList(aitemstack.length);

         for (ItemStack itemstack : aitemstack) {
            this.stackingIds.add(StackedContents.getStackingIndex(itemstack));
         }

         this.stackingIds.sort(IntComparators.NATURAL_COMPARATOR);
      }

      return this.stackingIds;
   }

   public boolean isEmpty() {
      return this.values.length == 0 && !this.isCustom();
   }

   public boolean hasNoItems() {
      ItemStack[] items = this.getItems();
      if (items.length == 0) {
         return true;
      } else if (items.length != 1) {
         return false;
      } else {
         ItemStack item = items[0];
         return item.getItem() == Items.BARRIER && item.getHoverName() instanceof MutableComponent hoverName && hoverName.getString().startsWith("Empty Tag: ");
      }
   }

   @Override
   public boolean equals(Object p_301003_) {
      return p_301003_ instanceof Ingredient ingredient
         ? Objects.equals(this.customIngredient, ingredient.customIngredient) && Arrays.equals(this.values, ingredient.values)
         : false;
   }

   @Override
   public int hashCode() {
      return this.customIngredient != null ? this.customIngredient.hashCode() : Arrays.hashCode((Object[])this.values);
   }

   public Ingredient.Value[] getValues() {
      if (this.isCustom()) {
         throw new IllegalStateException("Cannot retrieve values from custom ingredient!");
      } else {
         return this.values;
      }
   }

   public boolean isSimple() {
      return this.customIngredient == null || this.customIngredient.isSimple();
   }

   @Nullable
   public ICustomIngredient getCustomIngredient() {
      return this.customIngredient;
   }

   public boolean isCustom() {
      return this.customIngredient != null;
   }

   public static Ingredient fromValues(Stream<? extends Ingredient.Value> p_43939_) {
      Ingredient ingredient = new Ingredient(p_43939_);
      return ingredient.isEmpty() ? EMPTY : ingredient;
   }

   public static Ingredient of() {
      return EMPTY;
   }

   public static Ingredient of(ItemLike... p_43930_) {
      return of(Arrays.stream(p_43930_).map(ItemStack::new));
   }

   public static Ingredient of(ItemStack... p_43928_) {
      return of(Arrays.stream(p_43928_));
   }

   public static Ingredient of(Stream<ItemStack> p_43922_) {
      return fromValues(p_43922_.filter(p_43944_ -> !p_43944_.isEmpty()).map(Ingredient.ItemValue::new));
   }

   public static Ingredient of(TagKey<Item> p_204133_) {
      return fromValues(Stream.of(new Ingredient.TagValue(p_204133_)));
   }

   @Deprecated
   private static Codec<Ingredient> codec(boolean p_301074_) {
      Codec<Ingredient.Value[]> codec = Codec.list(Ingredient.Value.CODEC)
         .comapFlatMap(
            p_300810_ -> !p_301074_ && p_300810_.size() < 1
               ? DataResult.error(() -> "Item array cannot be empty, at least one item must be defined")
               : DataResult.success(p_300810_.toArray(new Ingredient.Value[0])),
            List::of
         );
      return Codec.either(codec, Ingredient.Value.CODEC)
         .flatComapMap(
            p_300805_ -> (Ingredient)p_300805_.map(Ingredient::new, p_300806_ -> new Ingredient(new Ingredient.Value[]{p_300806_})),
            p_300808_ -> {
               if (p_300808_.values.length == 1) {
                  return DataResult.success(Either.right(p_300808_.values[0]));
               } else {
                  return p_300808_.values.length == 0 && !p_301074_
                     ? DataResult.error(() -> "Item array cannot be empty, at least one item must be defined")
                     : DataResult.success(Either.left(p_300808_.values));
               }
            }
         );
   }

   public record ItemValue(ItemStack item) implements Ingredient.Value {
      static final MapCodec<Ingredient.ItemValue> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_330109_ -> p_330109_.group(ItemStack.SIMPLE_ITEM_CODEC.fieldOf("item").forGetter(p_300919_ -> p_300919_.item))
            .apply(p_330109_, Ingredient.ItemValue::new)
      );
      static final Codec<Ingredient.ItemValue> CODEC = MAP_CODEC.codec();

      @Override
      public boolean equals(Object p_301316_) {
         return p_301316_ instanceof Ingredient.ItemValue ingredient$itemvalue
            ? ingredient$itemvalue.item.getItem().equals(this.item.getItem()) && ingredient$itemvalue.item.getCount() == this.item.getCount()
            : false;
      }

      @Override
      public int hashCode() {
         return 31 * this.item.getItem().hashCode() + this.item.getCount();
      }

      @Override
      public Collection<ItemStack> getItems() {
         return Collections.singleton(this.item);
      }
   }

   public record TagValue(TagKey<Item> tag) implements Ingredient.Value {
      static final MapCodec<Ingredient.TagValue> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_301118_ -> p_301118_.group(TagKey.codec(Registries.ITEM).fieldOf("tag").forGetter(p_301154_ -> p_301154_.tag))
            .apply(p_301118_, Ingredient.TagValue::new)
      );
      static final Codec<Ingredient.TagValue> CODEC = MAP_CODEC.codec();

      @Override
      public boolean equals(Object p_301162_) {
         return p_301162_ instanceof Ingredient.TagValue ingredient$tagvalue ? ingredient$tagvalue.tag.location().equals(this.tag.location()) : false;
      }

      @Override
      public Collection<ItemStack> getItems() {
         List<ItemStack> list = Lists.newArrayList();

         for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(this.tag)) {
            list.add(new ItemStack(holder));
         }

         if (list.isEmpty()) {
            ItemStack itemStack = new ItemStack(Blocks.BARRIER);
            itemStack.set(DataComponents.CUSTOM_NAME, Component.literal("Empty Tag: " + this.tag.location()));
            list.add(itemStack);
         }

         return list;
      }
   }

   public interface Value {
      MapCodec<Ingredient.Value> MAP_CODEC = NeoForgeExtraCodecs.xor(Ingredient.ItemValue.MAP_CODEC, Ingredient.TagValue.MAP_CODEC)
         .xmap(p_300956_ -> (Ingredient.Value)p_300956_.map(p_300932_ -> p_300932_, p_301313_ -> p_301313_), p_301304_ -> {
            if (p_301304_ instanceof Ingredient.TagValue ingredient$tagvalue) {
               return Either.right(ingredient$tagvalue);
            } else if (p_301304_ instanceof Ingredient.ItemValue ingredient$itemvalue) {
               return Either.left(ingredient$itemvalue);
            } else {
               throw new UnsupportedOperationException("This is neither an item value nor a tag value.");
            }
         });
      Codec<Ingredient.Value> CODEC = MAP_CODEC.codec();

      Collection<ItemStack> getItems();
   }
}
