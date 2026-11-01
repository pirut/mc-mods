package net.neoforged.neoforge.event.level;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.ApiStatus.Internal;

public abstract class ChunkEvent<T extends ChunkAccess> extends LevelEvent {
   private final T chunk;

   public ChunkEvent(T chunk) {
      super(chunk.getLevel());
      this.chunk = chunk;
   }

   public ChunkEvent(T chunk, LevelAccessor level) {
      super(level);
      this.chunk = chunk;
   }

   public T getChunk() {
      return this.chunk;
   }

   public static class Load extends ChunkEvent<LevelChunk> {
      private final boolean newChunk;

      @Internal
      public Load(LevelChunk chunk, boolean newChunk) {
         super(chunk);
         this.newChunk = newChunk;
      }

      public boolean isNewChunk() {
         return this.newChunk;
      }
   }

   public static class Unload extends ChunkEvent<LevelChunk> {
      public Unload(LevelChunk chunk) {
         super(chunk);
      }
   }
}
