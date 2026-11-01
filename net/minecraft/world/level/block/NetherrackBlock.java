package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class NetherrackBlock extends Block implements BonemealableBlock {
   public static final MapCodec<NetherrackBlock> CODEC = simpleCodec(NetherrackBlock::new);

   @Override
   public MapCodec<NetherrackBlock> codec() {
      return CODEC;
   }

   public NetherrackBlock(BlockBehaviour.Properties p_54995_) {
      super(p_54995_);
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader p_256620_, BlockPos p_55003_, BlockState p_55004_) {
      if (!p_256620_.getBlockState(p_55003_.above()).propagatesSkylightDown(p_256620_, p_55003_)) {
         return false;
      } else {
         for (BlockPos $$3 : BlockPos.betweenClosed(p_55003_.offset(-1, -1, -1), p_55003_.offset(1, 1, 1))) {
            if (p_256620_.getBlockState($$3).is(BlockTags.NYLIUM)) {
               return true;
            }
         }

         return false;
      }
   }

   @Override
   public boolean isBonemealSuccess(Level p_221816_, RandomSource p_221817_, BlockPos p_221818_, BlockState p_221819_) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel p_221811_, RandomSource p_221812_, BlockPos p_221813_, BlockState p_221814_) {
      boolean $$4 = false;
      boolean $$5 = false;

      for (BlockPos $$6 : BlockPos.betweenClosed(p_221813_.offset(-1, -1, -1), p_221813_.offset(1, 1, 1))) {
         BlockState $$7 = p_221811_.getBlockState($$6);
         if ($$7.is(Blocks.WARPED_NYLIUM)) {
            $$5 = true;
         }

         if ($$7.is(Blocks.CRIMSON_NYLIUM)) {
            $$4 = true;
         }

         if ($$5 && $$4) {
            break;
         }
      }

      if ($$5 && $$4) {
         p_221811_.setBlock(p_221813_, p_221812_.nextBoolean() ? Blocks.WARPED_NYLIUM.defaultBlockState() : Blocks.CRIMSON_NYLIUM.defaultBlockState(), 3);
      } else if ($$5) {
         p_221811_.setBlock(p_221813_, Blocks.WARPED_NYLIUM.defaultBlockState(), 3);
      } else if ($$4) {
         p_221811_.setBlock(p_221813_, Blocks.CRIMSON_NYLIUM.defaultBlockState(), 3);
      }
   }

   @Override
   public BonemealableBlock.Type getType() {
      return BonemealableBlock.Type.NEIGHBOR_SPREADER;
   }
}
