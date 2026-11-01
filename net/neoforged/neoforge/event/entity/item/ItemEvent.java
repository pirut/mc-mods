package net.neoforged.neoforge.event.entity.item;

import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.neoforge.event.entity.EntityEvent;

public abstract class ItemEvent extends EntityEvent {
   private final ItemEntity itemEntity;

   public ItemEvent(ItemEntity itemEntity) {
      super(itemEntity);
      this.itemEntity = itemEntity;
   }

   public ItemEntity getEntity() {
      return this.itemEntity;
   }
}
