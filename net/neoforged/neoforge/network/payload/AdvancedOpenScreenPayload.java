package net.neoforged.neoforge.network.payload;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record AdvancedOpenScreenPayload(int windowId, MenuType<?> menuType, Component name, byte[] additionalData) implements CustomPacketPayload {
   public static final CustomPacketPayload.Type<AdvancedOpenScreenPayload> TYPE = new CustomPacketPayload.Type<>(
      Identifier.fromNamespaceAndPath("neoforge", "advanced_open_screen")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, AdvancedOpenScreenPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      AdvancedOpenScreenPayload::windowId,
      ByteBufCodecs.idMapper(BuiltInRegistries.MENU),
      AdvancedOpenScreenPayload::menuType,
      ComponentSerialization.STREAM_CODEC,
      AdvancedOpenScreenPayload::name,
      NeoForgeStreamCodecs.UNBOUNDED_BYTE_ARRAY,
      AdvancedOpenScreenPayload::additionalData,
      AdvancedOpenScreenPayload::new
   );

   @Override
   public CustomPacketPayload.Type<AdvancedOpenScreenPayload> type() {
      return TYPE;
   }
}
