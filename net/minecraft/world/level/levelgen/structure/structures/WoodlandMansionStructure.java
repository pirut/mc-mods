package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class WoodlandMansionStructure extends Structure {
   public static final MapCodec<WoodlandMansionStructure> CODEC = simpleCodec(WoodlandMansionStructure::new);

   public WoodlandMansionStructure(Structure.StructureSettings p_230225_) {
      super(p_230225_);
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext p_230235_) {
      Rotation $$1 = Rotation.getRandom(p_230235_.random());
      BlockPos $$2 = this.getLowestYIn5by5BoxOffset7Blocks(p_230235_, $$1);
      return $$2.getY() < 60
         ? Optional.empty()
         : Optional.of(new Structure.GenerationStub($$2, (Consumer<StructurePiecesBuilder>)(p_230240_ -> this.generatePieces(p_230240_, p_230235_, $$2, $$1))));
   }

   private void generatePieces(StructurePiecesBuilder p_230242_, Structure.GenerationContext p_230243_, BlockPos p_230244_, Rotation p_230245_) {
      List<WoodlandMansionPieces.WoodlandMansionPiece> $$4 = Lists.newLinkedList();
      WoodlandMansionPieces.generateMansion(p_230243_.structureTemplateManager(), p_230244_, p_230245_, $$4, p_230243_.random());
      $$4.forEach(p_230242_::addPiece);
   }

   @Override
   public void afterPlace(
      WorldGenLevel p_230227_,
      StructureManager p_230228_,
      ChunkGenerator p_230229_,
      RandomSource p_230230_,
      BoundingBox p_230231_,
      ChunkPos p_230232_,
      PiecesContainer p_230233_
   ) {
      BlockPos.MutableBlockPos $$7 = new BlockPos.MutableBlockPos();
      int $$8 = p_230227_.getMinBuildHeight();
      BoundingBox $$9 = p_230233_.calculateBoundingBox();
      int $$10 = $$9.minY();

      for (int $$11 = p_230231_.minX(); $$11 <= p_230231_.maxX(); $$11++) {
         for (int $$12 = p_230231_.minZ(); $$12 <= p_230231_.maxZ(); $$12++) {
            $$7.set($$11, $$10, $$12);
            if (!p_230227_.isEmptyBlock($$7) && $$9.isInside($$7) && p_230233_.isInsidePiece($$7)) {
               for (int $$13 = $$10 - 1; $$13 > $$8; $$13--) {
                  $$7.setY($$13);
                  if (!p_230227_.isEmptyBlock($$7) && !p_230227_.getBlockState($$7).liquid()) {
                     break;
                  }

                  p_230227_.setBlock($$7, Blocks.COBBLESTONE.defaultBlockState(), 2);
               }
            }
         }
      }
   }

   @Override
   public StructureType<?> type() {
      return StructureType.WOODLAND_MANSION;
   }
}
