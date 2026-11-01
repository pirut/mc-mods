package net.neoforged.neoforge.common.world.chunk;

import java.util.Objects;
import java.util.UUID;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ForcedChunksSavedData;
import org.jetbrains.annotations.Nullable;

@ParametersAreNonnullByDefault
public record TicketController(ResourceLocation id, @Nullable LoadingValidationCallback callback) {
   public TicketController(ResourceLocation id, @Nullable LoadingValidationCallback callback) {
      Objects.requireNonNull(id, "id must not be null");
      this.id = id;
      this.callback = callback;
   }

   public TicketController(ResourceLocation id) {
      this(id, null);
   }

   public boolean forceChunk(ServerLevel level, BlockPos owner, int chunkX, int chunkZ, boolean add, boolean ticking) {
      return ForcedChunkManager.forceChunk(
         level,
         this.id,
         owner,
         chunkX,
         chunkZ,
         add,
         ticking,
         ticking ? ForcedChunkManager.BLOCK_TICKING : ForcedChunkManager.BLOCK,
         ForcedChunksSavedData::getBlockForcedChunks
      );
   }

   public boolean forceChunk(ServerLevel level, Entity owner, int chunkX, int chunkZ, boolean add, boolean ticking) {
      return this.forceChunk(level, owner.getUUID(), chunkX, chunkZ, add, ticking);
   }

   public boolean forceChunk(ServerLevel level, UUID owner, int chunkX, int chunkZ, boolean add, boolean ticking) {
      return ForcedChunkManager.forceChunk(
         level,
         this.id(),
         owner,
         chunkX,
         chunkZ,
         add,
         ticking,
         ticking ? ForcedChunkManager.ENTITY_TICKING : ForcedChunkManager.ENTITY,
         ForcedChunksSavedData::getEntityForcedChunks
      );
   }
}
