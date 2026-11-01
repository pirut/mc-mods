package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class WoodlandMansionPieces {
   public static void generateMansion(
      StructureTemplateManager p_229986_,
      BlockPos p_229987_,
      Rotation p_229988_,
      List<WoodlandMansionPieces.WoodlandMansionPiece> p_229989_,
      RandomSource p_229990_
   ) {
      WoodlandMansionPieces.MansionGrid $$5 = new WoodlandMansionPieces.MansionGrid(p_229990_);
      WoodlandMansionPieces.MansionPiecePlacer $$6 = new WoodlandMansionPieces.MansionPiecePlacer(p_229986_, p_229990_);
      $$6.createMansion(p_229987_, p_229988_, p_229989_, $$5);
   }

   static class FirstFloorRoomCollection extends WoodlandMansionPieces.FloorRoomCollection {
      @Override
      public String get1x1(RandomSource p_229995_) {
         return "1x1_a" + (p_229995_.nextInt(5) + 1);
      }

      @Override
      public String get1x1Secret(RandomSource p_230000_) {
         return "1x1_as" + (p_230000_.nextInt(4) + 1);
      }

      @Override
      public String get1x2SideEntrance(RandomSource p_229997_, boolean p_229998_) {
         return "1x2_a" + (p_229997_.nextInt(9) + 1);
      }

      @Override
      public String get1x2FrontEntrance(RandomSource p_230002_, boolean p_230003_) {
         return "1x2_b" + (p_230002_.nextInt(5) + 1);
      }

      @Override
      public String get1x2Secret(RandomSource p_230005_) {
         return "1x2_s" + (p_230005_.nextInt(2) + 1);
      }

      @Override
      public String get2x2(RandomSource p_230007_) {
         return "2x2_a" + (p_230007_.nextInt(4) + 1);
      }

      @Override
      public String get2x2Secret(RandomSource p_230009_) {
         return "2x2_s1";
      }
   }

   abstract static class FloorRoomCollection {
      public abstract String get1x1(RandomSource var1);

      public abstract String get1x1Secret(RandomSource var1);

      public abstract String get1x2SideEntrance(RandomSource var1, boolean var2);

      public abstract String get1x2FrontEntrance(RandomSource var1, boolean var2);

      public abstract String get1x2Secret(RandomSource var1);

      public abstract String get2x2(RandomSource var1);

      public abstract String get2x2Secret(RandomSource var1);
   }

   static class MansionGrid {
      private static final int DEFAULT_SIZE = 11;
      private static final int CLEAR = 0;
      private static final int CORRIDOR = 1;
      private static final int ROOM = 2;
      private static final int START_ROOM = 3;
      private static final int TEST_ROOM = 4;
      private static final int BLOCKED = 5;
      private static final int ROOM_1x1 = 65536;
      private static final int ROOM_1x2 = 131072;
      private static final int ROOM_2x2 = 262144;
      private static final int ROOM_ORIGIN_FLAG = 1048576;
      private static final int ROOM_DOOR_FLAG = 2097152;
      private static final int ROOM_STAIRS_FLAG = 4194304;
      private static final int ROOM_CORRIDOR_FLAG = 8388608;
      private static final int ROOM_TYPE_MASK = 983040;
      private static final int ROOM_ID_MASK = 65535;
      private final RandomSource random;
      final WoodlandMansionPieces.SimpleGrid baseGrid;
      final WoodlandMansionPieces.SimpleGrid thirdFloorGrid;
      final WoodlandMansionPieces.SimpleGrid[] floorRooms;
      final int entranceX;
      final int entranceY;

      public MansionGrid(RandomSource p_230043_) {
         this.random = p_230043_;
         int $$1 = 11;
         this.entranceX = 7;
         this.entranceY = 4;
         this.baseGrid = new WoodlandMansionPieces.SimpleGrid(11, 11, 5);
         this.baseGrid.set(this.entranceX, this.entranceY, this.entranceX + 1, this.entranceY + 1, 3);
         this.baseGrid.set(this.entranceX - 1, this.entranceY, this.entranceX - 1, this.entranceY + 1, 2);
         this.baseGrid.set(this.entranceX + 2, this.entranceY - 2, this.entranceX + 3, this.entranceY + 3, 5);
         this.baseGrid.set(this.entranceX + 1, this.entranceY - 2, this.entranceX + 1, this.entranceY - 1, 1);
         this.baseGrid.set(this.entranceX + 1, this.entranceY + 2, this.entranceX + 1, this.entranceY + 3, 1);
         this.baseGrid.set(this.entranceX - 1, this.entranceY - 1, 1);
         this.baseGrid.set(this.entranceX - 1, this.entranceY + 2, 1);
         this.baseGrid.set(0, 0, 11, 1, 5);
         this.baseGrid.set(0, 9, 11, 11, 5);
         this.recursiveCorridor(this.baseGrid, this.entranceX, this.entranceY - 2, Direction.WEST, 6);
         this.recursiveCorridor(this.baseGrid, this.entranceX, this.entranceY + 3, Direction.WEST, 6);
         this.recursiveCorridor(this.baseGrid, this.entranceX - 2, this.entranceY - 1, Direction.WEST, 3);
         this.recursiveCorridor(this.baseGrid, this.entranceX - 2, this.entranceY + 2, Direction.WEST, 3);

         while (this.cleanEdges(this.baseGrid)) {
         }

         this.floorRooms = new WoodlandMansionPieces.SimpleGrid[3];
         this.floorRooms[0] = new WoodlandMansionPieces.SimpleGrid(11, 11, 5);
         this.floorRooms[1] = new WoodlandMansionPieces.SimpleGrid(11, 11, 5);
         this.floorRooms[2] = new WoodlandMansionPieces.SimpleGrid(11, 11, 5);
         this.identifyRooms(this.baseGrid, this.floorRooms[0]);
         this.identifyRooms(this.baseGrid, this.floorRooms[1]);
         this.floorRooms[0].set(this.entranceX + 1, this.entranceY, this.entranceX + 1, this.entranceY + 1, 8388608);
         this.floorRooms[1].set(this.entranceX + 1, this.entranceY, this.entranceX + 1, this.entranceY + 1, 8388608);
         this.thirdFloorGrid = new WoodlandMansionPieces.SimpleGrid(this.baseGrid.width, this.baseGrid.height, 5);
         this.setupThirdFloor();
         this.identifyRooms(this.thirdFloorGrid, this.floorRooms[2]);
      }

      public static boolean isHouse(WoodlandMansionPieces.SimpleGrid p_230048_, int p_230049_, int p_230050_) {
         int $$3 = p_230048_.get(p_230049_, p_230050_);
         return $$3 == 1 || $$3 == 2 || $$3 == 3 || $$3 == 4;
      }

      public boolean isRoomId(WoodlandMansionPieces.SimpleGrid p_230052_, int p_230053_, int p_230054_, int p_230055_, int p_230056_) {
         return (this.floorRooms[p_230055_].get(p_230053_, p_230054_) & 65535) == p_230056_;
      }

      @Nullable
      public Direction get1x2RoomDirection(WoodlandMansionPieces.SimpleGrid p_230068_, int p_230069_, int p_230070_, int p_230071_, int p_230072_) {
         for (Direction $$5 : Direction.Plane.HORIZONTAL) {
            if (this.isRoomId(p_230068_, p_230069_ + $$5.getStepX(), p_230070_ + $$5.getStepZ(), p_230071_, p_230072_)) {
               return $$5;
            }
         }

         return null;
      }

      private void recursiveCorridor(WoodlandMansionPieces.SimpleGrid p_230058_, int p_230059_, int p_230060_, Direction p_230061_, int p_230062_) {
         if (p_230062_ > 0) {
            p_230058_.set(p_230059_, p_230060_, 1);
            p_230058_.setif(p_230059_ + p_230061_.getStepX(), p_230060_ + p_230061_.getStepZ(), 0, 1);

            for (int $$5 = 0; $$5 < 8; $$5++) {
               Direction $$6 = Direction.from2DDataValue(this.random.nextInt(4));
               if ($$6 != p_230061_.getOpposite() && ($$6 != Direction.EAST || !this.random.nextBoolean())) {
                  int $$7 = p_230059_ + p_230061_.getStepX();
                  int $$8 = p_230060_ + p_230061_.getStepZ();
                  if (p_230058_.get($$7 + $$6.getStepX(), $$8 + $$6.getStepZ()) == 0 && p_230058_.get($$7 + $$6.getStepX() * 2, $$8 + $$6.getStepZ() * 2) == 0) {
                     this.recursiveCorridor(
                        p_230058_, p_230059_ + p_230061_.getStepX() + $$6.getStepX(), p_230060_ + p_230061_.getStepZ() + $$6.getStepZ(), $$6, p_230062_ - 1
                     );
                     break;
                  }
               }
            }

            Direction $$9 = p_230061_.getClockWise();
            Direction $$10 = p_230061_.getCounterClockWise();
            p_230058_.setif(p_230059_ + $$9.getStepX(), p_230060_ + $$9.getStepZ(), 0, 2);
            p_230058_.setif(p_230059_ + $$10.getStepX(), p_230060_ + $$10.getStepZ(), 0, 2);
            p_230058_.setif(p_230059_ + p_230061_.getStepX() + $$9.getStepX(), p_230060_ + p_230061_.getStepZ() + $$9.getStepZ(), 0, 2);
            p_230058_.setif(p_230059_ + p_230061_.getStepX() + $$10.getStepX(), p_230060_ + p_230061_.getStepZ() + $$10.getStepZ(), 0, 2);
            p_230058_.setif(p_230059_ + p_230061_.getStepX() * 2, p_230060_ + p_230061_.getStepZ() * 2, 0, 2);
            p_230058_.setif(p_230059_ + $$9.getStepX() * 2, p_230060_ + $$9.getStepZ() * 2, 0, 2);
            p_230058_.setif(p_230059_ + $$10.getStepX() * 2, p_230060_ + $$10.getStepZ() * 2, 0, 2);
         }
      }

      private boolean cleanEdges(WoodlandMansionPieces.SimpleGrid p_230046_) {
         boolean $$1 = false;

         for (int $$2 = 0; $$2 < p_230046_.height; $$2++) {
            for (int $$3 = 0; $$3 < p_230046_.width; $$3++) {
               if (p_230046_.get($$3, $$2) == 0) {
                  int $$4 = 0;
                  $$4 += isHouse(p_230046_, $$3 + 1, $$2) ? 1 : 0;
                  $$4 += isHouse(p_230046_, $$3 - 1, $$2) ? 1 : 0;
                  $$4 += isHouse(p_230046_, $$3, $$2 + 1) ? 1 : 0;
                  $$4 += isHouse(p_230046_, $$3, $$2 - 1) ? 1 : 0;
                  if ($$4 >= 3) {
                     p_230046_.set($$3, $$2, 2);
                     $$1 = true;
                  } else if ($$4 == 2) {
                     int $$5 = 0;
                     $$5 += isHouse(p_230046_, $$3 + 1, $$2 + 1) ? 1 : 0;
                     $$5 += isHouse(p_230046_, $$3 - 1, $$2 + 1) ? 1 : 0;
                     $$5 += isHouse(p_230046_, $$3 + 1, $$2 - 1) ? 1 : 0;
                     $$5 += isHouse(p_230046_, $$3 - 1, $$2 - 1) ? 1 : 0;
                     if ($$5 <= 1) {
                        p_230046_.set($$3, $$2, 2);
                        $$1 = true;
                     }
                  }
               }
            }
         }

         return $$1;
      }

      private void setupThirdFloor() {
         List<Tuple<Integer, Integer>> $$0 = Lists.newArrayList();
         WoodlandMansionPieces.SimpleGrid $$1 = this.floorRooms[1];

         for (int $$2 = 0; $$2 < this.thirdFloorGrid.height; $$2++) {
            for (int $$3 = 0; $$3 < this.thirdFloorGrid.width; $$3++) {
               int $$4 = $$1.get($$3, $$2);
               int $$5 = $$4 & 983040;
               if ($$5 == 131072 && ($$4 & 2097152) == 2097152) {
                  $$0.add(new Tuple<>($$3, $$2));
               }
            }
         }

         if ($$0.isEmpty()) {
            this.thirdFloorGrid.set(0, 0, this.thirdFloorGrid.width, this.thirdFloorGrid.height, 5);
         } else {
            Tuple<Integer, Integer> $$6 = $$0.get(this.random.nextInt($$0.size()));
            int $$7 = $$1.get($$6.getA(), $$6.getB());
            $$1.set($$6.getA(), $$6.getB(), $$7 | 4194304);
            Direction $$8 = this.get1x2RoomDirection(this.baseGrid, $$6.getA(), $$6.getB(), 1, $$7 & 65535);
            int $$9 = $$6.getA() + $$8.getStepX();
            int $$10 = $$6.getB() + $$8.getStepZ();

            for (int $$11 = 0; $$11 < this.thirdFloorGrid.height; $$11++) {
               for (int $$12 = 0; $$12 < this.thirdFloorGrid.width; $$12++) {
                  if (!isHouse(this.baseGrid, $$12, $$11)) {
                     this.thirdFloorGrid.set($$12, $$11, 5);
                  } else if ($$12 == $$6.getA() && $$11 == $$6.getB()) {
                     this.thirdFloorGrid.set($$12, $$11, 3);
                  } else if ($$12 == $$9 && $$11 == $$10) {
                     this.thirdFloorGrid.set($$12, $$11, 3);
                     this.floorRooms[2].set($$12, $$11, 8388608);
                  }
               }
            }

            List<Direction> $$13 = Lists.newArrayList();

            for (Direction $$14 : Direction.Plane.HORIZONTAL) {
               if (this.thirdFloorGrid.get($$9 + $$14.getStepX(), $$10 + $$14.getStepZ()) == 0) {
                  $$13.add($$14);
               }
            }

            if ($$13.isEmpty()) {
               this.thirdFloorGrid.set(0, 0, this.thirdFloorGrid.width, this.thirdFloorGrid.height, 5);
               $$1.set($$6.getA(), $$6.getB(), $$7);
            } else {
               Direction $$15 = $$13.get(this.random.nextInt($$13.size()));
               this.recursiveCorridor(this.thirdFloorGrid, $$9 + $$15.getStepX(), $$10 + $$15.getStepZ(), $$15, 4);

               while (this.cleanEdges(this.thirdFloorGrid)) {
               }
            }
         }
      }

      private void identifyRooms(WoodlandMansionPieces.SimpleGrid p_230064_, WoodlandMansionPieces.SimpleGrid p_230065_) {
         ObjectArrayList<Tuple<Integer, Integer>> $$2 = new ObjectArrayList();

         for (int $$3 = 0; $$3 < p_230064_.height; $$3++) {
            for (int $$4 = 0; $$4 < p_230064_.width; $$4++) {
               if (p_230064_.get($$4, $$3) == 2) {
                  $$2.add(new Tuple<>($$4, $$3));
               }
            }
         }

         Util.shuffle($$2, this.random);
         int $$5 = 10;
         ObjectListIterator var20 = $$2.iterator();

         while (var20.hasNext()) {
            Tuple<Integer, Integer> $$6 = (Tuple<Integer, Integer>)var20.next();
            int $$7 = $$6.getA();
            int $$8 = $$6.getB();
            if (p_230065_.get($$7, $$8) == 0) {
               int $$9 = $$7;
               int $$10 = $$7;
               int $$11 = $$8;
               int $$12 = $$8;
               int $$13 = 65536;
               if (p_230065_.get($$7 + 1, $$8) == 0
                  && p_230065_.get($$7, $$8 + 1) == 0
                  && p_230065_.get($$7 + 1, $$8 + 1) == 0
                  && p_230064_.get($$7 + 1, $$8) == 2
                  && p_230064_.get($$7, $$8 + 1) == 2
                  && p_230064_.get($$7 + 1, $$8 + 1) == 2) {
                  $$10 = $$7 + 1;
                  $$12 = $$8 + 1;
                  $$13 = 262144;
               } else if (p_230065_.get($$7 - 1, $$8) == 0
                  && p_230065_.get($$7, $$8 + 1) == 0
                  && p_230065_.get($$7 - 1, $$8 + 1) == 0
                  && p_230064_.get($$7 - 1, $$8) == 2
                  && p_230064_.get($$7, $$8 + 1) == 2
                  && p_230064_.get($$7 - 1, $$8 + 1) == 2) {
                  $$9 = $$7 - 1;
                  $$12 = $$8 + 1;
                  $$13 = 262144;
               } else if (p_230065_.get($$7 - 1, $$8) == 0
                  && p_230065_.get($$7, $$8 - 1) == 0
                  && p_230065_.get($$7 - 1, $$8 - 1) == 0
                  && p_230064_.get($$7 - 1, $$8) == 2
                  && p_230064_.get($$7, $$8 - 1) == 2
                  && p_230064_.get($$7 - 1, $$8 - 1) == 2) {
                  $$9 = $$7 - 1;
                  $$11 = $$8 - 1;
                  $$13 = 262144;
               } else if (p_230065_.get($$7 + 1, $$8) == 0 && p_230064_.get($$7 + 1, $$8) == 2) {
                  $$10 = $$7 + 1;
                  $$13 = 131072;
               } else if (p_230065_.get($$7, $$8 + 1) == 0 && p_230064_.get($$7, $$8 + 1) == 2) {
                  $$12 = $$8 + 1;
                  $$13 = 131072;
               } else if (p_230065_.get($$7 - 1, $$8) == 0 && p_230064_.get($$7 - 1, $$8) == 2) {
                  $$9 = $$7 - 1;
                  $$13 = 131072;
               } else if (p_230065_.get($$7, $$8 - 1) == 0 && p_230064_.get($$7, $$8 - 1) == 2) {
                  $$11 = $$8 - 1;
                  $$13 = 131072;
               }

               int $$14 = this.random.nextBoolean() ? $$9 : $$10;
               int $$15 = this.random.nextBoolean() ? $$11 : $$12;
               int $$16 = 2097152;
               if (!p_230064_.edgesTo($$14, $$15, 1)) {
                  $$14 = $$14 == $$9 ? $$10 : $$9;
                  $$15 = $$15 == $$11 ? $$12 : $$11;
                  if (!p_230064_.edgesTo($$14, $$15, 1)) {
                     $$15 = $$15 == $$11 ? $$12 : $$11;
                     if (!p_230064_.edgesTo($$14, $$15, 1)) {
                        $$14 = $$14 == $$9 ? $$10 : $$9;
                        $$15 = $$15 == $$11 ? $$12 : $$11;
                        if (!p_230064_.edgesTo($$14, $$15, 1)) {
                           $$16 = 0;
                           $$14 = $$9;
                           $$15 = $$11;
                        }
                     }
                  }
               }

               for (int $$17 = $$11; $$17 <= $$12; $$17++) {
                  for (int $$18 = $$9; $$18 <= $$10; $$18++) {
                     if ($$18 == $$14 && $$17 == $$15) {
                        p_230065_.set($$18, $$17, 1048576 | $$16 | $$13 | $$5);
                     } else {
                        p_230065_.set($$18, $$17, $$13 | $$5);
                     }
                  }
               }

               $$5++;
            }
         }
      }
   }

   static class MansionPiecePlacer {
      private final StructureTemplateManager structureTemplateManager;
      private final RandomSource random;
      private int startX;
      private int startY;

      public MansionPiecePlacer(StructureTemplateManager p_230078_, RandomSource p_230079_) {
         this.structureTemplateManager = p_230078_;
         this.random = p_230079_;
      }

      public void createMansion(
         BlockPos p_230081_, Rotation p_230082_, List<WoodlandMansionPieces.WoodlandMansionPiece> p_230083_, WoodlandMansionPieces.MansionGrid p_230084_
      ) {
         WoodlandMansionPieces.PlacementData $$4 = new WoodlandMansionPieces.PlacementData();
         $$4.position = p_230081_;
         $$4.rotation = p_230082_;
         $$4.wallType = "wall_flat";
         WoodlandMansionPieces.PlacementData $$5 = new WoodlandMansionPieces.PlacementData();
         this.entrance(p_230083_, $$4);
         $$5.position = $$4.position.above(8);
         $$5.rotation = $$4.rotation;
         $$5.wallType = "wall_window";
         if (!p_230083_.isEmpty()) {
         }

         WoodlandMansionPieces.SimpleGrid $$6 = p_230084_.baseGrid;
         WoodlandMansionPieces.SimpleGrid $$7 = p_230084_.thirdFloorGrid;
         this.startX = p_230084_.entranceX + 1;
         this.startY = p_230084_.entranceY + 1;
         int $$8 = p_230084_.entranceX + 1;
         int $$9 = p_230084_.entranceY;
         this.traverseOuterWalls(p_230083_, $$4, $$6, Direction.SOUTH, this.startX, this.startY, $$8, $$9);
         this.traverseOuterWalls(p_230083_, $$5, $$6, Direction.SOUTH, this.startX, this.startY, $$8, $$9);
         WoodlandMansionPieces.PlacementData $$10 = new WoodlandMansionPieces.PlacementData();
         $$10.position = $$4.position.above(19);
         $$10.rotation = $$4.rotation;
         $$10.wallType = "wall_window";
         boolean $$11 = false;

         for (int $$12 = 0; $$12 < $$7.height && !$$11; $$12++) {
            for (int $$13 = $$7.width - 1; $$13 >= 0 && !$$11; $$13--) {
               if (WoodlandMansionPieces.MansionGrid.isHouse($$7, $$13, $$12)) {
                  $$10.position = $$10.position.relative(p_230082_.rotate(Direction.SOUTH), 8 + ($$12 - this.startY) * 8);
                  $$10.position = $$10.position.relative(p_230082_.rotate(Direction.EAST), ($$13 - this.startX) * 8);
                  this.traverseWallPiece(p_230083_, $$10);
                  this.traverseOuterWalls(p_230083_, $$10, $$7, Direction.SOUTH, $$13, $$12, $$13, $$12);
                  $$11 = true;
               }
            }
         }

         this.createRoof(p_230083_, p_230081_.above(16), p_230082_, $$6, $$7);
         this.createRoof(p_230083_, p_230081_.above(27), p_230082_, $$7, null);
         if (!p_230083_.isEmpty()) {
         }

         WoodlandMansionPieces.FloorRoomCollection[] $$14 = new WoodlandMansionPieces.FloorRoomCollection[]{
            new WoodlandMansionPieces.FirstFloorRoomCollection(),
            new WoodlandMansionPieces.SecondFloorRoomCollection(),
            new WoodlandMansionPieces.ThirdFloorRoomCollection()
         };

         for (int $$15 = 0; $$15 < 3; $$15++) {
            BlockPos $$16 = p_230081_.above(8 * $$15 + ($$15 == 2 ? 3 : 0));
            WoodlandMansionPieces.SimpleGrid $$17 = p_230084_.floorRooms[$$15];
            WoodlandMansionPieces.SimpleGrid $$18 = $$15 == 2 ? $$7 : $$6;
            String $$19 = $$15 == 0 ? "carpet_south_1" : "carpet_south_2";
            String $$20 = $$15 == 0 ? "carpet_west_1" : "carpet_west_2";

            for (int $$21 = 0; $$21 < $$18.height; $$21++) {
               for (int $$22 = 0; $$22 < $$18.width; $$22++) {
                  if ($$18.get($$22, $$21) == 1) {
                     BlockPos $$23 = $$16.relative(p_230082_.rotate(Direction.SOUTH), 8 + ($$21 - this.startY) * 8);
                     $$23 = $$23.relative(p_230082_.rotate(Direction.EAST), ($$22 - this.startX) * 8);
                     p_230083_.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "corridor_floor", $$23, p_230082_));
                     if ($$18.get($$22, $$21 - 1) == 1 || ($$17.get($$22, $$21 - 1) & 8388608) == 8388608) {
                        p_230083_.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "carpet_north", $$23.relative(p_230082_.rotate(Direction.EAST), 1).above(), p_230082_
                           )
                        );
                     }

                     if ($$18.get($$22 + 1, $$21) == 1 || ($$17.get($$22 + 1, $$21) & 8388608) == 8388608) {
                        p_230083_.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager,
                              "carpet_east",
                              $$23.relative(p_230082_.rotate(Direction.SOUTH), 1).relative(p_230082_.rotate(Direction.EAST), 5).above(),
                              p_230082_
                           )
                        );
                     }

                     if ($$18.get($$22, $$21 + 1) == 1 || ($$17.get($$22, $$21 + 1) & 8388608) == 8388608) {
                        p_230083_.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager,
                              $$19,
                              $$23.relative(p_230082_.rotate(Direction.SOUTH), 5).relative(p_230082_.rotate(Direction.WEST), 1),
                              p_230082_
                           )
                        );
                     }

                     if ($$18.get($$22 - 1, $$21) == 1 || ($$17.get($$22 - 1, $$21) & 8388608) == 8388608) {
                        p_230083_.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager,
                              $$20,
                              $$23.relative(p_230082_.rotate(Direction.WEST), 1).relative(p_230082_.rotate(Direction.NORTH), 1),
                              p_230082_
                           )
                        );
                     }
                  }
               }
            }

            String $$24 = $$15 == 0 ? "indoors_wall_1" : "indoors_wall_2";
            String $$25 = $$15 == 0 ? "indoors_door_1" : "indoors_door_2";
            List<Direction> $$26 = Lists.newArrayList();

            for (int $$27 = 0; $$27 < $$18.height; $$27++) {
               for (int $$28 = 0; $$28 < $$18.width; $$28++) {
                  boolean $$29 = $$15 == 2 && $$18.get($$28, $$27) == 3;
                  if ($$18.get($$28, $$27) == 2 || $$29) {
                     int $$30 = $$17.get($$28, $$27);
                     int $$31 = $$30 & 983040;
                     int $$32 = $$30 & 65535;
                     $$29 = $$29 && ($$30 & 8388608) == 8388608;
                     $$26.clear();
                     if (($$30 & 2097152) == 2097152) {
                        for (Direction $$33 : Direction.Plane.HORIZONTAL) {
                           if ($$18.get($$28 + $$33.getStepX(), $$27 + $$33.getStepZ()) == 1) {
                              $$26.add($$33);
                           }
                        }
                     }

                     Direction $$34 = null;
                     if (!$$26.isEmpty()) {
                        $$34 = $$26.get(this.random.nextInt($$26.size()));
                     } else if (($$30 & 1048576) == 1048576) {
                        $$34 = Direction.UP;
                     }

                     BlockPos $$35 = $$16.relative(p_230082_.rotate(Direction.SOUTH), 8 + ($$27 - this.startY) * 8);
                     $$35 = $$35.relative(p_230082_.rotate(Direction.EAST), -1 + ($$28 - this.startX) * 8);
                     if (WoodlandMansionPieces.MansionGrid.isHouse($$18, $$28 - 1, $$27) && !p_230084_.isRoomId($$18, $$28 - 1, $$27, $$15, $$32)) {
                        p_230083_.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, $$34 == Direction.WEST ? $$25 : $$24, $$35, p_230082_)
                        );
                     }

                     if ($$18.get($$28 + 1, $$27) == 1 && !$$29) {
                        BlockPos $$36 = $$35.relative(p_230082_.rotate(Direction.EAST), 8);
                        p_230083_.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, $$34 == Direction.EAST ? $$25 : $$24, $$36, p_230082_)
                        );
                     }

                     if (WoodlandMansionPieces.MansionGrid.isHouse($$18, $$28, $$27 + 1) && !p_230084_.isRoomId($$18, $$28, $$27 + 1, $$15, $$32)) {
                        BlockPos $$37 = $$35.relative(p_230082_.rotate(Direction.SOUTH), 7);
                        $$37 = $$37.relative(p_230082_.rotate(Direction.EAST), 7);
                        p_230083_.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, $$34 == Direction.SOUTH ? $$25 : $$24, $$37, p_230082_.getRotated(Rotation.CLOCKWISE_90)
                           )
                        );
                     }

                     if ($$18.get($$28, $$27 - 1) == 1 && !$$29) {
                        BlockPos $$38 = $$35.relative(p_230082_.rotate(Direction.NORTH), 1);
                        $$38 = $$38.relative(p_230082_.rotate(Direction.EAST), 7);
                        p_230083_.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, $$34 == Direction.NORTH ? $$25 : $$24, $$38, p_230082_.getRotated(Rotation.CLOCKWISE_90)
                           )
                        );
                     }

                     if ($$31 == 65536) {
                        this.addRoom1x1(p_230083_, $$35, p_230082_, $$34, $$14[$$15]);
                     } else if ($$31 == 131072 && $$34 != null) {
                        Direction $$39 = p_230084_.get1x2RoomDirection($$18, $$28, $$27, $$15, $$32);
                        boolean $$40 = ($$30 & 4194304) == 4194304;
                        this.addRoom1x2(p_230083_, $$35, p_230082_, $$39, $$34, $$14[$$15], $$40);
                     } else if ($$31 == 262144 && $$34 != null && $$34 != Direction.UP) {
                        Direction $$41 = $$34.getClockWise();
                        if (!p_230084_.isRoomId($$18, $$28 + $$41.getStepX(), $$27 + $$41.getStepZ(), $$15, $$32)) {
                           $$41 = $$41.getOpposite();
                        }

                        this.addRoom2x2(p_230083_, $$35, p_230082_, $$41, $$34, $$14[$$15]);
                     } else if ($$31 == 262144 && $$34 == Direction.UP) {
                        this.addRoom2x2Secret(p_230083_, $$35, p_230082_, $$14[$$15]);
                     }
                  }
               }
            }
         }
      }

      private void traverseOuterWalls(
         List<WoodlandMansionPieces.WoodlandMansionPiece> p_230089_,
         WoodlandMansionPieces.PlacementData p_230090_,
         WoodlandMansionPieces.SimpleGrid p_230091_,
         Direction p_230092_,
         int p_230093_,
         int p_230094_,
         int p_230095_,
         int p_230096_
      ) {
         int $$8 = p_230093_;
         int $$9 = p_230094_;
         Direction $$10 = p_230092_;

         do {
            if (!WoodlandMansionPieces.MansionGrid.isHouse(p_230091_, $$8 + p_230092_.getStepX(), $$9 + p_230092_.getStepZ())) {
               this.traverseTurn(p_230089_, p_230090_);
               p_230092_ = p_230092_.getClockWise();
               if ($$8 != p_230095_ || $$9 != p_230096_ || $$10 != p_230092_) {
                  this.traverseWallPiece(p_230089_, p_230090_);
               }
            } else if (WoodlandMansionPieces.MansionGrid.isHouse(p_230091_, $$8 + p_230092_.getStepX(), $$9 + p_230092_.getStepZ())
               && WoodlandMansionPieces.MansionGrid.isHouse(
                  p_230091_,
                  $$8 + p_230092_.getStepX() + p_230092_.getCounterClockWise().getStepX(),
                  $$9 + p_230092_.getStepZ() + p_230092_.getCounterClockWise().getStepZ()
               )) {
               this.traverseInnerTurn(p_230089_, p_230090_);
               $$8 += p_230092_.getStepX();
               $$9 += p_230092_.getStepZ();
               p_230092_ = p_230092_.getCounterClockWise();
            } else {
               $$8 += p_230092_.getStepX();
               $$9 += p_230092_.getStepZ();
               if ($$8 != p_230095_ || $$9 != p_230096_ || $$10 != p_230092_) {
                  this.traverseWallPiece(p_230089_, p_230090_);
               }
            }
         } while ($$8 != p_230095_ || $$9 != p_230096_ || $$10 != p_230092_);
      }

      private void createRoof(
         List<WoodlandMansionPieces.WoodlandMansionPiece> p_230103_,
         BlockPos p_230104_,
         Rotation p_230105_,
         WoodlandMansionPieces.SimpleGrid p_230106_,
         @Nullable WoodlandMansionPieces.SimpleGrid p_230107_
      ) {
         for (int $$5 = 0; $$5 < p_230106_.height; $$5++) {
            for (int $$6 = 0; $$6 < p_230106_.width; $$6++) {
               BlockPos $$27 = p_230104_.relative(p_230105_.rotate(Direction.SOUTH), 8 + ($$5 - this.startY) * 8);
               $$27 = $$27.relative(p_230105_.rotate(Direction.EAST), ($$6 - this.startX) * 8);
               boolean $$8 = p_230107_ != null && WoodlandMansionPieces.MansionGrid.isHouse(p_230107_, $$6, $$5);
               if (WoodlandMansionPieces.MansionGrid.isHouse(p_230106_, $$6, $$5) && !$$8) {
                  p_230103_.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "roof", $$27.above(3), p_230105_));
                  if (!WoodlandMansionPieces.MansionGrid.isHouse(p_230106_, $$6 + 1, $$5)) {
                     BlockPos $$9 = $$27.relative(p_230105_.rotate(Direction.EAST), 6);
                     p_230103_.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "roof_front", $$9, p_230105_));
                  }

                  if (!WoodlandMansionPieces.MansionGrid.isHouse(p_230106_, $$6 - 1, $$5)) {
                     BlockPos $$10 = $$27.relative(p_230105_.rotate(Direction.EAST), 0);
                     $$10 = $$10.relative(p_230105_.rotate(Direction.SOUTH), 7);
                     p_230103_.add(
                        new WoodlandMansionPieces.WoodlandMansionPiece(
                           this.structureTemplateManager, "roof_front", $$10, p_230105_.getRotated(Rotation.CLOCKWISE_180)
                        )
                     );
                  }

                  if (!WoodlandMansionPieces.MansionGrid.isHouse(p_230106_, $$6, $$5 - 1)) {
                     BlockPos $$11 = $$27.relative(p_230105_.rotate(Direction.WEST), 1);
                     p_230103_.add(
                        new WoodlandMansionPieces.WoodlandMansionPiece(
                           this.structureTemplateManager, "roof_front", $$11, p_230105_.getRotated(Rotation.COUNTERCLOCKWISE_90)
                        )
                     );
                  }

                  if (!WoodlandMansionPieces.MansionGrid.isHouse(p_230106_, $$6, $$5 + 1)) {
                     BlockPos $$12 = $$27.relative(p_230105_.rotate(Direction.EAST), 6);
                     $$12 = $$12.relative(p_230105_.rotate(Direction.SOUTH), 6);
                     p_230103_.add(
                        new WoodlandMansionPieces.WoodlandMansionPiece(
                           this.structureTemplateManager, "roof_front", $$12, p_230105_.getRotated(Rotation.CLOCKWISE_90)
                        )
                     );
                  }
               }
            }
         }

         if (p_230107_ != null) {
            for (int $$13 = 0; $$13 < p_230106_.height; $$13++) {
               for (int $$14 = 0; $$14 < p_230106_.width; $$14++) {
                  BlockPos var17 = p_230104_.relative(p_230105_.rotate(Direction.SOUTH), 8 + ($$13 - this.startY) * 8);
                  var17 = var17.relative(p_230105_.rotate(Direction.EAST), ($$14 - this.startX) * 8);
                  boolean $$16 = WoodlandMansionPieces.MansionGrid.isHouse(p_230107_, $$14, $$13);
                  if (WoodlandMansionPieces.MansionGrid.isHouse(p_230106_, $$14, $$13) && $$16) {
                     if (!WoodlandMansionPieces.MansionGrid.isHouse(p_230106_, $$14 + 1, $$13)) {
                        BlockPos $$17 = var17.relative(p_230105_.rotate(Direction.EAST), 7);
                        p_230103_.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "small_wall", $$17, p_230105_));
                     }

                     if (!WoodlandMansionPieces.MansionGrid.isHouse(p_230106_, $$14 - 1, $$13)) {
                        BlockPos $$18 = var17.relative(p_230105_.rotate(Direction.WEST), 1);
                        $$18 = $$18.relative(p_230105_.rotate(Direction.SOUTH), 6);
                        p_230103_.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "small_wall", $$18, p_230105_.getRotated(Rotation.CLOCKWISE_180)
                           )
                        );
                     }

                     if (!WoodlandMansionPieces.MansionGrid.isHouse(p_230106_, $$14, $$13 - 1)) {
                        BlockPos $$19 = var17.relative(p_230105_.rotate(Direction.WEST), 0);
                        $$19 = $$19.relative(p_230105_.rotate(Direction.NORTH), 1);
                        p_230103_.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "small_wall", $$19, p_230105_.getRotated(Rotation.COUNTERCLOCKWISE_90)
                           )
                        );
                     }

                     if (!WoodlandMansionPieces.MansionGrid.isHouse(p_230106_, $$14, $$13 + 1)) {
                        BlockPos $$20 = var17.relative(p_230105_.rotate(Direction.EAST), 6);
                        $$20 = $$20.relative(p_230105_.rotate(Direction.SOUTH), 7);
                        p_230103_.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "small_wall", $$20, p_230105_.getRotated(Rotation.CLOCKWISE_90)
                           )
                        );
                     }

                     if (!WoodlandMansionPieces.MansionGrid.isHouse(p_230106_, $$14 + 1, $$13)) {
                        if (!WoodlandMansionPieces.MansionGrid.isHouse(p_230106_, $$14, $$13 - 1)) {
                           BlockPos $$21 = var17.relative(p_230105_.rotate(Direction.EAST), 7);
                           $$21 = $$21.relative(p_230105_.rotate(Direction.NORTH), 2);
                           p_230103_.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "small_wall_corner", $$21, p_230105_));
                        }

                        if (!WoodlandMansionPieces.MansionGrid.isHouse(p_230106_, $$14, $$13 + 1)) {
                           BlockPos $$22 = var17.relative(p_230105_.rotate(Direction.EAST), 8);
                           $$22 = $$22.relative(p_230105_.rotate(Direction.SOUTH), 7);
                           p_230103_.add(
                              new WoodlandMansionPieces.WoodlandMansionPiece(
                                 this.structureTemplateManager, "small_wall_corner", $$22, p_230105_.getRotated(Rotation.CLOCKWISE_90)
                              )
                           );
                        }
                     }

                     if (!WoodlandMansionPieces.MansionGrid.isHouse(p_230106_, $$14 - 1, $$13)) {
                        if (!WoodlandMansionPieces.MansionGrid.isHouse(p_230106_, $$14, $$13 - 1)) {
                           BlockPos $$23 = var17.relative(p_230105_.rotate(Direction.WEST), 2);
                           $$23 = $$23.relative(p_230105_.rotate(Direction.NORTH), 1);
                           p_230103_.add(
                              new WoodlandMansionPieces.WoodlandMansionPiece(
                                 this.structureTemplateManager, "small_wall_corner", $$23, p_230105_.getRotated(Rotation.COUNTERCLOCKWISE_90)
                              )
                           );
                        }

                        if (!WoodlandMansionPieces.MansionGrid.isHouse(p_230106_, $$14, $$13 + 1)) {
                           BlockPos $$24 = var17.relative(p_230105_.rotate(Direction.WEST), 1);
                           $$24 = $$24.relative(p_230105_.rotate(Direction.SOUTH), 8);
                           p_230103_.add(
                              new WoodlandMansionPieces.WoodlandMansionPiece(
                                 this.structureTemplateManager, "small_wall_corner", $$24, p_230105_.getRotated(Rotation.CLOCKWISE_180)
                              )
                           );
                        }
                     }
                  }
               }
            }
         }

         for (int $$25 = 0; $$25 < p_230106_.height; $$25++) {
            for (int $$26 = 0; $$26 < p_230106_.width; $$26++) {
               BlockPos var19 = p_230104_.relative(p_230105_.rotate(Direction.SOUTH), 8 + ($$25 - this.startY) * 8);
               var19 = var19.relative(p_230105_.rotate(Direction.EAST), ($$26 - this.startX) * 8);
               boolean $$28 = p_230107_ != null && WoodlandMansionPieces.MansionGrid.isHouse(p_230107_, $$26, $$25);
               if (WoodlandMansionPieces.MansionGrid.isHouse(p_230106_, $$26, $$25) && !$$28) {
                  if (!WoodlandMansionPieces.MansionGrid.isHouse(p_230106_, $$26 + 1, $$25)) {
                     BlockPos $$29 = var19.relative(p_230105_.rotate(Direction.EAST), 6);
                     if (!WoodlandMansionPieces.MansionGrid.isHouse(p_230106_, $$26, $$25 + 1)) {
                        BlockPos $$30 = $$29.relative(p_230105_.rotate(Direction.SOUTH), 6);
                        p_230103_.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "roof_corner", $$30, p_230105_));
                     } else if (WoodlandMansionPieces.MansionGrid.isHouse(p_230106_, $$26 + 1, $$25 + 1)) {
                        BlockPos $$31 = $$29.relative(p_230105_.rotate(Direction.SOUTH), 5);
                        p_230103_.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "roof_inner_corner", $$31, p_230105_));
                     }

                     if (!WoodlandMansionPieces.MansionGrid.isHouse(p_230106_, $$26, $$25 - 1)) {
                        p_230103_.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "roof_corner", $$29, p_230105_.getRotated(Rotation.COUNTERCLOCKWISE_90)
                           )
                        );
                     } else if (WoodlandMansionPieces.MansionGrid.isHouse(p_230106_, $$26 + 1, $$25 - 1)) {
                        BlockPos $$32 = var19.relative(p_230105_.rotate(Direction.EAST), 9);
                        $$32 = $$32.relative(p_230105_.rotate(Direction.NORTH), 2);
                        p_230103_.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "roof_inner_corner", $$32, p_230105_.getRotated(Rotation.CLOCKWISE_90)
                           )
                        );
                     }
                  }

                  if (!WoodlandMansionPieces.MansionGrid.isHouse(p_230106_, $$26 - 1, $$25)) {
                     BlockPos $$33 = var19.relative(p_230105_.rotate(Direction.EAST), 0);
                     $$33 = $$33.relative(p_230105_.rotate(Direction.SOUTH), 0);
                     if (!WoodlandMansionPieces.MansionGrid.isHouse(p_230106_, $$26, $$25 + 1)) {
                        BlockPos $$34 = $$33.relative(p_230105_.rotate(Direction.SOUTH), 6);
                        p_230103_.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "roof_corner", $$34, p_230105_.getRotated(Rotation.CLOCKWISE_90)
                           )
                        );
                     } else if (WoodlandMansionPieces.MansionGrid.isHouse(p_230106_, $$26 - 1, $$25 + 1)) {
                        BlockPos $$35 = $$33.relative(p_230105_.rotate(Direction.SOUTH), 8);
                        $$35 = $$35.relative(p_230105_.rotate(Direction.WEST), 3);
                        p_230103_.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "roof_inner_corner", $$35, p_230105_.getRotated(Rotation.COUNTERCLOCKWISE_90)
                           )
                        );
                     }

                     if (!WoodlandMansionPieces.MansionGrid.isHouse(p_230106_, $$26, $$25 - 1)) {
                        p_230103_.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "roof_corner", $$33, p_230105_.getRotated(Rotation.CLOCKWISE_180)
                           )
                        );
                     } else if (WoodlandMansionPieces.MansionGrid.isHouse(p_230106_, $$26 - 1, $$25 - 1)) {
                        BlockPos $$36 = $$33.relative(p_230105_.rotate(Direction.SOUTH), 1);
                        p_230103_.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "roof_inner_corner", $$36, p_230105_.getRotated(Rotation.CLOCKWISE_180)
                           )
                        );
                     }
                  }
               }
            }
         }
      }

      private void entrance(List<WoodlandMansionPieces.WoodlandMansionPiece> p_230086_, WoodlandMansionPieces.PlacementData p_230087_) {
         Direction $$2 = p_230087_.rotation.rotate(Direction.WEST);
         p_230086_.add(
            new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "entrance", p_230087_.position.relative($$2, 9), p_230087_.rotation)
         );
         p_230087_.position = p_230087_.position.relative(p_230087_.rotation.rotate(Direction.SOUTH), 16);
      }

      private void traverseWallPiece(List<WoodlandMansionPieces.WoodlandMansionPiece> p_230130_, WoodlandMansionPieces.PlacementData p_230131_) {
         p_230130_.add(
            new WoodlandMansionPieces.WoodlandMansionPiece(
               this.structureTemplateManager, p_230131_.wallType, p_230131_.position.relative(p_230131_.rotation.rotate(Direction.EAST), 7), p_230131_.rotation
            )
         );
         p_230131_.position = p_230131_.position.relative(p_230131_.rotation.rotate(Direction.SOUTH), 8);
      }

      private void traverseTurn(List<WoodlandMansionPieces.WoodlandMansionPiece> p_230133_, WoodlandMansionPieces.PlacementData p_230134_) {
         p_230134_.position = p_230134_.position.relative(p_230134_.rotation.rotate(Direction.SOUTH), -1);
         p_230133_.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "wall_corner", p_230134_.position, p_230134_.rotation));
         p_230134_.position = p_230134_.position.relative(p_230134_.rotation.rotate(Direction.SOUTH), -7);
         p_230134_.position = p_230134_.position.relative(p_230134_.rotation.rotate(Direction.WEST), -6);
         p_230134_.rotation = p_230134_.rotation.getRotated(Rotation.CLOCKWISE_90);
      }

      private void traverseInnerTurn(List<WoodlandMansionPieces.WoodlandMansionPiece> p_230136_, WoodlandMansionPieces.PlacementData p_230137_) {
         p_230137_.position = p_230137_.position.relative(p_230137_.rotation.rotate(Direction.SOUTH), 6);
         p_230137_.position = p_230137_.position.relative(p_230137_.rotation.rotate(Direction.EAST), 8);
         p_230137_.rotation = p_230137_.rotation.getRotated(Rotation.COUNTERCLOCKWISE_90);
      }

      private void addRoom1x1(
         List<WoodlandMansionPieces.WoodlandMansionPiece> p_230109_,
         BlockPos p_230110_,
         Rotation p_230111_,
         Direction p_230112_,
         WoodlandMansionPieces.FloorRoomCollection p_230113_
      ) {
         Rotation $$5 = Rotation.NONE;
         String $$6 = p_230113_.get1x1(this.random);
         if (p_230112_ != Direction.EAST) {
            if (p_230112_ == Direction.NORTH) {
               $$5 = $$5.getRotated(Rotation.COUNTERCLOCKWISE_90);
            } else if (p_230112_ == Direction.WEST) {
               $$5 = $$5.getRotated(Rotation.CLOCKWISE_180);
            } else if (p_230112_ == Direction.SOUTH) {
               $$5 = $$5.getRotated(Rotation.CLOCKWISE_90);
            } else {
               $$6 = p_230113_.get1x1Secret(this.random);
            }
         }

         BlockPos $$7 = StructureTemplate.getZeroPositionWithTransform(new BlockPos(1, 0, 0), Mirror.NONE, $$5, 7, 7);
         $$5 = $$5.getRotated(p_230111_);
         $$7 = $$7.rotate(p_230111_);
         BlockPos $$8 = p_230110_.offset($$7.getX(), 0, $$7.getZ());
         p_230109_.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, $$6, $$8, $$5));
      }

      private void addRoom1x2(
         List<WoodlandMansionPieces.WoodlandMansionPiece> p_230122_,
         BlockPos p_230123_,
         Rotation p_230124_,
         Direction p_230125_,
         Direction p_230126_,
         WoodlandMansionPieces.FloorRoomCollection p_230127_,
         boolean p_230128_
      ) {
         if (p_230126_ == Direction.EAST && p_230125_ == Direction.SOUTH) {
            BlockPos $$7 = p_230123_.relative(p_230124_.rotate(Direction.EAST), 1);
            p_230122_.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, p_230127_.get1x2SideEntrance(this.random, p_230128_), $$7, p_230124_
               )
            );
         } else if (p_230126_ == Direction.EAST && p_230125_ == Direction.NORTH) {
            BlockPos $$8 = p_230123_.relative(p_230124_.rotate(Direction.EAST), 1);
            $$8 = $$8.relative(p_230124_.rotate(Direction.SOUTH), 6);
            p_230122_.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, p_230127_.get1x2SideEntrance(this.random, p_230128_), $$8, p_230124_, Mirror.LEFT_RIGHT
               )
            );
         } else if (p_230126_ == Direction.WEST && p_230125_ == Direction.NORTH) {
            BlockPos $$9 = p_230123_.relative(p_230124_.rotate(Direction.EAST), 7);
            $$9 = $$9.relative(p_230124_.rotate(Direction.SOUTH), 6);
            p_230122_.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, p_230127_.get1x2SideEntrance(this.random, p_230128_), $$9, p_230124_.getRotated(Rotation.CLOCKWISE_180)
               )
            );
         } else if (p_230126_ == Direction.WEST && p_230125_ == Direction.SOUTH) {
            BlockPos $$10 = p_230123_.relative(p_230124_.rotate(Direction.EAST), 7);
            p_230122_.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, p_230127_.get1x2SideEntrance(this.random, p_230128_), $$10, p_230124_, Mirror.FRONT_BACK
               )
            );
         } else if (p_230126_ == Direction.SOUTH && p_230125_ == Direction.EAST) {
            BlockPos $$11 = p_230123_.relative(p_230124_.rotate(Direction.EAST), 1);
            p_230122_.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager,
                  p_230127_.get1x2SideEntrance(this.random, p_230128_),
                  $$11,
                  p_230124_.getRotated(Rotation.CLOCKWISE_90),
                  Mirror.LEFT_RIGHT
               )
            );
         } else if (p_230126_ == Direction.SOUTH && p_230125_ == Direction.WEST) {
            BlockPos $$12 = p_230123_.relative(p_230124_.rotate(Direction.EAST), 7);
            p_230122_.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, p_230127_.get1x2SideEntrance(this.random, p_230128_), $$12, p_230124_.getRotated(Rotation.CLOCKWISE_90)
               )
            );
         } else if (p_230126_ == Direction.NORTH && p_230125_ == Direction.WEST) {
            BlockPos $$13 = p_230123_.relative(p_230124_.rotate(Direction.EAST), 7);
            $$13 = $$13.relative(p_230124_.rotate(Direction.SOUTH), 6);
            p_230122_.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager,
                  p_230127_.get1x2SideEntrance(this.random, p_230128_),
                  $$13,
                  p_230124_.getRotated(Rotation.CLOCKWISE_90),
                  Mirror.FRONT_BACK
               )
            );
         } else if (p_230126_ == Direction.NORTH && p_230125_ == Direction.EAST) {
            BlockPos $$14 = p_230123_.relative(p_230124_.rotate(Direction.EAST), 1);
            $$14 = $$14.relative(p_230124_.rotate(Direction.SOUTH), 6);
            p_230122_.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, p_230127_.get1x2SideEntrance(this.random, p_230128_), $$14, p_230124_.getRotated(Rotation.COUNTERCLOCKWISE_90)
               )
            );
         } else if (p_230126_ == Direction.SOUTH && p_230125_ == Direction.NORTH) {
            BlockPos $$15 = p_230123_.relative(p_230124_.rotate(Direction.EAST), 1);
            $$15 = $$15.relative(p_230124_.rotate(Direction.NORTH), 8);
            p_230122_.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, p_230127_.get1x2FrontEntrance(this.random, p_230128_), $$15, p_230124_
               )
            );
         } else if (p_230126_ == Direction.NORTH && p_230125_ == Direction.SOUTH) {
            BlockPos $$16 = p_230123_.relative(p_230124_.rotate(Direction.EAST), 7);
            $$16 = $$16.relative(p_230124_.rotate(Direction.SOUTH), 14);
            p_230122_.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, p_230127_.get1x2FrontEntrance(this.random, p_230128_), $$16, p_230124_.getRotated(Rotation.CLOCKWISE_180)
               )
            );
         } else if (p_230126_ == Direction.WEST && p_230125_ == Direction.EAST) {
            BlockPos $$17 = p_230123_.relative(p_230124_.rotate(Direction.EAST), 15);
            p_230122_.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, p_230127_.get1x2FrontEntrance(this.random, p_230128_), $$17, p_230124_.getRotated(Rotation.CLOCKWISE_90)
               )
            );
         } else if (p_230126_ == Direction.EAST && p_230125_ == Direction.WEST) {
            BlockPos $$18 = p_230123_.relative(p_230124_.rotate(Direction.WEST), 7);
            $$18 = $$18.relative(p_230124_.rotate(Direction.SOUTH), 6);
            p_230122_.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager,
                  p_230127_.get1x2FrontEntrance(this.random, p_230128_),
                  $$18,
                  p_230124_.getRotated(Rotation.COUNTERCLOCKWISE_90)
               )
            );
         } else if (p_230126_ == Direction.UP && p_230125_ == Direction.EAST) {
            BlockPos $$19 = p_230123_.relative(p_230124_.rotate(Direction.EAST), 15);
            p_230122_.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, p_230127_.get1x2Secret(this.random), $$19, p_230124_.getRotated(Rotation.CLOCKWISE_90)
               )
            );
         } else if (p_230126_ == Direction.UP && p_230125_ == Direction.SOUTH) {
            BlockPos $$20 = p_230123_.relative(p_230124_.rotate(Direction.EAST), 1);
            $$20 = $$20.relative(p_230124_.rotate(Direction.NORTH), 0);
            p_230122_.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, p_230127_.get1x2Secret(this.random), $$20, p_230124_));
         }
      }

      private void addRoom2x2(
         List<WoodlandMansionPieces.WoodlandMansionPiece> p_230115_,
         BlockPos p_230116_,
         Rotation p_230117_,
         Direction p_230118_,
         Direction p_230119_,
         WoodlandMansionPieces.FloorRoomCollection p_230120_
      ) {
         int $$6 = 0;
         int $$7 = 0;
         Rotation $$8 = p_230117_;
         Mirror $$9 = Mirror.NONE;
         if (p_230119_ == Direction.EAST && p_230118_ == Direction.SOUTH) {
            $$6 = -7;
         } else if (p_230119_ == Direction.EAST && p_230118_ == Direction.NORTH) {
            $$6 = -7;
            $$7 = 6;
            $$9 = Mirror.LEFT_RIGHT;
         } else if (p_230119_ == Direction.NORTH && p_230118_ == Direction.EAST) {
            $$6 = 1;
            $$7 = 14;
            $$8 = p_230117_.getRotated(Rotation.COUNTERCLOCKWISE_90);
         } else if (p_230119_ == Direction.NORTH && p_230118_ == Direction.WEST) {
            $$6 = 7;
            $$7 = 14;
            $$8 = p_230117_.getRotated(Rotation.COUNTERCLOCKWISE_90);
            $$9 = Mirror.LEFT_RIGHT;
         } else if (p_230119_ == Direction.SOUTH && p_230118_ == Direction.WEST) {
            $$6 = 7;
            $$7 = -8;
            $$8 = p_230117_.getRotated(Rotation.CLOCKWISE_90);
         } else if (p_230119_ == Direction.SOUTH && p_230118_ == Direction.EAST) {
            $$6 = 1;
            $$7 = -8;
            $$8 = p_230117_.getRotated(Rotation.CLOCKWISE_90);
            $$9 = Mirror.LEFT_RIGHT;
         } else if (p_230119_ == Direction.WEST && p_230118_ == Direction.NORTH) {
            $$6 = 15;
            $$7 = 6;
            $$8 = p_230117_.getRotated(Rotation.CLOCKWISE_180);
         } else if (p_230119_ == Direction.WEST && p_230118_ == Direction.SOUTH) {
            $$6 = 15;
            $$9 = Mirror.FRONT_BACK;
         }

         BlockPos $$10 = p_230116_.relative(p_230117_.rotate(Direction.EAST), $$6);
         $$10 = $$10.relative(p_230117_.rotate(Direction.SOUTH), $$7);
         p_230115_.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, p_230120_.get2x2(this.random), $$10, $$8, $$9));
      }

      private void addRoom2x2Secret(
         List<WoodlandMansionPieces.WoodlandMansionPiece> p_230098_,
         BlockPos p_230099_,
         Rotation p_230100_,
         WoodlandMansionPieces.FloorRoomCollection p_230101_
      ) {
         BlockPos $$4 = p_230099_.relative(p_230100_.rotate(Direction.EAST), 1);
         p_230098_.add(
            new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, p_230101_.get2x2Secret(this.random), $$4, p_230100_, Mirror.NONE)
         );
      }
   }

   static class PlacementData {
      public Rotation rotation;
      public BlockPos position;
      public String wallType;
   }

   static class SecondFloorRoomCollection extends WoodlandMansionPieces.FloorRoomCollection {
      @Override
      public String get1x1(RandomSource p_230144_) {
         return "1x1_b" + (p_230144_.nextInt(4) + 1);
      }

      @Override
      public String get1x1Secret(RandomSource p_230149_) {
         return "1x1_as" + (p_230149_.nextInt(4) + 1);
      }

      @Override
      public String get1x2SideEntrance(RandomSource p_230146_, boolean p_230147_) {
         return p_230147_ ? "1x2_c_stairs" : "1x2_c" + (p_230146_.nextInt(4) + 1);
      }

      @Override
      public String get1x2FrontEntrance(RandomSource p_230151_, boolean p_230152_) {
         return p_230152_ ? "1x2_d_stairs" : "1x2_d" + (p_230151_.nextInt(5) + 1);
      }

      @Override
      public String get1x2Secret(RandomSource p_230154_) {
         return "1x2_se" + (p_230154_.nextInt(1) + 1);
      }

      @Override
      public String get2x2(RandomSource p_230156_) {
         return "2x2_b" + (p_230156_.nextInt(5) + 1);
      }

      @Override
      public String get2x2Secret(RandomSource p_230158_) {
         return "2x2_s1";
      }
   }

   static class SimpleGrid {
      private final int[][] grid;
      final int width;
      final int height;
      private final int valueIfOutside;

      public SimpleGrid(int p_230164_, int p_230165_, int p_230166_) {
         this.width = p_230164_;
         this.height = p_230165_;
         this.valueIfOutside = p_230166_;
         this.grid = new int[p_230164_][p_230165_];
      }

      public void set(int p_230171_, int p_230172_, int p_230173_) {
         if (p_230171_ >= 0 && p_230171_ < this.width && p_230172_ >= 0 && p_230172_ < this.height) {
            this.grid[p_230171_][p_230172_] = p_230173_;
         }
      }

      public void set(int p_230180_, int p_230181_, int p_230182_, int p_230183_, int p_230184_) {
         for (int $$5 = p_230181_; $$5 <= p_230183_; $$5++) {
            for (int $$6 = p_230180_; $$6 <= p_230182_; $$6++) {
               this.set($$6, $$5, p_230184_);
            }
         }
      }

      public int get(int p_230168_, int p_230169_) {
         return p_230168_ >= 0 && p_230168_ < this.width && p_230169_ >= 0 && p_230169_ < this.height ? this.grid[p_230168_][p_230169_] : this.valueIfOutside;
      }

      public void setif(int p_230175_, int p_230176_, int p_230177_, int p_230178_) {
         if (this.get(p_230175_, p_230176_) == p_230177_) {
            this.set(p_230175_, p_230176_, p_230178_);
         }
      }

      public boolean edgesTo(int p_230186_, int p_230187_, int p_230188_) {
         return this.get(p_230186_ - 1, p_230187_) == p_230188_
            || this.get(p_230186_ + 1, p_230187_) == p_230188_
            || this.get(p_230186_, p_230187_ + 1) == p_230188_
            || this.get(p_230186_, p_230187_ - 1) == p_230188_;
      }
   }

   static class ThirdFloorRoomCollection extends WoodlandMansionPieces.SecondFloorRoomCollection {
   }

   public static class WoodlandMansionPiece extends TemplateStructurePiece {
      public WoodlandMansionPiece(StructureTemplateManager p_230191_, String p_230192_, BlockPos p_230193_, Rotation p_230194_) {
         this(p_230191_, p_230192_, p_230193_, p_230194_, Mirror.NONE);
      }

      public WoodlandMansionPiece(StructureTemplateManager p_230196_, String p_230197_, BlockPos p_230198_, Rotation p_230199_, Mirror p_230200_) {
         super(StructurePieceType.WOODLAND_MANSION_PIECE, 0, p_230196_, makeLocation(p_230197_), p_230197_, makeSettings(p_230200_, p_230199_), p_230198_);
      }

      public WoodlandMansionPiece(StructureTemplateManager p_230202_, CompoundTag p_230203_) {
         super(
            StructurePieceType.WOODLAND_MANSION_PIECE,
            p_230203_,
            p_230202_,
            p_230220_ -> makeSettings(Mirror.valueOf(p_230203_.getString("Mi")), Rotation.valueOf(p_230203_.getString("Rot")))
         );
      }

      @Override
      protected ResourceLocation makeTemplateLocation() {
         return makeLocation(this.templateName);
      }

      private static ResourceLocation makeLocation(String p_230211_) {
         return ResourceLocation.withDefaultNamespace("woodland_mansion/" + p_230211_);
      }

      private static StructurePlaceSettings makeSettings(Mirror p_230205_, Rotation p_230206_) {
         return new StructurePlaceSettings()
            .setIgnoreEntities(true)
            .setRotation(p_230206_)
            .setMirror(p_230205_)
            .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext p_230208_, CompoundTag p_230209_) {
         super.addAdditionalSaveData(p_230208_, p_230209_);
         p_230209_.putString("Rot", this.placeSettings.getRotation().name());
         p_230209_.putString("Mi", this.placeSettings.getMirror().name());
      }

      @Override
      protected void handleDataMarker(String p_230213_, BlockPos p_230214_, ServerLevelAccessor p_230215_, RandomSource p_230216_, BoundingBox p_230217_) {
         if (p_230213_.startsWith("Chest")) {
            Rotation $$5 = this.placeSettings.getRotation();
            BlockState $$6 = Blocks.CHEST.defaultBlockState();
            if ("ChestWest".equals(p_230213_)) {
               $$6 = $$6.setValue(ChestBlock.FACING, $$5.rotate(Direction.WEST));
            } else if ("ChestEast".equals(p_230213_)) {
               $$6 = $$6.setValue(ChestBlock.FACING, $$5.rotate(Direction.EAST));
            } else if ("ChestSouth".equals(p_230213_)) {
               $$6 = $$6.setValue(ChestBlock.FACING, $$5.rotate(Direction.SOUTH));
            } else if ("ChestNorth".equals(p_230213_)) {
               $$6 = $$6.setValue(ChestBlock.FACING, $$5.rotate(Direction.NORTH));
            }

            this.createChest(p_230215_, p_230217_, p_230216_, p_230214_, BuiltInLootTables.WOODLAND_MANSION, $$6);
         } else {
            List<Mob> $$7 = new ArrayList<>();
            switch (p_230213_) {
               case "Mage":
                  $$7.add(EntityType.EVOKER.create(p_230215_.getLevel()));
                  break;
               case "Warrior":
                  $$7.add(EntityType.VINDICATOR.create(p_230215_.getLevel()));
                  break;
               case "Group of Allays":
                  int $$8 = p_230215_.getRandom().nextInt(3) + 1;

                  for (int $$9 = 0; $$9 < $$8; $$9++) {
                     $$7.add(EntityType.ALLAY.create(p_230215_.getLevel()));
                  }
                  break;
               default:
                  return;
            }

            for (Mob $$10 : $$7) {
               if ($$10 != null) {
                  $$10.setPersistenceRequired();
                  $$10.moveTo(p_230214_, 0.0F, 0.0F);
                  $$10.finalizeSpawn(p_230215_, p_230215_.getCurrentDifficultyAt($$10.blockPosition()), MobSpawnType.STRUCTURE, null);
                  p_230215_.addFreshEntityWithPassengers($$10);
                  p_230215_.setBlock(p_230214_, Blocks.AIR.defaultBlockState(), 2);
               }
            }
         }
      }
   }
}
