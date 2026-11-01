package net.minecraft.client.renderer.block;

import java.util.Map;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.EmptyBlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockModelShaper {
   private Map<BlockState, BlockStateModel> modelByStateCache = Map.of();
   private final ModelManager modelManager;

   public BlockModelShaper(ModelManager p_110880_) {
      this.modelManager = p_110880_;
   }

   @Deprecated
   public TextureAtlasSprite getParticleIcon(BlockState p_110883_) {
      return this.getParticleIcon(p_110883_, EmptyBlockAndTintGetter.INSTANCE, BlockPos.ZERO);
   }

   public TextureAtlasSprite getParticleIcon(BlockState p_110883_, BlockAndTintGetter level, BlockPos pos) {
      BlockStateModel model = this.getBlockModel(p_110883_);
      return model.particleIcon(level, pos, p_110883_);
   }

   public BlockStateModel getBlockModel(BlockState p_110894_) {
      BlockStateModel blockstatemodel = this.modelByStateCache.get(p_110894_);
      if (blockstatemodel == null) {
         blockstatemodel = this.modelManager.getMissingBlockStateModel();
      }

      return blockstatemodel;
   }

   public ModelManager getModelManager() {
      return this.modelManager;
   }

   public void replaceCache(Map<BlockState, BlockStateModel> p_248582_) {
      this.modelByStateCache = p_248582_;
   }
}
