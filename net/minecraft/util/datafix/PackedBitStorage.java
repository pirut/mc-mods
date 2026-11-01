package net.minecraft.util.datafix;

import net.minecraft.util.Mth;
import org.apache.commons.lang3.Validate;

public class PackedBitStorage {
   private static final int BIT_TO_LONG_SHIFT = 6;
   private final long[] data;
   private final int bits;
   private final long mask;
   private final int size;

   public PackedBitStorage(int p_14555_, int p_14556_) {
      this(p_14555_, p_14556_, new long[Mth.roundToward(p_14556_ * p_14555_, 64) / 64]);
   }

   public PackedBitStorage(int p_14558_, int p_14559_, long[] p_14560_) {
      Validate.inclusiveBetween(1L, 32L, p_14558_);
      this.size = p_14559_;
      this.bits = p_14558_;
      this.data = p_14560_;
      this.mask = (1L << p_14558_) - 1L;
      int $$3 = Mth.roundToward(p_14559_ * p_14558_, 64) / 64;
      if (p_14560_.length != $$3) {
         throw new IllegalArgumentException("Invalid length given for storage, got: " + p_14560_.length + " but expected: " + $$3);
      }
   }

   public void set(int p_14565_, int p_14566_) {
      Validate.inclusiveBetween(0L, this.size - 1, p_14565_);
      Validate.inclusiveBetween(0L, this.mask, p_14566_);
      int $$2 = p_14565_ * this.bits;
      int $$3 = $$2 >> 6;
      int $$4 = (p_14565_ + 1) * this.bits - 1 >> 6;
      int $$5 = $$2 ^ $$3 << 6;
      this.data[$$3] = this.data[$$3] & ~(this.mask << $$5) | (p_14566_ & this.mask) << $$5;
      if ($$3 != $$4) {
         int $$6 = 64 - $$5;
         int $$7 = this.bits - $$6;
         this.data[$$4] = this.data[$$4] >>> $$7 << $$7 | (p_14566_ & this.mask) >> $$6;
      }
   }

   public int get(int p_14563_) {
      Validate.inclusiveBetween(0L, this.size - 1, p_14563_);
      int $$1 = p_14563_ * this.bits;
      int $$2 = $$1 >> 6;
      int $$3 = (p_14563_ + 1) * this.bits - 1 >> 6;
      int $$4 = $$1 ^ $$2 << 6;
      if ($$2 == $$3) {
         return (int)(this.data[$$2] >>> $$4 & this.mask);
      } else {
         int $$5 = 64 - $$4;
         return (int)((this.data[$$2] >>> $$4 | this.data[$$3] << $$5) & this.mask);
      }
   }

   public long[] getRaw() {
      return this.data;
   }

   public int getBits() {
      return this.bits;
   }
}
