package net.minecraft.server.level;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.level.ChunkPos;

public class TickingTracker extends ChunkTracker {
   public static final int MAX_LEVEL = 33;
   private static final int INITIAL_TICKET_LIST_CAPACITY = 4;
   protected final Long2ByteMap chunks = new Long2ByteOpenHashMap();
   private final Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> tickets = new Long2ObjectOpenHashMap();

   public TickingTracker() {
      super(34, 16, 256);
      this.chunks.defaultReturnValue((byte)33);
   }

   private SortedArraySet<Ticket<?>> getTickets(long p_184178_) {
      return (SortedArraySet<Ticket<?>>)this.tickets.computeIfAbsent(p_184178_, p_184180_ -> SortedArraySet.create(4));
   }

   private int getTicketLevelAt(SortedArraySet<Ticket<?>> p_184160_) {
      return p_184160_.isEmpty() ? 34 : p_184160_.first().getTicketLevel();
   }

   public void addTicket(long p_184152_, Ticket<?> p_184153_) {
      SortedArraySet<Ticket<?>> $$2 = this.getTickets(p_184152_);
      int $$3 = this.getTicketLevelAt($$2);
      $$2.add(p_184153_);
      if (p_184153_.getTicketLevel() < $$3) {
         this.update(p_184152_, p_184153_.getTicketLevel(), true);
      }
   }

   public void removeTicket(long p_184166_, Ticket<?> p_184167_) {
      SortedArraySet<Ticket<?>> $$2 = this.getTickets(p_184166_);
      $$2.remove(p_184167_);
      if ($$2.isEmpty()) {
         this.tickets.remove(p_184166_);
      }

      this.update(p_184166_, this.getTicketLevelAt($$2), false);
   }

   public <T> void addTicket(TicketType<T> p_184155_, ChunkPos p_184156_, int p_184157_, T p_184158_) {
      this.addTicket(p_184156_.toLong(), new Ticket<>(p_184155_, p_184157_, p_184158_));
   }

   public <T> void removeTicket(TicketType<T> p_184169_, ChunkPos p_184170_, int p_184171_, T p_184172_) {
      Ticket<T> $$4 = new Ticket<>(p_184169_, p_184171_, p_184172_);
      this.removeTicket(p_184170_.toLong(), $$4);
   }

   public void replacePlayerTicketsLevel(int p_184147_) {
      List<Pair<Ticket<ChunkPos>, Long>> $$1 = new ArrayList<>();
      ObjectIterator var3 = this.tickets.long2ObjectEntrySet().iterator();

      while (var3.hasNext()) {
         Entry<SortedArraySet<Ticket<?>>> $$2 = (Entry<SortedArraySet<Ticket<?>>>)var3.next();

         for (Ticket<?> $$3 : (SortedArraySet)$$2.getValue()) {
            if ($$3.getType() == TicketType.PLAYER) {
               $$1.add(Pair.of($$3, $$2.getLongKey()));
            }
         }
      }

      for (Pair<Ticket<ChunkPos>, Long> $$4 : $$1) {
         Long $$5 = (Long)$$4.getSecond();
         Ticket<ChunkPos> $$6 = (Ticket<ChunkPos>)$$4.getFirst();
         this.removeTicket($$5, $$6);
         ChunkPos $$7 = new ChunkPos($$5);
         TicketType<ChunkPos> $$8 = $$6.getType();
         this.addTicket($$8, $$7, p_184147_, $$7);
      }
   }

   @Override
   protected int getLevelFromSource(long p_184164_) {
      SortedArraySet<Ticket<?>> $$1 = (SortedArraySet<Ticket<?>>)this.tickets.get(p_184164_);
      return $$1 != null && !$$1.isEmpty() ? $$1.first().getTicketLevel() : Integer.MAX_VALUE;
   }

   public int getLevel(ChunkPos p_184162_) {
      return this.getLevel(p_184162_.toLong());
   }

   @Override
   protected int getLevel(long p_184174_) {
      return this.chunks.get(p_184174_);
   }

   @Override
   protected void setLevel(long p_184149_, int p_184150_) {
      if (p_184150_ >= 33) {
         this.chunks.remove(p_184149_);
      } else {
         this.chunks.put(p_184149_, (byte)p_184150_);
      }
   }

   public void runAllUpdates() {
      this.runUpdates(Integer.MAX_VALUE);
   }

   public String getTicketDebugString(long p_184176_) {
      SortedArraySet<Ticket<?>> $$1 = (SortedArraySet<Ticket<?>>)this.tickets.get(p_184176_);
      return $$1 != null && !$$1.isEmpty() ? $$1.first().toString() : "no_ticket";
   }
}
