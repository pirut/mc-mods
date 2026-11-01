package net.minecraft.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class LocalFrameDecoder extends ChannelInboundHandlerAdapter {
   public void channelRead(ChannelHandlerContext p_390480_, Object p_390511_) {
      p_390480_.fireChannelRead(HiddenByteBuf.unpack(p_390511_));
   }
}
