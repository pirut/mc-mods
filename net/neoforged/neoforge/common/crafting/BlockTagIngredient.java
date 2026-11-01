package net.neoforged.neoforge.common.crafting;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForgeMod;
import org.jetbrains.annotations.Nullable;

public class BlockTagIngredient implements ICustomIngredient {
   public static final MapCodec<BlockTagIngredient> CODEC = TagKey.codec(Registries.BLOCK)
      .xmap(BlockTagIngredient::new, BlockTagIngredient::getTag)
      .fieldOf("tag");
   protected final TagKey<Block> tag;
   @Nullable
   protected ItemStack[] itemStacks;

   public BlockTagIngredient(TagKey<Block> tag) {
      this.tag = tag;
   }

   protected void dissolve() {
      if (this.itemStacks == null) {
         List<ItemStack> list = new ArrayList<>();

         for (Holder<Block> block : BuiltInRegistries.BLOCK.getTagOrEmpty(this.tag)) {
            ItemStack stack = new ItemStack(block.value());
            if (!stack.isEmpty()) {
               list.add(stack);
            }
         }

         if (list.isEmpty()) {
            ItemStack itemStack = new ItemStack(Blocks.BARRIER);
            itemStack.set(DataComponents.CUSTOM_NAME, Component.literal("Empty Tag: " + this.tag.location()));
            list.add(itemStack);
         }

         this.itemStacks = list.toArray(ItemStack[]::new);
      }
   }

   @Override
   public Stream<ItemStack> getItems() {
      this.dissolve();
      return Stream.of(this.itemStacks);
   }

   @Override
   public boolean test(@Nullable ItemStack stack) {
      if (stack == null) {
         return false;
      } else {
         this.dissolve();

         for (ItemStack itemStack : this.itemStacks) {
            if (itemStack.is(stack.getItem())) {
               return true;
            }
         }

         return false;
      }
   }

   public TagKey<Block> getTag() {
      return this.tag;
   }

   @Override
   public boolean isSimple() {
      return true;
   }

   @Override
   public IngredientType<?> getType() {
      return NeoForgeMod.BLOCK_TAG_INGREDIENT.get();
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else {
         return o instanceof BlockTagIngredient that ? this.tag.equals(that.tag) : false;
      }
   }

   @Override
   public int hashCode() {
      return this.tag.hashCode();
   }
}
