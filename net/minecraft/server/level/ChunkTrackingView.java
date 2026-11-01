package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import java.util.function.Consumer;
import net.minecraft.world.level.ChunkPos;

public interface ChunkTrackingView {
   ChunkTrackingView EMPTY = new ChunkTrackingView() {
      @Override
      public boolean contains(int p_294225_, int p_294897_, boolean p_294644_) {
         return false;
      }

      @Override
      public void forEach(Consumer<ChunkPos> p_295201_) {
      }
   };

   static ChunkTrackingView of(ChunkPos p_296254_, int p_295979_) {
      return new ChunkTrackingView.Positioned(p_296254_, p_295979_);
   }

   static void difference(ChunkTrackingView p_294391_, ChunkTrackingView p_294272_, Consumer<ChunkPos> p_295078_, Consumer<ChunkPos> p_294115_) {
      if (!p_294391_.equals(p_294272_)) {
         if (p_294391_ instanceof ChunkTrackingView.Positioned $$4 && p_294272_ instanceof ChunkTrackingView.Positioned $$5 && $$4.squareIntersects($$5)) {
            int $$6 = Math.min($$4.minX(), $$5.minX());
            int $$7 = Math.min($$4.minZ(), $$5.minZ());
            int $$8 = Math.max($$4.maxX(), $$5.maxX());
            int $$9 = Math.max($$4.maxZ(), $$5.maxZ());

            for (int $$10 = $$6; $$10 <= $$8; $$10++) {
               for (int $$11 = $$7; $$11 <= $$9; $$11++) {
                  boolean $$12 = $$4.contains($$10, $$11);
                  boolean $$13 = $$5.contains($$10, $$11);
                  if ($$12 != $$13) {
                     if ($$13) {
                        p_295078_.accept(new ChunkPos($$10, $$11));
                     } else {
                        p_294115_.accept(new ChunkPos($$10, $$11));
                     }
                  }
               }
            }
         } else {
            p_294391_.forEach(p_294115_);
            p_294272_.forEach(p_295078_);
         }
      }
   }

   default boolean contains(ChunkPos p_296112_) {
      return this.contains(p_296112_.x, p_296112_.z);
   }

   default boolean contains(int p_295374_, int p_296479_) {
      return this.contains(p_295374_, p_296479_, true);
   }

   boolean contains(int var1, int var2, boolean var3);

   void forEach(Consumer<ChunkPos> var1);

   default boolean isInViewDistance(int p_295863_, int p_294569_) {
      return this.contains(p_295863_, p_294569_, false);
   }

   static boolean isInViewDistance(int p_294551_, int p_294918_, int p_296415_, int p_296475_, int p_295248_) {
      return isWithinDistance(p_294551_, p_294918_, p_296415_, p_296475_, p_295248_, false);
   }

   static boolean isWithinDistance(int p_294927_, int p_295703_, int p_294990_, int p_295161_, int p_295394_, boolean p_295219_) {
      int $$6 = Math.max(0, Math.abs(p_295161_ - p_294927_) - 1);
      int $$7 = Math.max(0, Math.abs(p_295394_ - p_295703_) - 1);
      long $$8 = Math.max(0, Math.max($$6, $$7) - (p_295219_ ? 1 : 0));
      long $$9 = Math.min($$6, $$7);
      long $$10 = $$9 * $$9 + $$8 * $$8;
      int $$11 = p_294990_ * p_294990_;
      return $$10 < $$11;
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
      protected boolean squareIntersects(ChunkTrackingView.Positioned p_295100_) {
         return this.minX() <= p_295100_.maxX() && this.maxX() >= p_295100_.minX() && this.minZ() <= p_295100_.maxZ() && this.maxZ() >= p_295100_.minZ();
      }

      @Override
      public boolean contains(int p_295177_, int p_294248_, boolean p_294703_) {
         return ChunkTrackingView.isWithinDistance(this.center.x, this.center.z, this.viewDistance, p_295177_, p_294248_, p_294703_);
      }

      @Override
      public void forEach(Consumer<ChunkPos> p_294236_) {
         for (int $$1 = this.minX(); $$1 <= this.maxX(); $$1++) {
            for (int $$2 = this.minZ(); $$2 <= this.maxZ(); $$2++) {
               if (this.contains($$1, $$2)) {
                  p_294236_.accept(new ChunkPos($$1, $$2));
               }
            }
         }
      }
   }
}
