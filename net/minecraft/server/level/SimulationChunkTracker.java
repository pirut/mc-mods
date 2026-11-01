package net.minecraft.server.level;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TicketStorage;

public class SimulationChunkTracker extends ChunkTracker {
   public static final int MAX_LEVEL = 33;
   protected final Long2ByteMap chunks = new Long2ByteOpenHashMap();
   private final TicketStorage ticketStorage;

   public SimulationChunkTracker(TicketStorage p_393548_) {
      super(34, 16, 256);
      this.ticketStorage = p_393548_;
      p_393548_.setSimulationChunkUpdatedListener(this::update);
      this.chunks.defaultReturnValue((byte)33);
   }

   @Override
   protected int getLevelFromSource(long p_394279_) {
      return this.ticketStorage.getTicketLevelAt(p_394279_, true);
   }

   public int getLevel(ChunkPos p_393600_) {
      return this.getLevel(p_393600_.toLong());
   }

   @Override
   protected int getLevel(long p_393916_) {
      return this.chunks.get(p_393916_);
   }

   @Override
   protected void setLevel(long p_393860_, int p_393864_) {
      if (p_393864_ >= 33) {
         this.chunks.remove(p_393860_);
      } else {
         this.chunks.put(p_393860_, (byte)p_393864_);
      }
   }

   public void runAllUpdates() {
      this.runUpdates(Integer.MAX_VALUE);
   }
}
