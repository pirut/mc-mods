package net.minecraft.world.level.chunk;

import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.util.VisibleForDebug;

public class DataLayer {
   public static final int LAYER_COUNT = 16;
   public static final int LAYER_SIZE = 128;
   public static final int SIZE = 2048;
   private static final int NIBBLE_SIZE = 4;
   @Nullable
   protected byte[] data;
   private int defaultValue;

   public DataLayer() {
      this(0);
   }

   public DataLayer(int p_62554_) {
      this.defaultValue = p_62554_;
   }

   public DataLayer(byte[] p_62556_) {
      this.data = p_62556_;
      this.defaultValue = 0;
      if (p_62556_.length != 2048) {
         throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException("DataLayer should be 2048 bytes not: " + p_62556_.length));
      }
   }

   public int get(int p_62561_, int p_62562_, int p_62563_) {
      return this.get(getIndex(p_62561_, p_62562_, p_62563_));
   }

   public void set(int p_62565_, int p_62566_, int p_62567_, int p_62568_) {
      this.set(getIndex(p_62565_, p_62566_, p_62567_), p_62568_);
   }

   private static int getIndex(int p_62572_, int p_62573_, int p_62574_) {
      return p_62573_ << 8 | p_62574_ << 4 | p_62572_;
   }

   private int get(int p_62571_) {
      if (this.data == null) {
         return this.defaultValue;
      } else {
         int $$1 = getByteIndex(p_62571_);
         int $$2 = getNibbleIndex(p_62571_);
         return this.data[$$1] >> 4 * $$2 & 15;
      }
   }

   private void set(int p_62558_, int p_62559_) {
      byte[] $$2 = this.getData();
      int $$3 = getByteIndex(p_62558_);
      int $$4 = getNibbleIndex(p_62558_);
      int $$5 = ~(15 << 4 * $$4);
      int $$6 = (p_62559_ & 15) << 4 * $$4;
      $$2[$$3] = (byte)($$2[$$3] & $$5 | $$6);
   }

   private static int getNibbleIndex(int p_182482_) {
      return p_182482_ & 1;
   }

   private static int getByteIndex(int p_62579_) {
      return p_62579_ >> 1;
   }

   public void fill(int p_285142_) {
      this.defaultValue = p_285142_;
      this.data = null;
   }

   private static byte packFilled(int p_282176_) {
      byte $$1 = (byte)p_282176_;

      for (int $$2 = 4; $$2 < 8; $$2 += 4) {
         $$1 = (byte)($$1 | p_282176_ << $$2);
      }

      return $$1;
   }

   public byte[] getData() {
      if (this.data == null) {
         this.data = new byte[2048];
         if (this.defaultValue != 0) {
            Arrays.fill(this.data, packFilled(this.defaultValue));
         }
      }

      return this.data;
   }

   public DataLayer copy() {
      return this.data == null ? new DataLayer(this.defaultValue) : new DataLayer((byte[])this.data.clone());
   }

   @Override
   public String toString() {
      StringBuilder $$0 = new StringBuilder();

      for (int $$1 = 0; $$1 < 4096; $$1++) {
         $$0.append(Integer.toHexString(this.get($$1)));
         if (($$1 & 15) == 15) {
            $$0.append("\n");
         }

         if (($$1 & 0xFF) == 255) {
            $$0.append("\n");
         }
      }

      return $$0.toString();
   }

   @VisibleForDebug
   public String layerToString(int p_156342_) {
      StringBuilder $$1 = new StringBuilder();

      for (int $$2 = 0; $$2 < 256; $$2++) {
         $$1.append(Integer.toHexString(this.get($$2)));
         if (($$2 & 15) == 15) {
            $$1.append("\n");
         }
      }

      return $$1.toString();
   }

   public boolean isDefinitelyHomogenous() {
      return this.data == null;
   }

   public boolean isDefinitelyFilledWith(int p_281763_) {
      return this.data == null && this.defaultValue == p_281763_;
   }

   public boolean isEmpty() {
      return this.data == null && this.defaultValue == 0;
   }
}
