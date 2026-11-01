package net.minecraft.client.multiplayer;

import com.google.common.base.Splitter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.List;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.server.network.LegacyProtocolUtils;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LegacyServerPinger extends SimpleChannelInboundHandler<ByteBuf> {
   private static final Splitter SPLITTER = Splitter.on('\u0000').limit(6);
   private final ServerAddress address;
   private final LegacyServerPinger.Output output;

   public LegacyServerPinger(ServerAddress p_295697_, LegacyServerPinger.Output p_295291_) {
      this.address = p_295697_;
      this.output = p_295291_;
   }

   public void channelActive(ChannelHandlerContext p_294106_) throws Exception {
      super.channelActive(p_294106_);
      ByteBuf $$1 = p_294106_.alloc().buffer();

      try {
         $$1.writeByte(254);
         $$1.writeByte(1);
         $$1.writeByte(250);
         LegacyProtocolUtils.writeLegacyString($$1, "MC|PingHost");
         int $$2 = $$1.writerIndex();
         $$1.writeShort(0);
         int $$3 = $$1.writerIndex();
         $$1.writeByte(127);
         LegacyProtocolUtils.writeLegacyString($$1, this.address.getHost());
         $$1.writeInt(this.address.getPort());
         int $$4 = $$1.writerIndex() - $$3;
         $$1.setShort($$2, $$4);
         p_294106_.channel().writeAndFlush($$1).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
      } catch (Exception var6) {
         $$1.release();
         throw var6;
      }
   }

   protected void channelRead0(ChannelHandlerContext p_295830_, ByteBuf p_294393_) {
      short $$2 = p_294393_.readUnsignedByte();
      if ($$2 == 255) {
         String $$3 = LegacyProtocolUtils.readLegacyString(p_294393_);
         List<String> $$4 = SPLITTER.splitToList($$3);
         if ("§1".equals($$4.get(0))) {
            int $$5 = Mth.getInt($$4.get(1), 0);
            String $$6 = $$4.get(2);
            String $$7 = $$4.get(3);
            int $$8 = Mth.getInt($$4.get(4), -1);
            int $$9 = Mth.getInt($$4.get(5), -1);
            this.output.handleResponse($$5, $$6, $$7, $$8, $$9);
         }
      }

      p_295830_.close();
   }

   public void exceptionCaught(ChannelHandlerContext p_296319_, Throwable p_296239_) {
      p_296319_.close();
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface Output {
      void handleResponse(int var1, String var2, String var3, int var4, int var5);
   }
}
