package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.SinglePieceStructure;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class DesertPyramidStructure extends SinglePieceStructure {
   public static final MapCodec<DesertPyramidStructure> CODEC = simpleCodec(DesertPyramidStructure::new);

   public DesertPyramidStructure(Structure.StructureSettings p_227418_) {
      super(DesertPyramidPiece::new, 21, 21, p_227418_);
   }

   @Override
   public void afterPlace(
      WorldGenLevel p_273644_,
      StructureManager p_272615_,
      ChunkGenerator p_273655_,
      RandomSource p_272939_,
      BoundingBox p_273179_,
      ChunkPos p_273334_,
      PiecesContainer p_273575_
   ) {
      Set<BlockPos> $$7 = SortedArraySet.create(Vec3i::compareTo);

      for (StructurePiece $$8 : p_273575_.pieces()) {
         if ($$8 instanceof DesertPyramidPiece $$9) {
            $$7.addAll($$9.getPotentialSuspiciousSandWorldPositions());
            placeSuspiciousSand(p_273179_, p_273644_, $$9.getRandomCollapsedRoofPos());
         }
      }

      ObjectArrayList<BlockPos> $$10 = new ObjectArrayList($$7.stream().toList());
      RandomSource $$11 = RandomSource.create(p_273644_.getSeed()).forkPositional().at(p_273575_.calculateBoundingBox().getCenter());
      Util.shuffle($$10, $$11);
      int $$12 = Math.min($$7.size(), $$11.nextInt(5, 8));
      ObjectListIterator var12 = $$10.iterator();

      while (var12.hasNext()) {
         BlockPos $$13 = (BlockPos)var12.next();
         if ($$12 > 0) {
            $$12--;
            placeSuspiciousSand(p_273179_, p_273644_, $$13);
         } else if (p_273179_.isInside($$13)) {
            p_273644_.setBlock($$13, Blocks.SAND.defaultBlockState(), 2);
         }
      }
   }

   private static void placeSuspiciousSand(BoundingBox p_279472_, WorldGenLevel p_279193_, BlockPos p_279136_) {
      if (p_279472_.isInside(p_279136_)) {
         p_279193_.setBlock(p_279136_, Blocks.SUSPICIOUS_SAND.defaultBlockState(), 2);
         p_279193_.getBlockEntity(p_279136_, BlockEntityType.BRUSHABLE_BLOCK)
            .ifPresent(p_335309_ -> p_335309_.setLootTable(BuiltInLootTables.DESERT_PYRAMID_ARCHAEOLOGY, p_279136_.asLong()));
      }
   }

   @Override
   public StructureType<?> type() {
      return StructureType.DESERT_PYRAMID;
   }
}
