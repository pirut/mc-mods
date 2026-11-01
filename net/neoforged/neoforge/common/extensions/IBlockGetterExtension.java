package net.neoforged.neoforge.common.extensions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LightChunk;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import net.neoforged.neoforge.model.data.ModelData;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jspecify.annotations.Nullable;

public interface IBlockGetterExtension {
   @NonExtendable
   @Nullable
   default AuxiliaryLightManager getAuxLightManager(BlockPos pos) {
      return this.getAuxLightManager(new ChunkPos(pos));
   }

   @Nullable
   default AuxiliaryLightManager getAuxLightManager(ChunkPos pos) {
      if (this instanceof LevelAccessor level) {
         LightChunk chunk = level.getChunkSource().getChunkForLighting(pos.x, pos.z);
         return chunk != null ? chunk.getAuxLightManager(pos) : null;
      } else {
         return this instanceof ImposterProtoChunk chunk ? chunk.getWrapped().getAuxLightManager(pos) : null;
      }
   }

   default ModelData getModelData(BlockPos pos) {
      return ModelData.EMPTY;
   }
}
