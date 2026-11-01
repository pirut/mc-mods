package net.neoforged.neoforge.client.extensions;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.util.TriState;
import net.minecraft.world.level.block.state.BlockState;

public interface BlockModelPartExtension {
   default ChunkSectionLayer getRenderType(BlockState state) {
      return ItemBlockRenderTypes.getChunkRenderType(state);
   }

   default TriState ambientOcclusion() {
      return this.self().useAmbientOcclusion() ? TriState.DEFAULT : TriState.FALSE;
   }

   private BlockModelPart self() {
      return (BlockModelPart)this;
   }
}
