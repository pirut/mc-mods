package net.minecraft.client.renderer.block;

import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.world.level.block.state.BlockState;

@Environment(EnvType.CLIENT)
public class BlockModelShaper {
   private Map<BlockState, BlockStateModel> modelByStateCache = Map.of();
   private final ModelManager modelManager;

   public BlockModelShaper(ModelManager modelManager) {
      this.modelManager = modelManager;
   }

   public TextureAtlasSprite getParticleIcon(BlockState state) {
      return this.getBlockModel(state).particleIcon();
   }

   public BlockStateModel getBlockModel(BlockState state) {
      BlockStateModel blockStateModel = this.modelByStateCache.get(state);
      if (blockStateModel == null) {
         blockStateModel = this.modelManager.getMissingBlockStateModel();
      }

      return blockStateModel;
   }

   public ModelManager getModelManager() {
      return this.modelManager;
   }

   public void replaceCache(Map<BlockState, BlockStateModel> modelByStateCache) {
      this.modelByStateCache = modelByStateCache;
   }
}
