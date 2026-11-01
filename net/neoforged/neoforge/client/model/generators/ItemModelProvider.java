package net.neoforged.neoforge.client.model.generators;

import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public abstract class ItemModelProvider extends ModelProvider<ItemModelBuilder> {
   public ItemModelProvider(PackOutput output, String modid, ExistingFileHelper existingFileHelper) {
      super(output, modid, "item", ItemModelBuilder::new, existingFileHelper);
   }

   public ItemModelBuilder basicItem(Item item) {
      return this.basicItem(Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(item)));
   }

   public ItemModelBuilder basicItem(ResourceLocation item) {
      return this.getBuilder(item.toString())
         .parent(new ModelFile.UncheckedModelFile("item/generated"))
         .texture("layer0", ResourceLocation.fromNamespaceAndPath(item.getNamespace(), "item/" + item.getPath()));
   }

   public ItemModelBuilder handheldItem(Item item) {
      return this.handheldItem(Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(item)));
   }

   public ItemModelBuilder handheldItem(ResourceLocation item) {
      return this.getBuilder(item.toString())
         .parent(new ModelFile.UncheckedModelFile("item/handheld"))
         .texture("layer0", ResourceLocation.fromNamespaceAndPath(item.getNamespace(), "item/" + item.getPath()));
   }

   public ItemModelBuilder spawnEggItem(Item item) {
      return this.spawnEggItem(Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(item)));
   }

   public ItemModelBuilder spawnEggItem(ResourceLocation item) {
      return this.getBuilder(item.toString()).parent(new ModelFile.UncheckedModelFile("item/template_spawn_egg"));
   }

   public ItemModelBuilder simpleBlockItem(Block block) {
      return this.simpleBlockItem(Objects.requireNonNull(BuiltInRegistries.BLOCK.getKey(block)));
   }

   public ItemModelBuilder simpleBlockItem(ResourceLocation block) {
      return this.withExistingParent(block.toString(), ResourceLocation.fromNamespaceAndPath(block.getNamespace(), "block/" + block.getPath()));
   }

   @Override
   public String getName() {
      return "Item Models: " + this.modid;
   }
}
