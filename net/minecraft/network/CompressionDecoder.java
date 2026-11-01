package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class CompressionDecoder extends ByteToMessageDecoder {
   public static final int MAXIMUM_COMPRESSED_LENGTH = 2097152;
   public static final int MAXIMUM_UNCOMPRESSED_LENGTH = 8388608;
   private final Inflater inflater;
   private int threshold;
   private boolean validateDecompressed;

   public CompressionDecoder(int p_182675_, boolean p_182676_) {
      this.threshold = p_182675_;
      this.validateDecompressed = p_182676_;
      this.inflater = new Inflater();
   }

   protected void decode(ChannelHandlerContext p_129441_, ByteBuf p_129442_, List<Object> p_129443_) throws Exception {
      if (p_129442_.readableBytes() != 0) {
         int $$3 = VarInt.read(p_129442_);
         if ($$3 == 0) {
            p_129443_.add(p_129442_.readBytes(p_129442_.readableBytes()));
         } else {
            if (this.validateDecompressed) {
               if ($$3 < this.threshold) {
                  throw new DecoderException("Badly compressed packet - size of " + $$3 + " is below server threshold of " + this.threshold);
               }

               if ($$3 > 8388608) {
                  throw new DecoderException("Badly compressed packet - size of " + $$3 + " is larger than protocol maximum of 8388608");
               }
            }

            this.setupInflaterInput(p_129442_);
            ByteBuf $$4 = this.inflate(p_129441_, $$3);
            this.inflater.reset();
            p_129443_.add($$4);
         }
      }
   }

   private void setupInflaterInput(ByteBuf p_296004_) {
      ByteBuffer $$1;
      if (p_296004_.nioBufferCount() > 0) {
         $$1 = p_296004_.nioBuffer();
         p_296004_.skipBytes(p_296004_.readableBytes());
      } else {
         $$1 = ByteBuffer.allocateDirect(p_296004_.readableBytes());
         p_296004_.readBytes($$1);
         $$1.flip();
      }

      this.inflater.setInput($$1);
   }

   private ByteBuf inflate(ChannelHandlerContext p_295791_, int p_295281_) throws DataFormatException {
      ByteBuf $$2 = p_295791_.alloc().directBuffer(p_295281_);

      try {
         ByteBuffer $$3 = $$2.internalNioBuffer(0, p_295281_);
         int $$4 = $$3.position();
         this.inflater.inflate($$3);
         int $$5 = $$3.position() - $$4;
         if ($$5 != p_295281_) {
            throw new DecoderException(
               "Badly compressed packet - actual length of uncompressed payload " + $$5 + " is does not match declared size " + p_295281_
            );
         } else {
            $$2.writerIndex($$2.writerIndex() + $$5);
            return $$2;
         }
      } catch (Exception var7) {
         $$2.release();
         throw var7;
      }
   }

   public void setThreshold(int p_182678_, boolean p_182679_) {
      this.threshold = p_182678_;
      this.validateDecompressed = p_182679_;
   }
}
