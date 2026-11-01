package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class MonitoredLocalFrameDecoder extends ChannelInboundHandlerAdapter {
   private final BandwidthDebugMonitor monitor;

   public MonitoredLocalFrameDecoder(BandwidthDebugMonitor p_390410_) {
      this.monitor = p_390410_;
   }

   public void channelRead(ChannelHandlerContext p_390444_, Object p_390381_) {
      p_390381_ = HiddenByteBuf.unpack(p_390381_);
      if (p_390381_ instanceof ByteBuf bytebuf) {
         this.monitor.onReceive(bytebuf.readableBytes());
      }

      p_390444_.fireChannelRead(p_390381_);
   }
}
