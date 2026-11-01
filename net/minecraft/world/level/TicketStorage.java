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
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.minecraft.SharedConstants;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class TicketStorage extends SavedData {
   private static final int INITIAL_TICKET_LIST_CAPACITY = 4;
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Codec<Pair<ChunkPos, Ticket>> TICKET_ENTRY = Codec.mapPair(ChunkPos.CODEC.fieldOf("chunk_pos"), Ticket.CODEC).codec();
   public static final Codec<TicketStorage> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(TICKET_ENTRY.listOf().optionalFieldOf("tickets", List.of()).forGetter(TicketStorage::packTickets))
         .apply(instance, TicketStorage::fromPacked)
   );
   public static final SavedDataType<TicketStorage> TYPE = new SavedDataType<>("chunks", TicketStorage::new, CODEC, DataFixTypes.SAVED_DATA_FORCED_CHUNKS);
   private final Long2ObjectOpenHashMap<List<Ticket>> tickets;
   private final Long2ObjectOpenHashMap<List<Ticket>> deactivatedTickets;
   private LongSet chunksWithForcedTickets = new LongOpenHashSet();
   @Nullable
   private TicketStorage.ChunkUpdated loadingChunkUpdatedListener;
   @Nullable
   private TicketStorage.ChunkUpdated simulationChunkUpdatedListener;

   private TicketStorage(Long2ObjectOpenHashMap<List<Ticket>> tickets, Long2ObjectOpenHashMap<List<Ticket>> deactivatedTickets) {
      this.tickets = tickets;
      this.deactivatedTickets = deactivatedTickets;
      this.updateForcedChunks();
   }

   public TicketStorage() {
      this(new Long2ObjectOpenHashMap(4), new Long2ObjectOpenHashMap());
   }

   private static TicketStorage fromPacked(List<Pair<ChunkPos, Ticket>> packed) {
      Long2ObjectOpenHashMap<List<Ticket>> long2ObjectOpenHashMap = new Long2ObjectOpenHashMap();

      for (Pair<ChunkPos, Ticket> pair : packed) {
         ChunkPos chunkPos = (ChunkPos)pair.getFirst();
         List<Ticket> list = (List<Ticket>)long2ObjectOpenHashMap.computeIfAbsent(chunkPos.toLong(), l -> new ObjectArrayList(4));
         list.add((Ticket)pair.getSecond());
      }

      return new TicketStorage(new Long2ObjectOpenHashMap(4), long2ObjectOpenHashMap);
   }

   private List<Pair<ChunkPos, Ticket>> packTickets() {
      List<Pair<ChunkPos, Ticket>> list = new ArrayList<>();
      this.forEachTicket((chunkPos, ticket) -> {
         if (ticket.getType().persist()) {
            list.add(new Pair(chunkPos, ticket));
         }
      });
      return list;
   }

   private void forEachTicket(BiConsumer<ChunkPos, Ticket> action) {
      forEachTicket(action, this.tickets);
      forEachTicket(action, this.deactivatedTickets);
   }

   private static void forEachTicket(BiConsumer<ChunkPos, Ticket> action, Long2ObjectOpenHashMap<List<Ticket>> tickets) {
      ObjectIterator var2 = Long2ObjectMaps.fastIterable(tickets).iterator();

      while (var2.hasNext()) {
         Entry<List<Ticket>> entry = (Entry<List<Ticket>>)var2.next();
         ChunkPos chunkPos = new ChunkPos(entry.getLongKey());

         for (Ticket ticket : (List)entry.getValue()) {
            action.accept(chunkPos, ticket);
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

   public void setLoadingChunkUpdatedListener(@Nullable TicketStorage.ChunkUpdated loadingChunkUpdatedListener) {
      this.loadingChunkUpdatedListener = loadingChunkUpdatedListener;
   }

   public void setSimulationChunkUpdatedListener(@Nullable TicketStorage.ChunkUpdated simulationChunkUpdatedListener) {
      this.simulationChunkUpdatedListener = simulationChunkUpdatedListener;
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

   public List<Ticket> getTickets(long chunkPos) {
      return (List<Ticket>)this.tickets.getOrDefault(chunkPos, List.of());
   }

   private List<Ticket> getOrCreateTickets(long chunkPos) {
      return (List<Ticket>)this.tickets.computeIfAbsent(chunkPos, l -> new ObjectArrayList(4));
   }

   public void addTicketWithRadius(TicketType ticketType, ChunkPos chunkPos, int radius) {
      Ticket ticket = new Ticket(ticketType, ChunkLevel.byStatus(FullChunkStatus.FULL) - radius);
      this.addTicket(chunkPos.toLong(), ticket);
   }

   public void addTicket(Ticket ticket, ChunkPos chunkPos) {
      this.addTicket(chunkPos.toLong(), ticket);
   }

   public boolean addTicket(long chunkPos, Ticket ticket) {
      List<Ticket> list = this.getOrCreateTickets(chunkPos);

      for (Ticket ticket2 : list) {
         if (isTicketSameTypeAndLevel(ticket, ticket2)) {
            ticket2.resetTicksLeft();
            this.setDirty();
            return false;
         }
      }

      int i = getTicketLevelAt(list, true);
      int j = getTicketLevelAt(list, false);
      list.add(ticket);
      if (SharedConstants.DEBUG_VERBOSE_SERVER_EVENTS) {
         LOGGER.debug("ATI {} {}", new ChunkPos(chunkPos), ticket);
      }

      if (ticket.getType().doesSimulate() && ticket.getTicketLevel() < i && this.simulationChunkUpdatedListener != null) {
         this.simulationChunkUpdatedListener.update(chunkPos, ticket.getTicketLevel(), true);
      }

      if (ticket.getType().doesLoad() && ticket.getTicketLevel() < j && this.loadingChunkUpdatedListener != null) {
         this.loadingChunkUpdatedListener.update(chunkPos, ticket.getTicketLevel(), true);
      }

      if (ticket.getType().equals(TicketType.FORCED)) {
         this.chunksWithForcedTickets.add(chunkPos);
      }

      this.setDirty();
      return true;
   }

   private static boolean isTicketSameTypeAndLevel(Ticket first, Ticket second) {
      return second.getType() == first.getType() && second.getTicketLevel() == first.getTicketLevel();
   }

   public int getTicketLevelAt(long chunkPos, boolean requireSimulation) {
      return getTicketLevelAt(this.getTickets(chunkPos), requireSimulation);
   }

   private static int getTicketLevelAt(List<Ticket> tickets, boolean requireSimulation) {
      Ticket ticket = getLowestTicket(tickets, requireSimulation);
      return ticket == null ? ChunkLevel.MAX_LEVEL + 1 : ticket.getTicketLevel();
   }

   @Nullable
   private static Ticket getLowestTicket(@Nullable List<Ticket> tickets, boolean requireSimulation) {
      if (tickets == null) {
         return null;
      } else {
         Ticket ticket = null;

         for (Ticket ticket2 : tickets) {
            if (ticket == null || ticket2.getTicketLevel() < ticket.getTicketLevel()) {
               if (requireSimulation && ticket2.getType().doesSimulate()) {
                  ticket = ticket2;
               } else if (!requireSimulation && ticket2.getType().doesLoad()) {
                  ticket = ticket2;
               }
            }
         }

         return ticket;
      }
   }

   public void removeTicketWithRadius(TicketType ticketType, ChunkPos chunkPos, int radius) {
      Ticket ticket = new Ticket(ticketType, ChunkLevel.byStatus(FullChunkStatus.FULL) - radius);
      this.removeTicket(chunkPos.toLong(), ticket);
   }

   public void removeTicket(Ticket ticket, ChunkPos chunkPos) {
      this.removeTicket(chunkPos.toLong(), ticket);
   }

   public boolean removeTicket(long chunkPos, Ticket ticket) {
      List<Ticket> list = (List<Ticket>)this.tickets.get(chunkPos);
      if (list == null) {
         return false;
      } else {
         boolean bl = false;
         Iterator<Ticket> iterator = list.iterator();

         while (iterator.hasNext()) {
            Ticket ticket2 = iterator.next();
            if (isTicketSameTypeAndLevel(ticket, ticket2)) {
               iterator.remove();
               if (SharedConstants.DEBUG_VERBOSE_SERVER_EVENTS) {
                  LOGGER.debug("RTI {} {}", new ChunkPos(chunkPos), ticket2);
               }

               bl = true;
               break;
            }
         }

         if (!bl) {
            return false;
         } else {
            if (list.isEmpty()) {
               this.tickets.remove(chunkPos);
            }

            if (ticket.getType().doesSimulate() && this.simulationChunkUpdatedListener != null) {
               this.simulationChunkUpdatedListener.update(chunkPos, getTicketLevelAt(list, true), false);
            }

            if (ticket.getType().doesLoad() && this.loadingChunkUpdatedListener != null) {
               this.loadingChunkUpdatedListener.update(chunkPos, getTicketLevelAt(list, false), false);
            }

            if (ticket.getType().equals(TicketType.FORCED)) {
               this.updateForcedChunks();
            }

            this.setDirty();
            return true;
         }
      }
   }

   private void updateForcedChunks() {
      this.chunksWithForcedTickets = this.getAllChunksWithTicketThat(ticket -> ticket.getType().equals(TicketType.FORCED));
   }

   public String getTicketDebugString(long chunkPos, boolean requireSimulation) {
      List<Ticket> list = this.getTickets(chunkPos);
      Ticket ticket = getLowestTicket(list, requireSimulation);
      return ticket == null ? "no_ticket" : ticket.toString();
   }

   public void purgeStaleTickets(ChunkMap map) {
      this.removeTicketIf((ticket, l) -> {
         if (this.canTicketExpire(map, ticket, l)) {
            ticket.decreaseTicksLeft();
            return ticket.isTimedOut();
         } else {
            return false;
         }
      }, null);
      this.setDirty();
   }

   private boolean canTicketExpire(ChunkMap chunkMap, Ticket ticket, long chunkPos) {
      if (!ticket.getType().hasTimeout()) {
         return false;
      } else if (ticket.getType().canExpireIfUnloaded()) {
         return true;
      } else {
         ChunkHolder chunkHolder = chunkMap.getUpdatingChunkIfPresent(chunkPos);
         return chunkHolder == null || chunkHolder.isReadyForSaving();
      }
   }

   public void deactivateTicketsOnClosing() {
      this.removeTicketIf((ticket, l) -> ticket.getType() != TicketType.UNKNOWN, this.deactivatedTickets);
   }

   public void removeTicketIf(TicketStorage.TicketPredicate predicate, @Nullable Long2ObjectOpenHashMap<List<Ticket>> tickets) {
      ObjectIterator<Entry<List<Ticket>>> objectIterator = this.tickets.long2ObjectEntrySet().fastIterator();
      boolean bl = false;

      while (objectIterator.hasNext()) {
         Entry<List<Ticket>> entry = (Entry<List<Ticket>>)objectIterator.next();
         Iterator<Ticket> iterator = ((List)entry.getValue()).iterator();
         long l = entry.getLongKey();
         boolean bl2 = false;
         boolean bl3 = false;

         while (iterator.hasNext()) {
            Ticket ticket = iterator.next();
            if (predicate.test(ticket, l)) {
               if (tickets != null) {
                  List<Ticket> list = (List<Ticket>)tickets.computeIfAbsent(l, lx -> new ObjectArrayList(((List)entry.getValue()).size()));
                  list.add(ticket);
               }

               iterator.remove();
               if (ticket.getType().doesLoad()) {
                  bl3 = true;
               }

               if (ticket.getType().doesSimulate()) {
                  bl2 = true;
               }

               if (ticket.getType().equals(TicketType.FORCED)) {
                  bl = true;
               }
            }
         }

         if (bl3 || bl2) {
            if (bl3 && this.loadingChunkUpdatedListener != null) {
               this.loadingChunkUpdatedListener.update(l, getTicketLevelAt((List<Ticket>)entry.getValue(), false), false);
            }

            if (bl2 && this.simulationChunkUpdatedListener != null) {
               this.simulationChunkUpdatedListener.update(l, getTicketLevelAt((List<Ticket>)entry.getValue(), true), false);
            }

            this.setDirty();
            if (((List)entry.getValue()).isEmpty()) {
               objectIterator.remove();
            }
         }
      }

      if (bl) {
         this.updateForcedChunks();
      }
   }

   public void replaceTicketLevelOfType(int level, TicketType type) {
      List<Pair<Ticket, Long>> list = new ArrayList<>();
      ObjectIterator var4 = this.tickets.long2ObjectEntrySet().iterator();

      while (var4.hasNext()) {
         Entry<List<Ticket>> entry = (Entry<List<Ticket>>)var4.next();

         for (Ticket ticket : (List)entry.getValue()) {
            if (ticket.getType() == type) {
               list.add(Pair.of(ticket, entry.getLongKey()));
            }
         }
      }

      for (Pair<Ticket, Long> pair : list) {
         Long long_ = (Long)pair.getSecond();
         Ticket ticketx = (Ticket)pair.getFirst();
         this.removeTicket(long_, ticketx);
         TicketType ticketType = ticketx.getType();
         this.addTicket(long_, new Ticket(ticketType, level));
      }
   }

   public boolean updateChunkForced(ChunkPos chunkPos, boolean add) {
      Ticket ticket = new Ticket(TicketType.FORCED, ChunkMap.FORCED_TICKET_LEVEL);
      return add ? this.addTicket(chunkPos.toLong(), ticket) : this.removeTicket(chunkPos.toLong(), ticket);
   }

   public LongSet getForceLoadedChunks() {
      return this.chunksWithForcedTickets;
   }

   private LongSet getAllChunksWithTicketThat(Predicate<Ticket> predicate) {
      LongOpenHashSet longOpenHashSet = new LongOpenHashSet();
      ObjectIterator var3 = Long2ObjectMaps.fastIterable(this.tickets).iterator();

      while (var3.hasNext()) {
         Entry<List<Ticket>> entry = (Entry<List<Ticket>>)var3.next();

         for (Ticket ticket : (List)entry.getValue()) {
            if (predicate.test(ticket)) {
               longOpenHashSet.add(entry.getLongKey());
               break;
            }
         }
      }

      return longOpenHashSet;
   }

   @FunctionalInterface
   public interface ChunkUpdated {
      void update(long l, int i, boolean bl);
   }

   public interface TicketPredicate {
      boolean test(Ticket ticket, long l);
   }
}
