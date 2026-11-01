package net.neoforged.neoforge.client.model;

import java.util.List;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.EmptyBlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public interface DynamicBlockStateModel extends BlockStateModel {
   @Deprecated
   @Override
   default void collectParts(RandomSource random, List<BlockModelPart> parts) {
      this.collectParts(EmptyBlockAndTintGetter.INSTANCE, BlockPos.ZERO, Blocks.AIR.defaultBlockState(), random, parts);
   }

   @Override
   void collectParts(BlockAndTintGetter var1, BlockPos var2, BlockState var3, RandomSource var4, List<BlockModelPart> var5);
}
