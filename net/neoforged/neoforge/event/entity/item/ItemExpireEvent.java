package net.neoforged.neoforge.event.entity.item;

import net.minecraft.world.entity.item.ItemEntity;

public class ItemExpireEvent extends ItemEvent {
   private int extraLife = 0;

   public ItemExpireEvent(ItemEntity itemEntity) {
      super(itemEntity);
   }

   public int getExtraLife() {
      return this.extraLife;
   }

   public void setExtraLife(int extraLife) {
      this.extraLife = extraLife;
   }

   public void addExtraLife(int extraLife) {
      this.extraLife += extraLife;
   }
}
