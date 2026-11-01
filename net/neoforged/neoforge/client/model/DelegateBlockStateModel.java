package net.neoforged.neoforge.client.model;

import java.util.List;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

public abstract class DelegateBlockStateModel implements BlockStateModel {
   protected final BlockStateModel delegate;

   protected DelegateBlockStateModel(BlockStateModel delegate) {
      this.delegate = delegate;
   }

   @Deprecated
   @Override
   public void collectParts(RandomSource random, List<BlockModelPart> parts) {
      this.delegate.collectParts(random, parts);
   }

   @Override
   public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockModelPart> parts) {
      this.delegate.collectParts(level, pos, state, random, parts);
   }

   @Deprecated
   @Override
   public TextureAtlasSprite particleIcon() {
      return this.delegate.particleIcon();
   }

   @Override
   public TextureAtlasSprite particleIcon(BlockAndTintGetter level, BlockPos pos, BlockState state) {
      return this.delegate.particleIcon(level, pos, state);
   }
}
