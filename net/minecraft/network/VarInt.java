package net.minecraft.network;

import io.netty.buffer.ByteBuf;

public class VarInt {
   private static final int MAX_VARINT_SIZE = 5;
   private static final int DATA_BITS_MASK = 127;
   private static final int CONTINUATION_BIT_MASK = 128;
   private static final int DATA_BITS_PER_BYTE = 7;

   public static int getByteSize(int p_295994_) {
      for (int $$1 = 1; $$1 < 5; $$1++) {
         if ((p_295994_ & -1 << $$1 * 7) == 0) {
            return $$1;
         }
      }

      return 5;
   }

   public static boolean hasContinuationBit(byte p_294861_) {
      return (p_294861_ & 128) == 128;
   }

   public static int read(ByteBuf p_296169_) {
      int $$1 = 0;
      int $$2 = 0;

      byte $$3;
      do {
         $$3 = p_296169_.readByte();
         $$1 |= ($$3 & 127) << $$2++ * 7;
         if ($$2 > 5) {
            throw new RuntimeException("VarInt too big");
         }
      } while (hasContinuationBit($$3));

      return $$1;
   }

   public static ByteBuf write(ByteBuf p_295539_, int p_294256_) {
      while ((p_294256_ & -128) != 0) {
         p_295539_.writeByte(p_294256_ & 127 | 128);
         p_294256_ >>>= 7;
      }

      p_295539_.writeByte(p_294256_);
      return p_295539_;
   }
}
