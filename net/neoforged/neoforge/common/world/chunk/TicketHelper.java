package net.neoforged.neoforge.common.world.chunk;

import it.unimi.dsi.fastutil.longs.LongIterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.TicketStorage;
import org.jspecify.annotations.Nullable;

public class TicketHelper {
   private final Map<BlockPos, TicketSet> blockTickets;
   private final Map<UUID, TicketSet> entityTickets;
   private final TicketStorage saveData;
   private final Identifier controllerId;

   TicketHelper(TicketStorage saveData, Identifier controllerId, Map<BlockPos, TicketSet> blockTickets, Map<UUID, TicketSet> entityTickets) {
      this.saveData = saveData;
      this.controllerId = controllerId;
      this.blockTickets = blockTickets;
      this.entityTickets = entityTickets;
   }

   public Map<BlockPos, TicketSet> getBlockTickets() {
      return this.blockTickets;
   }

   public Map<UUID, TicketSet> getEntityTickets() {
      return this.entityTickets;
   }

   public void removeAllTickets(BlockPos owner) {
      this.removeAllTickets(this.saveData.getBlockForcedChunks(), owner, this.blockTickets.get(owner));
   }

   public void removeAllTickets(UUID owner) {
      this.removeAllTickets(this.saveData.getEntityForcedChunks(), owner, this.entityTickets.get(owner));
   }

   private <T extends Comparable<? super T>> void removeAllTickets(ForcedChunkManager.TicketTracker<T> tickets, T owner, @Nullable TicketSet existingTickets) {
      if (existingTickets != null && !existingTickets.isEmpty()) {
         ForcedChunkManager.TicketOwner<T> ticketOwner = new ForcedChunkManager.TicketOwner<>(this.controllerId, owner);
         LongIterator var5 = existingTickets.normal().iterator();

         while (var5.hasNext()) {
            long chunk = (Long)var5.next();
            tickets.remove(ticketOwner, chunk, false, true);
         }

         var5 = existingTickets.naturalSpawning().iterator();

         while (var5.hasNext()) {
            long chunk = (Long)var5.next();
            tickets.remove(ticketOwner, chunk, true, true);
         }
      }
   }

   public void removeTicket(BlockPos owner, long chunk, boolean forceNaturalSpawning) {
      TicketSet ticketSet = this.blockTickets.get(owner);
      if (ticketSet != null && ticketSet.getChunks(forceNaturalSpawning).contains(chunk)) {
         this.removeTicket(this.saveData.getBlockForcedChunks(), owner, chunk, forceNaturalSpawning);
      }
   }

   public void removeTicket(UUID owner, long chunk, boolean forceNaturalSpawning) {
      TicketSet ticketSet = this.entityTickets.get(owner);
      if (ticketSet != null && ticketSet.getChunks(forceNaturalSpawning).contains(chunk)) {
         this.removeTicket(this.saveData.getEntityForcedChunks(), owner, chunk, forceNaturalSpawning);
      }
   }

   private <T extends Comparable<? super T>> void removeTicket(ForcedChunkManager.TicketTracker<T> tickets, T owner, long chunk, boolean forceNaturalSpawning) {
      tickets.remove(new ForcedChunkManager.TicketOwner<>(this.controllerId, owner), chunk, forceNaturalSpawning, true);
   }
}
