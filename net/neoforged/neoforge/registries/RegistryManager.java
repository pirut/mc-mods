package net.neoforged.neoforge.registries;

import com.mojang.logging.LogUtils;
import io.netty.util.AttributeKey;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.network.configuration.RegistryDataMapNegotiation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.payload.FrozenRegistryPayload;
import net.neoforged.neoforge.network.payload.KnownRegistryDataMapsReplyPayload;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

@Internal
public class RegistryManager {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Marker REGISTRIES = MarkerFactory.getMarker("REGISTRIES");
   private static Set<Identifier> pendingModdedRegistries = new HashSet<>();
   private static Set<Identifier> vanillaRegistryKeys = Set.of();
   private static Map<Identifier, RegistrySnapshot> vanillaSnapshot = null;
   private static Map<Identifier, RegistrySnapshot> frozenSnapshot = null;
   private static Map<ResourceKey<Registry<?>>, Map<Identifier, DataMapType<?, ?>>> dataMaps = Map.of();
   public static final AttributeKey<Map<ResourceKey<? extends Registry<?>>, Collection<Identifier>>> ATTRIBUTE_KNOWN_DATA_MAPS = AttributeKey.valueOf(
      "neoforge:known_data_maps"
   );

   static synchronized void trackModdedRegistry(Identifier registry) {
      Objects.requireNonNull(registry);
      if (pendingModdedRegistries == null) {
         throw new IllegalStateException("Attempting to instantiate registry with name " + registry + " after NewRegistryEvent was fired!");
      } else if (!pendingModdedRegistries.add(registry)) {
         throw new IllegalStateException("Registry with name " + registry + " was already instantiated once, cannot instantiate it again!");
      }
   }

   @Nullable
   public static <R> DataMapType<R, ?> getDataMap(ResourceKey<? extends Registry<R>> registry, Identifier key) {
      Map<Identifier, DataMapType<?, ?>> map = dataMaps.get(registry);
      return map == null ? null : (DataMapType)map.get(key);
   }

   public static Map<ResourceKey<Registry<?>>, Map<Identifier, DataMapType<?, ?>>> getDataMaps() {
      return dataMaps;
   }

   public static void postNewRegistryEvent() {
      NewRegistryEvent event = new NewRegistryEvent();
      DataPackRegistryEvent.NewRegistry dataPackEvent = new DataPackRegistryEvent.NewRegistry();
      vanillaRegistryKeys = Set.copyOf(BuiltInRegistries.REGISTRY.keySet());
      ModLoader.postEventWrapContainerInModOrder(event);
      ModLoader.postEventWrapContainerInModOrder(dataPackEvent);
      event.fill();
      dataPackEvent.process();
      ModLoader.postEvent(new ModifyRegistriesEvent());
      pendingModdedRegistries.removeIf(BuiltInRegistries.REGISTRY::containsKey);
      if (!pendingModdedRegistries.isEmpty()) {
         throw new IllegalStateException(
            "The following registries were created but not registered to NewRegistryEvent:"
               + pendingModdedRegistries.stream().map(Identifier::toString).collect(Collectors.joining("\n\t - ", "\n\t - ", ""))
         );
      } else {
         pendingModdedRegistries = null;
      }
   }

   public static void initDataMaps() {
      Map<ResourceKey<Registry<?>>, Map<Identifier, DataMapType<?, ?>>> dataMapTypes = new HashMap<>();
      ModLoader.postEvent(new RegisterDataMapTypesEvent(dataMapTypes));
      dataMaps = new IdentityHashMap<>();
      dataMapTypes.forEach(
         (key, values) -> dataMaps.put(
            (ResourceKey<Registry<?>>)key, Collections.unmodifiableMap((Map<? extends Identifier, ? extends DataMapType<?, ?>>)values)
         )
      );
      dataMaps = Collections.unmodifiableMap(dataMapTypes);
   }

   static void takeVanillaSnapshot() {
      vanillaSnapshot = takeSnapshot(RegistryManager.SnapshotType.FULL);
   }

   static void takeFrozenSnapshot() {
      frozenSnapshot = takeSnapshot(RegistryManager.SnapshotType.SYNC_TO_CLIENT);
   }

   public static void revertToVanilla() {
      applySnapshot(vanillaSnapshot, true);
   }

   public static void revertToFrozen() {
      applySnapshot(frozenSnapshot, true);
   }

   public static Set<ResourceKey<?>> applySnapshot(Map<Identifier, RegistrySnapshot> snapshots, boolean isLocalWorld) {
      Set<ResourceKey<?>> missingEntries = new HashSet<>();
      snapshots.forEach((registryName, snapshot) -> {
         if (!BuiltInRegistries.REGISTRY.containsKey(registryName)) {
            if (!snapshot.getIds().isEmpty()) {
               throw new IllegalStateException("Tried to applied snapshot with registry name " + registryName + " but was not found");
            }
         } else {
            MappedRegistry<?> registry = (MappedRegistry<?>)BuiltInRegistries.REGISTRY.getValue(registryName);
            applySnapshot(registry, snapshot, missingEntries);
         }
      });
      if (missingEntries.isEmpty()) {
         return Set.of();
      } else {
         LOGGER.debug(REGISTRIES, "There are {} mappings missing", missingEntries.size());
         if (isLocalWorld && LOGGER.isWarnEnabled(REGISTRIES)) {
            StringBuilder builder = new StringBuilder("NeoForge detected missing registry entries.\n\n")
               .append("There are ")
               .append(missingEntries.size())
               .append(" missing entries in this save.\n")
               .append("These missing entries will be deleted from the save file on next save.");
            missingEntries.forEach(key -> builder.append("Missing ").append(key).append('\n'));
            LOGGER.warn(REGISTRIES, builder.toString());
         }

         return Set.copyOf(missingEntries);
      }
   }

   private static <T> void applySnapshot(MappedRegistry<T> registry, RegistrySnapshot snapshot, Set<ResourceKey<?>> missing) {
      BaseMappedRegistry<T> forgeRegistry = registry;
      ResourceKey<? extends Registry<T>> registryKey = registry.key();
      Registry<T> backup = snapshot.getFullBackup();
      registry.unfreeze(false);
      if (backup == null) {
         registry.clear(false);
         ObjectBidirectionalIterator var6 = snapshot.getIds().int2ObjectEntrySet().iterator();

         while (var6.hasNext()) {
            Entry<Identifier> entry = (Entry<Identifier>)var6.next();
            ResourceKey<T> key = ResourceKey.create(registryKey, (Identifier)entry.getValue());
            if (!registry.containsKey(key)) {
               missing.add(key);
            } else {
               forgeRegistry.registerIdMapping(key, entry.getIntKey());
            }
         }
      } else {
         registry.clear(true);

         for (java.util.Map.Entry<ResourceKey<T>, T> entry : backup.entrySet()) {
            ResourceKey<T> key = entry.getKey();
            T value = entry.getValue();
            registry.register(backup.getId(key), key, value, backup.registrationInfo(key).orElse(RegistrationInfo.BUILT_IN));
         }
      }

      snapshot.getAliases().forEach(registry::addAlias);
      forgeRegistry.freeze();
   }

   public static Map<Identifier, RegistrySnapshot> takeSnapshot(RegistryManager.SnapshotType snapshotType) {
      Map<Identifier, RegistrySnapshot> map = new HashMap<>();
      boolean full = snapshotType == RegistryManager.SnapshotType.FULL;

      for (Registry<?> registry : BuiltInRegistries.REGISTRY) {
         if (snapshotType != RegistryManager.SnapshotType.SYNC_TO_CLIENT || registry.doesSync()) {
            map.put(registry.key().identifier(), new RegistrySnapshot(registry, full));
         }
      }

      return map;
   }

   public static List<FrozenRegistryPayload> generateRegistryPackets(boolean isLocal) {
      return isLocal
         ? List.of()
         : takeSnapshot(RegistryManager.SnapshotType.SYNC_TO_CLIENT).entrySet().stream().map(e -> new FrozenRegistryPayload(e.getKey(), e.getValue())).toList();
   }

   public static List<Identifier> getRegistryNamesForSyncToClient() {
      List<Identifier> list = new ArrayList<>();
      BuiltInRegistries.REGISTRY.entrySet().forEach(e -> {
         if (e.getValue().doesSync()) {
            list.add(e.getKey().identifier());
         }
      });
      return list;
   }

   public static Set<Identifier> getVanillaRegistryKeys() {
      return vanillaRegistryKeys;
   }

   @Internal
   public static void handleKnownDataMapsReply(KnownRegistryDataMapsReplyPayload payload, IPayloadContext context) {
      context.channelHandlerContext().attr(ATTRIBUTE_KNOWN_DATA_MAPS).set(payload.dataMaps());
      context.finishCurrentTask(RegistryDataMapNegotiation.TYPE);
   }

   public static boolean isNonSyncedBuiltInRegistry(Registry<?> registry) {
      return !BuiltInRegistries.REGISTRY.containsKey(registry.key()) ? false : !registry.doesSync();
   }

   public static enum SnapshotType {
      SYNC_TO_CLIENT,
      FULL;
   }
}
