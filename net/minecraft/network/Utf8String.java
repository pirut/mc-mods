package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.nio.charset.StandardCharsets;

public class Utf8String {
   public static String read(ByteBuf p_295677_, int p_295190_) {
      int $$2 = ByteBufUtil.utf8MaxBytes(p_295190_);
      int $$3 = VarInt.read(p_295677_);
      if ($$3 > $$2) {
         throw new DecoderException("The received encoded string buffer length is longer than maximum allowed (" + $$3 + " > " + $$2 + ")");
      } else if ($$3 < 0) {
         throw new DecoderException("The received encoded string buffer length is less than zero! Weird string!");
      } else {
         int $$4 = p_295677_.readableBytes();
         if ($$3 > $$4) {
            throw new DecoderException("Not enough bytes in buffer, expected " + $$3 + ", but got " + $$4);
         } else {
            String $$5 = p_295677_.toString(p_295677_.readerIndex(), $$3, StandardCharsets.UTF_8);
            p_295677_.readerIndex(p_295677_.readerIndex() + $$3);
            if ($$5.length() > p_295190_) {
               throw new DecoderException("The received string length is longer than maximum allowed (" + $$5.length() + " > " + p_295190_ + ")");
            } else {
               return $$5;
            }
         }
      }
   }

   public static void write(ByteBuf p_295811_, CharSequence p_295702_, int p_295310_) {
      if (p_295702_.length() > p_295310_) {
         throw new EncoderException("String too big (was " + p_295702_.length() + " characters, max " + p_295310_ + ")");
      } else {
         int $$3 = ByteBufUtil.utf8MaxBytes(p_295702_);
         ByteBuf $$4 = p_295811_.alloc().buffer($$3);

         try {
            int $$5 = ByteBufUtil.writeUtf8($$4, p_295702_);
            int $$6 = ByteBufUtil.utf8MaxBytes(p_295310_);
            if ($$5 > $$6) {
               throw new EncoderException("String too big (was " + $$5 + " bytes encoded, max " + $$6 + ")");
            }

            VarInt.write(p_295811_, $$5);
            p_295811_.writeBytes($$4);
         } finally {
            $$4.release();
         }
      }
   }
}
