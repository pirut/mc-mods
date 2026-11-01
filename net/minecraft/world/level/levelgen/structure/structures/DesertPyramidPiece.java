package net.minecraft.world.level.levelgen.structure.structures;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class DesertPyramidPiece extends ScatteredFeaturePiece {
   public static final int WIDTH = 21;
   public static final int DEPTH = 21;
   private final boolean[] hasPlacedChest = new boolean[4];
   private final List<BlockPos> potentialSuspiciousSandWorldPositions = new ArrayList<>();
   private BlockPos randomCollapsedRoofPos = BlockPos.ZERO;

   public DesertPyramidPiece(RandomSource p_227399_, int p_227400_, int p_227401_) {
      super(StructurePieceType.DESERT_PYRAMID_PIECE, p_227400_, 64, p_227401_, 21, 15, 21, getRandomHorizontalDirection(p_227399_));
   }

   public DesertPyramidPiece(CompoundTag p_227403_) {
      super(StructurePieceType.DESERT_PYRAMID_PIECE, p_227403_);
      this.hasPlacedChest[0] = p_227403_.getBoolean("hasPlacedChest0");
      this.hasPlacedChest[1] = p_227403_.getBoolean("hasPlacedChest1");
      this.hasPlacedChest[2] = p_227403_.getBoolean("hasPlacedChest2");
      this.hasPlacedChest[3] = p_227403_.getBoolean("hasPlacedChest3");
   }

   @Override
   protected void addAdditionalSaveData(StructurePieceSerializationContext p_227413_, CompoundTag p_227414_) {
      super.addAdditionalSaveData(p_227413_, p_227414_);
      p_227414_.putBoolean("hasPlacedChest0", this.hasPlacedChest[0]);
      p_227414_.putBoolean("hasPlacedChest1", this.hasPlacedChest[1]);
      p_227414_.putBoolean("hasPlacedChest2", this.hasPlacedChest[2]);
      p_227414_.putBoolean("hasPlacedChest3", this.hasPlacedChest[3]);
   }

   @Override
   public void postProcess(
      WorldGenLevel p_227405_,
      StructureManager p_227406_,
      ChunkGenerator p_227407_,
      RandomSource p_227408_,
      BoundingBox p_227409_,
      ChunkPos p_227410_,
      BlockPos p_227411_
   ) {
      if (this.updateHeightPositionToLowestGroundHeight(p_227405_, -p_227408_.nextInt(3))) {
         this.generateBox(
            p_227405_,
            p_227409_,
            0,
            -4,
            0,
            this.width - 1,
            0,
            this.depth - 1,
            Blocks.SANDSTONE.defaultBlockState(),
            Blocks.SANDSTONE.defaultBlockState(),
            false
         );

         for (int $$7 = 1; $$7 <= 9; $$7++) {
            this.generateBox(
               p_227405_,
               p_227409_,
               $$7,
               $$7,
               $$7,
               this.width - 1 - $$7,
               $$7,
               this.depth - 1 - $$7,
               Blocks.SANDSTONE.defaultBlockState(),
               Blocks.SANDSTONE.defaultBlockState(),
               false
            );
            this.generateBox(
               p_227405_,
               p_227409_,
               $$7 + 1,
               $$7,
               $$7 + 1,
               this.width - 2 - $$7,
               $$7,
               this.depth - 2 - $$7,
               Blocks.AIR.defaultBlockState(),
               Blocks.AIR.defaultBlockState(),
               false
            );
         }

         for (int $$8 = 0; $$8 < this.width; $$8++) {
            for (int $$9 = 0; $$9 < this.depth; $$9++) {
               int $$10 = -5;
               this.fillColumnDown(p_227405_, Blocks.SANDSTONE.defaultBlockState(), $$8, -5, $$9, p_227409_);
            }
         }

         BlockState $$11 = Blocks.SANDSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
         BlockState $$12 = Blocks.SANDSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
         BlockState $$13 = Blocks.SANDSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
         BlockState $$14 = Blocks.SANDSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
         this.generateBox(p_227405_, p_227409_, 0, 0, 0, 4, 9, 4, Blocks.SANDSTONE.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(p_227405_, p_227409_, 1, 10, 1, 3, 10, 3, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false);
         this.placeBlock(p_227405_, $$11, 2, 10, 0, p_227409_);
         this.placeBlock(p_227405_, $$12, 2, 10, 4, p_227409_);
         this.placeBlock(p_227405_, $$13, 0, 10, 2, p_227409_);
         this.placeBlock(p_227405_, $$14, 4, 10, 2, p_227409_);
         this.generateBox(
            p_227405_, p_227409_, this.width - 5, 0, 0, this.width - 1, 9, 4, Blocks.SANDSTONE.defaultBlockState(), Blocks.AIR.defaultBlockState(), false
         );
         this.generateBox(
            p_227405_,
            p_227409_,
            this.width - 4,
            10,
            1,
            this.width - 2,
            10,
            3,
            Blocks.SANDSTONE.defaultBlockState(),
            Blocks.SANDSTONE.defaultBlockState(),
            false
         );
         this.placeBlock(p_227405_, $$11, this.width - 3, 10, 0, p_227409_);
         this.placeBlock(p_227405_, $$12, this.width - 3, 10, 4, p_227409_);
         this.placeBlock(p_227405_, $$13, this.width - 5, 10, 2, p_227409_);
         this.placeBlock(p_227405_, $$14, this.width - 1, 10, 2, p_227409_);
         this.generateBox(p_227405_, p_227409_, 8, 0, 0, 12, 4, 4, Blocks.SANDSTONE.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(p_227405_, p_227409_, 9, 1, 0, 11, 3, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.placeBlock(p_227405_, Blocks.CUT_SANDSTONE.defaultBlockState(), 9, 1, 1, p_227409_);
         this.placeBlock(p_227405_, Blocks.CUT_SANDSTONE.defaultBlockState(), 9, 2, 1, p_227409_);
         this.placeBlock(p_227405_, Blocks.CUT_SANDSTONE.defaultBlockState(), 9, 3, 1, p_227409_);
         this.placeBlock(p_227405_, Blocks.CUT_SANDSTONE.defaultBlockState(), 10, 3, 1, p_227409_);
         this.placeBlock(p_227405_, Blocks.CUT_SANDSTONE.defaultBlockState(), 11, 3, 1, p_227409_);
         this.placeBlock(p_227405_, Blocks.CUT_SANDSTONE.defaultBlockState(), 11, 2, 1, p_227409_);
         this.placeBlock(p_227405_, Blocks.CUT_SANDSTONE.defaultBlockState(), 11, 1, 1, p_227409_);
         this.generateBox(p_227405_, p_227409_, 4, 1, 1, 8, 3, 3, Blocks.SANDSTONE.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(p_227405_, p_227409_, 4, 1, 2, 8, 2, 2, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(p_227405_, p_227409_, 12, 1, 1, 16, 3, 3, Blocks.SANDSTONE.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(p_227405_, p_227409_, 12, 1, 2, 16, 2, 2, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(
            p_227405_, p_227409_, 5, 4, 5, this.width - 6, 4, this.depth - 6, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false
         );
         this.generateBox(p_227405_, p_227409_, 9, 4, 9, 11, 4, 11, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(p_227405_, p_227409_, 8, 1, 8, 8, 3, 8, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false);
         this.generateBox(p_227405_, p_227409_, 12, 1, 8, 12, 3, 8, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false);
         this.generateBox(p_227405_, p_227409_, 8, 1, 12, 8, 3, 12, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false);
         this.generateBox(p_227405_, p_227409_, 12, 1, 12, 12, 3, 12, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false);
         this.generateBox(p_227405_, p_227409_, 1, 1, 5, 4, 4, 11, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false);
         this.generateBox(
            p_227405_,
            p_227409_,
            this.width - 5,
            1,
            5,
            this.width - 2,
            4,
            11,
            Blocks.SANDSTONE.defaultBlockState(),
            Blocks.SANDSTONE.defaultBlockState(),
            false
         );
         this.generateBox(p_227405_, p_227409_, 6, 7, 9, 6, 7, 11, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false);
         this.generateBox(
            p_227405_,
            p_227409_,
            this.width - 7,
            7,
            9,
            this.width - 7,
            7,
            11,
            Blocks.SANDSTONE.defaultBlockState(),
            Blocks.SANDSTONE.defaultBlockState(),
            false
         );
         this.generateBox(p_227405_, p_227409_, 5, 5, 9, 5, 7, 11, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false);
         this.generateBox(
            p_227405_,
            p_227409_,
            this.width - 6,
            5,
            9,
            this.width - 6,
            7,
            11,
            Blocks.CUT_SANDSTONE.defaultBlockState(),
            Blocks.CUT_SANDSTONE.defaultBlockState(),
            false
         );
         this.placeBlock(p_227405_, Blocks.AIR.defaultBlockState(), 5, 5, 10, p_227409_);
         this.placeBlock(p_227405_, Blocks.AIR.defaultBlockState(), 5, 6, 10, p_227409_);
         this.placeBlock(p_227405_, Blocks.AIR.defaultBlockState(), 6, 6, 10, p_227409_);
         this.placeBlock(p_227405_, Blocks.AIR.defaultBlockState(), this.width - 6, 5, 10, p_227409_);
         this.placeBlock(p_227405_, Blocks.AIR.defaultBlockState(), this.width - 6, 6, 10, p_227409_);
         this.placeBlock(p_227405_, Blocks.AIR.defaultBlockState(), this.width - 7, 6, 10, p_227409_);
         this.generateBox(p_227405_, p_227409_, 2, 4, 4, 2, 6, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(
            p_227405_, p_227409_, this.width - 3, 4, 4, this.width - 3, 6, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false
         );
         this.placeBlock(p_227405_, $$11, 2, 4, 5, p_227409_);
         this.placeBlock(p_227405_, $$11, 2, 3, 4, p_227409_);
         this.placeBlock(p_227405_, $$11, this.width - 3, 4, 5, p_227409_);
         this.placeBlock(p_227405_, $$11, this.width - 3, 3, 4, p_227409_);
         this.generateBox(p_227405_, p_227409_, 1, 1, 3, 2, 2, 3, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false);
         this.generateBox(
            p_227405_, p_227409_, this.width - 3, 1, 3, this.width - 2, 2, 3, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false
         );
         this.placeBlock(p_227405_, Blocks.SANDSTONE.defaultBlockState(), 1, 1, 2, p_227409_);
         this.placeBlock(p_227405_, Blocks.SANDSTONE.defaultBlockState(), this.width - 2, 1, 2, p_227409_);
         this.placeBlock(p_227405_, Blocks.SANDSTONE_SLAB.defaultBlockState(), 1, 2, 2, p_227409_);
         this.placeBlock(p_227405_, Blocks.SANDSTONE_SLAB.defaultBlockState(), this.width - 2, 2, 2, p_227409_);
         this.placeBlock(p_227405_, $$14, 2, 1, 2, p_227409_);
         this.placeBlock(p_227405_, $$13, this.width - 3, 1, 2, p_227409_);
         this.generateBox(p_227405_, p_227409_, 4, 3, 5, 4, 3, 17, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false);
         this.generateBox(
            p_227405_,
            p_227409_,
            this.width - 5,
            3,
            5,
            this.width - 5,
            3,
            17,
            Blocks.SANDSTONE.defaultBlockState(),
            Blocks.SANDSTONE.defaultBlockState(),
            false
         );
         this.generateBox(p_227405_, p_227409_, 3, 1, 5, 4, 2, 16, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(
            p_227405_, p_227409_, this.width - 6, 1, 5, this.width - 5, 2, 16, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false
         );

         for (int $$15 = 5; $$15 <= 17; $$15 += 2) {
            this.placeBlock(p_227405_, Blocks.CUT_SANDSTONE.defaultBlockState(), 4, 1, $$15, p_227409_);
            this.placeBlock(p_227405_, Blocks.CHISELED_SANDSTONE.defaultBlockState(), 4, 2, $$15, p_227409_);
            this.placeBlock(p_227405_, Blocks.CUT_SANDSTONE.defaultBlockState(), this.width - 5, 1, $$15, p_227409_);
            this.placeBlock(p_227405_, Blocks.CHISELED_SANDSTONE.defaultBlockState(), this.width - 5, 2, $$15, p_227409_);
         }

         this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 10, 0, 7, p_227409_);
         this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 10, 0, 8, p_227409_);
         this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 9, 0, 9, p_227409_);
         this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 11, 0, 9, p_227409_);
         this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 8, 0, 10, p_227409_);
         this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 12, 0, 10, p_227409_);
         this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 7, 0, 10, p_227409_);
         this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 13, 0, 10, p_227409_);
         this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 9, 0, 11, p_227409_);
         this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 11, 0, 11, p_227409_);
         this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 10, 0, 12, p_227409_);
         this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 10, 0, 13, p_227409_);
         this.placeBlock(p_227405_, Blocks.BLUE_TERRACOTTA.defaultBlockState(), 10, 0, 10, p_227409_);

         for (int $$16 = 0; $$16 <= this.width - 1; $$16 += this.width - 1) {
            this.placeBlock(p_227405_, Blocks.CUT_SANDSTONE.defaultBlockState(), $$16, 2, 1, p_227409_);
            this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$16, 2, 2, p_227409_);
            this.placeBlock(p_227405_, Blocks.CUT_SANDSTONE.defaultBlockState(), $$16, 2, 3, p_227409_);
            this.placeBlock(p_227405_, Blocks.CUT_SANDSTONE.defaultBlockState(), $$16, 3, 1, p_227409_);
            this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$16, 3, 2, p_227409_);
            this.placeBlock(p_227405_, Blocks.CUT_SANDSTONE.defaultBlockState(), $$16, 3, 3, p_227409_);
            this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$16, 4, 1, p_227409_);
            this.placeBlock(p_227405_, Blocks.CHISELED_SANDSTONE.defaultBlockState(), $$16, 4, 2, p_227409_);
            this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$16, 4, 3, p_227409_);
            this.placeBlock(p_227405_, Blocks.CUT_SANDSTONE.defaultBlockState(), $$16, 5, 1, p_227409_);
            this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$16, 5, 2, p_227409_);
            this.placeBlock(p_227405_, Blocks.CUT_SANDSTONE.defaultBlockState(), $$16, 5, 3, p_227409_);
            this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$16, 6, 1, p_227409_);
            this.placeBlock(p_227405_, Blocks.CHISELED_SANDSTONE.defaultBlockState(), $$16, 6, 2, p_227409_);
            this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$16, 6, 3, p_227409_);
            this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$16, 7, 1, p_227409_);
            this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$16, 7, 2, p_227409_);
            this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$16, 7, 3, p_227409_);
            this.placeBlock(p_227405_, Blocks.CUT_SANDSTONE.defaultBlockState(), $$16, 8, 1, p_227409_);
            this.placeBlock(p_227405_, Blocks.CUT_SANDSTONE.defaultBlockState(), $$16, 8, 2, p_227409_);
            this.placeBlock(p_227405_, Blocks.CUT_SANDSTONE.defaultBlockState(), $$16, 8, 3, p_227409_);
         }

         for (int $$17 = 2; $$17 <= this.width - 3; $$17 += this.width - 3 - 2) {
            this.placeBlock(p_227405_, Blocks.CUT_SANDSTONE.defaultBlockState(), $$17 - 1, 2, 0, p_227409_);
            this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$17, 2, 0, p_227409_);
            this.placeBlock(p_227405_, Blocks.CUT_SANDSTONE.defaultBlockState(), $$17 + 1, 2, 0, p_227409_);
            this.placeBlock(p_227405_, Blocks.CUT_SANDSTONE.defaultBlockState(), $$17 - 1, 3, 0, p_227409_);
            this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$17, 3, 0, p_227409_);
            this.placeBlock(p_227405_, Blocks.CUT_SANDSTONE.defaultBlockState(), $$17 + 1, 3, 0, p_227409_);
            this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$17 - 1, 4, 0, p_227409_);
            this.placeBlock(p_227405_, Blocks.CHISELED_SANDSTONE.defaultBlockState(), $$17, 4, 0, p_227409_);
            this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$17 + 1, 4, 0, p_227409_);
            this.placeBlock(p_227405_, Blocks.CUT_SANDSTONE.defaultBlockState(), $$17 - 1, 5, 0, p_227409_);
            this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$17, 5, 0, p_227409_);
            this.placeBlock(p_227405_, Blocks.CUT_SANDSTONE.defaultBlockState(), $$17 + 1, 5, 0, p_227409_);
            this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$17 - 1, 6, 0, p_227409_);
            this.placeBlock(p_227405_, Blocks.CHISELED_SANDSTONE.defaultBlockState(), $$17, 6, 0, p_227409_);
            this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$17 + 1, 6, 0, p_227409_);
            this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$17 - 1, 7, 0, p_227409_);
            this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$17, 7, 0, p_227409_);
            this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$17 + 1, 7, 0, p_227409_);
            this.placeBlock(p_227405_, Blocks.CUT_SANDSTONE.defaultBlockState(), $$17 - 1, 8, 0, p_227409_);
            this.placeBlock(p_227405_, Blocks.CUT_SANDSTONE.defaultBlockState(), $$17, 8, 0, p_227409_);
            this.placeBlock(p_227405_, Blocks.CUT_SANDSTONE.defaultBlockState(), $$17 + 1, 8, 0, p_227409_);
         }

         this.generateBox(p_227405_, p_227409_, 8, 4, 0, 12, 6, 0, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false);
         this.placeBlock(p_227405_, Blocks.AIR.defaultBlockState(), 8, 6, 0, p_227409_);
         this.placeBlock(p_227405_, Blocks.AIR.defaultBlockState(), 12, 6, 0, p_227409_);
         this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 9, 5, 0, p_227409_);
         this.placeBlock(p_227405_, Blocks.CHISELED_SANDSTONE.defaultBlockState(), 10, 5, 0, p_227409_);
         this.placeBlock(p_227405_, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 11, 5, 0, p_227409_);
         this.generateBox(
            p_227405_, p_227409_, 8, -14, 8, 12, -11, 12, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false
         );
         this.generateBox(
            p_227405_, p_227409_, 8, -10, 8, 12, -10, 12, Blocks.CHISELED_SANDSTONE.defaultBlockState(), Blocks.CHISELED_SANDSTONE.defaultBlockState(), false
         );
         this.generateBox(p_227405_, p_227409_, 8, -9, 8, 12, -9, 12, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false);
         this.generateBox(p_227405_, p_227409_, 8, -8, 8, 12, -1, 12, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false);
         this.generateBox(p_227405_, p_227409_, 9, -11, 9, 11, -1, 11, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.placeBlock(p_227405_, Blocks.STONE_PRESSURE_PLATE.defaultBlockState(), 10, -11, 10, p_227409_);
         this.generateBox(p_227405_, p_227409_, 9, -13, 9, 11, -13, 11, Blocks.TNT.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.placeBlock(p_227405_, Blocks.AIR.defaultBlockState(), 8, -11, 10, p_227409_);
         this.placeBlock(p_227405_, Blocks.AIR.defaultBlockState(), 8, -10, 10, p_227409_);
         this.placeBlock(p_227405_, Blocks.CHISELED_SANDSTONE.defaultBlockState(), 7, -10, 10, p_227409_);
         this.placeBlock(p_227405_, Blocks.CUT_SANDSTONE.defaultBlockState(), 7, -11, 10, p_227409_);
         this.placeBlock(p_227405_, Blocks.AIR.defaultBlockState(), 12, -11, 10, p_227409_);
         this.placeBlock(p_227405_, Blocks.AIR.defaultBlockState(), 12, -10, 10, p_227409_);
         this.placeBlock(p_227405_, Blocks.CHISELED_SANDSTONE.defaultBlockState(), 13, -10, 10, p_227409_);
         this.placeBlock(p_227405_, Blocks.CUT_SANDSTONE.defaultBlockState(), 13, -11, 10, p_227409_);
         this.placeBlock(p_227405_, Blocks.AIR.defaultBlockState(), 10, -11, 8, p_227409_);
         this.placeBlock(p_227405_, Blocks.AIR.defaultBlockState(), 10, -10, 8, p_227409_);
         this.placeBlock(p_227405_, Blocks.CHISELED_SANDSTONE.defaultBlockState(), 10, -10, 7, p_227409_);
         this.placeBlock(p_227405_, Blocks.CUT_SANDSTONE.defaultBlockState(), 10, -11, 7, p_227409_);
         this.placeBlock(p_227405_, Blocks.AIR.defaultBlockState(), 10, -11, 12, p_227409_);
         this.placeBlock(p_227405_, Blocks.AIR.defaultBlockState(), 10, -10, 12, p_227409_);
         this.placeBlock(p_227405_, Blocks.CHISELED_SANDSTONE.defaultBlockState(), 10, -10, 13, p_227409_);
         this.placeBlock(p_227405_, Blocks.CUT_SANDSTONE.defaultBlockState(), 10, -11, 13, p_227409_);

         for (Direction $$18 : Direction.Plane.HORIZONTAL) {
            if (!this.hasPlacedChest[$$18.get2DDataValue()]) {
               int $$19 = $$18.getStepX() * 2;
               int $$20 = $$18.getStepZ() * 2;
               this.hasPlacedChest[$$18.get2DDataValue()] = this.createChest(
                  p_227405_, p_227409_, p_227408_, 10 + $$19, -11, 10 + $$20, BuiltInLootTables.DESERT_PYRAMID
               );
            }
         }

         this.addCellar(p_227405_, p_227409_);
      }
   }

   private void addCellar(WorldGenLevel p_272769_, BoundingBox p_273155_) {
      BlockPos $$2 = new BlockPos(16, -4, 13);
      this.addCellarStairs($$2, p_272769_, p_273155_);
      this.addCellarRoom($$2, p_272769_, p_273155_);
   }

   private void addCellarStairs(BlockPos p_272997_, WorldGenLevel p_272699_, BoundingBox p_273559_) {
      int $$3 = p_272997_.getX();
      int $$4 = p_272997_.getY();
      int $$5 = p_272997_.getZ();
      BlockState $$6 = Blocks.SANDSTONE_STAIRS.defaultBlockState();
      this.placeBlock(p_272699_, $$6.rotate(Rotation.COUNTERCLOCKWISE_90), 13, -1, 17, p_273559_);
      this.placeBlock(p_272699_, $$6.rotate(Rotation.COUNTERCLOCKWISE_90), 14, -2, 17, p_273559_);
      this.placeBlock(p_272699_, $$6.rotate(Rotation.COUNTERCLOCKWISE_90), 15, -3, 17, p_273559_);
      BlockState $$7 = Blocks.SAND.defaultBlockState();
      BlockState $$8 = Blocks.SANDSTONE.defaultBlockState();
      boolean $$9 = p_272699_.getRandom().nextBoolean();
      this.placeBlock(p_272699_, $$7, $$3 - 4, $$4 + 4, $$5 + 4, p_273559_);
      this.placeBlock(p_272699_, $$7, $$3 - 3, $$4 + 4, $$5 + 4, p_273559_);
      this.placeBlock(p_272699_, $$7, $$3 - 2, $$4 + 4, $$5 + 4, p_273559_);
      this.placeBlock(p_272699_, $$7, $$3 - 1, $$4 + 4, $$5 + 4, p_273559_);
      this.placeBlock(p_272699_, $$7, $$3, $$4 + 4, $$5 + 4, p_273559_);
      this.placeBlock(p_272699_, $$7, $$3 - 2, $$4 + 3, $$5 + 4, p_273559_);
      this.placeBlock(p_272699_, $$9 ? $$7 : $$8, $$3 - 1, $$4 + 3, $$5 + 4, p_273559_);
      this.placeBlock(p_272699_, !$$9 ? $$7 : $$8, $$3, $$4 + 3, $$5 + 4, p_273559_);
      this.placeBlock(p_272699_, $$7, $$3 - 1, $$4 + 2, $$5 + 4, p_273559_);
      this.placeBlock(p_272699_, $$8, $$3, $$4 + 2, $$5 + 4, p_273559_);
      this.placeBlock(p_272699_, $$7, $$3, $$4 + 1, $$5 + 4, p_273559_);
   }

   private void addCellarRoom(BlockPos p_272733_, WorldGenLevel p_273390_, BoundingBox p_273517_) {
      int $$3 = p_272733_.getX();
      int $$4 = p_272733_.getY();
      int $$5 = p_272733_.getZ();
      BlockState $$6 = Blocks.CUT_SANDSTONE.defaultBlockState();
      BlockState $$7 = Blocks.CHISELED_SANDSTONE.defaultBlockState();
      this.generateBox(p_273390_, p_273517_, $$3 - 3, $$4 + 1, $$5 - 3, $$3 - 3, $$4 + 1, $$5 + 2, $$6, $$6, true);
      this.generateBox(p_273390_, p_273517_, $$3 + 3, $$4 + 1, $$5 - 3, $$3 + 3, $$4 + 1, $$5 + 2, $$6, $$6, true);
      this.generateBox(p_273390_, p_273517_, $$3 - 3, $$4 + 1, $$5 - 3, $$3 + 3, $$4 + 1, $$5 - 2, $$6, $$6, true);
      this.generateBox(p_273390_, p_273517_, $$3 - 3, $$4 + 1, $$5 + 3, $$3 + 3, $$4 + 1, $$5 + 3, $$6, $$6, true);
      this.generateBox(p_273390_, p_273517_, $$3 - 3, $$4 + 2, $$5 - 3, $$3 - 3, $$4 + 2, $$5 + 2, $$7, $$7, true);
      this.generateBox(p_273390_, p_273517_, $$3 + 3, $$4 + 2, $$5 - 3, $$3 + 3, $$4 + 2, $$5 + 2, $$7, $$7, true);
      this.generateBox(p_273390_, p_273517_, $$3 - 3, $$4 + 2, $$5 - 3, $$3 + 3, $$4 + 2, $$5 - 2, $$7, $$7, true);
      this.generateBox(p_273390_, p_273517_, $$3 - 3, $$4 + 2, $$5 + 3, $$3 + 3, $$4 + 2, $$5 + 3, $$7, $$7, true);
      this.generateBox(p_273390_, p_273517_, $$3 - 3, -1, $$5 - 3, $$3 - 3, -1, $$5 + 2, $$6, $$6, true);
      this.generateBox(p_273390_, p_273517_, $$3 + 3, -1, $$5 - 3, $$3 + 3, -1, $$5 + 2, $$6, $$6, true);
      this.generateBox(p_273390_, p_273517_, $$3 - 3, -1, $$5 - 3, $$3 + 3, -1, $$5 - 2, $$6, $$6, true);
      this.generateBox(p_273390_, p_273517_, $$3 - 3, -1, $$5 + 3, $$3 + 3, -1, $$5 + 3, $$6, $$6, true);
      this.placeSandBox($$3 - 2, $$4 + 1, $$5 - 2, $$3 + 2, $$4 + 3, $$5 + 2);
      this.placeCollapsedRoof(p_273390_, p_273517_, $$3 - 2, $$4 + 4, $$5 - 2, $$3 + 2, $$5 + 2);
      BlockState $$8 = Blocks.ORANGE_TERRACOTTA.defaultBlockState();
      BlockState $$9 = Blocks.BLUE_TERRACOTTA.defaultBlockState();
      this.placeBlock(p_273390_, $$9, $$3, $$4, $$5, p_273517_);
      this.placeBlock(p_273390_, $$8, $$3 + 1, $$4, $$5 - 1, p_273517_);
      this.placeBlock(p_273390_, $$8, $$3 + 1, $$4, $$5 + 1, p_273517_);
      this.placeBlock(p_273390_, $$8, $$3 - 1, $$4, $$5 - 1, p_273517_);
      this.placeBlock(p_273390_, $$8, $$3 - 1, $$4, $$5 + 1, p_273517_);
      this.placeBlock(p_273390_, $$8, $$3 + 2, $$4, $$5, p_273517_);
      this.placeBlock(p_273390_, $$8, $$3 - 2, $$4, $$5, p_273517_);
      this.placeBlock(p_273390_, $$8, $$3, $$4, $$5 + 2, p_273517_);
      this.placeBlock(p_273390_, $$8, $$3, $$4, $$5 - 2, p_273517_);
      this.placeBlock(p_273390_, $$8, $$3 + 3, $$4, $$5, p_273517_);
      this.placeSand($$3 + 3, $$4 + 1, $$5);
      this.placeSand($$3 + 3, $$4 + 2, $$5);
      this.placeBlock(p_273390_, $$6, $$3 + 4, $$4 + 1, $$5, p_273517_);
      this.placeBlock(p_273390_, $$7, $$3 + 4, $$4 + 2, $$5, p_273517_);
      this.placeBlock(p_273390_, $$8, $$3 - 3, $$4, $$5, p_273517_);
      this.placeSand($$3 - 3, $$4 + 1, $$5);
      this.placeSand($$3 - 3, $$4 + 2, $$5);
      this.placeBlock(p_273390_, $$6, $$3 - 4, $$4 + 1, $$5, p_273517_);
      this.placeBlock(p_273390_, $$7, $$3 - 4, $$4 + 2, $$5, p_273517_);
      this.placeBlock(p_273390_, $$8, $$3, $$4, $$5 + 3, p_273517_);
      this.placeSand($$3, $$4 + 1, $$5 + 3);
      this.placeSand($$3, $$4 + 2, $$5 + 3);
      this.placeBlock(p_273390_, $$8, $$3, $$4, $$5 - 3, p_273517_);
      this.placeSand($$3, $$4 + 1, $$5 - 3);
      this.placeSand($$3, $$4 + 2, $$5 - 3);
      this.placeBlock(p_273390_, $$6, $$3, $$4 + 1, $$5 - 4, p_273517_);
      this.placeBlock(p_273390_, $$7, $$3, -2, $$5 - 4, p_273517_);
   }

   private void placeSand(int p_279401_, int p_279451_, int p_279265_) {
      BlockPos $$3 = this.getWorldPos(p_279401_, p_279451_, p_279265_);
      this.potentialSuspiciousSandWorldPositions.add($$3);
   }

   private void placeSandBox(int p_279483_, int p_279321_, int p_279271_, int p_279471_, int p_279229_, int p_279111_) {
      for (int $$6 = p_279321_; $$6 <= p_279229_; $$6++) {
         for (int $$7 = p_279483_; $$7 <= p_279471_; $$7++) {
            for (int $$8 = p_279271_; $$8 <= p_279111_; $$8++) {
               this.placeSand($$7, $$6, $$8);
            }
         }
      }
   }

   private void placeCollapsedRoofPiece(WorldGenLevel p_272965_, int p_272618_, int p_273415_, int p_273110_, BoundingBox p_272645_) {
      if (p_272965_.getRandom().nextFloat() < 0.33F) {
         BlockState $$5 = Blocks.SANDSTONE.defaultBlockState();
         this.placeBlock(p_272965_, $$5, p_272618_, p_273415_, p_273110_, p_272645_);
      } else {
         BlockState $$6 = Blocks.SAND.defaultBlockState();
         this.placeBlock(p_272965_, $$6, p_272618_, p_273415_, p_273110_, p_272645_);
      }
   }

   private void placeCollapsedRoof(WorldGenLevel p_273438_, BoundingBox p_273058_, int p_272638_, int p_272826_, int p_273026_, int p_272750_, int p_272639_) {
      for (int $$7 = p_272638_; $$7 <= p_272750_; $$7++) {
         for (int $$8 = p_273026_; $$8 <= p_272639_; $$8++) {
            this.placeCollapsedRoofPiece(p_273438_, $$7, p_272826_, $$8, p_273058_);
         }
      }

      RandomSource $$9 = RandomSource.create(p_273438_.getSeed()).forkPositional().at(this.getWorldPos(p_272638_, p_272826_, p_273026_));
      int $$10 = $$9.nextIntBetweenInclusive(p_272638_, p_272750_);
      int $$11 = $$9.nextIntBetweenInclusive(p_273026_, p_272639_);
      this.randomCollapsedRoofPos = new BlockPos(this.getWorldX($$10, $$11), this.getWorldY(p_272826_), this.getWorldZ($$10, $$11));
   }

   public List<BlockPos> getPotentialSuspiciousSandWorldPositions() {
      return this.potentialSuspiciousSandWorldPositions;
   }

   public BlockPos getRandomCollapsedRoofPos() {
      return this.randomCollapsedRoofPos;
   }
}
