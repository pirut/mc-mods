package net.neoforged.neoforge.common.world.chunk;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.TicketStorage;
import org.jspecify.annotations.Nullable;

public record TicketController(Identifier id, @Nullable LoadingValidationCallback callback) {
   public TicketController(Identifier id, @Nullable LoadingValidationCallback callback) {
      Objects.requireNonNull(id, "id must not be null");
      this.id = id;
      this.callback = callback;
   }

   public TicketController(Identifier id) {
      this(id, null);
   }

   public boolean forceChunk(ServerLevel level, BlockPos owner, int chunkX, int chunkZ, boolean add, boolean forceNaturalSpawning) {
      return ForcedChunkManager.forceChunk(level, this.id, owner, chunkX, chunkZ, add, forceNaturalSpawning, TicketStorage::getBlockForcedChunks);
   }

   public boolean forceChunk(ServerLevel level, Entity owner, int chunkX, int chunkZ, boolean add, boolean forceNaturalSpawning) {
      return this.forceChunk(level, owner.getUUID(), chunkX, chunkZ, add, forceNaturalSpawning);
   }

   public boolean forceChunk(ServerLevel level, UUID owner, int chunkX, int chunkZ, boolean add, boolean ticking) {
      return ForcedChunkManager.forceChunk(level, this.id(), owner, chunkX, chunkZ, add, ticking, TicketStorage::getEntityForcedChunks);
   }
}
