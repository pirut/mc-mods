package net.neoforged.neoforge.common.extensions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LightChunk;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

public interface IBlockGetterExtension {
   @Nullable
   @NonExtendable
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
