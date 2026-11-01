package net.neoforged.neoforge.common.world.chunk;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TicketStorage;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.common.util.NeoForgeExtraCodecs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

public class ForcedChunkManager {
   private static final Logger LOGGER = LogManager.getLogger();
   private static boolean initialised = false;
   private static Map<Identifier, TicketController> controllers = Map.of();

   @Internal
   public static synchronized void init() {
      if (initialised) {
         throw new UnsupportedOperationException("Cannot init ticket controllers multiple times!");
      } else {
         initialised = true;
         Map<Identifier, TicketController> controllers = new HashMap<>();
         ModLoader.postEvent(new RegisterTicketControllersEvent(controller -> {
            if (controllers.containsKey(controller.id())) {
               throw new IllegalArgumentException("Attempted to register two controllers with the same ID " + controller.id());
            } else {
               controllers.put(controller.id(), controller);
            }
         }));
         ForcedChunkManager.controllers = Map.copyOf(controllers);
      }
   }

   public static boolean hasForcedChunks(ServerLevel level) {
      TicketStorage data = level.getDataStorage().get(TicketStorage.TYPE);
      return data == null ? false : !data.getBlockForcedChunks().isEmpty() || !data.getEntityForcedChunks().isEmpty();
   }

   static <T extends Comparable<? super T>> boolean forceChunk(
      ServerLevel level,
      Identifier id,
      T owner,
      int chunkX,
      int chunkZ,
      boolean add,
      boolean forceNaturalSpawning,
      Function<TicketStorage, ForcedChunkManager.TicketTracker<T>> ticketGetter
   ) {
      if (!controllers.containsKey(id)) {
         throw new IllegalArgumentException("Controller with ID " + id + " is not registered!");
      } else {
         TicketStorage saveData = level.getDataStorage().computeIfAbsent(TicketStorage.TYPE);
         ChunkPos pos = new ChunkPos(chunkX, chunkZ);
         long chunk = pos.toLong();
         ForcedChunkManager.TicketTracker<T> tickets = ticketGetter.apply(saveData);
         ForcedChunkManager.TicketOwner<T> ticketOwner = new ForcedChunkManager.TicketOwner<>(id, owner);
         if (add) {
            boolean success = tickets.add(ticketOwner, chunk, forceNaturalSpawning);
            if (success) {
               level.getChunk(chunkX, chunkZ);
            }

            return success;
         } else {
            return tickets.remove(ticketOwner, chunk, forceNaturalSpawning, false);
         }
      }
   }

   @Internal
   public static void activateAllDeactivatedTickets(ServerLevel level, TicketStorage saveData) {
      ForcedChunkManager.TicketTracker<BlockPos> blockForcedChunks = saveData.getBlockForcedChunks();
      ForcedChunkManager.TicketTracker<UUID> entityForcedChunks = saveData.getEntityForcedChunks();
      if (!blockForcedChunks.hasNoDeactivatedTickets() || !entityForcedChunks.hasNoDeactivatedTickets()) {
         List<Entry<Identifier, TicketController>> controllers = ForcedChunkManager.controllers
            .entrySet()
            .stream()
            .filter(c -> c.getValue().callback() != null)
            .toList();
         if (!controllers.isEmpty()) {
            Map<Identifier, Map<BlockPos, TicketSet>> blockTickets = gatherTicketsById(blockForcedChunks, false, true);
            Map<Identifier, Map<UUID, TicketSet>> entityTickets = gatherTicketsById(entityForcedChunks, false, true);
            controllers.forEach(
               value -> {
                  boolean hasBlockTicket = blockTickets.containsKey(value.getKey());
                  boolean hasEntityTicket = entityTickets.containsKey(value.getKey());
                  if (hasBlockTicket || hasEntityTicket) {
                     Map<BlockPos, TicketSet> ownedBlockTickets = hasBlockTicket
                        ? Collections.unmodifiableMap(blockTickets.get(value.getKey()))
                        : Collections.emptyMap();
                     Map<UUID, TicketSet> ownedEntityTickets = hasEntityTicket
                        ? Collections.unmodifiableMap(entityTickets.get(value.getKey()))
                        : Collections.emptyMap();
                     value.getValue().callback().validateTickets(level, new TicketHelper(saveData, value.getKey(), ownedBlockTickets, ownedEntityTickets));
                  }
               }
            );
         }

         blockForcedChunks.activateAllDeactivatedSources();
         entityForcedChunks.activateAllDeactivatedSources();
      }
   }

   private static <T extends Comparable<? super T>> Map<Identifier, Map<T, TicketSet>> gatherTicketsById(
      ForcedChunkManager.TicketTracker<T> tickets, boolean includeLoaded, boolean includeDeactivated
   ) {
      Map<Identifier, Map<T, TicketSet>> modSortedOwnedChunks = new HashMap<>();
      if (includeLoaded) {
         gatherTicketsById(tickets.sourcesLoading, TicketSet::normal, modSortedOwnedChunks);
         gatherTicketsById(tickets.sourcesLoadingNaturalSpawning, TicketSet::naturalSpawning, modSortedOwnedChunks);
      }

      if (includeDeactivated) {
         gatherTicketsById(tickets.deactivatedSourcesLoading, TicketSet::normal, modSortedOwnedChunks);
         gatherTicketsById(tickets.deactivatedSourcesLoadingNaturalSpawning, TicketSet::naturalSpawning, modSortedOwnedChunks);
      }

      return modSortedOwnedChunks;
   }

   private static <T extends Comparable<? super T>> void gatherTicketsById(
      Long2ObjectMap<Set<ForcedChunkManager.TicketOwner<T>>> tickets,
      Function<TicketSet, LongSet> typeGetter,
      Map<Identifier, Map<T, TicketSet>> modSortedOwnedChunks
   ) {
      ObjectIterator var3 = Long2ObjectMaps.fastIterable(tickets).iterator();

      while (var3.hasNext()) {
         it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<Set<ForcedChunkManager.TicketOwner<T>>> entry = (it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<Set<ForcedChunkManager.TicketOwner<T>>>)var3.next();
         long chunk = entry.getLongKey();

         for (ForcedChunkManager.TicketOwner<T> owner : (Set)entry.getValue()) {
            TicketSet pair = modSortedOwnedChunks.computeIfAbsent(owner.id, modId -> new HashMap<>())
               .computeIfAbsent(owner.owner, o -> new TicketSet(new LongOpenHashSet(), new LongOpenHashSet()));
            typeGetter.apply(pair).add(chunk);
         }
      }
   }

   @Internal
   public static App<Mu<TicketStorage>, List<ForcedChunkManager.OwnedChunks>> defineExtraStorageParams() {
      return ForcedChunkManager.OwnedChunks.CODEC.listOf().optionalFieldOf("neo_ticket_data", List.of()).forGetter(storage -> {
         Map<Identifier, Map<BlockPos, TicketSet>> blockTickets = gatherTicketsById(storage.getBlockForcedChunks(), true, true);
         Map<Identifier, Map<UUID, TicketSet>> entityTickets = gatherTicketsById(storage.getEntityForcedChunks(), true, true);
         Map<Identifier, ForcedChunkManager.OwnedChunks> ownedChunks = new HashMap<>();

         for (Entry<Identifier, Map<BlockPos, TicketSet>> entry : blockTickets.entrySet()) {
            Identifier controllerId = entry.getKey();
            ownedChunks.put(controllerId, new ForcedChunkManager.OwnedChunks(controllerId, entry.getValue(), new HashMap<>()));
         }

         for (Entry<Identifier, Map<UUID, TicketSet>> entry : entityTickets.entrySet()) {
            Identifier controllerId = entry.getKey();
            ForcedChunkManager.OwnedChunks owned = ownedChunks.get(controllerId);
            if (owned == null) {
               ownedChunks.put(controllerId, new ForcedChunkManager.OwnedChunks(controllerId, new HashMap<>(), entry.getValue()));
            } else {
               owned.entityChunks().putAll(entry.getValue());
            }
         }

         return List.copyOf(ownedChunks.values());
      });
   }

   @Internal
   public static TicketStorage readStoredTickets(
      Function<List<Pair<ChunkPos, Ticket>>, TicketStorage> vanillaInitializer,
      List<Pair<ChunkPos, Ticket>> tickets,
      List<ForcedChunkManager.OwnedChunks> ownedChunks
   ) {
      TicketStorage ticketStorage = vanillaInitializer.apply(tickets);
      ForcedChunkManager.TicketTracker<BlockPos> blockForcedChunks = ticketStorage.getBlockForcedChunks();
      ForcedChunkManager.TicketTracker<UUID> entityForcedChunks = ticketStorage.getEntityForcedChunks();

      for (ForcedChunkManager.OwnedChunks ownedChunk : ownedChunks) {
         Identifier controllerId = ownedChunk.controller();
         if (controllers.containsKey(controllerId)) {
            for (Entry<BlockPos, TicketSet> entry : ownedChunk.blockChunks().entrySet()) {
               blockForcedChunks.inheritDeactivated(new ForcedChunkManager.TicketOwner<>(controllerId, entry.getKey()), entry.getValue());
            }

            for (Entry<UUID, TicketSet> entry : ownedChunk.entityChunks().entrySet()) {
               entityForcedChunks.inheritDeactivated(new ForcedChunkManager.TicketOwner<>(controllerId, entry.getKey()), entry.getValue());
            }
         } else {
            LOGGER.warn(
               "Found chunk loading data for controller id {} which is currently not available or active - it will be removed from the level save.",
               controllerId
            );
         }
      }

      return ticketStorage;
   }

   public record OwnedChunks(Identifier controller, Map<BlockPos, TicketSet> blockChunks, Map<UUID, TicketSet> entityChunks) {
      private static final Codec<Map<BlockPos, TicketSet>> BLOCK_CHUNK_CODEC = NeoForgeExtraCodecs.unboundedMapAsList(
         "position", BlockPos.CODEC, "tickets", TicketSet.CODEC
      );
      private static final Codec<Map<UUID, TicketSet>> ENTITY_CHUNK_CODEC = NeoForgeExtraCodecs.unboundedMapAsList(
         "uuid", UUIDUtil.CODEC, "tickets", TicketSet.CODEC
      );
      public static final Codec<ForcedChunkManager.OwnedChunks> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               Identifier.CODEC.fieldOf("controller").forGetter(ForcedChunkManager.OwnedChunks::controller),
               BLOCK_CHUNK_CODEC.optionalFieldOf("block_chunks", Map.of()).forGetter(ForcedChunkManager.OwnedChunks::blockChunks),
               ENTITY_CHUNK_CODEC.optionalFieldOf("entity_chunks", Map.of()).forGetter(ForcedChunkManager.OwnedChunks::entityChunks)
            )
            .apply(instance, ForcedChunkManager.OwnedChunks::new)
      );
   }

   static class TicketOwner<T extends Comparable<? super T>> implements Comparable<ForcedChunkManager.TicketOwner<T>> {
      private final Identifier id;
      private final T owner;

      TicketOwner(Identifier id, T owner) {
         this.id = id;
         this.owner = owner;
      }

      public int compareTo(ForcedChunkManager.TicketOwner<T> other) {
         int res = this.id.compareTo(other.id);
         return res == 0 ? this.owner.compareTo(other.owner) : res;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else if (o != null && this.getClass() == o.getClass()) {
            ForcedChunkManager.TicketOwner<?> that = (ForcedChunkManager.TicketOwner<?>)o;
            return Objects.equals(this.id, that.id) && Objects.equals(this.owner, that.owner);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.id, this.owner);
      }
   }

   public static class TicketTracker<T extends Comparable<? super T>> {
      private final Long2ObjectMap<Set<ForcedChunkManager.TicketOwner<T>>> sourcesLoading = new Long2ObjectOpenHashMap();
      private final Long2ObjectMap<Set<ForcedChunkManager.TicketOwner<T>>> sourcesLoadingNaturalSpawning = new Long2ObjectOpenHashMap();
      private final Long2ObjectMap<Set<ForcedChunkManager.TicketOwner<T>>> deactivatedSourcesLoading = new Long2ObjectOpenHashMap();
      private final Long2ObjectMap<Set<ForcedChunkManager.TicketOwner<T>>> deactivatedSourcesLoadingNaturalSpawning = new Long2ObjectOpenHashMap();
      private final Holder<TicketType> naturalSpawningTicketType;
      private final Holder<TicketType> ticketType;
      private final TicketStorage ticketStorage;

      public TicketTracker(TicketStorage ticketStorage, Holder<TicketType> ticketType, Holder<TicketType> naturalSpawningTicketType) {
         this.ticketStorage = ticketStorage;
         this.ticketType = ticketType;
         this.naturalSpawningTicketType = naturalSpawningTicketType;
      }

      public void deactivateTicketsOnClosing() {
         this.inheritSources(null, this.sourcesLoading, this.deactivatedSourcesLoading);
         this.inheritSources(null, this.sourcesLoadingNaturalSpawning, this.deactivatedSourcesLoadingNaturalSpawning);
      }

      private void inheritDeactivated(ForcedChunkManager.TicketOwner<T> owner, TicketSet ticketSet) {
         LongIterator var3 = ticketSet.normal().iterator();

         while (var3.hasNext()) {
            long chunk = (Long)var3.next();
            ((Set)this.deactivatedSourcesLoading.computeIfAbsent(chunk, c -> new HashSet())).add(owner);
         }

         var3 = ticketSet.naturalSpawning().iterator();

         while (var3.hasNext()) {
            long chunk = (Long)var3.next();
            ((Set)this.deactivatedSourcesLoadingNaturalSpawning.computeIfAbsent(chunk, c -> new HashSet())).add(owner);
         }
      }

      private void activateAllDeactivatedSources() {
         this.inheritSources(this.ticketType, this.deactivatedSourcesLoading, this.sourcesLoading);
         this.inheritSources(this.naturalSpawningTicketType, this.deactivatedSourcesLoadingNaturalSpawning, this.sourcesLoadingNaturalSpawning);
      }

      private void inheritSources(
         @Nullable Holder<TicketType> ticketType,
         Long2ObjectMap<Set<ForcedChunkManager.TicketOwner<T>>> fromSource,
         Long2ObjectMap<Set<ForcedChunkManager.TicketOwner<T>>> toSource
      ) {
         Ticket ticket = ticketType == null ? null : new Ticket(ticketType.value(), ChunkMap.FORCED_TICKET_LEVEL);
         ObjectIterator var5 = Long2ObjectMaps.fastIterable(fromSource).iterator();

         while (var5.hasNext()) {
            it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<Set<ForcedChunkManager.TicketOwner<T>>> entry = (it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<Set<ForcedChunkManager.TicketOwner<T>>>)var5.next();
            long chunk = entry.getLongKey();
            if (ticket != null) {
               this.ticketStorage.addTicket(entry.getLongKey(), ticket);
            }

            ((Set)toSource.computeIfAbsent(chunk, c -> new HashSet())).addAll((Collection)entry.getValue());
         }

         fromSource.clear();
      }

      public boolean hasNoDeactivatedTickets() {
         return this.deactivatedSourcesLoading.isEmpty() && this.deactivatedSourcesLoadingNaturalSpawning.isEmpty();
      }

      public boolean isEmpty() {
         return this.sourcesLoading.isEmpty() && this.sourcesLoadingNaturalSpawning.isEmpty();
      }

      private Long2ObjectMap<Set<ForcedChunkManager.TicketOwner<T>>> getSourcesLoading(boolean forceNaturalSpawning, boolean targetDeactivated) {
         if (targetDeactivated) {
            return forceNaturalSpawning ? this.deactivatedSourcesLoadingNaturalSpawning : this.deactivatedSourcesLoading;
         } else {
            return forceNaturalSpawning ? this.sourcesLoadingNaturalSpawning : this.sourcesLoading;
         }
      }

      private Ticket makeTicket(boolean forceNaturalSpawning) {
         Holder<TicketType> type = forceNaturalSpawning ? this.naturalSpawningTicketType : this.ticketType;
         return new Ticket(type.value(), ChunkMap.FORCED_TICKET_LEVEL);
      }

      public boolean remove(ForcedChunkManager.TicketOwner<T> owner, long chunk, boolean forceNaturalSpawning, boolean targetDeactivated) {
         Long2ObjectMap<Set<ForcedChunkManager.TicketOwner<T>>> sourcesLoading = this.getSourcesLoading(forceNaturalSpawning, targetDeactivated);
         Set<ForcedChunkManager.TicketOwner<T>> sources = (Set<ForcedChunkManager.TicketOwner<T>>)sourcesLoading.get(chunk);
         if (sources != null && sources.remove(owner)) {
            if (sources.isEmpty()) {
               sourcesLoading.remove(chunk);
               if (!targetDeactivated) {
                  this.ticketStorage.removeTicket(chunk, this.makeTicket(forceNaturalSpawning));
               }
            }

            this.ticketStorage.setDirty();
            return true;
         } else {
            return false;
         }
      }

      private boolean add(ForcedChunkManager.TicketOwner<T> owner, long chunk, boolean forceNaturalSpawning) {
         Long2ObjectMap<Set<ForcedChunkManager.TicketOwner<T>>> sourcesLoading = this.getSourcesLoading(forceNaturalSpawning, false);
         Set<ForcedChunkManager.TicketOwner<T>> sources = (Set<ForcedChunkManager.TicketOwner<T>>)sourcesLoading.computeIfAbsent(chunk, c -> new HashSet());
         if (sources.isEmpty()) {
            this.ticketStorage.addTicket(chunk, this.makeTicket(forceNaturalSpawning));
         }

         if (sources.add(owner)) {
            this.ticketStorage.setDirty();
            return true;
         } else {
            return false;
         }
      }
   }
}
