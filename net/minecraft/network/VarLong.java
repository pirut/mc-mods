package net.minecraft.network;

import io.netty.buffer.ByteBuf;

public class VarLong {
   private static final int MAX_VARLONG_SIZE = 10;
   private static final int DATA_BITS_MASK = 127;
   private static final int CONTINUATION_BIT_MASK = 128;
   private static final int DATA_BITS_PER_BYTE = 7;

   public static int getByteSize(long p_295931_) {
      for (int $$1 = 1; $$1 < 10; $$1++) {
         if ((p_295931_ & -1L << $$1 * 7) == 0L) {
            return $$1;
         }
      }

      return 10;
   }

   public static boolean hasContinuationBit(byte p_294159_) {
      return (p_294159_ & 128) == 128;
   }

   public static long read(ByteBuf p_294443_) {
      long $$1 = 0L;
      int $$2 = 0;

      byte $$3;
      do {
         $$3 = p_294443_.readByte();
         $$1 |= (long)($$3 & 127) << $$2++ * 7;
         if ($$2 > 10) {
            throw new RuntimeException("VarLong too big");
         }
      } while (hasContinuationBit($$3));

      return $$1;
   }

   public static ByteBuf write(ByteBuf p_296144_, long p_295377_) {
      while ((p_295377_ & -128L) != 0L) {
         p_296144_.writeByte((int)(p_295377_ & 127L) | 128);
         p_295377_ >>>= 7;
      }

      p_296144_.writeByte((int)p_295377_);
      return p_296144_;
   }
}
