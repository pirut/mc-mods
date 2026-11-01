package net.neoforged.neoforge.network.payload;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.registration.PayloadRegistration;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record ModdedNetworkQueryPayload(Map<ConnectionProtocol, Set<ModdedNetworkQueryComponent>> queries) implements CustomPacketPayload {
   public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("neoforge", "register");
   public static final CustomPacketPayload.Type<ModdedNetworkQueryPayload> TYPE = new CustomPacketPayload.Type<>(ID);
   public static StreamCodec<FriendlyByteBuf, ModdedNetworkQueryPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.map(
         IdentityHashMap::new,
         ByteBufCodecs.idMapper(b -> ConnectionProtocol.values()[b], Enum::ordinal),
         ByteBufCodecs.collection(HashSet::new, ModdedNetworkQueryComponent.STREAM_CODEC)
      ),
      ModdedNetworkQueryPayload::queries,
      ModdedNetworkQueryPayload::new
   );

   @Override
   public CustomPacketPayload.Type<ModdedNetworkQueryPayload> type() {
      return TYPE;
   }

   public static ModdedNetworkQueryPayload fromRegistry(Map<ConnectionProtocol, Map<ResourceLocation, PayloadRegistration<?>>> registry) {
      Map<ConnectionProtocol, Set<ModdedNetworkQueryComponent>> queries = registry.entrySet()
         .stream()
         .map(entry -> Map.entry(entry.getKey(), entry.getValue().values().stream().map(ModdedNetworkQueryComponent::new).collect(Collectors.toSet())))
         .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
      return new ModdedNetworkQueryPayload(queries);
   }
}
