package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import java.util.function.Consumer;
import net.minecraft.world.level.ChunkPos;

public interface ChunkTrackingView {
   ChunkTrackingView EMPTY = new ChunkTrackingView() {
      @Override
      public boolean contains(int x, int z, boolean includeOuterChunksAdjacentToViewBorder) {
         return false;
      }

      @Override
      public void forEach(Consumer<ChunkPos> action) {
      }
   };

   static ChunkTrackingView of(ChunkPos center, int viewDistance) {
      return new ChunkTrackingView.Positioned(center, viewDistance);
   }

   static void difference(
      ChunkTrackingView oldChunkTrackingView, ChunkTrackingView newChunkTrackingView, Consumer<ChunkPos> chunkMarker, Consumer<ChunkPos> chunkDropper
   ) {
      if (!oldChunkTrackingView.equals(newChunkTrackingView)) {
         if (oldChunkTrackingView instanceof ChunkTrackingView.Positioned positioned
            && newChunkTrackingView instanceof ChunkTrackingView.Positioned positioned2
            && positioned.squareIntersects(positioned2)) {
            int i = Math.min(positioned.minX(), positioned2.minX());
            int j = Math.min(positioned.minZ(), positioned2.minZ());
            int k = Math.max(positioned.maxX(), positioned2.maxX());
            int l = Math.max(positioned.maxZ(), positioned2.maxZ());

            for (int m = i; m <= k; m++) {
               for (int n = j; n <= l; n++) {
                  boolean bl = positioned.contains(m, n);
                  boolean bl2 = positioned2.contains(m, n);
                  if (bl != bl2) {
                     if (bl2) {
                        chunkMarker.accept(new ChunkPos(m, n));
                     } else {
                        chunkDropper.accept(new ChunkPos(m, n));
                     }
                  }
               }
            }
         } else {
            oldChunkTrackingView.forEach(chunkDropper);
            newChunkTrackingView.forEach(chunkMarker);
         }
      }
   }

   default boolean contains(ChunkPos chunkPos) {
      return this.contains(chunkPos.x, chunkPos.z);
   }

   default boolean contains(int x, int z) {
      return this.contains(x, z, true);
   }

   boolean contains(int x, int z, boolean includeOuterChunksAdjacentToViewBorder);

   void forEach(Consumer<ChunkPos> action);

   default boolean isInViewDistance(int x, int z) {
      return this.contains(x, z, false);
   }

   static boolean isInViewDistance(int centerX, int centerZ, int viewDistance, int x, int z) {
      return isWithinDistance(centerX, centerZ, viewDistance, x, z, false);
   }

   static boolean isWithinDistance(int centerX, int centerZ, int viewDistance, int x, int z, boolean includeOuterChunksAdjacentToViewBorder) {
      int i = includeOuterChunksAdjacentToViewBorder ? 2 : 1;
      long l = Math.max(0, Math.abs(x - centerX) - i);
      long m = Math.max(0, Math.abs(z - centerZ) - i);
      long n = l * l + m * m;
      int j = viewDistance * viewDistance;
      return n < j;
   }

   public record Positioned(ChunkPos center, int viewDistance) implements ChunkTrackingView {
      int minX() {
         return this.center.x - this.viewDistance - 1;
      }

      int minZ() {
         return this.center.z - this.viewDistance - 1;
      }

      int maxX() {
         return this.center.x + this.viewDistance + 1;
      }

      int maxZ() {
         return this.center.z + this.viewDistance + 1;
      }

      @VisibleForTesting
      protected boolean squareIntersects(ChunkTrackingView.Positioned other) {
         return this.minX() <= other.maxX() && this.maxX() >= other.minX() && this.minZ() <= other.maxZ() && this.maxZ() >= other.minZ();
      }

      @Override
      public boolean contains(int x, int z, boolean includeOuterChunksAdjacentToViewBorder) {
         return ChunkTrackingView.isWithinDistance(this.center.x, this.center.z, this.viewDistance, x, z, includeOuterChunksAdjacentToViewBorder);
      }

      @Override
      public void forEach(Consumer<ChunkPos> action) {
         for (int i = this.minX(); i <= this.maxX(); i++) {
            for (int j = this.minZ(); j <= this.maxZ(); j++) {
               if (this.contains(i, j)) {
                  action.accept(new ChunkPos(i, j));
               }
            }
         }
      }
   }
}
