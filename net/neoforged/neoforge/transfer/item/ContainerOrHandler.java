package net.neoforged.neoforge.transfer.item;

import net.minecraft.world.Container;
import net.neoforged.neoforge.transfer.ResourceHandler;
import org.jspecify.annotations.Nullable;

public record ContainerOrHandler(@Nullable Container container, @Nullable ResourceHandler<ItemResource> itemHandler) {
   public static final ContainerOrHandler EMPTY = new ContainerOrHandler(null, null);

   public ContainerOrHandler(@Nullable Container container, @Nullable ResourceHandler<ItemResource> itemHandler) {
      if (container != null && itemHandler != null) {
         throw new IllegalArgumentException("Cannot have both a container and an item handler.");
      } else {
         this.container = container;
         this.itemHandler = itemHandler;
      }
   }

   public boolean isEmpty() {
      return this.container == null && this.itemHandler == null;
   }
}
