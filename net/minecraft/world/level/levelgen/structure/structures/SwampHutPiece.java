package net.minecraft.world.level.levelgen.structure.structures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

public class SwampHutPiece extends ScatteredFeaturePiece {
   private boolean spawnedWitch;
   private boolean spawnedCat;

   public SwampHutPiece(RandomSource p_229952_, int p_229953_, int p_229954_) {
      super(StructurePieceType.SWAMPLAND_HUT, p_229953_, 64, p_229954_, 7, 7, 9, getRandomHorizontalDirection(p_229952_));
   }

   public SwampHutPiece(CompoundTag p_229956_) {
      super(StructurePieceType.SWAMPLAND_HUT, p_229956_);
      this.spawnedWitch = p_229956_.getBoolean("Witch");
      this.spawnedCat = p_229956_.getBoolean("Cat");
   }

   @Override
   protected void addAdditionalSaveData(StructurePieceSerializationContext p_229969_, CompoundTag p_229970_) {
      super.addAdditionalSaveData(p_229969_, p_229970_);
      p_229970_.putBoolean("Witch", this.spawnedWitch);
      p_229970_.putBoolean("Cat", this.spawnedCat);
   }

   @Override
   public void postProcess(
      WorldGenLevel p_229961_,
      StructureManager p_229962_,
      ChunkGenerator p_229963_,
      RandomSource p_229964_,
      BoundingBox p_229965_,
      ChunkPos p_229966_,
      BlockPos p_229967_
   ) {
      if (this.updateAverageGroundHeight(p_229961_, p_229965_, 0)) {
         this.generateBox(p_229961_, p_229965_, 1, 1, 1, 5, 1, 7, Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState(), false);
         this.generateBox(p_229961_, p_229965_, 1, 4, 2, 5, 4, 7, Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState(), false);
         this.generateBox(p_229961_, p_229965_, 2, 1, 0, 4, 1, 0, Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState(), false);
         this.generateBox(p_229961_, p_229965_, 2, 2, 2, 3, 3, 2, Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState(), false);
         this.generateBox(p_229961_, p_229965_, 1, 2, 3, 1, 3, 6, Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState(), false);
         this.generateBox(p_229961_, p_229965_, 5, 2, 3, 5, 3, 6, Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState(), false);
         this.generateBox(p_229961_, p_229965_, 2, 2, 7, 4, 3, 7, Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState(), false);
         this.generateBox(p_229961_, p_229965_, 1, 0, 2, 1, 3, 2, Blocks.OAK_LOG.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState(), false);
         this.generateBox(p_229961_, p_229965_, 5, 0, 2, 5, 3, 2, Blocks.OAK_LOG.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState(), false);
         this.generateBox(p_229961_, p_229965_, 1, 0, 7, 1, 3, 7, Blocks.OAK_LOG.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState(), false);
         this.generateBox(p_229961_, p_229965_, 5, 0, 7, 5, 3, 7, Blocks.OAK_LOG.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState(), false);
         this.placeBlock(p_229961_, Blocks.OAK_FENCE.defaultBlockState(), 2, 3, 2, p_229965_);
         this.placeBlock(p_229961_, Blocks.OAK_FENCE.defaultBlockState(), 3, 3, 7, p_229965_);
         this.placeBlock(p_229961_, Blocks.AIR.defaultBlockState(), 1, 3, 4, p_229965_);
         this.placeBlock(p_229961_, Blocks.AIR.defaultBlockState(), 5, 3, 4, p_229965_);
         this.placeBlock(p_229961_, Blocks.AIR.defaultBlockState(), 5, 3, 5, p_229965_);
         this.placeBlock(p_229961_, Blocks.POTTED_RED_MUSHROOM.defaultBlockState(), 1, 3, 5, p_229965_);
         this.placeBlock(p_229961_, Blocks.CRAFTING_TABLE.defaultBlockState(), 3, 2, 6, p_229965_);
         this.placeBlock(p_229961_, Blocks.CAULDRON.defaultBlockState(), 4, 2, 6, p_229965_);
         this.placeBlock(p_229961_, Blocks.OAK_FENCE.defaultBlockState(), 1, 2, 1, p_229965_);
         this.placeBlock(p_229961_, Blocks.OAK_FENCE.defaultBlockState(), 5, 2, 1, p_229965_);
         BlockState $$7 = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
         BlockState $$8 = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
         BlockState $$9 = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
         BlockState $$10 = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
         this.generateBox(p_229961_, p_229965_, 0, 4, 1, 6, 4, 1, $$7, $$7, false);
         this.generateBox(p_229961_, p_229965_, 0, 4, 2, 0, 4, 7, $$8, $$8, false);
         this.generateBox(p_229961_, p_229965_, 6, 4, 2, 6, 4, 7, $$9, $$9, false);
         this.generateBox(p_229961_, p_229965_, 0, 4, 8, 6, 4, 8, $$10, $$10, false);
         this.placeBlock(p_229961_, $$7.setValue(StairBlock.SHAPE, StairsShape.OUTER_RIGHT), 0, 4, 1, p_229965_);
         this.placeBlock(p_229961_, $$7.setValue(StairBlock.SHAPE, StairsShape.OUTER_LEFT), 6, 4, 1, p_229965_);
         this.placeBlock(p_229961_, $$10.setValue(StairBlock.SHAPE, StairsShape.OUTER_LEFT), 0, 4, 8, p_229965_);
         this.placeBlock(p_229961_, $$10.setValue(StairBlock.SHAPE, StairsShape.OUTER_RIGHT), 6, 4, 8, p_229965_);

         for (int $$11 = 2; $$11 <= 7; $$11 += 5) {
            for (int $$12 = 1; $$12 <= 5; $$12 += 4) {
               this.fillColumnDown(p_229961_, Blocks.OAK_LOG.defaultBlockState(), $$12, -1, $$11, p_229965_);
            }
         }

         if (!this.spawnedWitch) {
            BlockPos $$13 = this.getWorldPos(2, 2, 5);
            if (p_229965_.isInside($$13)) {
               this.spawnedWitch = true;
               Witch $$14 = EntityType.WITCH.create(p_229961_.getLevel());
               if ($$14 != null) {
                  $$14.setPersistenceRequired();
                  $$14.moveTo($$13.getX() + 0.5, $$13.getY(), $$13.getZ() + 0.5, 0.0F, 0.0F);
                  $$14.finalizeSpawn(p_229961_, p_229961_.getCurrentDifficultyAt($$13), MobSpawnType.STRUCTURE, null);
                  p_229961_.addFreshEntityWithPassengers($$14);
               }
            }
         }

         this.spawnCat(p_229961_, p_229965_);
      }
   }

   private void spawnCat(ServerLevelAccessor p_229958_, BoundingBox p_229959_) {
      if (!this.spawnedCat) {
         BlockPos $$2 = this.getWorldPos(2, 2, 5);
         if (p_229959_.isInside($$2)) {
            this.spawnedCat = true;
            Cat $$3 = EntityType.CAT.create(p_229958_.getLevel());
            if ($$3 != null) {
               $$3.setPersistenceRequired();
               $$3.moveTo($$2.getX() + 0.5, $$2.getY(), $$2.getZ() + 0.5, 0.0F, 0.0F);
               $$3.finalizeSpawn(p_229958_, p_229958_.getCurrentDifficultyAt($$2), MobSpawnType.STRUCTURE, null);
               p_229958_.addFreshEntityWithPassengers($$3);
            }
         }
      }
   }
}
