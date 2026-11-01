package net.minecraft.network.protocol.common.custom;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public record DiscardedPayload(Identifier id) implements CustomPacketPayload {
   public static <T extends FriendlyByteBuf> StreamCodec<T, DiscardedPayload> codec(Identifier p_468409_, int p_319929_) {
      return CustomPacketPayload.codec((p_320462_, p_319882_) -> {}, p_466117_ -> {
         int i = p_466117_.readableBytes();
         if (i >= 0 && i <= p_319929_) {
            p_466117_.skipBytes(i);
            return new DiscardedPayload(p_468409_);
         } else {
            throw new IllegalArgumentException("Payload may not be larger than " + p_319929_ + " bytes");
         }
      });
   }

   @Override
   public CustomPacketPayload.Type<DiscardedPayload> type() {
      return new CustomPacketPayload.Type<>(this.id);
   }
}
