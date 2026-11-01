package net.minecraft.network.protocol;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.network.ClientboundPacketListener;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.ServerboundPacketListener;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Unit;
import org.jspecify.annotations.Nullable;

public class ProtocolInfoBuilder<T extends PacketListener, B extends ByteBuf, C> {
   final ConnectionProtocol protocol;
   final PacketFlow flow;
   private final List<ProtocolInfoBuilder.CodecEntry<T, ?, B, C>> codecs = new ArrayList<>();
   @Nullable
   private BundlerInfo bundlerInfo;

   public ProtocolInfoBuilder(ConnectionProtocol p_320213_, PacketFlow p_320424_) {
      this.protocol = p_320213_;
      this.flow = p_320424_;
   }

   public <P extends Packet<? super T>> ProtocolInfoBuilder<T, B, C> addPacket(PacketType<P> p_320673_, StreamCodec<? super B, P> p_319828_) {
      this.codecs.add(new ProtocolInfoBuilder.CodecEntry<>(p_320673_, p_319828_, null));
      return this;
   }

   public <P extends Packet<? super T>> ProtocolInfoBuilder<T, B, C> addPacket(
      PacketType<P> p_412394_, StreamCodec<? super B, P> p_412152_, CodecModifier<B, P, C> p_412496_
   ) {
      this.codecs.add(new ProtocolInfoBuilder.CodecEntry<>(p_412394_, p_412152_, p_412496_));
      return this;
   }

   public <P extends BundlePacket<? super T>, D extends BundleDelimiterPacket<? super T>> ProtocolInfoBuilder<T, B, C> withBundlePacket(
      PacketType<P> p_320954_, Function<Iterable<Packet<? super T>>, P> p_320241_, D p_320202_
   ) {
      StreamCodec<ByteBuf, D> streamcodec = StreamCodec.unit(p_320202_);
      PacketType<D> packettype = (PacketType<D>)p_320202_.type();
      this.codecs.add(new ProtocolInfoBuilder.CodecEntry<>(packettype, streamcodec, null));
      this.bundlerInfo = BundlerInfo.createForPacket(p_320954_, p_320241_, p_320202_);
      return this;
   }

   StreamCodec<ByteBuf, Packet<? super T>> buildPacketCodec(
      Function<ByteBuf, B> p_320922_, List<ProtocolInfoBuilder.CodecEntry<T, ?, B, C>> p_320733_, C p_412355_
   ) {
      ProtocolCodecBuilder<ByteBuf, T> protocolcodecbuilder = new ProtocolCodecBuilder<>(this.flow);

      for (ProtocolInfoBuilder.CodecEntry<T, ?, B, C> codecentry : p_320733_) {
         codecentry.addToBuilder(protocolcodecbuilder, p_320922_, p_412355_);
      }

      return protocolcodecbuilder.build();
   }

   private static ProtocolInfo.Details buildDetails(
      final ConnectionProtocol p_412771_, final PacketFlow p_412073_, final List<? extends ProtocolInfoBuilder.CodecEntry<?, ?, ?, ?>> p_412644_
   ) {
      return new ProtocolInfo.Details() {
         @Override
         public ConnectionProtocol id() {
            return p_412771_;
         }

         @Override
         public PacketFlow flow() {
            return p_412073_;
         }

         @Override
         public void listPackets(ProtocolInfo.Details.PacketVisitor p_412067_) {
            for (int i = 0; i < p_412644_.size(); i++) {
               ProtocolInfoBuilder.CodecEntry<?, ?, ?, ?> codecentry = (ProtocolInfoBuilder.CodecEntry<?, ?, ?, ?>)p_412644_.get(i);
               p_412067_.accept(codecentry.type, i);
            }
         }
      };
   }

   public SimpleUnboundProtocol<T, B> buildUnbound(final C p_412107_) {
      final List<ProtocolInfoBuilder.CodecEntry<T, ?, B, C>> list = List.copyOf(this.codecs);
      final BundlerInfo bundlerinfo = this.bundlerInfo;
      final ProtocolInfo.Details protocolinfo$details = buildDetails(this.protocol, this.flow, list);
      return new SimpleUnboundProtocol<T, B>() {
         @Override
         public ProtocolInfo<T> bind(Function<ByteBuf, B> p_412690_) {
            return new ProtocolInfoBuilder.Implementation<>(
               ProtocolInfoBuilder.this.protocol,
               ProtocolInfoBuilder.this.flow,
               ProtocolInfoBuilder.this.buildPacketCodec(p_412690_, list, p_412107_),
               bundlerinfo
            );
         }

         @Override
         public ProtocolInfo.Details details() {
            return protocolinfo$details;
         }
      };
   }

   public UnboundProtocol<T, B, C> buildUnbound() {
      final List<ProtocolInfoBuilder.CodecEntry<T, ?, B, C>> list = List.copyOf(this.codecs);
      final BundlerInfo bundlerinfo = this.bundlerInfo;
      final ProtocolInfo.Details protocolinfo$details = buildDetails(this.protocol, this.flow, list);
      return new UnboundProtocol<T, B, C>() {
         @Override
         public ProtocolInfo<T> bind(Function<ByteBuf, B> p_412297_, C p_412114_) {
            return new ProtocolInfoBuilder.Implementation<>(
               ProtocolInfoBuilder.this.protocol,
               ProtocolInfoBuilder.this.flow,
               ProtocolInfoBuilder.this.buildPacketCodec(p_412297_, list, p_412114_),
               bundlerinfo
            );
         }

         @Override
         public ProtocolInfo.Details details() {
            return protocolinfo$details;
         }
      };
   }

   private static <L extends PacketListener, B extends ByteBuf> SimpleUnboundProtocol<L, B> protocol(
      ConnectionProtocol p_320849_, PacketFlow p_320146_, Consumer<ProtocolInfoBuilder<L, B, Unit>> p_320140_
   ) {
      ProtocolInfoBuilder<L, B, Unit> protocolinfobuilder = new ProtocolInfoBuilder<>(p_320849_, p_320146_);
      p_320140_.accept(protocolinfobuilder);
      return protocolinfobuilder.buildUnbound(Unit.INSTANCE);
   }

   public static <T extends ServerboundPacketListener, B extends ByteBuf> SimpleUnboundProtocol<T, B> serverboundProtocol(
      ConnectionProtocol p_319767_, Consumer<ProtocolInfoBuilder<T, B, Unit>> p_320799_
   ) {
      return protocol(p_319767_, PacketFlow.SERVERBOUND, p_320799_);
   }

   public static <T extends ClientboundPacketListener, B extends ByteBuf> SimpleUnboundProtocol<T, B> clientboundProtocol(
      ConnectionProtocol p_320428_, Consumer<ProtocolInfoBuilder<T, B, Unit>> p_320292_
   ) {
      return protocol(p_320428_, PacketFlow.CLIENTBOUND, p_320292_);
   }

   private static <L extends PacketListener, B extends ByteBuf, C> UnboundProtocol<L, B, C> contextProtocol(
      ConnectionProtocol p_412739_, PacketFlow p_412457_, Consumer<ProtocolInfoBuilder<L, B, C>> p_412460_
   ) {
      ProtocolInfoBuilder<L, B, C> protocolinfobuilder = new ProtocolInfoBuilder<>(p_412739_, p_412457_);
      p_412460_.accept(protocolinfobuilder);
      return protocolinfobuilder.buildUnbound();
   }

   public static <T extends ServerboundPacketListener, B extends ByteBuf, C> UnboundProtocol<T, B, C> contextServerboundProtocol(
      ConnectionProtocol p_412637_, Consumer<ProtocolInfoBuilder<T, B, C>> p_412492_
   ) {
      return contextProtocol(p_412637_, PacketFlow.SERVERBOUND, p_412492_);
   }

   public static <T extends ClientboundPacketListener, B extends ByteBuf, C> UnboundProtocol<T, B, C> contextClientboundProtocol(
      ConnectionProtocol p_412735_, Consumer<ProtocolInfoBuilder<T, B, C>> p_412617_
   ) {
      return contextProtocol(p_412735_, PacketFlow.CLIENTBOUND, p_412617_);
   }

   record CodecEntry<T extends PacketListener, P extends Packet<? super T>, B extends ByteBuf, C>(
      PacketType<P> type, StreamCodec<? super B, P> serializer, @Nullable CodecModifier<B, P, C> modifier
   ) {
      public void addToBuilder(ProtocolCodecBuilder<ByteBuf, T> p_320857_, Function<ByteBuf, B> p_320646_, C p_412144_) {
         StreamCodec<? super B, P> streamcodec;
         if (this.modifier != null) {
            streamcodec = this.modifier.apply(this.serializer, p_412144_);
         } else {
            streamcodec = this.serializer;
         }

         StreamCodec<ByteBuf, P> streamcodec1 = streamcodec.mapStream(p_320646_);
         p_320857_.add(this.type, streamcodec1);
      }
   }

   record Implementation<L extends PacketListener>(
      ConnectionProtocol id, PacketFlow flow, StreamCodec<ByteBuf, Packet<? super L>> codec, @Nullable BundlerInfo bundlerInfo
   ) implements ProtocolInfo<L> {
   }
}
