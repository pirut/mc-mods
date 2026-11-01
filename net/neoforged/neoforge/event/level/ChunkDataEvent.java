package net.neoforged.neoforge.event.level;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;

public abstract class ChunkDataEvent extends ChunkEvent<ChunkAccess> {
   private final SerializableChunkData data;

   public ChunkDataEvent(ChunkAccess chunk, SerializableChunkData data) {
      super(chunk);
      this.data = data;
   }

   public ChunkDataEvent(ChunkAccess chunk, LevelAccessor world, SerializableChunkData data) {
      super(chunk, world);
      this.data = data;
   }

   public SerializableChunkData getData() {
      return this.data;
   }

   public static class Load extends ChunkDataEvent {
      private final ChunkType status;

      public Load(ChunkAccess chunk, SerializableChunkData data) {
         super(chunk, data);
         this.status = chunk.getPersistedStatus().getChunkType();
      }

      public ChunkType getType() {
         return this.status;
      }
   }

   public static class Save extends ChunkDataEvent {
      public Save(ChunkAccess chunk, LevelAccessor world, SerializableChunkData data) {
         super(chunk, world, data);
      }
   }
}
