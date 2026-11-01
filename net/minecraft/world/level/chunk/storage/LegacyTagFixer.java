package net.minecraft.world.level.chunk.storage;

import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;

@FunctionalInterface
public interface LegacyTagFixer {
   Supplier<LegacyTagFixer> EMPTY = () -> p_459146_ -> p_459146_;

   CompoundTag applyFix(CompoundTag var1);

   default void markChunkDone(ChunkPos p_459001_) {
   }

   default int targetDataVersion() {
      return -1;
   }
}
