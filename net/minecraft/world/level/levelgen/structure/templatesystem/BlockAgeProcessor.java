package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

public class BlockAgeProcessor extends StructureProcessor {
   public static final MapCodec<BlockAgeProcessor> CODEC = Codec.FLOAT.fieldOf("mossiness").xmap(BlockAgeProcessor::new, p_74023_ -> p_74023_.mossiness);
   private static final float PROBABILITY_OF_REPLACING_FULL_BLOCK = 0.5F;
   private static final float PROBABILITY_OF_REPLACING_STAIRS = 0.5F;
   private static final float PROBABILITY_OF_REPLACING_OBSIDIAN = 0.15F;
   private static final BlockState[] NON_MOSSY_REPLACEMENTS = new BlockState[]{
      Blocks.STONE_SLAB.defaultBlockState(), Blocks.STONE_BRICK_SLAB.defaultBlockState()
   };
   private final float mossiness;

   public BlockAgeProcessor(float p_74013_) {
      this.mossiness = p_74013_;
   }

   @Nullable
   @Override
   public StructureTemplate.StructureBlockInfo processBlock(
      LevelReader p_74016_,
      BlockPos p_74017_,
      BlockPos p_74018_,
      StructureTemplate.StructureBlockInfo p_74019_,
      StructureTemplate.StructureBlockInfo p_74020_,
      StructurePlaceSettings p_74021_
   ) {
      RandomSource $$6 = p_74021_.getRandom(p_74020_.pos());
      BlockState $$7 = p_74020_.state();
      BlockPos $$8 = p_74020_.pos();
      BlockState $$9 = null;
      if ($$7.is(Blocks.STONE_BRICKS) || $$7.is(Blocks.STONE) || $$7.is(Blocks.CHISELED_STONE_BRICKS)) {
         $$9 = this.maybeReplaceFullStoneBlock($$6);
      } else if ($$7.is(BlockTags.STAIRS)) {
         $$9 = this.maybeReplaceStairs($$6, p_74020_.state());
      } else if ($$7.is(BlockTags.SLABS)) {
         $$9 = this.maybeReplaceSlab($$6);
      } else if ($$7.is(BlockTags.WALLS)) {
         $$9 = this.maybeReplaceWall($$6);
      } else if ($$7.is(Blocks.OBSIDIAN)) {
         $$9 = this.maybeReplaceObsidian($$6);
      }

      return $$9 != null ? new StructureTemplate.StructureBlockInfo($$8, $$9, p_74020_.nbt()) : p_74020_;
   }

   @Nullable
   private BlockState maybeReplaceFullStoneBlock(RandomSource p_230256_) {
      if (p_230256_.nextFloat() >= 0.5F) {
         return null;
      } else {
         BlockState[] $$1 = new BlockState[]{Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), getRandomFacingStairs(p_230256_, Blocks.STONE_BRICK_STAIRS)};
         BlockState[] $$2 = new BlockState[]{Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), getRandomFacingStairs(p_230256_, Blocks.MOSSY_STONE_BRICK_STAIRS)};
         return this.getRandomBlock(p_230256_, $$1, $$2);
      }
   }

   @Nullable
   private BlockState maybeReplaceStairs(RandomSource p_230261_, BlockState p_230262_) {
      Direction $$2 = p_230262_.getValue(StairBlock.FACING);
      Half $$3 = p_230262_.getValue(StairBlock.HALF);
      if (p_230261_.nextFloat() >= 0.5F) {
         return null;
      } else {
         BlockState[] $$4 = new BlockState[]{
            Blocks.MOSSY_STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, $$2).setValue(StairBlock.HALF, $$3),
            Blocks.MOSSY_STONE_BRICK_SLAB.defaultBlockState()
         };
         return this.getRandomBlock(p_230261_, NON_MOSSY_REPLACEMENTS, $$4);
      }
   }

   @Nullable
   private BlockState maybeReplaceSlab(RandomSource p_230271_) {
      return p_230271_.nextFloat() < this.mossiness ? Blocks.MOSSY_STONE_BRICK_SLAB.defaultBlockState() : null;
   }

   @Nullable
   private BlockState maybeReplaceWall(RandomSource p_230273_) {
      return p_230273_.nextFloat() < this.mossiness ? Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState() : null;
   }

   @Nullable
   private BlockState maybeReplaceObsidian(RandomSource p_230275_) {
      return p_230275_.nextFloat() < 0.15F ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : null;
   }

   private static BlockState getRandomFacingStairs(RandomSource p_230258_, Block p_230259_) {
      return p_230259_.defaultBlockState()
         .setValue(StairBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(p_230258_))
         .setValue(StairBlock.HALF, Util.getRandom(Half.values(), p_230258_));
   }

   private BlockState getRandomBlock(RandomSource p_230267_, BlockState[] p_230268_, BlockState[] p_230269_) {
      return p_230267_.nextFloat() < this.mossiness ? getRandomBlock(p_230267_, p_230269_) : getRandomBlock(p_230267_, p_230268_);
   }

   private static BlockState getRandomBlock(RandomSource p_230264_, BlockState[] p_230265_) {
      return p_230265_[p_230264_.nextInt(p_230265_.length)];
   }

   @Override
   protected StructureProcessorType<?> getType() {
      return StructureProcessorType.BLOCK_AGE;
   }
}
