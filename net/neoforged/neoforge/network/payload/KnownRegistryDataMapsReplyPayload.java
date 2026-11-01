package net.neoforged.neoforge.network.payload;

import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record KnownRegistryDataMapsReplyPayload(Map<ResourceKey<? extends Registry<?>>, Collection<ResourceLocation>> dataMaps) implements CustomPacketPayload {
   public static final CustomPacketPayload.Type<KnownRegistryDataMapsReplyPayload> TYPE = new CustomPacketPayload.Type<>(
      ResourceLocation.fromNamespaceAndPath("neoforge", "known_registry_data_maps_reply")
   );
   public static final StreamCodec<FriendlyByteBuf, KnownRegistryDataMapsReplyPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.map(
         Maps::newHashMapWithExpectedSize, NeoForgeStreamCodecs.registryKey(), ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.collection(ArrayList::new))
      ),
      KnownRegistryDataMapsReplyPayload::dataMaps,
      KnownRegistryDataMapsReplyPayload::new
   );

   @Override
   public CustomPacketPayload.Type<KnownRegistryDataMapsReplyPayload> type() {
      return TYPE;
   }
}
