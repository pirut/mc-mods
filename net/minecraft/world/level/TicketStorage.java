package net.minecraft.world.level;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.world.chunk.ForcedChunkManager;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class TicketStorage extends SavedData {
   private static final int INITIAL_TICKET_LIST_CAPACITY = 4;
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Codec<Pair<ChunkPos, Ticket>> TICKET_ENTRY = Codec.mapPair(ChunkPos.CODEC.fieldOf("chunk_pos"), Ticket.CODEC).codec();
   public static final Codec<TicketStorage> CODEC = RecordCodecBuilder.create(
      p_400948_ -> p_400948_.group(TICKET_ENTRY.listOf().optionalFieldOf("tickets", List.of()).forGetter(TicketStorage::packTickets))
         .and(ForcedChunkManager.defineExtraStorageParams())
         .apply(p_400948_, (tickets, neoData) -> ForcedChunkManager.readStoredTickets(TicketStorage::fromPacked, tickets, neoData))
   );
   public static final SavedDataType<TicketStorage> TYPE = new SavedDataType<>("chunks", TicketStorage::new, CODEC, DataFixTypes.SAVED_DATA_FORCED_CHUNKS);
   private final Long2ObjectOpenHashMap<List<Ticket>> tickets;
   private final Long2ObjectOpenHashMap<List<Ticket>> deactivatedTickets;
   private LongSet chunksWithForcedTickets = new LongOpenHashSet();
   @Nullable
   private TicketStorage.ChunkUpdated loadingChunkUpdatedListener;
   @Nullable
   private TicketStorage.ChunkUpdated simulationChunkUpdatedListener;
   private final ForcedChunkManager.TicketTracker<BlockPos> blockForcedChunks = new ForcedChunkManager.TicketTracker<>(
      this, NeoForgeMod.BLOCK_TICKET, NeoForgeMod.BLOCK_WITH_NATURAL_SPAWNING_TICKET
   );
   private final ForcedChunkManager.TicketTracker<UUID> entityForcedChunks = new ForcedChunkManager.TicketTracker<>(
      this, NeoForgeMod.ENTITY_TICKET, NeoForgeMod.ENTITY_WITH_NATURAL_SPAWNING_TICKET
   );
   private LongSet chunksWithForceNaturalSpawning = new LongOpenHashSet();

   private TicketStorage(Long2ObjectOpenHashMap<List<Ticket>> p_393873_, Long2ObjectOpenHashMap<List<Ticket>> p_394615_) {
      this.tickets = p_393873_;
      this.deactivatedTickets = p_394615_;
      this.updateForcedChunks();
      this.updateForcedNaturalSpawning();
   }

   public TicketStorage() {
      this(new Long2ObjectOpenHashMap(4), new Long2ObjectOpenHashMap());
   }

   private static TicketStorage fromPacked(List<Pair<ChunkPos, Ticket>> p_401113_) {
      Long2ObjectOpenHashMap<List<Ticket>> long2objectopenhashmap = new Long2ObjectOpenHashMap();

      for (Pair<ChunkPos, Ticket> pair : p_401113_) {
         ChunkPos chunkpos = (ChunkPos)pair.getFirst();
         List<Ticket> list = (List<Ticket>)long2objectopenhashmap.computeIfAbsent(chunkpos.toLong(), p_393722_ -> new ObjectArrayList(4));
         list.add((Ticket)pair.getSecond());
      }

      return new TicketStorage(new Long2ObjectOpenHashMap(4), long2objectopenhashmap);
   }

   private List<Pair<ChunkPos, Ticket>> packTickets() {
      List<Pair<ChunkPos, Ticket>> list = new ArrayList<>();
      this.forEachTicket((p_400946_, p_400947_) -> {
         if (p_400947_.getType().persist()) {
            list.add(new Pair(p_400946_, p_400947_));
         }
      });
      return list;
   }

   private void forEachTicket(BiConsumer<ChunkPos, Ticket> p_401023_) {
      forEachTicket(p_401023_, this.tickets);
      forEachTicket(p_401023_, this.deactivatedTickets);
   }

   private static void forEachTicket(BiConsumer<ChunkPos, Ticket> p_401366_, Long2ObjectOpenHashMap<List<Ticket>> p_401184_) {
      ObjectIterator var2 = Long2ObjectMaps.fastIterable(p_401184_).iterator();

      while (var2.hasNext()) {
         Entry<List<Ticket>> entry = (Entry<List<Ticket>>)var2.next();
         ChunkPos chunkpos = new ChunkPos(entry.getLongKey());

         for (Ticket ticket : (List)entry.getValue()) {
            p_401366_.accept(chunkpos, ticket);
         }
      }
   }

   public void activateAllDeactivatedTickets() {
      ObjectIterator var1 = Long2ObjectMaps.fastIterable(this.deactivatedTickets).iterator();

      while (var1.hasNext()) {
         Entry<List<Ticket>> entry = (Entry<List<Ticket>>)var1.next();

         for (Ticket ticket : (List)entry.getValue()) {
            this.addTicket(entry.getLongKey(), ticket);
         }
      }

      this.deactivatedTickets.clear();
   }

   public void setLoadingChunkUpdatedListener(@Nullable TicketStorage.ChunkUpdated p_393504_) {
      this.loadingChunkUpdatedListener = p_393504_;
   }

   public void setSimulationChunkUpdatedListener(@Nullable TicketStorage.ChunkUpdated p_394582_) {
      this.simulationChunkUpdatedListener = p_394582_;
   }

   public boolean hasTickets() {
      return !this.tickets.isEmpty();
   }

   public boolean shouldKeepDimensionActive() {
      ObjectIterator var1 = this.tickets.values().iterator();

      while (var1.hasNext()) {
         List<Ticket> list = (List<Ticket>)var1.next();

         for (Ticket ticket : list) {
            if (ticket.getType().shouldKeepDimensionActive()) {
               return true;
            }
         }
      }

      return false;
   }

   public List<Ticket> getTickets(long p_393509_) {
      return (List<Ticket>)this.tickets.getOrDefault(p_393509_, List.of());
   }

   private List<Ticket> getOrCreateTickets(long p_394358_) {
      return (List<Ticket>)this.tickets.computeIfAbsent(p_394358_, p_393919_ -> new ObjectArrayList(4));
   }

   public void addTicketWithRadius(TicketType p_394465_, ChunkPos p_394654_, int p_393587_) {
      Ticket ticket = new Ticket(p_394465_, ChunkLevel.byStatus(FullChunkStatus.FULL) - p_393587_);
      this.addTicket(p_394654_.toLong(), ticket);
   }

   public void addTicket(Ticket p_394208_, ChunkPos p_394290_) {
      this.addTicket(p_394290_.toLong(), p_394208_);
   }

   public boolean addTicket(long p_394247_, Ticket p_394469_) {
      List<Ticket> list = this.getOrCreateTickets(p_394247_);

      for (Ticket ticket : list) {
         if (isTicketSameTypeAndLevel(p_394469_, ticket)) {
            ticket.resetTicksLeft();
            this.setDirty();
            return false;
         }
      }

      int i = getTicketLevelAt(list, true);
      int j = getTicketLevelAt(list, false);
      list.add(p_394469_);
      if (SharedConstants.DEBUG_VERBOSE_SERVER_EVENTS) {
         LOGGER.debug("ATI {} {}", new ChunkPos(p_394247_), p_394469_);
      }

      if (p_394469_.getType().doesSimulate() && p_394469_.getTicketLevel() < i && this.simulationChunkUpdatedListener != null) {
         this.simulationChunkUpdatedListener.update(p_394247_, p_394469_.getTicketLevel(), true);
      }

      if (p_394469_.getType().doesLoad() && p_394469_.getTicketLevel() < j && this.loadingChunkUpdatedListener != null) {
         this.loadingChunkUpdatedListener.update(p_394247_, p_394469_.getTicketLevel(), true);
      }

      if (p_394469_.getType().equals(TicketType.FORCED)) {
         this.chunksWithForcedTickets.add(p_394247_);
      }

      if (p_394469_.getType().forceNaturalSpawning()) {
         this.chunksWithForceNaturalSpawning.add(p_394247_);
      }

      this.setDirty();
      return true;
   }

   private static boolean isTicketSameTypeAndLevel(Ticket p_394344_, Ticket p_394181_) {
      return p_394181_.getType() == p_394344_.getType() && p_394181_.getTicketLevel() == p_394344_.getTicketLevel();
   }

   public int getTicketLevelAt(long p_393578_, boolean p_393891_) {
      return getTicketLevelAt(this.getTickets(p_393578_), p_393891_);
   }

   private static int getTicketLevelAt(List<Ticket> p_394021_, boolean p_393941_) {
      Ticket ticket = getLowestTicket(p_394021_, p_393941_);
      return ticket == null ? ChunkLevel.MAX_LEVEL + 1 : ticket.getTicketLevel();
   }

   @Nullable
   private static Ticket getLowestTicket(@Nullable List<Ticket> p_394073_, boolean p_394430_) {
      if (p_394073_ == null) {
         return null;
      } else {
         Ticket ticket = null;

         for (Ticket ticket1 : p_394073_) {
            if (ticket == null || ticket1.getTicketLevel() < ticket.getTicketLevel()) {
               if (p_394430_ && ticket1.getType().doesSimulate()) {
                  ticket = ticket1;
               } else if (!p_394430_ && ticket1.getType().doesLoad()) {
                  ticket = ticket1;
               }
            }
         }

         return ticket;
      }
   }

   public void removeTicketWithRadius(TicketType p_394013_, ChunkPos p_393657_, int p_394336_) {
      Ticket ticket = new Ticket(p_394013_, ChunkLevel.byStatus(FullChunkStatus.FULL) - p_394336_);
      this.removeTicket(p_393657_.toLong(), ticket);
   }

   public void removeTicket(Ticket p_394399_, ChunkPos p_393510_) {
      this.removeTicket(p_393510_.toLong(), p_394399_);
   }

   public boolean removeTicket(long p_393896_, Ticket p_394054_) {
      List<Ticket> list = (List<Ticket>)this.tickets.get(p_393896_);
      if (list == null) {
         return false;
      } else {
         boolean flag = false;
         Iterator<Ticket> iterator = list.iterator();

         while (iterator.hasNext()) {
            Ticket ticket = iterator.next();
            if (isTicketSameTypeAndLevel(p_394054_, ticket)) {
               iterator.remove();
               if (SharedConstants.DEBUG_VERBOSE_SERVER_EVENTS) {
                  LOGGER.debug("RTI {} {}", new ChunkPos(p_393896_), ticket);
               }

               flag = true;
               break;
            }
         }

         if (!flag) {
            return false;
         } else {
            if (list.isEmpty()) {
               this.tickets.remove(p_393896_);
            }

            if (p_394054_.getType().doesSimulate() && this.simulationChunkUpdatedListener != null) {
               this.simulationChunkUpdatedListener.update(p_393896_, getTicketLevelAt(list, true), false);
            }

            if (p_394054_.getType().doesLoad() && this.loadingChunkUpdatedListener != null) {
               this.loadingChunkUpdatedListener.update(p_393896_, getTicketLevelAt(list, false), false);
            }

            if (p_394054_.getType().equals(TicketType.FORCED)) {
               this.updateForcedChunks();
            }

            if (p_394054_.getType().forceNaturalSpawning()) {
               this.updateForcedNaturalSpawning();
            }

            this.setDirty();
            return true;
         }
      }
   }

   private void updateForcedChunks() {
      this.chunksWithForcedTickets = this.getAllChunksWithTicketThat(p_393889_ -> p_393889_.getType().equals(TicketType.FORCED));
   }

   public String getTicketDebugString(long p_393749_, boolean p_394364_) {
      List<Ticket> list = this.getTickets(p_393749_);
      Ticket ticket = getLowestTicket(list, p_394364_);
      return ticket == null ? "no_ticket" : ticket.toString();
   }

   public void purgeStaleTickets(ChunkMap p_428689_) {
      this.removeTicketIf((p_432597_, p_432598_) -> {
         if (this.canTicketExpire(p_428689_, p_432597_, p_432598_)) {
            p_432597_.decreaseTicksLeft();
            return p_432597_.isTimedOut();
         } else {
            return false;
         }
      }, null);
      this.setDirty();
   }

   private boolean canTicketExpire(ChunkMap p_434716_, Ticket p_433563_, long p_433627_) {
      if (!p_433563_.getType().hasTimeout()) {
         return false;
      } else if (p_433563_.getType().canExpireIfUnloaded()) {
         return true;
      } else {
         ChunkHolder chunkholder = p_434716_.getUpdatingChunkIfPresent(p_433627_);
         return chunkholder == null || chunkholder.isReadyForSaving();
      }
   }

   public void deactivateTicketsOnClosing() {
      this.removeTicketIf((p_394604_, p_435717_) -> p_394604_.getType() != TicketType.UNKNOWN, this.deactivatedTickets);
      this.blockForcedChunks.deactivateTicketsOnClosing();
      this.entityForcedChunks.deactivateTicketsOnClosing();
   }

   public void removeTicketIf(TicketStorage.TicketPredicate p_435606_, @Nullable Long2ObjectOpenHashMap<List<Ticket>> p_393746_) {
      ObjectIterator<Entry<List<Ticket>>> objectiterator = this.tickets.long2ObjectEntrySet().fastIterator();
      boolean flag = false;
      boolean updateNaturalSpawning = false;

      while (objectiterator.hasNext()) {
         Entry<List<Ticket>> entry = (Entry<List<Ticket>>)objectiterator.next();
         Iterator<Ticket> iterator = ((List)entry.getValue()).iterator();
         long i = entry.getLongKey();
         boolean flag1 = false;
         boolean flag2 = false;

         while (iterator.hasNext()) {
            Ticket ticket = iterator.next();
            if (p_435606_.test(ticket, i)) {
               if (p_393746_ != null) {
                  List<Ticket> list = (List<Ticket>)p_393746_.computeIfAbsent(i, p_394107_ -> new ObjectArrayList(((List)entry.getValue()).size()));
                  list.add(ticket);
               }

               iterator.remove();
               if (ticket.getType().doesLoad()) {
                  flag2 = true;
               }

               if (ticket.getType().doesSimulate()) {
                  flag1 = true;
               }

               if (ticket.getType().equals(TicketType.FORCED)) {
                  flag = true;
               }

               if (ticket.getType().forceNaturalSpawning()) {
                  updateNaturalSpawning = true;
               }
            }
         }

         if (flag2 || flag1) {
            if (flag2 && this.loadingChunkUpdatedListener != null) {
               this.loadingChunkUpdatedListener.update(i, getTicketLevelAt((List<Ticket>)entry.getValue(), false), false);
            }

            if (flag1 && this.simulationChunkUpdatedListener != null) {
               this.simulationChunkUpdatedListener.update(i, getTicketLevelAt((List<Ticket>)entry.getValue(), true), false);
            }

            this.setDirty();
            if (((List)entry.getValue()).isEmpty()) {
               objectiterator.remove();
            }
         }
      }

      if (flag) {
         this.updateForcedChunks();
      }

      if (updateNaturalSpawning) {
         this.updateForcedNaturalSpawning();
      }
   }

   public void replaceTicketLevelOfType(int p_393875_, TicketType p_394261_) {
      List<Pair<Ticket, Long>> list = new ArrayList<>();
      ObjectIterator var4 = this.tickets.long2ObjectEntrySet().iterator();

      while (var4.hasNext()) {
         Entry<List<Ticket>> entry = (Entry<List<Ticket>>)var4.next();

         for (Ticket ticket : (List)entry.getValue()) {
            if (ticket.getType() == p_394261_) {
               list.add(Pair.of(ticket, entry.getLongKey()));
            }
         }
      }

      for (Pair<Ticket, Long> pair : list) {
         Long olong = (Long)pair.getSecond();
         Ticket ticket1 = (Ticket)pair.getFirst();
         this.removeTicket(olong, ticket1);
         TicketType tickettype = ticket1.getType();
         this.addTicket(olong, new Ticket(tickettype, p_393875_));
      }
   }

   public boolean updateChunkForced(ChunkPos p_394535_, boolean p_394618_) {
      Ticket ticket = new Ticket(TicketType.FORCED, ChunkMap.FORCED_TICKET_LEVEL);
      return p_394618_ ? this.addTicket(p_394535_.toLong(), ticket) : this.removeTicket(p_394535_.toLong(), ticket);
   }

   public LongSet getForceLoadedChunks() {
      return this.chunksWithForcedTickets;
   }

   private LongSet getAllChunksWithTicketThat(Predicate<Ticket> p_393731_) {
      LongOpenHashSet longopenhashset = new LongOpenHashSet();
      ObjectIterator var3 = Long2ObjectMaps.fastIterable(this.tickets).iterator();

      while (var3.hasNext()) {
         Entry<List<Ticket>> entry = (Entry<List<Ticket>>)var3.next();

         for (Ticket ticket : (List)entry.getValue()) {
            if (p_393731_.test(ticket)) {
               longopenhashset.add(entry.getLongKey());
               break;
            }
         }
      }

      return longopenhashset;
   }

   public ForcedChunkManager.TicketTracker<BlockPos> getBlockForcedChunks() {
      return this.blockForcedChunks;
   }

   public ForcedChunkManager.TicketTracker<UUID> getEntityForcedChunks() {
      return this.entityForcedChunks;
   }

   private void updateForcedNaturalSpawning() {
      this.chunksWithForceNaturalSpawning = this.getAllChunksWithTicketThat(ticket -> ticket.getType().forceNaturalSpawning());
   }

   public boolean shouldForceNaturalSpawning(ChunkPos chunkPos) {
      return this.chunksWithForceNaturalSpawning.contains(chunkPos.toLong());
   }

   @FunctionalInterface
   public interface ChunkUpdated {
      void update(long var1, int var3, boolean var4);
   }

   public interface TicketPredicate {
      boolean test(Ticket var1, long var2);
   }
}
