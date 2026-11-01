package net.neoforged.neoforge.common.crafting;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.NeoForgeMod;
import org.jspecify.annotations.Nullable;

public class BlockTagIngredient implements ICustomIngredient {
   public static final MapCodec<BlockTagIngredient> CODEC = TagKey.codec(Registries.BLOCK)
      .xmap(BlockTagIngredient::new, BlockTagIngredient::getTag)
      .fieldOf("tag");
   protected final TagKey<Block> tag;
   @Nullable
   protected HolderSet<Item> items;

   public BlockTagIngredient(TagKey<Block> tag) {
      this.tag = tag;
   }

   protected HolderSet<Item> dissolve() {
      if (this.items == null) {
         List<Holder<Item>> list = new ArrayList<>();

         for (Holder<Block> block : BuiltInRegistries.BLOCK.getTagOrEmpty(this.tag)) {
            Item item = block.value().asItem();
            if (item != Items.AIR) {
               list.add(item.builtInRegistryHolder());
            }
         }

         this.items = HolderSet.direct(list);
      }

      return this.items;
   }

   @Override
   public Stream<Holder<Item>> items() {
      return this.dissolve().stream();
   }

   @Override
   public boolean test(@Nullable ItemStack stack) {
      return stack == null ? false : this.dissolve().contains(stack.getItemHolder());
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
   public SlotDisplay display() {
      return new SlotDisplay.Composite(this.dissolve().stream().map(Ingredient::displayForSingleItem).toList());
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
