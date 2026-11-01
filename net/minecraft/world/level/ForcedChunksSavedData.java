package net.minecraft.world.level;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.common.world.chunk.ForcedChunkManager;

public class ForcedChunksSavedData extends SavedData {
   public static final String FILE_ID = "chunks";
   private static final String TAG_FORCED = "Forced";
   private final LongSet chunks;
   private final ForcedChunkManager.TicketTracker<BlockPos> blockForcedChunks = new ForcedChunkManager.TicketTracker<>();
   private final ForcedChunkManager.TicketTracker<UUID> entityForcedChunks = new ForcedChunkManager.TicketTracker<>();

   public static SavedData.Factory<ForcedChunksSavedData> factory() {
      return new SavedData.Factory<>(ForcedChunksSavedData::new, ForcedChunksSavedData::load, DataFixTypes.SAVED_DATA_FORCED_CHUNKS);
   }

   private ForcedChunksSavedData(LongSet p_151482_) {
      this.chunks = p_151482_;
   }

   public ForcedChunksSavedData() {
      this(new LongOpenHashSet());
   }

   public static ForcedChunksSavedData load(CompoundTag p_151484_, HolderLookup.Provider p_323940_) {
      ForcedChunksSavedData savedData = new ForcedChunksSavedData(new LongOpenHashSet(p_151484_.getLongArray("Forced")));
      ForcedChunkManager.readModForcedChunks(p_151484_, savedData.blockForcedChunks, savedData.entityForcedChunks);
      return savedData;
   }

   @Override
   public CompoundTag save(CompoundTag p_46120_, HolderLookup.Provider p_324573_) {
      p_46120_.putLongArray("Forced", this.chunks.toLongArray());
      ForcedChunkManager.writeModForcedChunks(p_46120_, this.blockForcedChunks, this.entityForcedChunks);
      return p_46120_;
   }

   public LongSet getChunks() {
      return this.chunks;
   }

   public ForcedChunkManager.TicketTracker<BlockPos> getBlockForcedChunks() {
      return this.blockForcedChunks;
   }

   public ForcedChunkManager.TicketTracker<UUID> getEntityForcedChunks() {
      return this.entityForcedChunks;
   }
}
