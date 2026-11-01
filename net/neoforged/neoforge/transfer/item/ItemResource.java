package net.neoforged.neoforge.transfer.item;

import com.mojang.serialization.Codec;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.resource.DataComponentHolderResource;

public final class ItemResource implements DataComponentHolderResource<Item> {
   public static final ItemResource EMPTY = new ItemResource(ItemStack.EMPTY);
   public static final Codec<ItemResource> CODEC = ItemStack.SINGLE_ITEM_CODEC.xmap(ItemResource::of, ItemResource::toStack);
   public static final Codec<ItemResource> OPTIONAL_CODEC = ExtraCodecs.optionalEmptyMap(CODEC)
      .xmap(optional -> optional.orElse(EMPTY), itemResource -> itemResource.isEmpty() ? Optional.empty() : Optional.of(itemResource));
   public static final StreamCodec<RegistryFriendlyByteBuf, ItemResource> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.holderRegistry(Registries.ITEM),
      ItemResource::getHolder,
      DataComponentPatch.STREAM_CODEC,
      ItemResource::getComponentsPatch,
      ItemResource::of
   );
   private final ItemStack innerStack;

   public static ItemResource of(ItemStack stack) {
      return !stack.isEmpty() && !stack.isComponentsPatchEmpty() ? new ItemResource(stack.copyWithCount(1)) : of(stack.getItem());
   }

   public static ItemResource of(ItemLike item) {
      Item value = item.asItem();
      return value == Items.AIR ? EMPTY : value.computeDefaultResource(i -> new ItemResource(new ItemStack(i)));
   }

   public static ItemResource of(ItemLike item, DataComponentPatch patch) {
      return of(item.asItem().builtInRegistryHolder(), patch);
   }

   public static ItemResource of(Holder<Item> holder) {
      return of(holder.value());
   }

   public static ItemResource of(Holder<Item> holder, DataComponentPatch patch) {
      return holder.value() != Items.AIR && !patch.isEmpty() ? new ItemResource(new ItemStack(holder, 1, patch)) : of(holder.value());
   }

   private ItemResource(ItemStack stack) {
      this.innerStack = stack;
   }

   public Item value() {
      return this.innerStack.getItem();
   }

   public Item getItem() {
      return this.value();
   }

   @Override
   public Holder<Item> getHolder() {
      return this.innerStack.getItemHolder();
   }

   @Override
   public boolean isEmpty() {
      return this.innerStack.isEmpty();
   }

   public boolean matches(ItemStack stack) {
      return ItemStack.isSameItemSameComponents(stack, this.innerStack);
   }

   public boolean is(ItemLike item) {
      return this.is(item.asItem());
   }

   public boolean test(Predicate<ItemStack> predicate) {
      return predicate.test(this.innerStack);
   }

   @Override
   public boolean isComponentsPatchEmpty() {
      return this.innerStack.isComponentsPatchEmpty();
   }

   public ItemResource withMergedPatch(DataComponentPatch patch) {
      if (!this.isEmpty() && !patch.isEmpty()) {
         ItemStack stack = this.innerStack.copy();
         stack.applyComponents(patch);
         return of(stack);
      } else {
         return this;
      }
   }

   public <D> ItemResource with(DataComponentType<D> type, D data) {
      if (this.isEmpty()) {
         return EMPTY;
      } else if (Objects.equals(this.get(type), data)) {
         return this;
      } else {
         ItemStack stack = this.innerStack.copy();
         stack.set(type, data);
         return of(stack);
      }
   }

   public <D> ItemResource with(Supplier<? extends DataComponentType<D>> type, D data) {
      return this.with((DataComponentType<D>)type.get(), data);
   }

   public ItemResource without(DataComponentType<?> type) {
      if (this.isEmpty()) {
         return EMPTY;
      } else if (this.get(type) == null) {
         return this;
      } else {
         ItemStack stack = this.innerStack.copy();
         stack.remove(type);
         return of(stack);
      }
   }

   public ItemResource without(Supplier<? extends DataComponentType<?>> type) {
      return this.without((DataComponentType<?>)type.get());
   }

   @Override
   public DataComponentMap getComponents() {
      return this.innerStack.immutableComponents();
   }

   @Override
   public DataComponentPatch getComponentsPatch() {
      return this.innerStack.getComponentsPatch();
   }

   public ItemStack toStack(int count) {
      TransferPreconditions.checkNonNegative(count);
      return count == 0 ? ItemStack.EMPTY : this.innerStack.copyWithCount(count);
   }

   public ItemStack toStack() {
      return this.innerStack.copyWithCount(1);
   }

   public int getMaxStackSize() {
      return this.innerStack.getMaxStackSize();
   }

   public Component getHoverName() {
      return this.innerStack.getHoverName();
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else if (obj != null && this.getClass() == obj.getClass()) {
         ItemResource other = (ItemResource)obj;
         return ItemStack.isSameItemSameComponents(this.innerStack, other.innerStack);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return ItemStack.hashItemAndComponents(this.innerStack);
   }

   @Override
   public String toString() {
      return this.value() + " [" + this.getComponentsPatch().size() + "]";
   }
}
