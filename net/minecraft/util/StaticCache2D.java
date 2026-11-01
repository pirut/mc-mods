package net.minecraft.util;

import java.util.Locale;
import java.util.function.Consumer;

public class StaticCache2D<T> {
   private final int minX;
   private final int minZ;
   private final int sizeX;
   private final int sizeZ;
   private final Object[] cache;

   public static <T> StaticCache2D<T> create(int p_347579_, int p_347687_, int p_347693_, StaticCache2D.Initializer<T> p_347478_) {
      int $$4 = p_347579_ - p_347693_;
      int $$5 = p_347687_ - p_347693_;
      int $$6 = 2 * p_347693_ + 1;
      return new StaticCache2D<>($$4, $$5, $$6, $$6, p_347478_);
   }

   private StaticCache2D(int p_347480_, int p_347568_, int p_347475_, int p_347530_, StaticCache2D.Initializer<T> p_347453_) {
      this.minX = p_347480_;
      this.minZ = p_347568_;
      this.sizeX = p_347475_;
      this.sizeZ = p_347530_;
      this.cache = new Object[this.sizeX * this.sizeZ];

      for (int $$5 = p_347480_; $$5 < p_347480_ + p_347475_; $$5++) {
         for (int $$6 = p_347568_; $$6 < p_347568_ + p_347530_; $$6++) {
            this.cache[this.getIndex($$5, $$6)] = p_347453_.get($$5, $$6);
         }
      }
   }

   public void forEach(Consumer<T> p_347572_) {
      for (Object $$1 : this.cache) {
         p_347572_.accept((T)$$1);
      }
   }

   public T get(int p_347699_, int p_347563_) {
      if (!this.contains(p_347699_, p_347563_)) {
         throw new IllegalArgumentException("Requested out of range value (" + p_347699_ + "," + p_347563_ + ") from " + this);
      } else {
         return (T)this.cache[this.getIndex(p_347699_, p_347563_)];
      }
   }

   public boolean contains(int p_347591_, int p_347645_) {
      int $$2 = p_347591_ - this.minX;
      int $$3 = p_347645_ - this.minZ;
      return $$2 >= 0 && $$2 < this.sizeX && $$3 >= 0 && $$3 < this.sizeZ;
   }

   @Override
   public String toString() {
      return String.format(Locale.ROOT, "StaticCache2D[%d, %d, %d, %d]", this.minX, this.minZ, this.minX + this.sizeX, this.minZ + this.sizeZ);
   }

   private int getIndex(int p_347703_, int p_347664_) {
      int $$2 = p_347703_ - this.minX;
      int $$3 = p_347664_ - this.minZ;
      return $$2 * this.sizeZ + $$3;
   }

   @FunctionalInterface
   public interface Initializer<T> {
      T get(int var1, int var2);
   }
}
