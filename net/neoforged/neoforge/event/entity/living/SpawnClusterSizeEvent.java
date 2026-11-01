package net.neoforged.neoforge.event.entity.living;

import net.minecraft.world.entity.Mob;

public class SpawnClusterSizeEvent extends LivingEvent {
   private int size;

   public SpawnClusterSizeEvent(Mob entity) {
      super(entity);
      this.size = entity.getMaxSpawnClusterSize();
   }

   public int getSize() {
      return this.size;
   }

   public void setSize(int size) {
      this.size = size;
   }

   public Mob getEntity() {
      return (Mob)super.getEntity();
   }
}
