package net.neoforged.neoforge.common.world.chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.fml.ModLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;

@ParametersAreNonnullByDefault
public class ForcedChunkManager {
   private static final Logger LOGGER = LogManager.getLogger();
   static final TicketType<ForcedChunkManager.TicketOwner<BlockPos>> BLOCK = TicketType.create("neoforge:block", Comparator.comparing(info -> info));
   static final TicketType<ForcedChunkManager.TicketOwner<BlockPos>> BLOCK_TICKING = TicketType.create(
      "neoforge:block_ticking", Comparator.comparing(info -> info)
   );
   static final TicketType<ForcedChunkManager.TicketOwner<UUID>> ENTITY = TicketType.create("neoforge:entity", Comparator.comparing(info -> info));
   static final TicketType<ForcedChunkManager.TicketOwner<UUID>> ENTITY_TICKING = TicketType.create(
      "neoforge:entity_ticking", Comparator.comparing(info -> info)
   );
   private static boolean initialised = false;
   private static Map<ResourceLocation, TicketController> controllers = Map.of();

   @Internal
   public static synchronized void init() {
      if (initialised) {
         throw new UnsupportedOperationException("Cannot init ticket controllers multiple times!");
      } else {
         initialised = true;
         Map<ResourceLocation, TicketController> controllers = new HashMap<>();
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
      ForcedChunksSavedData data = level.getDataStorage().get(new SavedData.Factory<>(ForcedChunksSavedData::new, ForcedChunksSavedData::load), "chunks");
      return data == null ? false : !data.getChunks().isEmpty() || !data.getBlockForcedChunks().isEmpty() || !data.getEntityForcedChunks().isEmpty();
   }

   static <T extends Comparable<? super T>> boolean forceChunk(
      ServerLevel level,
      ResourceLocation id,
      T owner,
      int chunkX,
      int chunkZ,
      boolean add,
      boolean ticking,
      TicketType<ForcedChunkManager.TicketOwner<T>> type,
      Function<ForcedChunksSavedData, ForcedChunkManager.TicketTracker<T>> ticketGetter
   ) {
      if (!controllers.containsKey(id)) {
         throw new IllegalArgumentException("Controller with ID " + id + " is not registered!");
      } else {
         ForcedChunksSavedData saveData = level.getDataStorage().computeIfAbsent(ForcedChunksSavedData.factory(), "chunks");
         ChunkPos pos = new ChunkPos(chunkX, chunkZ);
         long chunk = pos.toLong();
         ForcedChunkManager.TicketTracker<T> tickets = ticketGetter.apply(saveData);
         ForcedChunkManager.TicketOwner<T> ticketOwner = new ForcedChunkManager.TicketOwner<>(id, owner);
         boolean success;
         if (add) {
            success = tickets.add(ticketOwner, chunk, ticking);
            if (success) {
               level.getChunk(chunkX, chunkZ);
            }
         } else {
            success = tickets.remove(ticketOwner, chunk, ticking);
         }

         if (success) {
            saveData.setDirty(true);
            forceChunk(level, pos, type, ticketOwner, add, ticking);
         }

         return success;
      }
   }

   private static <T extends Comparable<? super T>> void forceChunk(
      ServerLevel level,
      ChunkPos pos,
      TicketType<ForcedChunkManager.TicketOwner<T>> type,
      ForcedChunkManager.TicketOwner<T> owner,
      boolean add,
      boolean ticking
   ) {
      if (add) {
         level.getChunkSource().addRegionTicket(type, pos, 2, owner, ticking);
      } else {
         level.getChunkSource().removeRegionTicket(type, pos, 2, owner, ticking);
      }
   }

   @Internal
   public static void reinstatePersistentChunks(ServerLevel level, ForcedChunksSavedData saveData) {
      List<Entry<ResourceLocation, TicketController>> controllers = ForcedChunkManager.controllers
         .entrySet()
         .stream()
         .filter(c -> c.getValue().callback() != null)
         .toList();
      if (!controllers.isEmpty()) {
         Map<ResourceLocation, Map<BlockPos, TicketSet>> blockTickets = gatherTicketsById(saveData.getBlockForcedChunks());
         Map<ResourceLocation, Map<UUID, TicketSet>> entityTickets = gatherTicketsById(saveData.getEntityForcedChunks());
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

      reinstatePersistentChunks(level, BLOCK, saveData.getBlockForcedChunks().chunks, false);
      reinstatePersistentChunks(level, BLOCK_TICKING, saveData.getBlockForcedChunks().tickingChunks, true);
      reinstatePersistentChunks(level, ENTITY, saveData.getEntityForcedChunks().chunks, false);
      reinstatePersistentChunks(level, ENTITY_TICKING, saveData.getEntityForcedChunks().tickingChunks, true);
   }

   private static <T extends Comparable<? super T>> Map<ResourceLocation, Map<T, TicketSet>> gatherTicketsById(ForcedChunkManager.TicketTracker<T> tickets) {
      Map<ResourceLocation, Map<T, TicketSet>> modSortedOwnedChunks = new HashMap<>();
      gatherTicketsById(tickets.chunks, TicketSet::nonTicking, modSortedOwnedChunks);
      gatherTicketsById(tickets.tickingChunks, TicketSet::ticking, modSortedOwnedChunks);
      return modSortedOwnedChunks;
   }

   private static <T extends Comparable<? super T>> void gatherTicketsById(
      Map<ForcedChunkManager.TicketOwner<T>, LongSet> tickets,
      Function<TicketSet, LongSet> typeGetter,
      Map<ResourceLocation, Map<T, TicketSet>> modSortedOwnedChunks
   ) {
      tickets.forEach(
         (owner, values) -> {
            TicketSet pair = modSortedOwnedChunks.computeIfAbsent(owner.id, modId -> new HashMap<>())
               .computeIfAbsent(owner.owner, o -> new TicketSet(new LongOpenHashSet(), new LongOpenHashSet()));
            typeGetter.apply(pair).addAll(values);
         }
      );
   }

   private static <T extends Comparable<? super T>> void reinstatePersistentChunks(
      ServerLevel level, TicketType<ForcedChunkManager.TicketOwner<T>> type, Map<ForcedChunkManager.TicketOwner<T>, LongSet> tickets, boolean ticking
   ) {
      tickets.forEach((owner, values) -> {
         LongIterator var5 = values.iterator();

         while (var5.hasNext()) {
            long chunk = (Long)var5.next();
            forceChunk(level, new ChunkPos(chunk), type, (ForcedChunkManager.TicketOwner<T>)owner, true, ticking);
         }
      });
   }

   @Internal
   public static void writeModForcedChunks(
      CompoundTag nbt, ForcedChunkManager.TicketTracker<BlockPos> blockForcedChunks, ForcedChunkManager.TicketTracker<UUID> entityForcedChunks
   ) {
      if (!blockForcedChunks.isEmpty() || !entityForcedChunks.isEmpty()) {
         Map<ResourceLocation, Long2ObjectMap<CompoundTag>> forcedEntries = new HashMap<>();
         writeForcedChunkOwners(forcedEntries, blockForcedChunks, "Blocks", 10, (pos, forcedBlocks) -> forcedBlocks.add(writeBlockPos(pos)));
         writeForcedChunkOwners(forcedEntries, entityForcedChunks, "Entities", 11, (uuid, forcedEntities) -> forcedEntities.add(NbtUtils.createUUID(uuid)));
         ListTag forcedChunks = new ListTag();

         for (Entry<ResourceLocation, Long2ObjectMap<CompoundTag>> entry : forcedEntries.entrySet()) {
            CompoundTag forcedEntry = new CompoundTag();
            forcedEntry.putString("Controller", entry.getKey().toString());
            ListTag modForced = new ListTag();
            modForced.addAll(entry.getValue().values());
            forcedEntry.put("ModForced", modForced);
            forcedChunks.add(forcedEntry);
         }

         nbt.put("ModForced", forcedChunks);
      }
   }

   private static <T extends Comparable<? super T>> void writeForcedChunkOwners(
      Map<ResourceLocation, Long2ObjectMap<CompoundTag>> forcedEntries,
      ForcedChunkManager.TicketTracker<T> tracker,
      String listKey,
      int listType,
      BiConsumer<T, ListTag> ownerWriter
   ) {
      writeForcedChunkOwners(forcedEntries, tracker.chunks, listKey, listType, ownerWriter);
      writeForcedChunkOwners(forcedEntries, tracker.tickingChunks, "Ticking" + listKey, listType, ownerWriter);
   }

   private static <T extends Comparable<? super T>> void writeForcedChunkOwners(
      Map<ResourceLocation, Long2ObjectMap<CompoundTag>> forcedEntries,
      Map<ForcedChunkManager.TicketOwner<T>, LongSet> forcedChunks,
      String listKey,
      int listType,
      BiConsumer<T, ListTag> ownerWriter
   ) {
      for (Entry<ForcedChunkManager.TicketOwner<T>, LongSet> entry : forcedChunks.entrySet()) {
         Long2ObjectMap<CompoundTag> modForced = forcedEntries.computeIfAbsent(entry.getKey().id, modId -> new Long2ObjectOpenHashMap());
         LongIterator var8 = entry.getValue().iterator();

         while (var8.hasNext()) {
            long chunk = (Long)var8.next();
            CompoundTag modEntry = (CompoundTag)modForced.computeIfAbsent(chunk, chunkPos -> {
               CompoundTag baseEntry = new CompoundTag();
               baseEntry.putLong("Chunk", chunkPos);
               return baseEntry;
            });
            ListTag ownerList = modEntry.getList(listKey, listType);
            ownerWriter.accept(entry.getKey().owner, ownerList);
            modEntry.put(listKey, ownerList);
         }
      }
   }

   @Internal
   public static void readModForcedChunks(
      CompoundTag nbt, ForcedChunkManager.TicketTracker<BlockPos> blockForcedChunks, ForcedChunkManager.TicketTracker<UUID> entityForcedChunks
   ) {
      ListTag forcedChunks = nbt.getList("ModForced", 10);

      for (int i = 0; i < forcedChunks.size(); i++) {
         CompoundTag forcedEntry = forcedChunks.getCompound(i);
         ResourceLocation controllerId;
         if (forcedEntry.contains("Controller", 8)) {
            controllerId = ResourceLocation.parse(forcedEntry.getString("Controller"));
         } else {
            controllerId = ResourceLocation.fromNamespaceAndPath(forcedEntry.getString("Mod"), "default");
         }

         if (controllers.containsKey(controllerId)) {
            ListTag modForced = forcedEntry.getList("ModForced", 10);

            for (int j = 0; j < modForced.size(); j++) {
               CompoundTag modEntry = modForced.getCompound(j);
               long chunkPos = modEntry.getLong("Chunk");
               readBlockForcedChunks(controllerId, chunkPos, modEntry, "Blocks", blockForcedChunks.chunks);
               readBlockForcedChunks(controllerId, chunkPos, modEntry, "TickingBlocks", blockForcedChunks.tickingChunks);
               readEntityForcedChunks(controllerId, chunkPos, modEntry, "Entities", entityForcedChunks.chunks);
               readEntityForcedChunks(controllerId, chunkPos, modEntry, "TickingEntities", entityForcedChunks.tickingChunks);
            }
         } else {
            LOGGER.warn(
               "Found chunk loading data for controller id {} which is currently not available or active - it will be removed from the level save.",
               controllerId
            );
         }
      }
   }

   private static void readBlockForcedChunks(
      ResourceLocation controllerId, long chunkPos, CompoundTag modEntry, String key, Map<ForcedChunkManager.TicketOwner<BlockPos>, LongSet> blockForcedChunks
   ) {
      ListTag forcedBlocks = modEntry.getList(key, 10);

      for (int k = 0; k < forcedBlocks.size(); k++) {
         blockForcedChunks.computeIfAbsent(
               new ForcedChunkManager.TicketOwner<>(controllerId, readBlockPos(forcedBlocks.getCompound(k))), owner -> new LongOpenHashSet()
            )
            .add(chunkPos);
      }
   }

   private static BlockPos readBlockPos(CompoundTag compoundtag) {
      return new BlockPos(compoundtag.getInt("X"), compoundtag.getInt("Y"), compoundtag.getInt("Z"));
   }

   public static CompoundTag writeBlockPos(BlockPos blockpos) {
      CompoundTag compoundtag = new CompoundTag();
      compoundtag.putInt("X", blockpos.getX());
      compoundtag.putInt("Y", blockpos.getY());
      compoundtag.putInt("Z", blockpos.getZ());
      return compoundtag;
   }

   private static void readEntityForcedChunks(
      ResourceLocation controllerId, long chunkPos, CompoundTag modEntry, String key, Map<ForcedChunkManager.TicketOwner<UUID>, LongSet> entityForcedChunks
   ) {
      for (Tag uuid : modEntry.getList(key, 11)) {
         entityForcedChunks.computeIfAbsent(new ForcedChunkManager.TicketOwner<>(controllerId, NbtUtils.loadUUID(uuid)), owner -> new LongOpenHashSet())
            .add(chunkPos);
      }
   }

   static class TicketOwner<T extends Comparable<? super T>> implements Comparable<ForcedChunkManager.TicketOwner<T>> {
      private final ResourceLocation id;
      private final T owner;

      TicketOwner(ResourceLocation id, T owner) {
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
      final Map<ForcedChunkManager.TicketOwner<T>, LongSet> chunks = new HashMap<>();
      final Map<ForcedChunkManager.TicketOwner<T>, LongSet> tickingChunks = new HashMap<>();

      public Map<ForcedChunkManager.TicketOwner<T>, LongSet> getChunks() {
         return Collections.unmodifiableMap(this.chunks);
      }

      public Map<ForcedChunkManager.TicketOwner<T>, LongSet> getTickingChunks() {
         return Collections.unmodifiableMap(this.tickingChunks);
      }

      public boolean isEmpty() {
         return this.chunks.isEmpty() && this.tickingChunks.isEmpty();
      }

      private Map<ForcedChunkManager.TicketOwner<T>, LongSet> getTickets(boolean ticking) {
         return ticking ? this.tickingChunks : this.chunks;
      }

      public boolean remove(ForcedChunkManager.TicketOwner<T> owner, long chunk, boolean ticking) {
         Map<ForcedChunkManager.TicketOwner<T>, LongSet> tickets = this.getTickets(ticking);
         if (tickets.containsKey(owner)) {
            LongSet ticketChunks = tickets.get(owner);
            if (ticketChunks.remove(chunk)) {
               if (ticketChunks.isEmpty()) {
                  tickets.remove(owner);
               }

               return true;
            }
         }

         return false;
      }

      private boolean add(ForcedChunkManager.TicketOwner<T> owner, long chunk, boolean ticking) {
         return this.getTickets(ticking).computeIfAbsent(owner, o -> new LongOpenHashSet()).add(chunk);
      }
   }
}
