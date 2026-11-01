package net.neoforged.neoforge.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record ClientboundCustomSetTimePayload(long gameTime, long dayTime, boolean gameRule, float dayTimeFraction, float dayTimePerTick)
   implements CustomPacketPayload {
   public static final CustomPacketPayload.Type<ClientboundCustomSetTimePayload> TYPE = new CustomPacketPayload.Type<>(
      ResourceLocation.fromNamespaceAndPath("neoforge", "custom_time_packet")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundCustomSetTimePayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_LONG,
      ClientboundCustomSetTimePayload::gameTime,
      ByteBufCodecs.VAR_LONG,
      ClientboundCustomSetTimePayload::dayTime,
      ByteBufCodecs.BOOL,
      ClientboundCustomSetTimePayload::gameRule,
      ByteBufCodecs.FLOAT,
      ClientboundCustomSetTimePayload::dayTimeFraction,
      ByteBufCodecs.FLOAT,
      ClientboundCustomSetTimePayload::dayTimePerTick,
      ClientboundCustomSetTimePayload::new
   );

   @Override
   public CustomPacketPayload.Type<ClientboundCustomSetTimePayload> type() {
      return TYPE;
   }
}
