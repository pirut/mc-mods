package net.neoforged.neoforge.registries;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

public class DeferredBlock<T extends Block> extends DeferredHolder<Block, T> implements ItemLike {
   public ItemStack toStack() {
      return this.toStack(1);
   }

   public ItemStack toStack(int count) {
      ItemStack stack = this.asItem().getDefaultInstance();
      if (stack.isEmpty()) {
         throw new IllegalStateException("Block does not have a corresponding item: " + this.key);
      } else {
         stack.setCount(count);
         return stack;
      }
   }

   public static <T extends Block> DeferredBlock<T> createBlock(Identifier key) {
      return createBlock(ResourceKey.create(Registries.BLOCK, key));
   }

   public static <T extends Block> DeferredBlock<T> createBlock(ResourceKey<Block> key) {
      return new DeferredBlock<>(key);
   }

   protected DeferredBlock(ResourceKey<Block> key) {
      super(key);
   }

   @Override
   public Item asItem() {
      return this.get().asItem();
   }
}
