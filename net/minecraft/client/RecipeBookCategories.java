package net.minecraft.client;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.common.asm.enumextension.ExtensionInfo;
import net.neoforged.fml.common.asm.enumextension.IExtensibleEnum;
import net.neoforged.fml.common.asm.enumextension.ReservedConstructor;
import net.neoforged.neoforge.client.RecipeBookManager;
import net.neoforged.neoforge.common.util.Lazy;

@OnlyIn(Dist.CLIENT)
public enum RecipeBookCategories implements IExtensibleEnum {
   CRAFTING_SEARCH(new ItemStack(Items.COMPASS)),
   CRAFTING_BUILDING_BLOCKS(new ItemStack(Blocks.BRICKS)),
   CRAFTING_REDSTONE(new ItemStack(Items.REDSTONE)),
   CRAFTING_EQUIPMENT(new ItemStack(Items.IRON_AXE), new ItemStack(Items.GOLDEN_SWORD)),
   CRAFTING_MISC(new ItemStack(Items.LAVA_BUCKET), new ItemStack(Items.APPLE)),
   FURNACE_SEARCH(new ItemStack(Items.COMPASS)),
   FURNACE_FOOD(new ItemStack(Items.PORKCHOP)),
   FURNACE_BLOCKS(new ItemStack(Blocks.STONE)),
   FURNACE_MISC(new ItemStack(Items.LAVA_BUCKET), new ItemStack(Items.EMERALD)),
   BLAST_FURNACE_SEARCH(new ItemStack(Items.COMPASS)),
   BLAST_FURNACE_BLOCKS(new ItemStack(Blocks.REDSTONE_ORE)),
   BLAST_FURNACE_MISC(new ItemStack(Items.IRON_SHOVEL), new ItemStack(Items.GOLDEN_LEGGINGS)),
   SMOKER_SEARCH(new ItemStack(Items.COMPASS)),
   SMOKER_FOOD(new ItemStack(Items.PORKCHOP)),
   STONECUTTER(new ItemStack(Items.CHISELED_STONE_BRICKS)),
   SMITHING(new ItemStack(Items.NETHERITE_CHESTPLATE)),
   CAMPFIRE(new ItemStack(Items.PORKCHOP)),
   UNKNOWN(new ItemStack(Items.BARRIER));

   public static final List<RecipeBookCategories> SMOKER_CATEGORIES = ImmutableList.of(SMOKER_SEARCH, SMOKER_FOOD);
   public static final List<RecipeBookCategories> BLAST_FURNACE_CATEGORIES = ImmutableList.of(BLAST_FURNACE_SEARCH, BLAST_FURNACE_BLOCKS, BLAST_FURNACE_MISC);
   public static final List<RecipeBookCategories> FURNACE_CATEGORIES = ImmutableList.of(FURNACE_SEARCH, FURNACE_FOOD, FURNACE_BLOCKS, FURNACE_MISC);
   public static final List<RecipeBookCategories> CRAFTING_CATEGORIES = ImmutableList.of(
      CRAFTING_SEARCH, CRAFTING_EQUIPMENT, CRAFTING_BUILDING_BLOCKS, CRAFTING_MISC, CRAFTING_REDSTONE
   );
   public static final Map<RecipeBookCategories, List<RecipeBookCategories>> AGGREGATE_CATEGORIES = RecipeBookManager.getAggregateCategories();
   @Deprecated
   private final List<ItemStack> itemIcons;
   private final Supplier<List<ItemStack>> itemIconsSupplier;

   @ReservedConstructor
   private RecipeBookCategories(ItemStack... p_92267_) {
      this.itemIcons = ImmutableList.copyOf(p_92267_);
      this.itemIconsSupplier = () -> this.itemIcons;
   }

   private RecipeBookCategories(Supplier<List<ItemStack>> itemIconsSupplier) {
      this.itemIcons = List.of();
      this.itemIconsSupplier = Lazy.of(itemIconsSupplier);
   }

   public static List<RecipeBookCategories> getCategories(RecipeBookType p_92270_) {
      return switch (p_92270_) {
         case CRAFTING -> CRAFTING_CATEGORIES;
         case FURNACE -> FURNACE_CATEGORIES;
         case BLAST_FURNACE -> BLAST_FURNACE_CATEGORIES;
         case SMOKER -> SMOKER_CATEGORIES;
         default -> RecipeBookManager.getCustomCategoriesOrEmpty(p_92270_);
      };
   }

   public List<ItemStack> getIconItems() {
      return this.itemIconsSupplier.get();
   }

   public static ExtensionInfo getExtensionInfo() {
      return ExtensionInfo.nonExtended(RecipeBookCategories.class);
   }
}
