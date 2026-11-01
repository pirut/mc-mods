package net.neoforged.neoforge.transfer.resource;

import net.neoforged.neoforge.transfer.ResourceHandlerUtil;

public record ResourceStack<T extends Resource>(T resource, int amount) {
   public boolean isEmpty() {
      return ResourceHandlerUtil.isEmpty(this.resource(), this.amount());
   }

   @Override
   public String toString() {
      return this.amount + "x " + this.resource;
   }
}
