package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import org.slf4j.Logger;

public class MineshaftPieces {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final int DEFAULT_SHAFT_WIDTH = 3;
   private static final int DEFAULT_SHAFT_HEIGHT = 3;
   private static final int DEFAULT_SHAFT_LENGTH = 5;
   private static final int MAX_PILLAR_HEIGHT = 20;
   private static final int MAX_CHAIN_HEIGHT = 50;
   private static final int MAX_DEPTH = 8;
   public static final int MAGIC_START_Y = 50;

   private static MineshaftPieces.MineShaftPiece createRandomShaftPiece(
      StructurePieceAccessor p_227716_,
      RandomSource p_227717_,
      int p_227718_,
      int p_227719_,
      int p_227720_,
      @Nullable Direction p_227721_,
      int p_227722_,
      MineshaftStructure.Type p_227723_
   ) {
      int $$8 = p_227717_.nextInt(100);
      if ($$8 >= 80) {
         BoundingBox $$9 = MineshaftPieces.MineShaftCrossing.findCrossing(p_227716_, p_227717_, p_227718_, p_227719_, p_227720_, p_227721_);
         if ($$9 != null) {
            return new MineshaftPieces.MineShaftCrossing(p_227722_, $$9, p_227721_, p_227723_);
         }
      } else if ($$8 >= 70) {
         BoundingBox $$10 = MineshaftPieces.MineShaftStairs.findStairs(p_227716_, p_227717_, p_227718_, p_227719_, p_227720_, p_227721_);
         if ($$10 != null) {
            return new MineshaftPieces.MineShaftStairs(p_227722_, $$10, p_227721_, p_227723_);
         }
      } else {
         BoundingBox $$11 = MineshaftPieces.MineShaftCorridor.findCorridorSize(p_227716_, p_227717_, p_227718_, p_227719_, p_227720_, p_227721_);
         if ($$11 != null) {
            return new MineshaftPieces.MineShaftCorridor(p_227722_, p_227717_, $$11, p_227721_, p_227723_);
         }
      }

      return null;
   }

   static MineshaftPieces.MineShaftPiece generateAndAddPiece(
      StructurePiece p_227707_,
      StructurePieceAccessor p_227708_,
      RandomSource p_227709_,
      int p_227710_,
      int p_227711_,
      int p_227712_,
      Direction p_227713_,
      int p_227714_
   ) {
      if (p_227714_ > 8) {
         return null;
      } else if (Math.abs(p_227710_ - p_227707_.getBoundingBox().minX()) <= 80 && Math.abs(p_227712_ - p_227707_.getBoundingBox().minZ()) <= 80) {
         MineshaftStructure.Type $$8 = ((MineshaftPieces.MineShaftPiece)p_227707_).type;
         MineshaftPieces.MineShaftPiece $$9 = createRandomShaftPiece(p_227708_, p_227709_, p_227710_, p_227711_, p_227712_, p_227713_, p_227714_ + 1, $$8);
         if ($$9 != null) {
            p_227708_.addPiece($$9);
            $$9.addChildren(p_227707_, p_227708_, p_227709_);
         }

         return $$9;
      } else {
         return null;
      }
   }

   public static class MineShaftCorridor extends MineshaftPieces.MineShaftPiece {
      private final boolean hasRails;
      private final boolean spiderCorridor;
      private boolean hasPlacedSpider;
      private final int numSections;

      public MineShaftCorridor(CompoundTag p_227737_) {
         super(StructurePieceType.MINE_SHAFT_CORRIDOR, p_227737_);
         this.hasRails = p_227737_.getBoolean("hr");
         this.spiderCorridor = p_227737_.getBoolean("sc");
         this.hasPlacedSpider = p_227737_.getBoolean("hps");
         this.numSections = p_227737_.getInt("Num");
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext p_227806_, CompoundTag p_227807_) {
         super.addAdditionalSaveData(p_227806_, p_227807_);
         p_227807_.putBoolean("hr", this.hasRails);
         p_227807_.putBoolean("sc", this.spiderCorridor);
         p_227807_.putBoolean("hps", this.hasPlacedSpider);
         p_227807_.putInt("Num", this.numSections);
      }

      public MineShaftCorridor(int p_227731_, RandomSource p_227732_, BoundingBox p_227733_, Direction p_227734_, MineshaftStructure.Type p_227735_) {
         super(StructurePieceType.MINE_SHAFT_CORRIDOR, p_227731_, p_227735_, p_227733_);
         this.setOrientation(p_227734_);
         this.hasRails = p_227732_.nextInt(3) == 0;
         this.spiderCorridor = !this.hasRails && p_227732_.nextInt(23) == 0;
         if (this.getOrientation().getAxis() == Direction.Axis.Z) {
            this.numSections = p_227733_.getZSpan() / 5;
         } else {
            this.numSections = p_227733_.getXSpan() / 5;
         }
      }

      @Nullable
      public static BoundingBox findCorridorSize(
         StructurePieceAccessor p_227799_, RandomSource p_227800_, int p_227801_, int p_227802_, int p_227803_, Direction p_227804_
      ) {
         for (int $$6 = p_227800_.nextInt(3) + 2; $$6 > 0; $$6--) {
            int $$7 = $$6 * 5;

            BoundingBox $$11 = switch (p_227804_) {
               default -> new BoundingBox(0, 0, -($$7 - 1), 2, 2, 0);
               case SOUTH -> new BoundingBox(0, 0, 0, 2, 2, $$7 - 1);
               case WEST -> new BoundingBox(-($$7 - 1), 0, 0, 0, 2, 2);
               case EAST -> new BoundingBox(0, 0, 0, $$7 - 1, 2, 2);
            };
            $$11.move(p_227801_, p_227802_, p_227803_);
            if (p_227799_.findCollisionPiece($$11) == null) {
               return $$11;
            }
         }

         return null;
      }

      @Override
      public void addChildren(StructurePiece p_227795_, StructurePieceAccessor p_227796_, RandomSource p_227797_) {
         int $$3 = this.getGenDepth();
         int $$4 = p_227797_.nextInt(4);
         Direction $$5 = this.getOrientation();
         if ($$5 != null) {
            switch ($$5) {
               case NORTH:
               default:
                  if ($$4 <= 1) {
                     MineshaftPieces.generateAndAddPiece(
                        p_227795_,
                        p_227796_,
                        p_227797_,
                        this.boundingBox.minX(),
                        this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                        this.boundingBox.minZ() - 1,
                        $$5,
                        $$3
                     );
                  } else if ($$4 == 2) {
                     MineshaftPieces.generateAndAddPiece(
                        p_227795_,
                        p_227796_,
                        p_227797_,
                        this.boundingBox.minX() - 1,
                        this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                        this.boundingBox.minZ(),
                        Direction.WEST,
                        $$3
                     );
                  } else {
                     MineshaftPieces.generateAndAddPiece(
                        p_227795_,
                        p_227796_,
                        p_227797_,
                        this.boundingBox.maxX() + 1,
                        this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                        this.boundingBox.minZ(),
                        Direction.EAST,
                        $$3
                     );
                  }
                  break;
               case SOUTH:
                  if ($$4 <= 1) {
                     MineshaftPieces.generateAndAddPiece(
                        p_227795_,
                        p_227796_,
                        p_227797_,
                        this.boundingBox.minX(),
                        this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                        this.boundingBox.maxZ() + 1,
                        $$5,
                        $$3
                     );
                  } else if ($$4 == 2) {
                     MineshaftPieces.generateAndAddPiece(
                        p_227795_,
                        p_227796_,
                        p_227797_,
                        this.boundingBox.minX() - 1,
                        this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                        this.boundingBox.maxZ() - 3,
                        Direction.WEST,
                        $$3
                     );
                  } else {
                     MineshaftPieces.generateAndAddPiece(
                        p_227795_,
                        p_227796_,
                        p_227797_,
                        this.boundingBox.maxX() + 1,
                        this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                        this.boundingBox.maxZ() - 3,
                        Direction.EAST,
                        $$3
                     );
                  }
                  break;
               case WEST:
                  if ($$4 <= 1) {
                     MineshaftPieces.generateAndAddPiece(
                        p_227795_,
                        p_227796_,
                        p_227797_,
                        this.boundingBox.minX() - 1,
                        this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                        this.boundingBox.minZ(),
                        $$5,
                        $$3
                     );
                  } else if ($$4 == 2) {
                     MineshaftPieces.generateAndAddPiece(
                        p_227795_,
                        p_227796_,
                        p_227797_,
                        this.boundingBox.minX(),
                        this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                        this.boundingBox.minZ() - 1,
                        Direction.NORTH,
                        $$3
                     );
                  } else {
                     MineshaftPieces.generateAndAddPiece(
                        p_227795_,
                        p_227796_,
                        p_227797_,
                        this.boundingBox.minX(),
                        this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                        this.boundingBox.maxZ() + 1,
                        Direction.SOUTH,
                        $$3
                     );
                  }
                  break;
               case EAST:
                  if ($$4 <= 1) {
                     MineshaftPieces.generateAndAddPiece(
                        p_227795_,
                        p_227796_,
                        p_227797_,
                        this.boundingBox.maxX() + 1,
                        this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                        this.boundingBox.minZ(),
                        $$5,
                        $$3
                     );
                  } else if ($$4 == 2) {
                     MineshaftPieces.generateAndAddPiece(
                        p_227795_,
                        p_227796_,
                        p_227797_,
                        this.boundingBox.maxX() - 3,
                        this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                        this.boundingBox.minZ() - 1,
                        Direction.NORTH,
                        $$3
                     );
                  } else {
                     MineshaftPieces.generateAndAddPiece(
                        p_227795_,
                        p_227796_,
                        p_227797_,
                        this.boundingBox.maxX() - 3,
                        this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                        this.boundingBox.maxZ() + 1,
                        Direction.SOUTH,
                        $$3
                     );
                  }
            }
         }

         if ($$3 < 8) {
            if ($$5 != Direction.NORTH && $$5 != Direction.SOUTH) {
               for (int $$8 = this.boundingBox.minX() + 3; $$8 + 3 <= this.boundingBox.maxX(); $$8 += 5) {
                  int $$9 = p_227797_.nextInt(5);
                  if ($$9 == 0) {
                     MineshaftPieces.generateAndAddPiece(
                        p_227795_, p_227796_, p_227797_, $$8, this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, $$3 + 1
                     );
                  } else if ($$9 == 1) {
                     MineshaftPieces.generateAndAddPiece(
                        p_227795_, p_227796_, p_227797_, $$8, this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, $$3 + 1
                     );
                  }
               }
            } else {
               for (int $$6 = this.boundingBox.minZ() + 3; $$6 + 3 <= this.boundingBox.maxZ(); $$6 += 5) {
                  int $$7 = p_227797_.nextInt(5);
                  if ($$7 == 0) {
                     MineshaftPieces.generateAndAddPiece(
                        p_227795_, p_227796_, p_227797_, this.boundingBox.minX() - 1, this.boundingBox.minY(), $$6, Direction.WEST, $$3 + 1
                     );
                  } else if ($$7 == 1) {
                     MineshaftPieces.generateAndAddPiece(
                        p_227795_, p_227796_, p_227797_, this.boundingBox.maxX() + 1, this.boundingBox.minY(), $$6, Direction.EAST, $$3 + 1
                     );
                  }
               }
            }
         }
      }

      @Override
      protected boolean createChest(
         WorldGenLevel p_227787_, BoundingBox p_227788_, RandomSource p_227789_, int p_227790_, int p_227791_, int p_227792_, ResourceKey<LootTable> p_335869_
      ) {
         BlockPos $$7 = this.getWorldPos(p_227790_, p_227791_, p_227792_);
         if (p_227788_.isInside($$7) && p_227787_.getBlockState($$7).isAir() && !p_227787_.getBlockState($$7.below()).isAir()) {
            BlockState $$8 = Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, p_227789_.nextBoolean() ? RailShape.NORTH_SOUTH : RailShape.EAST_WEST);
            this.placeBlock(p_227787_, $$8, p_227790_, p_227791_, p_227792_, p_227788_);
            MinecartChest $$9 = new MinecartChest(p_227787_.getLevel(), $$7.getX() + 0.5, $$7.getY() + 0.5, $$7.getZ() + 0.5);
            $$9.setLootTable(p_335869_, p_227789_.nextLong());
            p_227787_.addFreshEntity($$9);
            return true;
         } else {
            return false;
         }
      }

      @Override
      public void postProcess(
         WorldGenLevel p_227743_,
         StructureManager p_227744_,
         ChunkGenerator p_227745_,
         RandomSource p_227746_,
         BoundingBox p_227747_,
         ChunkPos p_227748_,
         BlockPos p_227749_
      ) {
         if (!this.isInInvalidLocation(p_227743_, p_227747_)) {
            int $$7 = 0;
            int $$8 = 2;
            int $$9 = 0;
            int $$10 = 2;
            int $$11 = this.numSections * 5 - 1;
            BlockState $$12 = this.type.getPlanksState();
            this.generateBox(p_227743_, p_227747_, 0, 0, 0, 2, 1, $$11, CAVE_AIR, CAVE_AIR, false);
            this.generateMaybeBox(p_227743_, p_227747_, p_227746_, 0.8F, 0, 2, 0, 2, 2, $$11, CAVE_AIR, CAVE_AIR, false, false);
            if (this.spiderCorridor) {
               this.generateMaybeBox(p_227743_, p_227747_, p_227746_, 0.6F, 0, 0, 0, 2, 1, $$11, Blocks.COBWEB.defaultBlockState(), CAVE_AIR, false, true);
            }

            for (int $$13 = 0; $$13 < this.numSections; $$13++) {
               int $$14 = 2 + $$13 * 5;
               this.placeSupport(p_227743_, p_227747_, 0, 0, $$14, 2, 2, p_227746_);
               this.maybePlaceCobWeb(p_227743_, p_227747_, p_227746_, 0.1F, 0, 2, $$14 - 1);
               this.maybePlaceCobWeb(p_227743_, p_227747_, p_227746_, 0.1F, 2, 2, $$14 - 1);
               this.maybePlaceCobWeb(p_227743_, p_227747_, p_227746_, 0.1F, 0, 2, $$14 + 1);
               this.maybePlaceCobWeb(p_227743_, p_227747_, p_227746_, 0.1F, 2, 2, $$14 + 1);
               this.maybePlaceCobWeb(p_227743_, p_227747_, p_227746_, 0.05F, 0, 2, $$14 - 2);
               this.maybePlaceCobWeb(p_227743_, p_227747_, p_227746_, 0.05F, 2, 2, $$14 - 2);
               this.maybePlaceCobWeb(p_227743_, p_227747_, p_227746_, 0.05F, 0, 2, $$14 + 2);
               this.maybePlaceCobWeb(p_227743_, p_227747_, p_227746_, 0.05F, 2, 2, $$14 + 2);
               if (p_227746_.nextInt(100) == 0) {
                  this.createChest(p_227743_, p_227747_, p_227746_, 2, 0, $$14 - 1, BuiltInLootTables.ABANDONED_MINESHAFT);
               }

               if (p_227746_.nextInt(100) == 0) {
                  this.createChest(p_227743_, p_227747_, p_227746_, 0, 0, $$14 + 1, BuiltInLootTables.ABANDONED_MINESHAFT);
               }

               if (this.spiderCorridor && !this.hasPlacedSpider) {
                  int $$15 = 1;
                  int $$16 = $$14 - 1 + p_227746_.nextInt(3);
                  BlockPos $$17 = this.getWorldPos(1, 0, $$16);
                  if (p_227747_.isInside($$17) && this.isInterior(p_227743_, 1, 0, $$16, p_227747_)) {
                     this.hasPlacedSpider = true;
                     p_227743_.setBlock($$17, Blocks.SPAWNER.defaultBlockState(), 2);
                     if (p_227743_.getBlockEntity($$17) instanceof SpawnerBlockEntity $$19) {
                        $$19.setEntityId(EntityType.CAVE_SPIDER, p_227746_);
                     }
                  }
               }
            }

            for (int $$20 = 0; $$20 <= 2; $$20++) {
               for (int $$21 = 0; $$21 <= $$11; $$21++) {
                  this.setPlanksBlock(p_227743_, p_227747_, $$12, $$20, -1, $$21);
               }
            }

            int $$22 = 2;
            this.placeDoubleLowerOrUpperSupport(p_227743_, p_227747_, 0, -1, 2);
            if (this.numSections > 1) {
               int $$23 = $$11 - 2;
               this.placeDoubleLowerOrUpperSupport(p_227743_, p_227747_, 0, -1, $$23);
            }

            if (this.hasRails) {
               BlockState $$24 = Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.NORTH_SOUTH);

               for (int $$25 = 0; $$25 <= $$11; $$25++) {
                  BlockState $$26 = this.getBlock(p_227743_, 1, -1, $$25, p_227747_);
                  if (!$$26.isAir() && $$26.isSolidRender(p_227743_, this.getWorldPos(1, -1, $$25))) {
                     float $$27 = this.isInterior(p_227743_, 1, 0, $$25, p_227747_) ? 0.7F : 0.9F;
                     this.maybeGenerateBlock(p_227743_, p_227747_, p_227746_, $$27, 1, 0, $$25, $$24);
                  }
               }
            }
         }
      }

      private void placeDoubleLowerOrUpperSupport(WorldGenLevel p_227757_, BoundingBox p_227758_, int p_227759_, int p_227760_, int p_227761_) {
         BlockState $$5 = this.type.getWoodState();
         BlockState $$6 = this.type.getPlanksState();
         if (this.getBlock(p_227757_, p_227759_, p_227760_, p_227761_, p_227758_).is($$6.getBlock())) {
            this.fillPillarDownOrChainUp(p_227757_, $$5, p_227759_, p_227760_, p_227761_, p_227758_);
         }

         if (this.getBlock(p_227757_, p_227759_ + 2, p_227760_, p_227761_, p_227758_).is($$6.getBlock())) {
            this.fillPillarDownOrChainUp(p_227757_, $$5, p_227759_ + 2, p_227760_, p_227761_, p_227758_);
         }
      }

      @Override
      protected void fillColumnDown(WorldGenLevel p_227813_, BlockState p_227814_, int p_227815_, int p_227816_, int p_227817_, BoundingBox p_227818_) {
         BlockPos.MutableBlockPos $$6 = this.getWorldPos(p_227815_, p_227816_, p_227817_);
         if (p_227818_.isInside($$6)) {
            int $$7 = $$6.getY();

            while (this.isReplaceableByStructures(p_227813_.getBlockState($$6)) && $$6.getY() > p_227813_.getMinBuildHeight() + 1) {
               $$6.move(Direction.DOWN);
            }

            if (this.canPlaceColumnOnTopOf(p_227813_, $$6, p_227813_.getBlockState($$6))) {
               while ($$6.getY() < $$7) {
                  $$6.move(Direction.UP);
                  p_227813_.setBlock($$6, p_227814_, 2);
               }
            }
         }
      }

      protected void fillPillarDownOrChainUp(WorldGenLevel p_227820_, BlockState p_227821_, int p_227822_, int p_227823_, int p_227824_, BoundingBox p_227825_) {
         BlockPos.MutableBlockPos $$6 = this.getWorldPos(p_227822_, p_227823_, p_227824_);
         if (p_227825_.isInside($$6)) {
            int $$7 = $$6.getY();
            int $$8 = 1;
            boolean $$9 = true;

            for (boolean $$10 = true; $$9 || $$10; $$8++) {
               if ($$9) {
                  $$6.setY($$7 - $$8);
                  BlockState $$11 = p_227820_.getBlockState($$6);
                  boolean $$12 = this.isReplaceableByStructures($$11) && !$$11.is(Blocks.LAVA);
                  if (!$$12 && this.canPlaceColumnOnTopOf(p_227820_, $$6, $$11)) {
                     fillColumnBetween(p_227820_, p_227821_, $$6, $$7 - $$8 + 1, $$7);
                     return;
                  }

                  $$9 = $$8 <= 20 && $$12 && $$6.getY() > p_227820_.getMinBuildHeight() + 1;
               }

               if ($$10) {
                  $$6.setY($$7 + $$8);
                  BlockState $$13 = p_227820_.getBlockState($$6);
                  boolean $$14 = this.isReplaceableByStructures($$13);
                  if (!$$14 && this.canHangChainBelow(p_227820_, $$6, $$13)) {
                     p_227820_.setBlock($$6.setY($$7 + 1), this.type.getFenceState(), 2);
                     fillColumnBetween(p_227820_, Blocks.CHAIN.defaultBlockState(), $$6, $$7 + 2, $$7 + $$8);
                     return;
                  }

                  $$10 = $$8 <= 50 && $$14 && $$6.getY() < p_227820_.getMaxBuildHeight() - 1;
               }
            }
         }
      }

      private static void fillColumnBetween(WorldGenLevel p_227751_, BlockState p_227752_, BlockPos.MutableBlockPos p_227753_, int p_227754_, int p_227755_) {
         for (int $$5 = p_227754_; $$5 < p_227755_; $$5++) {
            p_227751_.setBlock(p_227753_.setY($$5), p_227752_, 2);
         }
      }

      private boolean canPlaceColumnOnTopOf(LevelReader p_227739_, BlockPos p_227740_, BlockState p_227741_) {
         return p_227741_.isFaceSturdy(p_227739_, p_227740_, Direction.UP);
      }

      private boolean canHangChainBelow(LevelReader p_227809_, BlockPos p_227810_, BlockState p_227811_) {
         return Block.canSupportCenter(p_227809_, p_227810_, Direction.DOWN) && !(p_227811_.getBlock() instanceof FallingBlock);
      }

      private void placeSupport(
         WorldGenLevel p_227770_, BoundingBox p_227771_, int p_227772_, int p_227773_, int p_227774_, int p_227775_, int p_227776_, RandomSource p_227777_
      ) {
         if (this.isSupportingBox(p_227770_, p_227771_, p_227772_, p_227776_, p_227775_, p_227774_)) {
            BlockState $$8 = this.type.getPlanksState();
            BlockState $$9 = this.type.getFenceState();
            this.generateBox(
               p_227770_, p_227771_, p_227772_, p_227773_, p_227774_, p_227772_, p_227775_ - 1, p_227774_, $$9.setValue(FenceBlock.WEST, true), CAVE_AIR, false
            );
            this.generateBox(
               p_227770_, p_227771_, p_227776_, p_227773_, p_227774_, p_227776_, p_227775_ - 1, p_227774_, $$9.setValue(FenceBlock.EAST, true), CAVE_AIR, false
            );
            if (p_227777_.nextInt(4) == 0) {
               this.generateBox(p_227770_, p_227771_, p_227772_, p_227775_, p_227774_, p_227772_, p_227775_, p_227774_, $$8, CAVE_AIR, false);
               this.generateBox(p_227770_, p_227771_, p_227776_, p_227775_, p_227774_, p_227776_, p_227775_, p_227774_, $$8, CAVE_AIR, false);
            } else {
               this.generateBox(p_227770_, p_227771_, p_227772_, p_227775_, p_227774_, p_227776_, p_227775_, p_227774_, $$8, CAVE_AIR, false);
               this.maybeGenerateBlock(
                  p_227770_,
                  p_227771_,
                  p_227777_,
                  0.05F,
                  p_227772_ + 1,
                  p_227775_,
                  p_227774_ - 1,
                  Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH)
               );
               this.maybeGenerateBlock(
                  p_227770_,
                  p_227771_,
                  p_227777_,
                  0.05F,
                  p_227772_ + 1,
                  p_227775_,
                  p_227774_ + 1,
                  Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.NORTH)
               );
            }
         }
      }

      private void maybePlaceCobWeb(
         WorldGenLevel p_227779_, BoundingBox p_227780_, RandomSource p_227781_, float p_227782_, int p_227783_, int p_227784_, int p_227785_
      ) {
         if (this.isInterior(p_227779_, p_227783_, p_227784_, p_227785_, p_227780_)
            && p_227781_.nextFloat() < p_227782_
            && this.hasSturdyNeighbours(p_227779_, p_227780_, p_227783_, p_227784_, p_227785_, 2)) {
            this.placeBlock(p_227779_, Blocks.COBWEB.defaultBlockState(), p_227783_, p_227784_, p_227785_, p_227780_);
         }
      }

      private boolean hasSturdyNeighbours(WorldGenLevel p_227763_, BoundingBox p_227764_, int p_227765_, int p_227766_, int p_227767_, int p_227768_) {
         BlockPos.MutableBlockPos $$6 = this.getWorldPos(p_227765_, p_227766_, p_227767_);
         int $$7 = 0;

         for (Direction $$8 : Direction.values()) {
            $$6.move($$8);
            if (p_227764_.isInside($$6) && p_227763_.getBlockState($$6).isFaceSturdy(p_227763_, $$6, $$8.getOpposite())) {
               if (++$$7 >= p_227768_) {
                  return true;
               }
            }

            $$6.move($$8.getOpposite());
         }

         return false;
      }
   }

   public static class MineShaftCrossing extends MineshaftPieces.MineShaftPiece {
      private final Direction direction;
      private final boolean isTwoFloored;

      public MineShaftCrossing(CompoundTag p_227834_) {
         super(StructurePieceType.MINE_SHAFT_CROSSING, p_227834_);
         this.isTwoFloored = p_227834_.getBoolean("tf");
         this.direction = Direction.from2DDataValue(p_227834_.getInt("D"));
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext p_227862_, CompoundTag p_227863_) {
         super.addAdditionalSaveData(p_227862_, p_227863_);
         p_227863_.putBoolean("tf", this.isTwoFloored);
         p_227863_.putInt("D", this.direction.get2DDataValue());
      }

      public MineShaftCrossing(int p_227829_, BoundingBox p_227830_, @Nullable Direction p_227831_, MineshaftStructure.Type p_227832_) {
         super(StructurePieceType.MINE_SHAFT_CROSSING, p_227829_, p_227832_, p_227830_);
         this.direction = p_227831_;
         this.isTwoFloored = p_227830_.getYSpan() > 3;
      }

      @Nullable
      public static BoundingBox findCrossing(
         StructurePieceAccessor p_227855_, RandomSource p_227856_, int p_227857_, int p_227858_, int p_227859_, Direction p_227860_
      ) {
         int $$6;
         if (p_227856_.nextInt(4) == 0) {
            $$6 = 6;
         } else {
            $$6 = 2;
         }
         BoundingBox $$11 = switch (p_227860_) {
            default -> new BoundingBox(-1, 0, -4, 3, $$6, 0);
            case SOUTH -> new BoundingBox(-1, 0, 0, 3, $$6, 4);
            case WEST -> new BoundingBox(-4, 0, -1, 0, $$6, 3);
            case EAST -> new BoundingBox(0, 0, -1, 4, $$6, 3);
         };
         $$11.move(p_227857_, p_227858_, p_227859_);
         return p_227855_.findCollisionPiece($$11) != null ? null : $$11;
      }

      @Override
      public void addChildren(StructurePiece p_227851_, StructurePieceAccessor p_227852_, RandomSource p_227853_) {
         int $$3 = this.getGenDepth();
         switch (this.direction) {
            case NORTH:
            default:
               MineshaftPieces.generateAndAddPiece(
                  p_227851_, p_227852_, p_227853_, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, $$3
               );
               MineshaftPieces.generateAndAddPiece(
                  p_227851_, p_227852_, p_227853_, this.boundingBox.minX() - 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.WEST, $$3
               );
               MineshaftPieces.generateAndAddPiece(
                  p_227851_, p_227852_, p_227853_, this.boundingBox.maxX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.EAST, $$3
               );
               break;
            case SOUTH:
               MineshaftPieces.generateAndAddPiece(
                  p_227851_, p_227852_, p_227853_, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, $$3
               );
               MineshaftPieces.generateAndAddPiece(
                  p_227851_, p_227852_, p_227853_, this.boundingBox.minX() - 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.WEST, $$3
               );
               MineshaftPieces.generateAndAddPiece(
                  p_227851_, p_227852_, p_227853_, this.boundingBox.maxX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.EAST, $$3
               );
               break;
            case WEST:
               MineshaftPieces.generateAndAddPiece(
                  p_227851_, p_227852_, p_227853_, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, $$3
               );
               MineshaftPieces.generateAndAddPiece(
                  p_227851_, p_227852_, p_227853_, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, $$3
               );
               MineshaftPieces.generateAndAddPiece(
                  p_227851_, p_227852_, p_227853_, this.boundingBox.minX() - 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.WEST, $$3
               );
               break;
            case EAST:
               MineshaftPieces.generateAndAddPiece(
                  p_227851_, p_227852_, p_227853_, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, $$3
               );
               MineshaftPieces.generateAndAddPiece(
                  p_227851_, p_227852_, p_227853_, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, $$3
               );
               MineshaftPieces.generateAndAddPiece(
                  p_227851_, p_227852_, p_227853_, this.boundingBox.maxX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.EAST, $$3
               );
         }

         if (this.isTwoFloored) {
            if (p_227853_.nextBoolean()) {
               MineshaftPieces.generateAndAddPiece(
                  p_227851_,
                  p_227852_,
                  p_227853_,
                  this.boundingBox.minX() + 1,
                  this.boundingBox.minY() + 3 + 1,
                  this.boundingBox.minZ() - 1,
                  Direction.NORTH,
                  $$3
               );
            }

            if (p_227853_.nextBoolean()) {
               MineshaftPieces.generateAndAddPiece(
                  p_227851_,
                  p_227852_,
                  p_227853_,
                  this.boundingBox.minX() - 1,
                  this.boundingBox.minY() + 3 + 1,
                  this.boundingBox.minZ() + 1,
                  Direction.WEST,
                  $$3
               );
            }

            if (p_227853_.nextBoolean()) {
               MineshaftPieces.generateAndAddPiece(
                  p_227851_,
                  p_227852_,
                  p_227853_,
                  this.boundingBox.maxX() + 1,
                  this.boundingBox.minY() + 3 + 1,
                  this.boundingBox.minZ() + 1,
                  Direction.EAST,
                  $$3
               );
            }

            if (p_227853_.nextBoolean()) {
               MineshaftPieces.generateAndAddPiece(
                  p_227851_,
                  p_227852_,
                  p_227853_,
                  this.boundingBox.minX() + 1,
                  this.boundingBox.minY() + 3 + 1,
                  this.boundingBox.maxZ() + 1,
                  Direction.SOUTH,
                  $$3
               );
            }
         }
      }

      @Override
      public void postProcess(
         WorldGenLevel p_227836_,
         StructureManager p_227837_,
         ChunkGenerator p_227838_,
         RandomSource p_227839_,
         BoundingBox p_227840_,
         ChunkPos p_227841_,
         BlockPos p_227842_
      ) {
         if (!this.isInInvalidLocation(p_227836_, p_227840_)) {
            BlockState $$7 = this.type.getPlanksState();
            if (this.isTwoFloored) {
               this.generateBox(
                  p_227836_,
                  p_227840_,
                  this.boundingBox.minX() + 1,
                  this.boundingBox.minY(),
                  this.boundingBox.minZ(),
                  this.boundingBox.maxX() - 1,
                  this.boundingBox.minY() + 3 - 1,
                  this.boundingBox.maxZ(),
                  CAVE_AIR,
                  CAVE_AIR,
                  false
               );
               this.generateBox(
                  p_227836_,
                  p_227840_,
                  this.boundingBox.minX(),
                  this.boundingBox.minY(),
                  this.boundingBox.minZ() + 1,
                  this.boundingBox.maxX(),
                  this.boundingBox.minY() + 3 - 1,
                  this.boundingBox.maxZ() - 1,
                  CAVE_AIR,
                  CAVE_AIR,
                  false
               );
               this.generateBox(
                  p_227836_,
                  p_227840_,
                  this.boundingBox.minX() + 1,
                  this.boundingBox.maxY() - 2,
                  this.boundingBox.minZ(),
                  this.boundingBox.maxX() - 1,
                  this.boundingBox.maxY(),
                  this.boundingBox.maxZ(),
                  CAVE_AIR,
                  CAVE_AIR,
                  false
               );
               this.generateBox(
                  p_227836_,
                  p_227840_,
                  this.boundingBox.minX(),
                  this.boundingBox.maxY() - 2,
                  this.boundingBox.minZ() + 1,
                  this.boundingBox.maxX(),
                  this.boundingBox.maxY(),
                  this.boundingBox.maxZ() - 1,
                  CAVE_AIR,
                  CAVE_AIR,
                  false
               );
               this.generateBox(
                  p_227836_,
                  p_227840_,
                  this.boundingBox.minX() + 1,
                  this.boundingBox.minY() + 3,
                  this.boundingBox.minZ() + 1,
                  this.boundingBox.maxX() - 1,
                  this.boundingBox.minY() + 3,
                  this.boundingBox.maxZ() - 1,
                  CAVE_AIR,
                  CAVE_AIR,
                  false
               );
            } else {
               this.generateBox(
                  p_227836_,
                  p_227840_,
                  this.boundingBox.minX() + 1,
                  this.boundingBox.minY(),
                  this.boundingBox.minZ(),
                  this.boundingBox.maxX() - 1,
                  this.boundingBox.maxY(),
                  this.boundingBox.maxZ(),
                  CAVE_AIR,
                  CAVE_AIR,
                  false
               );
               this.generateBox(
                  p_227836_,
                  p_227840_,
                  this.boundingBox.minX(),
                  this.boundingBox.minY(),
                  this.boundingBox.minZ() + 1,
                  this.boundingBox.maxX(),
                  this.boundingBox.maxY(),
                  this.boundingBox.maxZ() - 1,
                  CAVE_AIR,
                  CAVE_AIR,
                  false
               );
            }

            this.placeSupportPillar(
               p_227836_, p_227840_, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, this.boundingBox.maxY()
            );
            this.placeSupportPillar(
               p_227836_, p_227840_, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.maxZ() - 1, this.boundingBox.maxY()
            );
            this.placeSupportPillar(
               p_227836_, p_227840_, this.boundingBox.maxX() - 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, this.boundingBox.maxY()
            );
            this.placeSupportPillar(
               p_227836_, p_227840_, this.boundingBox.maxX() - 1, this.boundingBox.minY(), this.boundingBox.maxZ() - 1, this.boundingBox.maxY()
            );
            int $$8 = this.boundingBox.minY() - 1;

            for (int $$9 = this.boundingBox.minX(); $$9 <= this.boundingBox.maxX(); $$9++) {
               for (int $$10 = this.boundingBox.minZ(); $$10 <= this.boundingBox.maxZ(); $$10++) {
                  this.setPlanksBlock(p_227836_, p_227840_, $$7, $$9, $$8, $$10);
               }
            }
         }
      }

      private void placeSupportPillar(WorldGenLevel p_227844_, BoundingBox p_227845_, int p_227846_, int p_227847_, int p_227848_, int p_227849_) {
         if (!this.getBlock(p_227844_, p_227846_, p_227849_ + 1, p_227848_, p_227845_).isAir()) {
            this.generateBox(
               p_227844_, p_227845_, p_227846_, p_227847_, p_227848_, p_227846_, p_227849_, p_227848_, this.type.getPlanksState(), CAVE_AIR, false
            );
         }
      }
   }

   abstract static class MineShaftPiece extends StructurePiece {
      protected MineshaftStructure.Type type;

      public MineShaftPiece(StructurePieceType p_227867_, int p_227868_, MineshaftStructure.Type p_227869_, BoundingBox p_227870_) {
         super(p_227867_, p_227868_, p_227870_);
         this.type = p_227869_;
      }

      public MineShaftPiece(StructurePieceType p_227872_, CompoundTag p_227873_) {
         super(p_227872_, p_227873_);
         this.type = MineshaftStructure.Type.byId(p_227873_.getInt("MST"));
      }

      @Override
      protected boolean canBeReplaced(LevelReader p_227885_, int p_227886_, int p_227887_, int p_227888_, BoundingBox p_227889_) {
         BlockState $$5 = this.getBlock(p_227885_, p_227886_, p_227887_, p_227888_, p_227889_);
         return !$$5.is(this.type.getPlanksState().getBlock())
            && !$$5.is(this.type.getWoodState().getBlock())
            && !$$5.is(this.type.getFenceState().getBlock())
            && !$$5.is(Blocks.CHAIN);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext p_227898_, CompoundTag p_227899_) {
         p_227899_.putInt("MST", this.type.ordinal());
      }

      protected boolean isSupportingBox(BlockGetter p_227875_, BoundingBox p_227876_, int p_227877_, int p_227878_, int p_227879_, int p_227880_) {
         for (int $$6 = p_227877_; $$6 <= p_227878_; $$6++) {
            if (this.getBlock(p_227875_, $$6, p_227879_ + 1, p_227880_, p_227876_).isAir()) {
               return false;
            }
         }

         return true;
      }

      protected boolean isInInvalidLocation(LevelAccessor p_227882_, BoundingBox p_227883_) {
         int $$2 = Math.max(this.boundingBox.minX() - 1, p_227883_.minX());
         int $$3 = Math.max(this.boundingBox.minY() - 1, p_227883_.minY());
         int $$4 = Math.max(this.boundingBox.minZ() - 1, p_227883_.minZ());
         int $$5 = Math.min(this.boundingBox.maxX() + 1, p_227883_.maxX());
         int $$6 = Math.min(this.boundingBox.maxY() + 1, p_227883_.maxY());
         int $$7 = Math.min(this.boundingBox.maxZ() + 1, p_227883_.maxZ());
         BlockPos.MutableBlockPos $$8 = new BlockPos.MutableBlockPos(($$2 + $$5) / 2, ($$3 + $$6) / 2, ($$4 + $$7) / 2);
         if (p_227882_.getBiome($$8).is(BiomeTags.MINESHAFT_BLOCKING)) {
            return true;
         } else {
            for (int $$9 = $$2; $$9 <= $$5; $$9++) {
               for (int $$10 = $$4; $$10 <= $$7; $$10++) {
                  if (p_227882_.getBlockState($$8.set($$9, $$3, $$10)).liquid()) {
                     return true;
                  }

                  if (p_227882_.getBlockState($$8.set($$9, $$6, $$10)).liquid()) {
                     return true;
                  }
               }
            }

            for (int $$11 = $$2; $$11 <= $$5; $$11++) {
               for (int $$12 = $$3; $$12 <= $$6; $$12++) {
                  if (p_227882_.getBlockState($$8.set($$11, $$12, $$4)).liquid()) {
                     return true;
                  }

                  if (p_227882_.getBlockState($$8.set($$11, $$12, $$7)).liquid()) {
                     return true;
                  }
               }
            }

            for (int $$13 = $$4; $$13 <= $$7; $$13++) {
               for (int $$14 = $$3; $$14 <= $$6; $$14++) {
                  if (p_227882_.getBlockState($$8.set($$2, $$14, $$13)).liquid()) {
                     return true;
                  }

                  if (p_227882_.getBlockState($$8.set($$5, $$14, $$13)).liquid()) {
                     return true;
                  }
               }
            }

            return false;
         }
      }

      protected void setPlanksBlock(WorldGenLevel p_227891_, BoundingBox p_227892_, BlockState p_227893_, int p_227894_, int p_227895_, int p_227896_) {
         if (this.isInterior(p_227891_, p_227894_, p_227895_, p_227896_, p_227892_)) {
            BlockPos $$6 = this.getWorldPos(p_227894_, p_227895_, p_227896_);
            BlockState $$7 = p_227891_.getBlockState($$6);
            if (!$$7.isFaceSturdy(p_227891_, $$6, Direction.UP)) {
               p_227891_.setBlock($$6, p_227893_, 2);
            }
         }
      }
   }

   public static class MineShaftRoom extends MineshaftPieces.MineShaftPiece {
      private final List<BoundingBox> childEntranceBoxes = Lists.newLinkedList();

      public MineShaftRoom(int p_227902_, RandomSource p_227903_, int p_227904_, int p_227905_, MineshaftStructure.Type p_227906_) {
         super(
            StructurePieceType.MINE_SHAFT_ROOM,
            p_227902_,
            p_227906_,
            new BoundingBox(p_227904_, 50, p_227905_, p_227904_ + 7 + p_227903_.nextInt(6), 54 + p_227903_.nextInt(6), p_227905_ + 7 + p_227903_.nextInt(6))
         );
         this.type = p_227906_;
      }

      public MineShaftRoom(CompoundTag p_227908_) {
         super(StructurePieceType.MINE_SHAFT_ROOM, p_227908_);
         BoundingBox.CODEC
            .listOf()
            .parse(NbtOps.INSTANCE, p_227908_.getList("Entrances", 11))
            .resultOrPartial(MineshaftPieces.LOGGER::error)
            .ifPresent(this.childEntranceBoxes::addAll);
      }

      @Override
      public void addChildren(StructurePiece p_227922_, StructurePieceAccessor p_227923_, RandomSource p_227924_) {
         int $$3 = this.getGenDepth();
         int $$4 = this.boundingBox.getYSpan() - 3 - 1;
         if ($$4 <= 0) {
            $$4 = 1;
         }

         int $$5 = 0;

         while ($$5 < this.boundingBox.getXSpan()) {
            $$5 += p_227924_.nextInt(this.boundingBox.getXSpan());
            if ($$5 + 3 > this.boundingBox.getXSpan()) {
               break;
            }

            MineshaftPieces.MineShaftPiece $$6 = MineshaftPieces.generateAndAddPiece(
               p_227922_,
               p_227923_,
               p_227924_,
               this.boundingBox.minX() + $$5,
               this.boundingBox.minY() + p_227924_.nextInt($$4) + 1,
               this.boundingBox.minZ() - 1,
               Direction.NORTH,
               $$3
            );
            if ($$6 != null) {
               BoundingBox $$7 = $$6.getBoundingBox();
               this.childEntranceBoxes
                  .add(new BoundingBox($$7.minX(), $$7.minY(), this.boundingBox.minZ(), $$7.maxX(), $$7.maxY(), this.boundingBox.minZ() + 1));
            }

            $$5 += 4;
         }

         $$5 = 0;

         while ($$5 < this.boundingBox.getXSpan()) {
            $$5 += p_227924_.nextInt(this.boundingBox.getXSpan());
            if ($$5 + 3 > this.boundingBox.getXSpan()) {
               break;
            }

            MineshaftPieces.MineShaftPiece $$8 = MineshaftPieces.generateAndAddPiece(
               p_227922_,
               p_227923_,
               p_227924_,
               this.boundingBox.minX() + $$5,
               this.boundingBox.minY() + p_227924_.nextInt($$4) + 1,
               this.boundingBox.maxZ() + 1,
               Direction.SOUTH,
               $$3
            );
            if ($$8 != null) {
               BoundingBox $$9 = $$8.getBoundingBox();
               this.childEntranceBoxes
                  .add(new BoundingBox($$9.minX(), $$9.minY(), this.boundingBox.maxZ() - 1, $$9.maxX(), $$9.maxY(), this.boundingBox.maxZ()));
            }

            $$5 += 4;
         }

         $$5 = 0;

         while ($$5 < this.boundingBox.getZSpan()) {
            $$5 += p_227924_.nextInt(this.boundingBox.getZSpan());
            if ($$5 + 3 > this.boundingBox.getZSpan()) {
               break;
            }

            MineshaftPieces.MineShaftPiece $$10 = MineshaftPieces.generateAndAddPiece(
               p_227922_,
               p_227923_,
               p_227924_,
               this.boundingBox.minX() - 1,
               this.boundingBox.minY() + p_227924_.nextInt($$4) + 1,
               this.boundingBox.minZ() + $$5,
               Direction.WEST,
               $$3
            );
            if ($$10 != null) {
               BoundingBox $$11 = $$10.getBoundingBox();
               this.childEntranceBoxes
                  .add(new BoundingBox(this.boundingBox.minX(), $$11.minY(), $$11.minZ(), this.boundingBox.minX() + 1, $$11.maxY(), $$11.maxZ()));
            }

            $$5 += 4;
         }

         $$5 = 0;

         while ($$5 < this.boundingBox.getZSpan()) {
            $$5 += p_227924_.nextInt(this.boundingBox.getZSpan());
            if ($$5 + 3 > this.boundingBox.getZSpan()) {
               break;
            }

            StructurePiece $$12 = MineshaftPieces.generateAndAddPiece(
               p_227922_,
               p_227923_,
               p_227924_,
               this.boundingBox.maxX() + 1,
               this.boundingBox.minY() + p_227924_.nextInt($$4) + 1,
               this.boundingBox.minZ() + $$5,
               Direction.EAST,
               $$3
            );
            if ($$12 != null) {
               BoundingBox $$13 = $$12.getBoundingBox();
               this.childEntranceBoxes
                  .add(new BoundingBox(this.boundingBox.maxX() - 1, $$13.minY(), $$13.minZ(), this.boundingBox.maxX(), $$13.maxY(), $$13.maxZ()));
            }

            $$5 += 4;
         }
      }

      @Override
      public void postProcess(
         WorldGenLevel p_227914_,
         StructureManager p_227915_,
         ChunkGenerator p_227916_,
         RandomSource p_227917_,
         BoundingBox p_227918_,
         ChunkPos p_227919_,
         BlockPos p_227920_
      ) {
         if (!this.isInInvalidLocation(p_227914_, p_227918_)) {
            this.generateBox(
               p_227914_,
               p_227918_,
               this.boundingBox.minX(),
               this.boundingBox.minY() + 1,
               this.boundingBox.minZ(),
               this.boundingBox.maxX(),
               Math.min(this.boundingBox.minY() + 3, this.boundingBox.maxY()),
               this.boundingBox.maxZ(),
               CAVE_AIR,
               CAVE_AIR,
               false
            );

            for (BoundingBox $$7 : this.childEntranceBoxes) {
               this.generateBox(p_227914_, p_227918_, $$7.minX(), $$7.maxY() - 2, $$7.minZ(), $$7.maxX(), $$7.maxY(), $$7.maxZ(), CAVE_AIR, CAVE_AIR, false);
            }

            this.generateUpperHalfSphere(
               p_227914_,
               p_227918_,
               this.boundingBox.minX(),
               this.boundingBox.minY() + 4,
               this.boundingBox.minZ(),
               this.boundingBox.maxX(),
               this.boundingBox.maxY(),
               this.boundingBox.maxZ(),
               CAVE_AIR,
               false
            );
         }
      }

      @Override
      public void move(int p_227910_, int p_227911_, int p_227912_) {
         super.move(p_227910_, p_227911_, p_227912_);

         for (BoundingBox $$3 : this.childEntranceBoxes) {
            $$3.move(p_227910_, p_227911_, p_227912_);
         }
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext p_227926_, CompoundTag p_227927_) {
         super.addAdditionalSaveData(p_227926_, p_227927_);
         BoundingBox.CODEC
            .listOf()
            .encodeStart(NbtOps.INSTANCE, this.childEntranceBoxes)
            .resultOrPartial(MineshaftPieces.LOGGER::error)
            .ifPresent(p_227930_ -> p_227927_.put("Entrances", p_227930_));
      }
   }

   public static class MineShaftStairs extends MineshaftPieces.MineShaftPiece {
      public MineShaftStairs(int p_227932_, BoundingBox p_227933_, Direction p_227934_, MineshaftStructure.Type p_227935_) {
         super(StructurePieceType.MINE_SHAFT_STAIRS, p_227932_, p_227935_, p_227933_);
         this.setOrientation(p_227934_);
      }

      public MineShaftStairs(CompoundTag p_227937_) {
         super(StructurePieceType.MINE_SHAFT_STAIRS, p_227937_);
      }

      @Nullable
      public static BoundingBox findStairs(
         StructurePieceAccessor p_227951_, RandomSource p_227952_, int p_227953_, int p_227954_, int p_227955_, Direction p_227956_
      ) {
         BoundingBox $$9 = switch (p_227956_) {
            default -> new BoundingBox(0, -5, -8, 2, 2, 0);
            case SOUTH -> new BoundingBox(0, -5, 0, 2, 2, 8);
            case WEST -> new BoundingBox(-8, -5, 0, 0, 2, 2);
            case EAST -> new BoundingBox(0, -5, 0, 8, 2, 2);
         };
         $$9.move(p_227953_, p_227954_, p_227955_);
         return p_227951_.findCollisionPiece($$9) != null ? null : $$9;
      }

      @Override
      public void addChildren(StructurePiece p_227947_, StructurePieceAccessor p_227948_, RandomSource p_227949_) {
         int $$3 = this.getGenDepth();
         Direction $$4 = this.getOrientation();
         if ($$4 != null) {
            switch ($$4) {
               case NORTH:
               default:
                  MineshaftPieces.generateAndAddPiece(
                     p_227947_, p_227948_, p_227949_, this.boundingBox.minX(), this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, $$3
                  );
                  break;
               case SOUTH:
                  MineshaftPieces.generateAndAddPiece(
                     p_227947_, p_227948_, p_227949_, this.boundingBox.minX(), this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, $$3
                  );
                  break;
               case WEST:
                  MineshaftPieces.generateAndAddPiece(
                     p_227947_, p_227948_, p_227949_, this.boundingBox.minX() - 1, this.boundingBox.minY(), this.boundingBox.minZ(), Direction.WEST, $$3
                  );
                  break;
               case EAST:
                  MineshaftPieces.generateAndAddPiece(
                     p_227947_, p_227948_, p_227949_, this.boundingBox.maxX() + 1, this.boundingBox.minY(), this.boundingBox.minZ(), Direction.EAST, $$3
                  );
            }
         }
      }

      @Override
      public void postProcess(
         WorldGenLevel p_227939_,
         StructureManager p_227940_,
         ChunkGenerator p_227941_,
         RandomSource p_227942_,
         BoundingBox p_227943_,
         ChunkPos p_227944_,
         BlockPos p_227945_
      ) {
         if (!this.isInInvalidLocation(p_227939_, p_227943_)) {
            this.generateBox(p_227939_, p_227943_, 0, 5, 0, 2, 7, 1, CAVE_AIR, CAVE_AIR, false);
            this.generateBox(p_227939_, p_227943_, 0, 0, 7, 2, 2, 8, CAVE_AIR, CAVE_AIR, false);

            for (int $$7 = 0; $$7 < 5; $$7++) {
               this.generateBox(p_227939_, p_227943_, 0, 5 - $$7 - ($$7 < 4 ? 1 : 0), 2 + $$7, 2, 7 - $$7, 2 + $$7, CAVE_AIR, CAVE_AIR, false);
            }
         }
      }
   }
}
