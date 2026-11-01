package net.neoforged.neoforge.network.filters;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketEncoder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.config.NeoForgeCommonConfig;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.payload.SplitPacketPayload;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;

@EventBusSubscriber(
   modid = "neoforge"
)
@Internal
public class GenericPacketSplitter extends MessageToMessageEncoder<Packet<?>> implements DynamicChannelHandler {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final GenericPacketSplitter.SizeLimits compressedSizeLimits = new GenericPacketSplitter.SizeLimits(2097152);
   private static final GenericPacketSplitter.SizeLimits uncompressedSizeLimits = new GenericPacketSplitter.SizeLimits(8388608);
   private static final byte STATE_FIRST = 1;
   private static final byte STATE_LAST = 2;
   private static final long ONE_MB = 1048576L;
   private static final int MAX_PACKETS_SAFETY_LIMIT = 65535;
   public static final String CHANNEL_HANDLER_NAME = "neoforge:splitter";
   private final List<byte[]> receivedBuffers = new ArrayList<>();
   private long sizeCounter = 0L;

   @SubscribeEvent
   private static void register(RegisterPayloadHandlersEvent event) {
      event.registrar("1")
         .optional()
         .commonBidirectional(SplitPacketPayload.TYPE, SplitPacketPayload.STREAM_CODEC, GenericPacketSplitter::handle, GenericPacketSplitter::handle);
   }

   private static void handle(SplitPacketPayload payload, IPayloadContext context) {
      if (context.channelHandlerContext().pipeline().get("neoforge:splitter") instanceof GenericPacketSplitter splitter) {
         if (!splitter.receivedPacket(payload, context)) {
            context.disconnect(Component.translatable("neoforge.network.packet_splitter.too_big"));
         }
      } else {
         LOGGER.error("Received split packet without a splitter");
         context.disconnect(Component.translatable("neoforge.network.packet_splitter.unknown"));
      }
   }

   protected void encode(ChannelHandlerContext ctx, Packet<?> packet, List<Object> out) throws Exception {
      if (packet instanceof ClientboundCustomPayloadPacket clientboundCustomPayloadPacket
         && clientboundCustomPayloadPacket.payload() instanceof SplitPacketPayload) {
         out.add(packet);
      } else if (packet instanceof ServerboundCustomPayloadPacket serverboundCustomPayloadPacket
         && serverboundCustomPayloadPacket.payload() instanceof SplitPacketPayload) {
         out.add(packet);
      } else if (ctx.pipeline().get("encoder") instanceof PacketEncoder<?> encoder) {
         boolean hasCompressor = ctx.pipeline().get("compress") != null;
         GenericPacketSplitter.SizeLimits sizeLimits = hasCompressor ? uncompressedSizeLimits : compressedSizeLimits;
         FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

         try {
            StreamCodec<ByteBuf, Packet<?>> codec = encoder.getProtocolInfo().codec();
            codec.encode(buf, packet);
            if (buf.readableBytes() <= sizeLimits.packet()) {
               out.add(packet);
               return;
            }

            int parts = (int)Math.ceil((double)buf.readableBytes() / sizeLimits.part());
            if (parts == 1) {
               out.add(packet);
               return;
            }

            byte[] packetData = buf.array();

            for (int part = 0; part < parts; part++) {
               int partSize = Math.min(sizeLimits.part(), packetData.length - part * sizeLimits.part());
               byte[] payloadSlice = new byte[partSize + 1];
               byte prefix = (byte)(part == 0 ? 1 : (part == parts - 1 ? 2 : 0));
               payloadSlice[0] = prefix;
               System.arraycopy(packetData, part * sizeLimits.part(), payloadSlice, 1, partSize);
               out.add(createPacket(encoder.getProtocolInfo().flow(), payloadSlice));
            }
         } finally {
            buf.release();
         }
      } else {
         out.add(packet);
      }
   }

   private boolean receivedPacket(SplitPacketPayload payload, IPayloadContext context) {
      byte state = payload.payload()[0];
      if (state == 1 && !this.receivedBuffers.isEmpty()) {
         LOGGER.warn("neoforge:split received out of order - inbound buffer not empty when receiving first");
         this.receivedBuffers.clear();
         this.sizeCounter = 0L;
      }

      int contentSize = payload.payload().length - 1;
      byte[] buffer = new byte[contentSize];
      System.arraycopy(payload.payload(), 1, buffer, 0, contentSize);
      this.receivedBuffers.add(buffer);
      this.sizeCounter += buffer.length;
      long maxSize = NeoForgeCommonConfig.INSTANCE.splitPacketMaxSizeLimitMb.getAsInt() * 1048576L;
      if (this.sizeCounter > maxSize || this.receivedBuffers.size() > 65535) {
         this.sizeCounter = 0L;
         this.receivedBuffers.clear();
         return false;
      } else {
         if (state == 2) {
            byte[][] buffers = this.receivedBuffers.toArray(new byte[0][]);
            FriendlyByteBuf full = new FriendlyByteBuf(Unpooled.wrappedBuffer(buffers));

            try {
               Packet<?> packet = context.connection().getInboundProtocol().codec().decode(full);
               context.enqueueWork(() -> context.handle(packet));
            } finally {
               this.sizeCounter = 0L;
               this.receivedBuffers.clear();
               full.release();
            }
         }

         return true;
      }
   }

   private static Packet<?> createPacket(PacketFlow flow, byte[] payload) {
      return (Packet<?>)(switch (flow) {
         case SERVERBOUND -> new ServerboundCustomPayloadPacket(new SplitPacketPayload(payload));
         case CLIENTBOUND -> new ClientboundCustomPayloadPacket(new SplitPacketPayload(payload));
      });
   }

   @Override
   public boolean isNecessary(Connection manager) {
      return isRemoteCompatible(manager);
   }

   public static GenericPacketSplitter.RemoteCompatibility getRemoteCompatibility(Connection manager) {
      return NetworkRegistry.hasChannel(manager, null, SplitPacketPayload.TYPE.id())
         ? GenericPacketSplitter.RemoteCompatibility.PRESENT
         : GenericPacketSplitter.RemoteCompatibility.ABSENT;
   }

   public static boolean isRemoteCompatible(Connection manager) {
      return getRemoteCompatibility(manager) != GenericPacketSplitter.RemoteCompatibility.ABSENT;
   }

   public static int determineMaxPayloadSize(int maxPacketSize) {
      FriendlyByteBuf temporaryBuf = new FriendlyByteBuf(Unpooled.buffer());
      temporaryBuf.writeByte(0);
      temporaryBuf.writeIdentifier(SplitPacketPayload.TYPE.id());
      temporaryBuf.writeByte((byte)1);
      temporaryBuf.writeVarInt(Integer.MAX_VALUE);
      temporaryBuf.writeInt(Integer.MAX_VALUE);
      int prefixSize = temporaryBuf.readableBytes();
      return maxPacketSize - prefixSize;
   }

   public static enum RemoteCompatibility {
      ABSENT,
      PRESENT;
   }

   private record SizeLimits(int packet, int part) {
      public SizeLimits(int packet) {
         this(packet, GenericPacketSplitter.determineMaxPayloadSize(packet));
      }
   }
}
