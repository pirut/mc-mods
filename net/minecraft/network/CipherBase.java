package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import javax.crypto.Cipher;
import javax.crypto.ShortBufferException;

public class CipherBase {
   private final Cipher cipher;
   private byte[] heapIn = new byte[0];
   private byte[] heapOut = new byte[0];

   protected CipherBase(Cipher p_129403_) {
      this.cipher = p_129403_;
   }

   private byte[] bufToByte(ByteBuf p_129405_) {
      int $$1 = p_129405_.readableBytes();
      if (this.heapIn.length < $$1) {
         this.heapIn = new byte[$$1];
      }

      p_129405_.readBytes(this.heapIn, 0, $$1);
      return this.heapIn;
   }

   protected ByteBuf decipher(ChannelHandlerContext p_129410_, ByteBuf p_129411_) throws ShortBufferException {
      int $$2 = p_129411_.readableBytes();
      byte[] $$3 = this.bufToByte(p_129411_);
      ByteBuf $$4 = p_129410_.alloc().heapBuffer(this.cipher.getOutputSize($$2));
      $$4.writerIndex(this.cipher.update($$3, 0, $$2, $$4.array(), $$4.arrayOffset()));
      return $$4;
   }

   protected void encipher(ByteBuf p_129407_, ByteBuf p_129408_) throws ShortBufferException {
      int $$2 = p_129407_.readableBytes();
      byte[] $$3 = this.bufToByte(p_129407_);
      int $$4 = this.cipher.getOutputSize($$2);
      if (this.heapOut.length < $$4) {
         this.heapOut = new byte[$$4];
      }

      p_129408_.writeBytes(this.heapOut, 0, this.cipher.update($$3, 0, $$2, this.heapOut));
   }
}
