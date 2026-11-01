package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.Long2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import java.util.NoSuchElementException;
import net.minecraft.util.Mth;

public class SpatialLongSet extends LongLinkedOpenHashSet {
   private final SpatialLongSet.InternalMap map;

   public SpatialLongSet(int p_164462_, float p_164463_) {
      super(p_164462_, p_164463_);
      this.map = new SpatialLongSet.InternalMap(p_164462_ / 64, p_164463_);
   }

   public boolean add(long p_164465_) {
      return this.map.addBit(p_164465_);
   }

   public boolean rem(long p_164468_) {
      return this.map.removeBit(p_164468_);
   }

   public long removeFirstLong() {
      return this.map.removeFirstBit();
   }

   public int size() {
      throw new UnsupportedOperationException();
   }

   public boolean isEmpty() {
      return this.map.isEmpty();
   }

   protected static class InternalMap extends Long2LongLinkedOpenHashMap {
      private static final int X_BITS = Mth.log2(60000000);
      private static final int Z_BITS = Mth.log2(60000000);
      private static final int Y_BITS = 64 - X_BITS - Z_BITS;
      private static final int Y_OFFSET = 0;
      private static final int Z_OFFSET = Y_BITS;
      private static final int X_OFFSET = Y_BITS + Z_BITS;
      private static final long OUTER_MASK = 3L << X_OFFSET | 3L | 3L << Z_OFFSET;
      private int lastPos = -1;
      private long lastOuterKey;
      private final int minSize;

      public InternalMap(int p_164483_, float p_164484_) {
         super(p_164483_, p_164484_);
         this.minSize = p_164483_;
      }

      static long getOuterKey(long p_164490_) {
         return p_164490_ & ~OUTER_MASK;
      }

      static int getInnerKey(long p_164498_) {
         int $$1 = (int)(p_164498_ >>> X_OFFSET & 3L);
         int $$2 = (int)(p_164498_ >>> 0 & 3L);
         int $$3 = (int)(p_164498_ >>> Z_OFFSET & 3L);
         return $$1 << 4 | $$3 << 2 | $$2;
      }

      static long getFullKey(long p_164492_, int p_164493_) {
         p_164492_ |= (long)(p_164493_ >>> 4 & 3) << X_OFFSET;
         p_164492_ |= (long)(p_164493_ >>> 2 & 3) << Z_OFFSET;
         return p_164492_ | (long)(p_164493_ >>> 0 & 3) << 0;
      }

      public boolean addBit(long p_164500_) {
         long $$1 = getOuterKey(p_164500_);
         int $$2 = getInnerKey(p_164500_);
         long $$3 = 1L << $$2;
         int $$4;
         if ($$1 == 0L) {
            if (this.containsNullKey) {
               return this.replaceBit(this.n, $$3);
            }

            this.containsNullKey = true;
            $$4 = this.n;
         } else {
            if (this.lastPos != -1 && $$1 == this.lastOuterKey) {
               return this.replaceBit(this.lastPos, $$3);
            }

            long[] $$5 = this.key;
            $$4 = (int)HashCommon.mix($$1) & this.mask;

            for (long $$7 = $$5[$$4]; $$7 != 0L; $$7 = $$5[$$4]) {
               if ($$7 == $$1) {
                  this.lastPos = $$4;
                  this.lastOuterKey = $$1;
                  return this.replaceBit($$4, $$3);
               }

               $$4 = $$4 + 1 & this.mask;
            }
         }

         this.key[$$4] = $$1;
         this.value[$$4] = $$3;
         if (this.size == 0) {
            this.first = this.last = $$4;
            this.link[$$4] = -1L;
         } else {
            this.link[this.last] = this.link[this.last] ^ (this.link[this.last] ^ $$4 & 4294967295L) & 4294967295L;
            this.link[$$4] = (this.last & 4294967295L) << 32 | 4294967295L;
            this.last = $$4;
         }

         if (this.size++ >= this.maxFill) {
            this.rehash(HashCommon.arraySize(this.size + 1, this.f));
         }

         return false;
      }

      private boolean replaceBit(int p_164487_, long p_164488_) {
         boolean $$2 = (this.value[p_164487_] & p_164488_) != 0L;
         this.value[p_164487_] = this.value[p_164487_] | p_164488_;
         return $$2;
      }

      public boolean removeBit(long p_164502_) {
         long $$1 = getOuterKey(p_164502_);
         int $$2 = getInnerKey(p_164502_);
         long $$3 = 1L << $$2;
         if ($$1 == 0L) {
            return this.containsNullKey ? this.removeFromNullEntry($$3) : false;
         } else if (this.lastPos != -1 && $$1 == this.lastOuterKey) {
            return this.removeFromEntry(this.lastPos, $$3);
         } else {
            long[] $$4 = this.key;
            int $$5 = (int)HashCommon.mix($$1) & this.mask;

            for (long $$6 = $$4[$$5]; $$6 != 0L; $$6 = $$4[$$5]) {
               if ($$1 == $$6) {
                  this.lastPos = $$5;
                  this.lastOuterKey = $$1;
                  return this.removeFromEntry($$5, $$3);
               }

               $$5 = $$5 + 1 & this.mask;
            }

            return false;
         }
      }

      private boolean removeFromNullEntry(long p_164504_) {
         if ((this.value[this.n] & p_164504_) == 0L) {
            return false;
         } else {
            this.value[this.n] = this.value[this.n] & ~p_164504_;
            if (this.value[this.n] != 0L) {
               return true;
            } else {
               this.containsNullKey = false;
               this.size--;
               this.fixPointers(this.n);
               if (this.size < this.maxFill / 4 && this.n > 16) {
                  this.rehash(this.n / 2);
               }

               return true;
            }
         }
      }

      private boolean removeFromEntry(int p_164495_, long p_164496_) {
         if ((this.value[p_164495_] & p_164496_) == 0L) {
            return false;
         } else {
            this.value[p_164495_] = this.value[p_164495_] & ~p_164496_;
            if (this.value[p_164495_] != 0L) {
               return true;
            } else {
               this.lastPos = -1;
               this.size--;
               this.fixPointers(p_164495_);
               this.shiftKeys(p_164495_);
               if (this.size < this.maxFill / 4 && this.n > 16) {
                  this.rehash(this.n / 2);
               }

               return true;
            }
         }
      }

      public long removeFirstBit() {
         if (this.size == 0) {
            throw new NoSuchElementException();
         } else {
            int $$0 = this.first;
            long $$1 = this.key[$$0];
            int $$2 = Long.numberOfTrailingZeros(this.value[$$0]);
            this.value[$$0] = this.value[$$0] & ~(1L << $$2);
            if (this.value[$$0] == 0L) {
               this.removeFirstLong();
               this.lastPos = -1;
            }

            return getFullKey($$1, $$2);
         }
      }

      protected void rehash(int p_164506_) {
         if (p_164506_ > this.minSize) {
            super.rehash(p_164506_);
         }
      }
   }
}
