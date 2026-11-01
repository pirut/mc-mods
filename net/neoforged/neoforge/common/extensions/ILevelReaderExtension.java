package net.neoforged.neoforge.common.extensions;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.LevelReader;

public interface ILevelReaderExtension {
   private LevelReader self() {
      return (LevelReader)this;
   }

   default boolean isAreaLoaded(BlockPos center, int range) {
      return this.self().hasChunksAt(center.offset(-range, -range, -range), center.offset(range, range, range));
   }

   default <T> Holder<T> holderOrThrow(ResourceKey<T> key) {
      return this.self().registryAccess().holderOrThrow(key);
   }

   default <T> Optional<Holder.Reference<T>> holder(ResourceKey<T> key) {
      return this.self().registryAccess().holder(key);
   }
}
