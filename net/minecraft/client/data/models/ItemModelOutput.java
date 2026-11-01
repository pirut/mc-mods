package net.minecraft.client.data.models;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.world.item.Item;

@Environment(EnvType.CLIENT)
public interface ItemModelOutput {
   default void accept(Item item, ItemModel.Unbaked model) {
      this.accept(item, model, ClientItem.Properties.DEFAULT);
   }

   void accept(Item item, ItemModel.Unbaked model, ClientItem.Properties properties);

   void copy(Item item1, Item item2);
}
