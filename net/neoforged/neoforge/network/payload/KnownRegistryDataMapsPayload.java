package net.neoforged.neoforge.network.payload;

import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record KnownRegistryDataMapsPayload(Map<ResourceKey<? extends Registry<?>>, List<KnownRegistryDataMapsPayload.KnownDataMap>> dataMaps)
   implements CustomPacketPayload {
   public static final CustomPacketPayload.Type<KnownRegistryDataMapsPayload> TYPE = new CustomPacketPayload.Type<>(
      Identifier.fromNamespaceAndPath("neoforge", "known_registry_data_maps")
   );
   public static final StreamCodec<FriendlyByteBuf, KnownRegistryDataMapsPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.map(
         Maps::newHashMapWithExpectedSize,
         NeoForgeStreamCodecs.registryKey(),
         KnownRegistryDataMapsPayload.KnownDataMap.STREAM_CODEC.apply(ByteBufCodecs.list())
      ),
      KnownRegistryDataMapsPayload::dataMaps,
      KnownRegistryDataMapsPayload::new
   );

   @Override
   public CustomPacketPayload.Type<KnownRegistryDataMapsPayload> type() {
      return TYPE;
   }

   public record KnownDataMap(Identifier id, boolean mandatory) {
      public static final StreamCodec<FriendlyByteBuf, KnownRegistryDataMapsPayload.KnownDataMap> STREAM_CODEC = StreamCodec.composite(
         Identifier.STREAM_CODEC,
         KnownRegistryDataMapsPayload.KnownDataMap::id,
         ByteBufCodecs.BOOL,
         KnownRegistryDataMapsPayload.KnownDataMap::mandatory,
         KnownRegistryDataMapsPayload.KnownDataMap::new
      );
   }
}
