package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import java.util.zip.Deflater;
import net.neoforged.neoforge.logging.PacketDump;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CompressionEncoder extends MessageToByteEncoder<ByteBuf> {
   private final byte[] encodeBuf = new byte[8192];
   private final Deflater deflater;
   private int threshold;
   private static final boolean DISABLE_PACKET_DEBUG = Boolean.parseBoolean(System.getProperty("neoforge.disablePacketCompressionDebug", "false"));
   private static final Logger LOGGER = LogManager.getLogger();

   public CompressionEncoder(int p_129448_) {
      this.threshold = p_129448_;
      this.deflater = new Deflater();
   }

   protected void encode(ChannelHandlerContext p_129452_, ByteBuf p_129453_, ByteBuf p_129454_) {
      int i = p_129453_.readableBytes();
      if (!DISABLE_PACKET_DEBUG && i > 8388608) {
         p_129453_.markReaderIndex();
         LOGGER.error("Attempted to send packet over maximum protocol size: {} > {}\nData:\n{}", i, 8388608, PacketDump.getContentDump(p_129453_));
         p_129453_.resetReaderIndex();
      }

      if (i > 8388608) {
         throw new IllegalArgumentException("Packet too big (is " + i + ", should be less than 8388608)");
      } else {
         if (i < this.threshold) {
            VarInt.write(p_129454_, 0);
            p_129454_.writeBytes(p_129453_);
         } else {
            byte[] abyte = new byte[i];
            p_129453_.readBytes(abyte);
            VarInt.write(p_129454_, abyte.length);
            this.deflater.setInput(abyte, 0, i);
            this.deflater.finish();

            while (!this.deflater.finished()) {
               int j = this.deflater.deflate(this.encodeBuf);
               p_129454_.writeBytes(this.encodeBuf, 0, j);
            }

            this.deflater.reset();
         }
      }
   }

   public int getThreshold() {
      return this.threshold;
   }

   public void setThreshold(int p_129450_) {
      this.threshold = p_129450_;
   }
}
