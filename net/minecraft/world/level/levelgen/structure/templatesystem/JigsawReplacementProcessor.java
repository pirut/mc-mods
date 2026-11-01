package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

public class JigsawReplacementProcessor extends StructureProcessor {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final MapCodec<JigsawReplacementProcessor> CODEC = MapCodec.unit(() -> JigsawReplacementProcessor.INSTANCE);
   public static final JigsawReplacementProcessor INSTANCE = new JigsawReplacementProcessor();

   private JigsawReplacementProcessor() {
   }

   @Nullable
   @Override
   public StructureTemplate.StructureBlockInfo processBlock(
      LevelReader p_74127_,
      BlockPos p_74128_,
      BlockPos p_74129_,
      StructureTemplate.StructureBlockInfo p_74130_,
      StructureTemplate.StructureBlockInfo p_74131_,
      StructurePlaceSettings p_74132_
   ) {
      BlockState $$6 = p_74131_.state();
      if ($$6.is(Blocks.JIGSAW)) {
         if (p_74131_.nbt() == null) {
            LOGGER.warn("Jigsaw block at {} is missing nbt, will not replace", p_74128_);
            return p_74131_;
         } else {
            String $$7 = p_74131_.nbt().getString("final_state");

            BlockState $$9;
            try {
               BlockStateParser.BlockResult $$8 = BlockStateParser.parseForBlock(p_74127_.holderLookup(Registries.BLOCK), $$7, true);
               $$9 = $$8.blockState();
            } catch (CommandSyntaxException var11) {
               LOGGER.error("Failed to parse jigsaw replacement state '{}' at {}: {}", new Object[]{$$7, p_74128_, var11.getMessage()});
               return null;
            }

            return $$9.is(Blocks.STRUCTURE_VOID) ? null : new StructureTemplate.StructureBlockInfo(p_74131_.pos(), $$9, null);
         }
      } else {
         return p_74131_;
      }
   }

   @Override
   protected StructureProcessorType<?> getType() {
      return StructureProcessorType.JIGSAW_REPLACEMENT;
   }
}
