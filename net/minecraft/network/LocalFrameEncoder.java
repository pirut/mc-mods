package net.minecraft.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class LocalFrameEncoder extends ChannelOutboundHandlerAdapter {
   public void write(ChannelHandlerContext p_390512_, Object p_390524_, ChannelPromise p_390506_) {
      p_390512_.write(HiddenByteBuf.pack(p_390524_), p_390506_);
   }
}
