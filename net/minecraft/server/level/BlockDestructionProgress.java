package net.minecraft.server.level;

import net.minecraft.core.BlockPos;

public class BlockDestructionProgress implements Comparable<BlockDestructionProgress> {
   private final int id;
   private final BlockPos pos;
   private int progress;
   private int updatedRenderTick;

   public BlockDestructionProgress(int id, BlockPos pos) {
      this.id = id;
      this.pos = pos;
   }

   public int getId() {
      return this.id;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public void setProgress(int damage) {
      if (damage > 10) {
         damage = 10;
      }

      this.progress = damage;
   }

   public int getProgress() {
      return this.progress;
   }

   public void updateTick(int createdAtCloudUpdateTick) {
      this.updatedRenderTick = createdAtCloudUpdateTick;
   }

   public int getUpdatedRenderTick() {
      return this.updatedRenderTick;
   }

   @Override
   public boolean equals(Object object) {
      if (this == object) {
         return true;
      } else if (object != null && this.getClass() == object.getClass()) {
         BlockDestructionProgress blockDestructionProgress = (BlockDestructionProgress)object;
         return this.id == blockDestructionProgress.id;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Integer.hashCode(this.id);
   }

   public int compareTo(BlockDestructionProgress other) {
      return this.progress != other.progress ? Integer.compare(this.progress, other.progress) : Integer.compare(this.id, other.id);
   }
}
