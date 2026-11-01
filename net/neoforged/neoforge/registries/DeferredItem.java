package net.neoforged.neoforge.registries;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class DeferredItem<T extends Item> extends DeferredHolder<Item, T> implements ItemLike {
   public ItemStack toStack() {
      return this.toStack(1);
   }

   public ItemStack toStack(int count) {
      ItemStack stack = this.asItem().getDefaultInstance();
      if (stack.isEmpty()) {
         throw new IllegalStateException("Obtained empty item stack; incorrect getDefaultInstance() call?");
      } else {
         stack.setCount(count);
         return stack;
      }
   }

   public static <T extends Item> DeferredItem<T> createItem(ResourceLocation key) {
      return createItem(ResourceKey.create(Registries.ITEM, key));
   }

   public static <T extends Item> DeferredItem<T> createItem(ResourceKey<Item> key) {
      return new DeferredItem<>(key);
   }

   protected DeferredItem(ResourceKey<Item> key) {
      super(key);
   }

   @Override
   public Item asItem() {
      return this.get();
   }
}
