package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.ServerLevelAccessor;

public class CappedProcessor extends StructureProcessor {
   public static final MapCodec<CappedProcessor> CODEC = RecordCodecBuilder.mapCodec(
      p_277598_ -> p_277598_.group(
            StructureProcessorType.SINGLE_CODEC.fieldOf("delegate").forGetter(p_277456_ -> p_277456_.delegate),
            IntProvider.POSITIVE_CODEC.fieldOf("limit").forGetter(p_277680_ -> p_277680_.limit)
         )
         .apply(p_277598_, CappedProcessor::new)
   );
   private final StructureProcessor delegate;
   private final IntProvider limit;

   public CappedProcessor(StructureProcessor p_277972_, IntProvider p_277402_) {
      this.delegate = p_277972_;
      this.limit = p_277402_;
   }

   @Override
   protected StructureProcessorType<?> getType() {
      return StructureProcessorType.CAPPED;
   }

   @Override
   public final List<StructureTemplate.StructureBlockInfo> finalizeProcessing(
      ServerLevelAccessor p_278291_,
      BlockPos p_278055_,
      BlockPos p_277825_,
      List<StructureTemplate.StructureBlockInfo> p_277746_,
      List<StructureTemplate.StructureBlockInfo> p_277676_,
      StructurePlaceSettings p_277728_
   ) {
      if (this.limit.getMaxValue() != 0 && !p_277676_.isEmpty()) {
         if (p_277746_.size() != p_277676_.size()) {
            Util.logAndPauseIfInIde(
               "Original block info list not in sync with processed list, skipping processing. Original size: "
                  + p_277746_.size()
                  + ", Processed size: "
                  + p_277676_.size()
            );
            return p_277676_;
         } else {
            RandomSource $$6 = RandomSource.create(p_278291_.getLevel().getSeed()).forkPositional().at(p_278055_);
            int $$7 = Math.min(this.limit.sample($$6), p_277676_.size());
            if ($$7 < 1) {
               return p_277676_;
            } else {
               IntArrayList $$8 = Util.toShuffledList(IntStream.range(0, p_277676_.size()), $$6);
               IntIterator $$9 = $$8.intIterator();
               int $$10 = 0;

               while ($$9.hasNext() && $$10 < $$7) {
                  int $$11 = $$9.nextInt();
                  StructureTemplate.StructureBlockInfo $$12 = p_277746_.get($$11);
                  StructureTemplate.StructureBlockInfo $$13 = p_277676_.get($$11);
                  StructureTemplate.StructureBlockInfo $$14 = this.delegate.processBlock(p_278291_, p_278055_, p_277825_, $$12, $$13, p_277728_);
                  if ($$14 != null && !$$13.equals($$14)) {
                     $$10++;
                     p_277676_.set($$11, $$14);
                  }
               }

               return p_277676_;
            }
         }
      } else {
         return p_277676_;
      }
   }
}
