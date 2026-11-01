package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class LavaSubmergedBlockProcessor extends StructureProcessor {
   public static final MapCodec<LavaSubmergedBlockProcessor> CODEC = MapCodec.unit(() -> LavaSubmergedBlockProcessor.INSTANCE);
   public static final LavaSubmergedBlockProcessor INSTANCE = new LavaSubmergedBlockProcessor();

   @Nullable
   @Override
   public StructureTemplate.StructureBlockInfo processBlock(
      LevelReader p_74140_,
      BlockPos p_74141_,
      BlockPos p_74142_,
      StructureTemplate.StructureBlockInfo p_74143_,
      StructureTemplate.StructureBlockInfo p_74144_,
      StructurePlaceSettings p_74145_
   ) {
      BlockPos $$6 = p_74144_.pos();
      boolean $$7 = p_74140_.getBlockState($$6).is(Blocks.LAVA);
      return $$7 && !Block.isShapeFullBlock(p_74144_.state().getShape(p_74140_, $$6))
         ? new StructureTemplate.StructureBlockInfo($$6, Blocks.LAVA.defaultBlockState(), p_74144_.nbt())
         : p_74144_;
   }

   @Override
   protected StructureProcessorType<?> getType() {
      return StructureProcessorType.LAVA_SUBMERGED_BLOCK;
   }
}
