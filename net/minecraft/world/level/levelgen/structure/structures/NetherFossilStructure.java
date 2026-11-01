package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class NetherFossilStructure extends Structure {
   public static final MapCodec<NetherFossilStructure> CODEC = RecordCodecBuilder.mapCodec(
      p_228585_ -> p_228585_.group(settingsCodec(p_228585_), HeightProvider.CODEC.fieldOf("height").forGetter(p_228583_ -> p_228583_.height))
         .apply(p_228585_, NetherFossilStructure::new)
   );
   public final HeightProvider height;

   public NetherFossilStructure(Structure.StructureSettings p_228573_, HeightProvider p_228574_) {
      super(p_228573_);
      this.height = p_228574_;
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext p_228576_) {
      WorldgenRandom $$1 = p_228576_.random();
      int $$2 = p_228576_.chunkPos().getMinBlockX() + $$1.nextInt(16);
      int $$3 = p_228576_.chunkPos().getMinBlockZ() + $$1.nextInt(16);
      int $$4 = p_228576_.chunkGenerator().getSeaLevel();
      WorldGenerationContext $$5 = new WorldGenerationContext(p_228576_.chunkGenerator(), p_228576_.heightAccessor());
      int $$6 = this.height.sample($$1, $$5);
      NoiseColumn $$7 = p_228576_.chunkGenerator().getBaseColumn($$2, $$3, p_228576_.heightAccessor(), p_228576_.randomState());
      BlockPos.MutableBlockPos $$8 = new BlockPos.MutableBlockPos($$2, $$6, $$3);

      while ($$6 > $$4) {
         BlockState $$9 = $$7.getBlock($$6);
         BlockState $$10 = $$7.getBlock(--$$6);
         if ($$9.isAir() && ($$10.is(Blocks.SOUL_SAND) || $$10.isFaceSturdy(EmptyBlockGetter.INSTANCE, $$8.setY($$6), Direction.UP))) {
            break;
         }
      }

      if ($$6 <= $$4) {
         return Optional.empty();
      } else {
         BlockPos $$11 = new BlockPos($$2, $$6, $$3);
         return Optional.of(
            new Structure.GenerationStub(
               $$11, (Consumer<StructurePiecesBuilder>)(p_228581_ -> NetherFossilPieces.addPieces(p_228576_.structureTemplateManager(), p_228581_, $$1, $$11))
            )
         );
      }
   }

   @Override
   public StructureType<?> type() {
      return StructureType.NETHER_FOSSIL;
   }
}
