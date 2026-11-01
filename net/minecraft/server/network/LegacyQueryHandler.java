package net.minecraft.server.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.net.SocketAddress;
import java.util.Locale;
import net.minecraft.server.ServerInfo;
import org.slf4j.Logger;

public class LegacyQueryHandler extends ChannelInboundHandlerAdapter {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ServerInfo server;

   public LegacyQueryHandler(ServerInfo p_296029_) {
      this.server = p_296029_;
   }

   public void channelRead(ChannelHandlerContext p_9686_, Object p_9687_) {
      ByteBuf $$2 = (ByteBuf)p_9687_;
      $$2.markReaderIndex();
      boolean $$3 = true;

      try {
         try {
            if ($$2.readUnsignedByte() != 254) {
               return;
            }

            SocketAddress $$4 = p_9686_.channel().remoteAddress();
            int $$5 = $$2.readableBytes();
            if ($$5 == 0) {
               LOGGER.debug("Ping: (<1.3.x) from {}", $$4);
               String $$6 = createVersion0Response(this.server);
               sendFlushAndClose(p_9686_, createLegacyDisconnectPacket(p_9686_.alloc(), $$6));
            } else {
               if ($$2.readUnsignedByte() != 1) {
                  return;
               }

               if ($$2.isReadable()) {
                  if (!readCustomPayloadPacket($$2)) {
                     return;
                  }

                  LOGGER.debug("Ping: (1.6) from {}", $$4);
               } else {
                  LOGGER.debug("Ping: (1.4-1.5.x) from {}", $$4);
               }

               String $$7 = createVersion1Response(this.server);
               sendFlushAndClose(p_9686_, createLegacyDisconnectPacket(p_9686_.alloc(), $$7));
            }

            $$2.release();
            $$3 = false;
         } catch (RuntimeException var11) {
         }
      } finally {
         if ($$3) {
            $$2.resetReaderIndex();
            p_9686_.channel().pipeline().remove(this);
            p_9686_.fireChannelRead(p_9687_);
         }
      }
   }

   private static boolean readCustomPayloadPacket(ByteBuf p_294725_) {
      short $$1 = p_294725_.readUnsignedByte();
      if ($$1 != 250) {
         return false;
      } else {
         String $$2 = LegacyProtocolUtils.readLegacyString(p_294725_);
         if (!"MC|PingHost".equals($$2)) {
            return false;
         } else {
            int $$3 = p_294725_.readUnsignedShort();
            if (p_294725_.readableBytes() != $$3) {
               return false;
            } else {
               short $$4 = p_294725_.readUnsignedByte();
               if ($$4 < 73) {
                  return false;
               } else {
                  String $$5 = LegacyProtocolUtils.readLegacyString(p_294725_);
                  int $$6 = p_294725_.readInt();
                  return $$6 <= 65535;
               }
            }
         }
      }
   }

   private static String createVersion0Response(ServerInfo p_294449_) {
      return String.format(Locale.ROOT, "%s§%d§%d", p_294449_.getMotd(), p_294449_.getPlayerCount(), p_294449_.getMaxPlayers());
   }

   private static String createVersion1Response(ServerInfo p_295503_) {
      return String.format(
         Locale.ROOT,
         "§1\u0000%d\u0000%s\u0000%s\u0000%d\u0000%d",
         127,
         p_295503_.getServerVersion(),
         p_295503_.getMotd(),
         p_295503_.getPlayerCount(),
         p_295503_.getMaxPlayers()
      );
   }

   private static void sendFlushAndClose(ChannelHandlerContext p_9681_, ByteBuf p_9682_) {
      p_9681_.pipeline().firstContext().writeAndFlush(p_9682_).addListener(ChannelFutureListener.CLOSE);
   }

   private static ByteBuf createLegacyDisconnectPacket(ByteBufAllocator p_294764_, String p_296452_) {
      ByteBuf $$2 = p_294764_.buffer();
      $$2.writeByte(255);
      LegacyProtocolUtils.writeLegacyString($$2, p_296452_);
      return $$2;
   }
}
