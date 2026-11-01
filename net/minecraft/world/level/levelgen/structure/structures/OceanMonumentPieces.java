package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.List;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

public class OceanMonumentPieces {
   private OceanMonumentPieces() {
   }

   static class FitDoubleXRoom implements OceanMonumentPieces.MonumentRoomFitter {
      @Override
      public boolean fits(OceanMonumentPieces.RoomDefinition p_228592_) {
         return p_228592_.hasOpening[Direction.EAST.get3DDataValue()] && !p_228592_.connections[Direction.EAST.get3DDataValue()].claimed;
      }

      @Override
      public OceanMonumentPieces.OceanMonumentPiece create(Direction p_228594_, OceanMonumentPieces.RoomDefinition p_228595_, RandomSource p_228596_) {
         p_228595_.claimed = true;
         p_228595_.connections[Direction.EAST.get3DDataValue()].claimed = true;
         return new OceanMonumentPieces.OceanMonumentDoubleXRoom(p_228594_, p_228595_);
      }
   }

   static class FitDoubleXYRoom implements OceanMonumentPieces.MonumentRoomFitter {
      @Override
      public boolean fits(OceanMonumentPieces.RoomDefinition p_228599_) {
         if (p_228599_.hasOpening[Direction.EAST.get3DDataValue()]
            && !p_228599_.connections[Direction.EAST.get3DDataValue()].claimed
            && p_228599_.hasOpening[Direction.UP.get3DDataValue()]
            && !p_228599_.connections[Direction.UP.get3DDataValue()].claimed) {
            OceanMonumentPieces.RoomDefinition $$1 = p_228599_.connections[Direction.EAST.get3DDataValue()];
            return $$1.hasOpening[Direction.UP.get3DDataValue()] && !$$1.connections[Direction.UP.get3DDataValue()].claimed;
         } else {
            return false;
         }
      }

      @Override
      public OceanMonumentPieces.OceanMonumentPiece create(Direction p_228601_, OceanMonumentPieces.RoomDefinition p_228602_, RandomSource p_228603_) {
         p_228602_.claimed = true;
         p_228602_.connections[Direction.EAST.get3DDataValue()].claimed = true;
         p_228602_.connections[Direction.UP.get3DDataValue()].claimed = true;
         p_228602_.connections[Direction.EAST.get3DDataValue()].connections[Direction.UP.get3DDataValue()].claimed = true;
         return new OceanMonumentPieces.OceanMonumentDoubleXYRoom(p_228601_, p_228602_);
      }
   }

   static class FitDoubleYRoom implements OceanMonumentPieces.MonumentRoomFitter {
      @Override
      public boolean fits(OceanMonumentPieces.RoomDefinition p_228606_) {
         return p_228606_.hasOpening[Direction.UP.get3DDataValue()] && !p_228606_.connections[Direction.UP.get3DDataValue()].claimed;
      }

      @Override
      public OceanMonumentPieces.OceanMonumentPiece create(Direction p_228608_, OceanMonumentPieces.RoomDefinition p_228609_, RandomSource p_228610_) {
         p_228609_.claimed = true;
         p_228609_.connections[Direction.UP.get3DDataValue()].claimed = true;
         return new OceanMonumentPieces.OceanMonumentDoubleYRoom(p_228608_, p_228609_);
      }
   }

   static class FitDoubleYZRoom implements OceanMonumentPieces.MonumentRoomFitter {
      @Override
      public boolean fits(OceanMonumentPieces.RoomDefinition p_228613_) {
         if (p_228613_.hasOpening[Direction.NORTH.get3DDataValue()]
            && !p_228613_.connections[Direction.NORTH.get3DDataValue()].claimed
            && p_228613_.hasOpening[Direction.UP.get3DDataValue()]
            && !p_228613_.connections[Direction.UP.get3DDataValue()].claimed) {
            OceanMonumentPieces.RoomDefinition $$1 = p_228613_.connections[Direction.NORTH.get3DDataValue()];
            return $$1.hasOpening[Direction.UP.get3DDataValue()] && !$$1.connections[Direction.UP.get3DDataValue()].claimed;
         } else {
            return false;
         }
      }

      @Override
      public OceanMonumentPieces.OceanMonumentPiece create(Direction p_228615_, OceanMonumentPieces.RoomDefinition p_228616_, RandomSource p_228617_) {
         p_228616_.claimed = true;
         p_228616_.connections[Direction.NORTH.get3DDataValue()].claimed = true;
         p_228616_.connections[Direction.UP.get3DDataValue()].claimed = true;
         p_228616_.connections[Direction.NORTH.get3DDataValue()].connections[Direction.UP.get3DDataValue()].claimed = true;
         return new OceanMonumentPieces.OceanMonumentDoubleYZRoom(p_228615_, p_228616_);
      }
   }

   static class FitDoubleZRoom implements OceanMonumentPieces.MonumentRoomFitter {
      @Override
      public boolean fits(OceanMonumentPieces.RoomDefinition p_228620_) {
         return p_228620_.hasOpening[Direction.NORTH.get3DDataValue()] && !p_228620_.connections[Direction.NORTH.get3DDataValue()].claimed;
      }

      @Override
      public OceanMonumentPieces.OceanMonumentPiece create(Direction p_228622_, OceanMonumentPieces.RoomDefinition p_228623_, RandomSource p_228624_) {
         OceanMonumentPieces.RoomDefinition $$3 = p_228623_;
         if (!p_228623_.hasOpening[Direction.NORTH.get3DDataValue()] || p_228623_.connections[Direction.NORTH.get3DDataValue()].claimed) {
            $$3 = p_228623_.connections[Direction.SOUTH.get3DDataValue()];
         }

         $$3.claimed = true;
         $$3.connections[Direction.NORTH.get3DDataValue()].claimed = true;
         return new OceanMonumentPieces.OceanMonumentDoubleZRoom(p_228622_, $$3);
      }
   }

   static class FitSimpleRoom implements OceanMonumentPieces.MonumentRoomFitter {
      @Override
      public boolean fits(OceanMonumentPieces.RoomDefinition p_228627_) {
         return true;
      }

      @Override
      public OceanMonumentPieces.OceanMonumentPiece create(Direction p_228629_, OceanMonumentPieces.RoomDefinition p_228630_, RandomSource p_228631_) {
         p_228630_.claimed = true;
         return new OceanMonumentPieces.OceanMonumentSimpleRoom(p_228629_, p_228630_, p_228631_);
      }
   }

   static class FitSimpleTopRoom implements OceanMonumentPieces.MonumentRoomFitter {
      @Override
      public boolean fits(OceanMonumentPieces.RoomDefinition p_228634_) {
         return !p_228634_.hasOpening[Direction.WEST.get3DDataValue()]
            && !p_228634_.hasOpening[Direction.EAST.get3DDataValue()]
            && !p_228634_.hasOpening[Direction.NORTH.get3DDataValue()]
            && !p_228634_.hasOpening[Direction.SOUTH.get3DDataValue()]
            && !p_228634_.hasOpening[Direction.UP.get3DDataValue()];
      }

      @Override
      public OceanMonumentPieces.OceanMonumentPiece create(Direction p_228636_, OceanMonumentPieces.RoomDefinition p_228637_, RandomSource p_228638_) {
         p_228637_.claimed = true;
         return new OceanMonumentPieces.OceanMonumentSimpleTopRoom(p_228636_, p_228637_);
      }
   }

   public static class MonumentBuilding extends OceanMonumentPieces.OceanMonumentPiece {
      private static final int WIDTH = 58;
      private static final int HEIGHT = 22;
      private static final int DEPTH = 58;
      public static final int BIOME_RANGE_CHECK = 29;
      private static final int TOP_POSITION = 61;
      private OceanMonumentPieces.RoomDefinition sourceRoom;
      private OceanMonumentPieces.RoomDefinition coreRoom;
      private final List<OceanMonumentPieces.OceanMonumentPiece> childPieces = Lists.newArrayList();

      public MonumentBuilding(RandomSource p_228648_, int p_228649_, int p_228650_, Direction p_228651_) {
         super(StructurePieceType.OCEAN_MONUMENT_BUILDING, p_228651_, 0, makeBoundingBox(p_228649_, 39, p_228650_, p_228651_, 58, 23, 58));
         this.setOrientation(p_228651_);
         List<OceanMonumentPieces.RoomDefinition> $$4 = this.generateRoomGraph(p_228648_);
         this.sourceRoom.claimed = true;
         this.childPieces.add(new OceanMonumentPieces.OceanMonumentEntryRoom(p_228651_, this.sourceRoom));
         this.childPieces.add(new OceanMonumentPieces.OceanMonumentCoreRoom(p_228651_, this.coreRoom));
         List<OceanMonumentPieces.MonumentRoomFitter> $$5 = Lists.newArrayList();
         $$5.add(new OceanMonumentPieces.FitDoubleXYRoom());
         $$5.add(new OceanMonumentPieces.FitDoubleYZRoom());
         $$5.add(new OceanMonumentPieces.FitDoubleZRoom());
         $$5.add(new OceanMonumentPieces.FitDoubleXRoom());
         $$5.add(new OceanMonumentPieces.FitDoubleYRoom());
         $$5.add(new OceanMonumentPieces.FitSimpleTopRoom());
         $$5.add(new OceanMonumentPieces.FitSimpleRoom());

         for (OceanMonumentPieces.RoomDefinition $$6 : $$4) {
            if (!$$6.claimed && !$$6.isSpecial()) {
               for (OceanMonumentPieces.MonumentRoomFitter $$7 : $$5) {
                  if ($$7.fits($$6)) {
                     this.childPieces.add($$7.create(p_228651_, $$6, p_228648_));
                     break;
                  }
               }
            }
         }

         BlockPos $$8 = this.getWorldPos(9, 0, 22);

         for (OceanMonumentPieces.OceanMonumentPiece $$9 : this.childPieces) {
            $$9.getBoundingBox().move($$8);
         }

         BoundingBox $$10 = BoundingBox.fromCorners(this.getWorldPos(1, 1, 1), this.getWorldPos(23, 8, 21));
         BoundingBox $$11 = BoundingBox.fromCorners(this.getWorldPos(34, 1, 1), this.getWorldPos(56, 8, 21));
         BoundingBox $$12 = BoundingBox.fromCorners(this.getWorldPos(22, 13, 22), this.getWorldPos(35, 17, 35));
         int $$13 = p_228648_.nextInt();
         this.childPieces.add(new OceanMonumentPieces.OceanMonumentWingRoom(p_228651_, $$10, $$13++));
         this.childPieces.add(new OceanMonumentPieces.OceanMonumentWingRoom(p_228651_, $$11, $$13++));
         this.childPieces.add(new OceanMonumentPieces.OceanMonumentPenthouse(p_228651_, $$12));
      }

      public MonumentBuilding(CompoundTag p_228653_) {
         super(StructurePieceType.OCEAN_MONUMENT_BUILDING, p_228653_);
      }

      private List<OceanMonumentPieces.RoomDefinition> generateRoomGraph(RandomSource p_228673_) {
         OceanMonumentPieces.RoomDefinition[] $$1 = new OceanMonumentPieces.RoomDefinition[75];

         for (int $$2 = 0; $$2 < 5; $$2++) {
            for (int $$3 = 0; $$3 < 4; $$3++) {
               int $$4 = 0;
               int $$5 = getRoomIndex($$2, 0, $$3);
               $$1[$$5] = new OceanMonumentPieces.RoomDefinition($$5);
            }
         }

         for (int $$6 = 0; $$6 < 5; $$6++) {
            for (int $$7 = 0; $$7 < 4; $$7++) {
               int $$8 = 1;
               int $$9 = getRoomIndex($$6, 1, $$7);
               $$1[$$9] = new OceanMonumentPieces.RoomDefinition($$9);
            }
         }

         for (int $$10 = 1; $$10 < 4; $$10++) {
            for (int $$11 = 0; $$11 < 2; $$11++) {
               int $$12 = 2;
               int $$13 = getRoomIndex($$10, 2, $$11);
               $$1[$$13] = new OceanMonumentPieces.RoomDefinition($$13);
            }
         }

         this.sourceRoom = $$1[GRIDROOM_SOURCE_INDEX];

         for (int $$14 = 0; $$14 < 5; $$14++) {
            for (int $$15 = 0; $$15 < 5; $$15++) {
               for (int $$16 = 0; $$16 < 3; $$16++) {
                  int $$17 = getRoomIndex($$14, $$16, $$15);
                  if ($$1[$$17] != null) {
                     for (Direction $$18 : Direction.values()) {
                        int $$19 = $$14 + $$18.getStepX();
                        int $$20 = $$16 + $$18.getStepY();
                        int $$21 = $$15 + $$18.getStepZ();
                        if ($$19 >= 0 && $$19 < 5 && $$21 >= 0 && $$21 < 5 && $$20 >= 0 && $$20 < 3) {
                           int $$22 = getRoomIndex($$19, $$20, $$21);
                           if ($$1[$$22] != null) {
                              if ($$21 == $$15) {
                                 $$1[$$17].setConnection($$18, $$1[$$22]);
                              } else {
                                 $$1[$$17].setConnection($$18.getOpposite(), $$1[$$22]);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         OceanMonumentPieces.RoomDefinition $$23 = new OceanMonumentPieces.RoomDefinition(1003);
         OceanMonumentPieces.RoomDefinition $$24 = new OceanMonumentPieces.RoomDefinition(1001);
         OceanMonumentPieces.RoomDefinition $$25 = new OceanMonumentPieces.RoomDefinition(1002);
         $$1[GRIDROOM_TOP_CONNECT_INDEX].setConnection(Direction.UP, $$23);
         $$1[GRIDROOM_LEFTWING_CONNECT_INDEX].setConnection(Direction.SOUTH, $$24);
         $$1[GRIDROOM_RIGHTWING_CONNECT_INDEX].setConnection(Direction.SOUTH, $$25);
         $$23.claimed = true;
         $$24.claimed = true;
         $$25.claimed = true;
         this.sourceRoom.isSource = true;
         this.coreRoom = $$1[getRoomIndex(p_228673_.nextInt(4), 0, 2)];
         this.coreRoom.claimed = true;
         this.coreRoom.connections[Direction.EAST.get3DDataValue()].claimed = true;
         this.coreRoom.connections[Direction.NORTH.get3DDataValue()].claimed = true;
         this.coreRoom.connections[Direction.EAST.get3DDataValue()].connections[Direction.NORTH.get3DDataValue()].claimed = true;
         this.coreRoom.connections[Direction.UP.get3DDataValue()].claimed = true;
         this.coreRoom.connections[Direction.EAST.get3DDataValue()].connections[Direction.UP.get3DDataValue()].claimed = true;
         this.coreRoom.connections[Direction.NORTH.get3DDataValue()].connections[Direction.UP.get3DDataValue()].claimed = true;
         this.coreRoom.connections[Direction.EAST.get3DDataValue()].connections[Direction.NORTH.get3DDataValue()].connections[Direction.UP.get3DDataValue()].claimed = true;
         ObjectArrayList<OceanMonumentPieces.RoomDefinition> $$26 = new ObjectArrayList();

         for (OceanMonumentPieces.RoomDefinition $$27 : $$1) {
            if ($$27 != null) {
               $$27.updateOpenings();
               $$26.add($$27);
            }
         }

         $$23.updateOpenings();
         Util.shuffle($$26, p_228673_);
         int $$28 = 1;
         ObjectListIterator var34 = $$26.iterator();

         while (var34.hasNext()) {
            OceanMonumentPieces.RoomDefinition $$29 = (OceanMonumentPieces.RoomDefinition)var34.next();
            int $$30 = 0;
            int $$31 = 0;

            while ($$30 < 2 && $$31 < 5) {
               $$31++;
               int $$32 = p_228673_.nextInt(6);
               if ($$29.hasOpening[$$32]) {
                  int $$33 = Direction.from3DDataValue($$32).getOpposite().get3DDataValue();
                  $$29.hasOpening[$$32] = false;
                  $$29.connections[$$32].hasOpening[$$33] = false;
                  if ($$29.findSource($$28++) && $$29.connections[$$32].findSource($$28++)) {
                     $$30++;
                  } else {
                     $$29.hasOpening[$$32] = true;
                     $$29.connections[$$32].hasOpening[$$33] = true;
                  }
               }
            }
         }

         $$26.add($$23);
         $$26.add($$24);
         $$26.add($$25);
         return $$26;
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228659_,
         StructureManager p_228660_,
         ChunkGenerator p_228661_,
         RandomSource p_228662_,
         BoundingBox p_228663_,
         ChunkPos p_228664_,
         BlockPos p_228665_
      ) {
         int $$7 = Math.max(p_228659_.getSeaLevel(), 64) - this.boundingBox.minY();
         this.generateWaterBox(p_228659_, p_228663_, 0, 0, 0, 58, $$7, 58);
         this.generateWing(false, 0, p_228659_, p_228662_, p_228663_);
         this.generateWing(true, 33, p_228659_, p_228662_, p_228663_);
         this.generateEntranceArchs(p_228659_, p_228662_, p_228663_);
         this.generateEntranceWall(p_228659_, p_228662_, p_228663_);
         this.generateRoofPiece(p_228659_, p_228662_, p_228663_);
         this.generateLowerWall(p_228659_, p_228662_, p_228663_);
         this.generateMiddleWall(p_228659_, p_228662_, p_228663_);
         this.generateUpperWall(p_228659_, p_228662_, p_228663_);

         for (int $$8 = 0; $$8 < 7; $$8++) {
            int $$9 = 0;

            while ($$9 < 7) {
               if ($$9 == 0 && $$8 == 3) {
                  $$9 = 6;
               }

               int $$10 = $$8 * 9;
               int $$11 = $$9 * 9;

               for (int $$12 = 0; $$12 < 4; $$12++) {
                  for (int $$13 = 0; $$13 < 4; $$13++) {
                     this.placeBlock(p_228659_, BASE_LIGHT, $$10 + $$12, 0, $$11 + $$13, p_228663_);
                     this.fillColumnDown(p_228659_, BASE_LIGHT, $$10 + $$12, -1, $$11 + $$13, p_228663_);
                  }
               }

               if ($$8 != 0 && $$8 != 6) {
                  $$9 += 6;
               } else {
                  $$9++;
               }
            }
         }

         for (int $$14 = 0; $$14 < 5; $$14++) {
            this.generateWaterBox(p_228659_, p_228663_, -1 - $$14, 0 + $$14 * 2, -1 - $$14, -1 - $$14, 23, 58 + $$14);
            this.generateWaterBox(p_228659_, p_228663_, 58 + $$14, 0 + $$14 * 2, -1 - $$14, 58 + $$14, 23, 58 + $$14);
            this.generateWaterBox(p_228659_, p_228663_, 0 - $$14, 0 + $$14 * 2, -1 - $$14, 57 + $$14, 23, -1 - $$14);
            this.generateWaterBox(p_228659_, p_228663_, 0 - $$14, 0 + $$14 * 2, 58 + $$14, 57 + $$14, 23, 58 + $$14);
         }

         for (OceanMonumentPieces.OceanMonumentPiece $$15 : this.childPieces) {
            if ($$15.getBoundingBox().intersects(p_228663_)) {
               $$15.postProcess(p_228659_, p_228660_, p_228661_, p_228662_, p_228663_, p_228664_, p_228665_);
            }
         }
      }

      private void generateWing(boolean p_228667_, int p_228668_, WorldGenLevel p_228669_, RandomSource p_228670_, BoundingBox p_228671_) {
         int $$5 = 24;
         if (this.chunkIntersects(p_228671_, p_228668_, 0, p_228668_ + 23, 20)) {
            this.generateBox(p_228669_, p_228671_, p_228668_ + 0, 0, 0, p_228668_ + 24, 0, 20, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(p_228669_, p_228671_, p_228668_ + 0, 1, 0, p_228668_ + 24, 10, 20);

            for (int $$6 = 0; $$6 < 4; $$6++) {
               this.generateBox(p_228669_, p_228671_, p_228668_ + $$6, $$6 + 1, $$6, p_228668_ + $$6, $$6 + 1, 20, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228669_, p_228671_, p_228668_ + $$6 + 7, $$6 + 5, $$6 + 7, p_228668_ + $$6 + 7, $$6 + 5, 20, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228669_, p_228671_, p_228668_ + 17 - $$6, $$6 + 5, $$6 + 7, p_228668_ + 17 - $$6, $$6 + 5, 20, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228669_, p_228671_, p_228668_ + 24 - $$6, $$6 + 1, $$6, p_228668_ + 24 - $$6, $$6 + 1, 20, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228669_, p_228671_, p_228668_ + $$6 + 1, $$6 + 1, $$6, p_228668_ + 23 - $$6, $$6 + 1, $$6, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(
                  p_228669_, p_228671_, p_228668_ + $$6 + 8, $$6 + 5, $$6 + 7, p_228668_ + 16 - $$6, $$6 + 5, $$6 + 7, BASE_LIGHT, BASE_LIGHT, false
               );
            }

            this.generateBox(p_228669_, p_228671_, p_228668_ + 4, 4, 4, p_228668_ + 6, 4, 20, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228669_, p_228671_, p_228668_ + 7, 4, 4, p_228668_ + 17, 4, 6, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228669_, p_228671_, p_228668_ + 18, 4, 4, p_228668_ + 20, 4, 20, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228669_, p_228671_, p_228668_ + 11, 8, 11, p_228668_ + 13, 8, 20, BASE_GRAY, BASE_GRAY, false);
            this.placeBlock(p_228669_, DOT_DECO_DATA, p_228668_ + 12, 9, 12, p_228671_);
            this.placeBlock(p_228669_, DOT_DECO_DATA, p_228668_ + 12, 9, 15, p_228671_);
            this.placeBlock(p_228669_, DOT_DECO_DATA, p_228668_ + 12, 9, 18, p_228671_);
            int $$7 = p_228668_ + (p_228667_ ? 19 : 5);
            int $$8 = p_228668_ + (p_228667_ ? 5 : 19);

            for (int $$9 = 20; $$9 >= 5; $$9 -= 3) {
               this.placeBlock(p_228669_, DOT_DECO_DATA, $$7, 5, $$9, p_228671_);
            }

            for (int $$10 = 19; $$10 >= 7; $$10 -= 3) {
               this.placeBlock(p_228669_, DOT_DECO_DATA, $$8, 5, $$10, p_228671_);
            }

            for (int $$11 = 0; $$11 < 4; $$11++) {
               int $$12 = p_228667_ ? p_228668_ + 24 - (17 - $$11 * 3) : p_228668_ + 17 - $$11 * 3;
               this.placeBlock(p_228669_, DOT_DECO_DATA, $$12, 5, 5, p_228671_);
            }

            this.placeBlock(p_228669_, DOT_DECO_DATA, $$8, 5, 5, p_228671_);
            this.generateBox(p_228669_, p_228671_, p_228668_ + 11, 1, 12, p_228668_ + 13, 7, 12, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228669_, p_228671_, p_228668_ + 12, 1, 11, p_228668_ + 12, 7, 13, BASE_GRAY, BASE_GRAY, false);
         }
      }

      private void generateEntranceArchs(WorldGenLevel p_228655_, RandomSource p_228656_, BoundingBox p_228657_) {
         if (this.chunkIntersects(p_228657_, 22, 5, 35, 17)) {
            this.generateWaterBox(p_228655_, p_228657_, 25, 0, 0, 32, 8, 20);

            for (int $$3 = 0; $$3 < 4; $$3++) {
               this.generateBox(p_228655_, p_228657_, 24, 2, 5 + $$3 * 4, 24, 4, 5 + $$3 * 4, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228655_, p_228657_, 22, 4, 5 + $$3 * 4, 23, 4, 5 + $$3 * 4, BASE_LIGHT, BASE_LIGHT, false);
               this.placeBlock(p_228655_, BASE_LIGHT, 25, 5, 5 + $$3 * 4, p_228657_);
               this.placeBlock(p_228655_, BASE_LIGHT, 26, 6, 5 + $$3 * 4, p_228657_);
               this.placeBlock(p_228655_, LAMP_BLOCK, 26, 5, 5 + $$3 * 4, p_228657_);
               this.generateBox(p_228655_, p_228657_, 33, 2, 5 + $$3 * 4, 33, 4, 5 + $$3 * 4, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228655_, p_228657_, 34, 4, 5 + $$3 * 4, 35, 4, 5 + $$3 * 4, BASE_LIGHT, BASE_LIGHT, false);
               this.placeBlock(p_228655_, BASE_LIGHT, 32, 5, 5 + $$3 * 4, p_228657_);
               this.placeBlock(p_228655_, BASE_LIGHT, 31, 6, 5 + $$3 * 4, p_228657_);
               this.placeBlock(p_228655_, LAMP_BLOCK, 31, 5, 5 + $$3 * 4, p_228657_);
               this.generateBox(p_228655_, p_228657_, 27, 6, 5 + $$3 * 4, 30, 6, 5 + $$3 * 4, BASE_GRAY, BASE_GRAY, false);
            }
         }
      }

      private void generateEntranceWall(WorldGenLevel p_228675_, RandomSource p_228676_, BoundingBox p_228677_) {
         if (this.chunkIntersects(p_228677_, 15, 20, 42, 21)) {
            this.generateBox(p_228675_, p_228677_, 15, 0, 21, 42, 0, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(p_228675_, p_228677_, 26, 1, 21, 31, 3, 21);
            this.generateBox(p_228675_, p_228677_, 21, 12, 21, 36, 12, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228675_, p_228677_, 17, 11, 21, 40, 11, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228675_, p_228677_, 16, 10, 21, 41, 10, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228675_, p_228677_, 15, 7, 21, 42, 9, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228675_, p_228677_, 16, 6, 21, 41, 6, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228675_, p_228677_, 17, 5, 21, 40, 5, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228675_, p_228677_, 21, 4, 21, 36, 4, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228675_, p_228677_, 22, 3, 21, 26, 3, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228675_, p_228677_, 31, 3, 21, 35, 3, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228675_, p_228677_, 23, 2, 21, 25, 2, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228675_, p_228677_, 32, 2, 21, 34, 2, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228675_, p_228677_, 28, 4, 20, 29, 4, 21, BASE_LIGHT, BASE_LIGHT, false);
            this.placeBlock(p_228675_, BASE_LIGHT, 27, 3, 21, p_228677_);
            this.placeBlock(p_228675_, BASE_LIGHT, 30, 3, 21, p_228677_);
            this.placeBlock(p_228675_, BASE_LIGHT, 26, 2, 21, p_228677_);
            this.placeBlock(p_228675_, BASE_LIGHT, 31, 2, 21, p_228677_);
            this.placeBlock(p_228675_, BASE_LIGHT, 25, 1, 21, p_228677_);
            this.placeBlock(p_228675_, BASE_LIGHT, 32, 1, 21, p_228677_);

            for (int $$3 = 0; $$3 < 7; $$3++) {
               this.placeBlock(p_228675_, BASE_BLACK, 28 - $$3, 6 + $$3, 21, p_228677_);
               this.placeBlock(p_228675_, BASE_BLACK, 29 + $$3, 6 + $$3, 21, p_228677_);
            }

            for (int $$4 = 0; $$4 < 4; $$4++) {
               this.placeBlock(p_228675_, BASE_BLACK, 28 - $$4, 9 + $$4, 21, p_228677_);
               this.placeBlock(p_228675_, BASE_BLACK, 29 + $$4, 9 + $$4, 21, p_228677_);
            }

            this.placeBlock(p_228675_, BASE_BLACK, 28, 12, 21, p_228677_);
            this.placeBlock(p_228675_, BASE_BLACK, 29, 12, 21, p_228677_);

            for (int $$5 = 0; $$5 < 3; $$5++) {
               this.placeBlock(p_228675_, BASE_BLACK, 22 - $$5 * 2, 8, 21, p_228677_);
               this.placeBlock(p_228675_, BASE_BLACK, 22 - $$5 * 2, 9, 21, p_228677_);
               this.placeBlock(p_228675_, BASE_BLACK, 35 + $$5 * 2, 8, 21, p_228677_);
               this.placeBlock(p_228675_, BASE_BLACK, 35 + $$5 * 2, 9, 21, p_228677_);
            }

            this.generateWaterBox(p_228675_, p_228677_, 15, 13, 21, 42, 15, 21);
            this.generateWaterBox(p_228675_, p_228677_, 15, 1, 21, 15, 6, 21);
            this.generateWaterBox(p_228675_, p_228677_, 16, 1, 21, 16, 5, 21);
            this.generateWaterBox(p_228675_, p_228677_, 17, 1, 21, 20, 4, 21);
            this.generateWaterBox(p_228675_, p_228677_, 21, 1, 21, 21, 3, 21);
            this.generateWaterBox(p_228675_, p_228677_, 22, 1, 21, 22, 2, 21);
            this.generateWaterBox(p_228675_, p_228677_, 23, 1, 21, 24, 1, 21);
            this.generateWaterBox(p_228675_, p_228677_, 42, 1, 21, 42, 6, 21);
            this.generateWaterBox(p_228675_, p_228677_, 41, 1, 21, 41, 5, 21);
            this.generateWaterBox(p_228675_, p_228677_, 37, 1, 21, 40, 4, 21);
            this.generateWaterBox(p_228675_, p_228677_, 36, 1, 21, 36, 3, 21);
            this.generateWaterBox(p_228675_, p_228677_, 33, 1, 21, 34, 1, 21);
            this.generateWaterBox(p_228675_, p_228677_, 35, 1, 21, 35, 2, 21);
         }
      }

      private void generateRoofPiece(WorldGenLevel p_228679_, RandomSource p_228680_, BoundingBox p_228681_) {
         if (this.chunkIntersects(p_228681_, 21, 21, 36, 36)) {
            this.generateBox(p_228679_, p_228681_, 21, 0, 22, 36, 0, 36, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(p_228679_, p_228681_, 21, 1, 22, 36, 23, 36);

            for (int $$3 = 0; $$3 < 4; $$3++) {
               this.generateBox(p_228679_, p_228681_, 21 + $$3, 13 + $$3, 21 + $$3, 36 - $$3, 13 + $$3, 21 + $$3, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228679_, p_228681_, 21 + $$3, 13 + $$3, 36 - $$3, 36 - $$3, 13 + $$3, 36 - $$3, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228679_, p_228681_, 21 + $$3, 13 + $$3, 22 + $$3, 21 + $$3, 13 + $$3, 35 - $$3, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228679_, p_228681_, 36 - $$3, 13 + $$3, 22 + $$3, 36 - $$3, 13 + $$3, 35 - $$3, BASE_LIGHT, BASE_LIGHT, false);
            }

            this.generateBox(p_228679_, p_228681_, 25, 16, 25, 32, 16, 32, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228679_, p_228681_, 25, 17, 25, 25, 19, 25, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228679_, p_228681_, 32, 17, 25, 32, 19, 25, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228679_, p_228681_, 25, 17, 32, 25, 19, 32, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228679_, p_228681_, 32, 17, 32, 32, 19, 32, BASE_LIGHT, BASE_LIGHT, false);
            this.placeBlock(p_228679_, BASE_LIGHT, 26, 20, 26, p_228681_);
            this.placeBlock(p_228679_, BASE_LIGHT, 27, 21, 27, p_228681_);
            this.placeBlock(p_228679_, LAMP_BLOCK, 27, 20, 27, p_228681_);
            this.placeBlock(p_228679_, BASE_LIGHT, 26, 20, 31, p_228681_);
            this.placeBlock(p_228679_, BASE_LIGHT, 27, 21, 30, p_228681_);
            this.placeBlock(p_228679_, LAMP_BLOCK, 27, 20, 30, p_228681_);
            this.placeBlock(p_228679_, BASE_LIGHT, 31, 20, 31, p_228681_);
            this.placeBlock(p_228679_, BASE_LIGHT, 30, 21, 30, p_228681_);
            this.placeBlock(p_228679_, LAMP_BLOCK, 30, 20, 30, p_228681_);
            this.placeBlock(p_228679_, BASE_LIGHT, 31, 20, 26, p_228681_);
            this.placeBlock(p_228679_, BASE_LIGHT, 30, 21, 27, p_228681_);
            this.placeBlock(p_228679_, LAMP_BLOCK, 30, 20, 27, p_228681_);
            this.generateBox(p_228679_, p_228681_, 28, 21, 27, 29, 21, 27, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228679_, p_228681_, 27, 21, 28, 27, 21, 29, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228679_, p_228681_, 28, 21, 30, 29, 21, 30, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228679_, p_228681_, 30, 21, 28, 30, 21, 29, BASE_GRAY, BASE_GRAY, false);
         }
      }

      private void generateLowerWall(WorldGenLevel p_228683_, RandomSource p_228684_, BoundingBox p_228685_) {
         if (this.chunkIntersects(p_228685_, 0, 21, 6, 58)) {
            this.generateBox(p_228683_, p_228685_, 0, 0, 21, 6, 0, 57, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(p_228683_, p_228685_, 0, 1, 21, 6, 7, 57);
            this.generateBox(p_228683_, p_228685_, 4, 4, 21, 6, 4, 53, BASE_GRAY, BASE_GRAY, false);

            for (int $$3 = 0; $$3 < 4; $$3++) {
               this.generateBox(p_228683_, p_228685_, $$3, $$3 + 1, 21, $$3, $$3 + 1, 57 - $$3, BASE_LIGHT, BASE_LIGHT, false);
            }

            for (int $$4 = 23; $$4 < 53; $$4 += 3) {
               this.placeBlock(p_228683_, DOT_DECO_DATA, 5, 5, $$4, p_228685_);
            }

            this.placeBlock(p_228683_, DOT_DECO_DATA, 5, 5, 52, p_228685_);

            for (int $$5 = 0; $$5 < 4; $$5++) {
               this.generateBox(p_228683_, p_228685_, $$5, $$5 + 1, 21, $$5, $$5 + 1, 57 - $$5, BASE_LIGHT, BASE_LIGHT, false);
            }

            this.generateBox(p_228683_, p_228685_, 4, 1, 52, 6, 3, 52, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228683_, p_228685_, 5, 1, 51, 5, 3, 53, BASE_GRAY, BASE_GRAY, false);
         }

         if (this.chunkIntersects(p_228685_, 51, 21, 58, 58)) {
            this.generateBox(p_228683_, p_228685_, 51, 0, 21, 57, 0, 57, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(p_228683_, p_228685_, 51, 1, 21, 57, 7, 57);
            this.generateBox(p_228683_, p_228685_, 51, 4, 21, 53, 4, 53, BASE_GRAY, BASE_GRAY, false);

            for (int $$6 = 0; $$6 < 4; $$6++) {
               this.generateBox(p_228683_, p_228685_, 57 - $$6, $$6 + 1, 21, 57 - $$6, $$6 + 1, 57 - $$6, BASE_LIGHT, BASE_LIGHT, false);
            }

            for (int $$7 = 23; $$7 < 53; $$7 += 3) {
               this.placeBlock(p_228683_, DOT_DECO_DATA, 52, 5, $$7, p_228685_);
            }

            this.placeBlock(p_228683_, DOT_DECO_DATA, 52, 5, 52, p_228685_);
            this.generateBox(p_228683_, p_228685_, 51, 1, 52, 53, 3, 52, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228683_, p_228685_, 52, 1, 51, 52, 3, 53, BASE_GRAY, BASE_GRAY, false);
         }

         if (this.chunkIntersects(p_228685_, 0, 51, 57, 57)) {
            this.generateBox(p_228683_, p_228685_, 7, 0, 51, 50, 0, 57, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(p_228683_, p_228685_, 7, 1, 51, 50, 10, 57);

            for (int $$8 = 0; $$8 < 4; $$8++) {
               this.generateBox(p_228683_, p_228685_, $$8 + 1, $$8 + 1, 57 - $$8, 56 - $$8, $$8 + 1, 57 - $$8, BASE_LIGHT, BASE_LIGHT, false);
            }
         }
      }

      private void generateMiddleWall(WorldGenLevel p_228687_, RandomSource p_228688_, BoundingBox p_228689_) {
         if (this.chunkIntersects(p_228689_, 7, 21, 13, 50)) {
            this.generateBox(p_228687_, p_228689_, 7, 0, 21, 13, 0, 50, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(p_228687_, p_228689_, 7, 1, 21, 13, 10, 50);
            this.generateBox(p_228687_, p_228689_, 11, 8, 21, 13, 8, 53, BASE_GRAY, BASE_GRAY, false);

            for (int $$3 = 0; $$3 < 4; $$3++) {
               this.generateBox(p_228687_, p_228689_, $$3 + 7, $$3 + 5, 21, $$3 + 7, $$3 + 5, 54, BASE_LIGHT, BASE_LIGHT, false);
            }

            for (int $$4 = 21; $$4 <= 45; $$4 += 3) {
               this.placeBlock(p_228687_, DOT_DECO_DATA, 12, 9, $$4, p_228689_);
            }
         }

         if (this.chunkIntersects(p_228689_, 44, 21, 50, 54)) {
            this.generateBox(p_228687_, p_228689_, 44, 0, 21, 50, 0, 50, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(p_228687_, p_228689_, 44, 1, 21, 50, 10, 50);
            this.generateBox(p_228687_, p_228689_, 44, 8, 21, 46, 8, 53, BASE_GRAY, BASE_GRAY, false);

            for (int $$5 = 0; $$5 < 4; $$5++) {
               this.generateBox(p_228687_, p_228689_, 50 - $$5, $$5 + 5, 21, 50 - $$5, $$5 + 5, 54, BASE_LIGHT, BASE_LIGHT, false);
            }

            for (int $$6 = 21; $$6 <= 45; $$6 += 3) {
               this.placeBlock(p_228687_, DOT_DECO_DATA, 45, 9, $$6, p_228689_);
            }
         }

         if (this.chunkIntersects(p_228689_, 8, 44, 49, 54)) {
            this.generateBox(p_228687_, p_228689_, 14, 0, 44, 43, 0, 50, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(p_228687_, p_228689_, 14, 1, 44, 43, 10, 50);

            for (int $$7 = 12; $$7 <= 45; $$7 += 3) {
               this.placeBlock(p_228687_, DOT_DECO_DATA, $$7, 9, 45, p_228689_);
               this.placeBlock(p_228687_, DOT_DECO_DATA, $$7, 9, 52, p_228689_);
               if ($$7 == 12 || $$7 == 18 || $$7 == 24 || $$7 == 33 || $$7 == 39 || $$7 == 45) {
                  this.placeBlock(p_228687_, DOT_DECO_DATA, $$7, 9, 47, p_228689_);
                  this.placeBlock(p_228687_, DOT_DECO_DATA, $$7, 9, 50, p_228689_);
                  this.placeBlock(p_228687_, DOT_DECO_DATA, $$7, 10, 45, p_228689_);
                  this.placeBlock(p_228687_, DOT_DECO_DATA, $$7, 10, 46, p_228689_);
                  this.placeBlock(p_228687_, DOT_DECO_DATA, $$7, 10, 51, p_228689_);
                  this.placeBlock(p_228687_, DOT_DECO_DATA, $$7, 10, 52, p_228689_);
                  this.placeBlock(p_228687_, DOT_DECO_DATA, $$7, 11, 47, p_228689_);
                  this.placeBlock(p_228687_, DOT_DECO_DATA, $$7, 11, 50, p_228689_);
                  this.placeBlock(p_228687_, DOT_DECO_DATA, $$7, 12, 48, p_228689_);
                  this.placeBlock(p_228687_, DOT_DECO_DATA, $$7, 12, 49, p_228689_);
               }
            }

            for (int $$8 = 0; $$8 < 3; $$8++) {
               this.generateBox(p_228687_, p_228689_, 8 + $$8, 5 + $$8, 54, 49 - $$8, 5 + $$8, 54, BASE_GRAY, BASE_GRAY, false);
            }

            this.generateBox(p_228687_, p_228689_, 11, 8, 54, 46, 8, 54, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228687_, p_228689_, 14, 8, 44, 43, 8, 53, BASE_GRAY, BASE_GRAY, false);
         }
      }

      private void generateUpperWall(WorldGenLevel p_228691_, RandomSource p_228692_, BoundingBox p_228693_) {
         if (this.chunkIntersects(p_228693_, 14, 21, 20, 43)) {
            this.generateBox(p_228691_, p_228693_, 14, 0, 21, 20, 0, 43, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(p_228691_, p_228693_, 14, 1, 22, 20, 14, 43);
            this.generateBox(p_228691_, p_228693_, 18, 12, 22, 20, 12, 39, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228691_, p_228693_, 18, 12, 21, 20, 12, 21, BASE_LIGHT, BASE_LIGHT, false);

            for (int $$3 = 0; $$3 < 4; $$3++) {
               this.generateBox(p_228691_, p_228693_, $$3 + 14, $$3 + 9, 21, $$3 + 14, $$3 + 9, 43 - $$3, BASE_LIGHT, BASE_LIGHT, false);
            }

            for (int $$4 = 23; $$4 <= 39; $$4 += 3) {
               this.placeBlock(p_228691_, DOT_DECO_DATA, 19, 13, $$4, p_228693_);
            }
         }

         if (this.chunkIntersects(p_228693_, 37, 21, 43, 43)) {
            this.generateBox(p_228691_, p_228693_, 37, 0, 21, 43, 0, 43, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(p_228691_, p_228693_, 37, 1, 22, 43, 14, 43);
            this.generateBox(p_228691_, p_228693_, 37, 12, 22, 39, 12, 39, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228691_, p_228693_, 37, 12, 21, 39, 12, 21, BASE_LIGHT, BASE_LIGHT, false);

            for (int $$5 = 0; $$5 < 4; $$5++) {
               this.generateBox(p_228691_, p_228693_, 43 - $$5, $$5 + 9, 21, 43 - $$5, $$5 + 9, 43 - $$5, BASE_LIGHT, BASE_LIGHT, false);
            }

            for (int $$6 = 23; $$6 <= 39; $$6 += 3) {
               this.placeBlock(p_228691_, DOT_DECO_DATA, 38, 13, $$6, p_228693_);
            }
         }

         if (this.chunkIntersects(p_228693_, 15, 37, 42, 43)) {
            this.generateBox(p_228691_, p_228693_, 21, 0, 37, 36, 0, 43, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(p_228691_, p_228693_, 21, 1, 37, 36, 14, 43);
            this.generateBox(p_228691_, p_228693_, 21, 12, 37, 36, 12, 39, BASE_GRAY, BASE_GRAY, false);

            for (int $$7 = 0; $$7 < 4; $$7++) {
               this.generateBox(p_228691_, p_228693_, 15 + $$7, $$7 + 9, 43 - $$7, 42 - $$7, $$7 + 9, 43 - $$7, BASE_LIGHT, BASE_LIGHT, false);
            }

            for (int $$8 = 21; $$8 <= 36; $$8 += 3) {
               this.placeBlock(p_228691_, DOT_DECO_DATA, $$8, 13, 38, p_228693_);
            }
         }
      }
   }

   interface MonumentRoomFitter {
      boolean fits(OceanMonumentPieces.RoomDefinition var1);

      OceanMonumentPieces.OceanMonumentPiece create(Direction var1, OceanMonumentPieces.RoomDefinition var2, RandomSource var3);
   }

   public static class OceanMonumentCoreRoom extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentCoreRoom(Direction p_228699_, OceanMonumentPieces.RoomDefinition p_228700_) {
         super(StructurePieceType.OCEAN_MONUMENT_CORE_ROOM, 1, p_228699_, p_228700_, 2, 2, 2);
      }

      public OceanMonumentCoreRoom(CompoundTag p_228702_) {
         super(StructurePieceType.OCEAN_MONUMENT_CORE_ROOM, p_228702_);
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228704_,
         StructureManager p_228705_,
         ChunkGenerator p_228706_,
         RandomSource p_228707_,
         BoundingBox p_228708_,
         ChunkPos p_228709_,
         BlockPos p_228710_
      ) {
         this.generateBoxOnFillOnly(p_228704_, p_228708_, 1, 8, 0, 14, 8, 14, BASE_GRAY);
         int $$7 = 7;
         BlockState $$8 = BASE_LIGHT;
         this.generateBox(p_228704_, p_228708_, 0, 7, 0, 0, 7, 15, $$8, $$8, false);
         this.generateBox(p_228704_, p_228708_, 15, 7, 0, 15, 7, 15, $$8, $$8, false);
         this.generateBox(p_228704_, p_228708_, 1, 7, 0, 15, 7, 0, $$8, $$8, false);
         this.generateBox(p_228704_, p_228708_, 1, 7, 15, 14, 7, 15, $$8, $$8, false);

         for (int $$9 = 1; $$9 <= 6; $$9++) {
            $$8 = BASE_LIGHT;
            if ($$9 == 2 || $$9 == 6) {
               $$8 = BASE_GRAY;
            }

            for (int $$11 = 0; $$11 <= 15; $$11 += 15) {
               this.generateBox(p_228704_, p_228708_, $$11, $$9, 0, $$11, $$9, 1, $$8, $$8, false);
               this.generateBox(p_228704_, p_228708_, $$11, $$9, 6, $$11, $$9, 9, $$8, $$8, false);
               this.generateBox(p_228704_, p_228708_, $$11, $$9, 14, $$11, $$9, 15, $$8, $$8, false);
            }

            this.generateBox(p_228704_, p_228708_, 1, $$9, 0, 1, $$9, 0, $$8, $$8, false);
            this.generateBox(p_228704_, p_228708_, 6, $$9, 0, 9, $$9, 0, $$8, $$8, false);
            this.generateBox(p_228704_, p_228708_, 14, $$9, 0, 14, $$9, 0, $$8, $$8, false);
            this.generateBox(p_228704_, p_228708_, 1, $$9, 15, 14, $$9, 15, $$8, $$8, false);
         }

         this.generateBox(p_228704_, p_228708_, 6, 3, 6, 9, 6, 9, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(p_228704_, p_228708_, 7, 4, 7, 8, 5, 8, Blocks.GOLD_BLOCK.defaultBlockState(), Blocks.GOLD_BLOCK.defaultBlockState(), false);

         for (int $$12 = 3; $$12 <= 6; $$12 += 3) {
            for (int $$13 = 6; $$13 <= 9; $$13 += 3) {
               this.placeBlock(p_228704_, LAMP_BLOCK, $$13, $$12, 6, p_228708_);
               this.placeBlock(p_228704_, LAMP_BLOCK, $$13, $$12, 9, p_228708_);
            }
         }

         this.generateBox(p_228704_, p_228708_, 5, 1, 6, 5, 2, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 5, 1, 9, 5, 2, 9, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 10, 1, 6, 10, 2, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 10, 1, 9, 10, 2, 9, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 6, 1, 5, 6, 2, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 9, 1, 5, 9, 2, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 6, 1, 10, 6, 2, 10, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 9, 1, 10, 9, 2, 10, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 5, 2, 5, 5, 6, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 5, 2, 10, 5, 6, 10, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 10, 2, 5, 10, 6, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 10, 2, 10, 10, 6, 10, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 5, 7, 1, 5, 7, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 10, 7, 1, 10, 7, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 5, 7, 9, 5, 7, 14, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 10, 7, 9, 10, 7, 14, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 1, 7, 5, 6, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 1, 7, 10, 6, 7, 10, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 9, 7, 5, 14, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 9, 7, 10, 14, 7, 10, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 2, 1, 2, 2, 1, 3, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 3, 1, 2, 3, 1, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 13, 1, 2, 13, 1, 3, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 12, 1, 2, 12, 1, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 2, 1, 12, 2, 1, 13, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 3, 1, 13, 3, 1, 13, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 13, 1, 12, 13, 1, 13, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 12, 1, 13, 12, 1, 13, BASE_LIGHT, BASE_LIGHT, false);
      }
   }

   public static class OceanMonumentDoubleXRoom extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentDoubleXRoom(Direction p_228712_, OceanMonumentPieces.RoomDefinition p_228713_) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_X_ROOM, 1, p_228712_, p_228713_, 2, 1, 1);
      }

      public OceanMonumentDoubleXRoom(CompoundTag p_228715_) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_X_ROOM, p_228715_);
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228717_,
         StructureManager p_228718_,
         ChunkGenerator p_228719_,
         RandomSource p_228720_,
         BoundingBox p_228721_,
         ChunkPos p_228722_,
         BlockPos p_228723_
      ) {
         OceanMonumentPieces.RoomDefinition $$7 = this.roomDefinition.connections[Direction.EAST.get3DDataValue()];
         OceanMonumentPieces.RoomDefinition $$8 = this.roomDefinition;
         if (this.roomDefinition.index / 25 > 0) {
            this.generateDefaultFloor(p_228717_, p_228721_, 8, 0, $$7.hasOpening[Direction.DOWN.get3DDataValue()]);
            this.generateDefaultFloor(p_228717_, p_228721_, 0, 0, $$8.hasOpening[Direction.DOWN.get3DDataValue()]);
         }

         if ($$8.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(p_228717_, p_228721_, 1, 4, 1, 7, 4, 6, BASE_GRAY);
         }

         if ($$7.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(p_228717_, p_228721_, 8, 4, 1, 14, 4, 6, BASE_GRAY);
         }

         this.generateBox(p_228717_, p_228721_, 0, 3, 0, 0, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228717_, p_228721_, 15, 3, 0, 15, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228717_, p_228721_, 1, 3, 0, 15, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228717_, p_228721_, 1, 3, 7, 14, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228717_, p_228721_, 0, 2, 0, 0, 2, 7, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(p_228717_, p_228721_, 15, 2, 0, 15, 2, 7, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(p_228717_, p_228721_, 1, 2, 0, 15, 2, 0, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(p_228717_, p_228721_, 1, 2, 7, 14, 2, 7, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(p_228717_, p_228721_, 0, 1, 0, 0, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228717_, p_228721_, 15, 1, 0, 15, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228717_, p_228721_, 1, 1, 0, 15, 1, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228717_, p_228721_, 1, 1, 7, 14, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228717_, p_228721_, 5, 1, 0, 10, 1, 4, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228717_, p_228721_, 6, 2, 0, 9, 2, 3, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(p_228717_, p_228721_, 5, 3, 0, 10, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
         this.placeBlock(p_228717_, LAMP_BLOCK, 6, 2, 3, p_228721_);
         this.placeBlock(p_228717_, LAMP_BLOCK, 9, 2, 3, p_228721_);
         if ($$8.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(p_228717_, p_228721_, 3, 1, 0, 4, 2, 0);
         }

         if ($$8.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(p_228717_, p_228721_, 3, 1, 7, 4, 2, 7);
         }

         if ($$8.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(p_228717_, p_228721_, 0, 1, 3, 0, 2, 4);
         }

         if ($$7.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(p_228717_, p_228721_, 11, 1, 0, 12, 2, 0);
         }

         if ($$7.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(p_228717_, p_228721_, 11, 1, 7, 12, 2, 7);
         }

         if ($$7.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(p_228717_, p_228721_, 15, 1, 3, 15, 2, 4);
         }
      }
   }

   public static class OceanMonumentDoubleXYRoom extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentDoubleXYRoom(Direction p_228725_, OceanMonumentPieces.RoomDefinition p_228726_) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_XY_ROOM, 1, p_228725_, p_228726_, 2, 2, 1);
      }

      public OceanMonumentDoubleXYRoom(CompoundTag p_228728_) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_XY_ROOM, p_228728_);
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228730_,
         StructureManager p_228731_,
         ChunkGenerator p_228732_,
         RandomSource p_228733_,
         BoundingBox p_228734_,
         ChunkPos p_228735_,
         BlockPos p_228736_
      ) {
         OceanMonumentPieces.RoomDefinition $$7 = this.roomDefinition.connections[Direction.EAST.get3DDataValue()];
         OceanMonumentPieces.RoomDefinition $$8 = this.roomDefinition;
         OceanMonumentPieces.RoomDefinition $$9 = $$8.connections[Direction.UP.get3DDataValue()];
         OceanMonumentPieces.RoomDefinition $$10 = $$7.connections[Direction.UP.get3DDataValue()];
         if (this.roomDefinition.index / 25 > 0) {
            this.generateDefaultFloor(p_228730_, p_228734_, 8, 0, $$7.hasOpening[Direction.DOWN.get3DDataValue()]);
            this.generateDefaultFloor(p_228730_, p_228734_, 0, 0, $$8.hasOpening[Direction.DOWN.get3DDataValue()]);
         }

         if ($$9.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(p_228730_, p_228734_, 1, 8, 1, 7, 8, 6, BASE_GRAY);
         }

         if ($$10.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(p_228730_, p_228734_, 8, 8, 1, 14, 8, 6, BASE_GRAY);
         }

         for (int $$11 = 1; $$11 <= 7; $$11++) {
            BlockState $$12 = BASE_LIGHT;
            if ($$11 == 2 || $$11 == 6) {
               $$12 = BASE_GRAY;
            }

            this.generateBox(p_228730_, p_228734_, 0, $$11, 0, 0, $$11, 7, $$12, $$12, false);
            this.generateBox(p_228730_, p_228734_, 15, $$11, 0, 15, $$11, 7, $$12, $$12, false);
            this.generateBox(p_228730_, p_228734_, 1, $$11, 0, 15, $$11, 0, $$12, $$12, false);
            this.generateBox(p_228730_, p_228734_, 1, $$11, 7, 14, $$11, 7, $$12, $$12, false);
         }

         this.generateBox(p_228730_, p_228734_, 2, 1, 3, 2, 7, 4, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228730_, p_228734_, 3, 1, 2, 4, 7, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228730_, p_228734_, 3, 1, 5, 4, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228730_, p_228734_, 13, 1, 3, 13, 7, 4, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228730_, p_228734_, 11, 1, 2, 12, 7, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228730_, p_228734_, 11, 1, 5, 12, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228730_, p_228734_, 5, 1, 3, 5, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228730_, p_228734_, 10, 1, 3, 10, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228730_, p_228734_, 5, 7, 2, 10, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228730_, p_228734_, 5, 5, 2, 5, 7, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228730_, p_228734_, 10, 5, 2, 10, 7, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228730_, p_228734_, 5, 5, 5, 5, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228730_, p_228734_, 10, 5, 5, 10, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.placeBlock(p_228730_, BASE_LIGHT, 6, 6, 2, p_228734_);
         this.placeBlock(p_228730_, BASE_LIGHT, 9, 6, 2, p_228734_);
         this.placeBlock(p_228730_, BASE_LIGHT, 6, 6, 5, p_228734_);
         this.placeBlock(p_228730_, BASE_LIGHT, 9, 6, 5, p_228734_);
         this.generateBox(p_228730_, p_228734_, 5, 4, 3, 6, 4, 4, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228730_, p_228734_, 9, 4, 3, 10, 4, 4, BASE_LIGHT, BASE_LIGHT, false);
         this.placeBlock(p_228730_, LAMP_BLOCK, 5, 4, 2, p_228734_);
         this.placeBlock(p_228730_, LAMP_BLOCK, 5, 4, 5, p_228734_);
         this.placeBlock(p_228730_, LAMP_BLOCK, 10, 4, 2, p_228734_);
         this.placeBlock(p_228730_, LAMP_BLOCK, 10, 4, 5, p_228734_);
         if ($$8.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(p_228730_, p_228734_, 3, 1, 0, 4, 2, 0);
         }

         if ($$8.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(p_228730_, p_228734_, 3, 1, 7, 4, 2, 7);
         }

         if ($$8.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(p_228730_, p_228734_, 0, 1, 3, 0, 2, 4);
         }

         if ($$7.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(p_228730_, p_228734_, 11, 1, 0, 12, 2, 0);
         }

         if ($$7.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(p_228730_, p_228734_, 11, 1, 7, 12, 2, 7);
         }

         if ($$7.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(p_228730_, p_228734_, 15, 1, 3, 15, 2, 4);
         }

         if ($$9.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(p_228730_, p_228734_, 3, 5, 0, 4, 6, 0);
         }

         if ($$9.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(p_228730_, p_228734_, 3, 5, 7, 4, 6, 7);
         }

         if ($$9.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(p_228730_, p_228734_, 0, 5, 3, 0, 6, 4);
         }

         if ($$10.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(p_228730_, p_228734_, 11, 5, 0, 12, 6, 0);
         }

         if ($$10.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(p_228730_, p_228734_, 11, 5, 7, 12, 6, 7);
         }

         if ($$10.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(p_228730_, p_228734_, 15, 5, 3, 15, 6, 4);
         }
      }
   }

   public static class OceanMonumentDoubleYRoom extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentDoubleYRoom(Direction p_228738_, OceanMonumentPieces.RoomDefinition p_228739_) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_Y_ROOM, 1, p_228738_, p_228739_, 1, 2, 1);
      }

      public OceanMonumentDoubleYRoom(CompoundTag p_228741_) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_Y_ROOM, p_228741_);
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228743_,
         StructureManager p_228744_,
         ChunkGenerator p_228745_,
         RandomSource p_228746_,
         BoundingBox p_228747_,
         ChunkPos p_228748_,
         BlockPos p_228749_
      ) {
         if (this.roomDefinition.index / 25 > 0) {
            this.generateDefaultFloor(p_228743_, p_228747_, 0, 0, this.roomDefinition.hasOpening[Direction.DOWN.get3DDataValue()]);
         }

         OceanMonumentPieces.RoomDefinition $$7 = this.roomDefinition.connections[Direction.UP.get3DDataValue()];
         if ($$7.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(p_228743_, p_228747_, 1, 8, 1, 6, 8, 6, BASE_GRAY);
         }

         this.generateBox(p_228743_, p_228747_, 0, 4, 0, 0, 4, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228743_, p_228747_, 7, 4, 0, 7, 4, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228743_, p_228747_, 1, 4, 0, 6, 4, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228743_, p_228747_, 1, 4, 7, 6, 4, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228743_, p_228747_, 2, 4, 1, 2, 4, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228743_, p_228747_, 1, 4, 2, 1, 4, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228743_, p_228747_, 5, 4, 1, 5, 4, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228743_, p_228747_, 6, 4, 2, 6, 4, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228743_, p_228747_, 2, 4, 5, 2, 4, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228743_, p_228747_, 1, 4, 5, 1, 4, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228743_, p_228747_, 5, 4, 5, 5, 4, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228743_, p_228747_, 6, 4, 5, 6, 4, 5, BASE_LIGHT, BASE_LIGHT, false);
         OceanMonumentPieces.RoomDefinition $$8 = this.roomDefinition;

         for (int $$9 = 1; $$9 <= 5; $$9 += 4) {
            int $$10 = 0;
            if ($$8.hasOpening[Direction.SOUTH.get3DDataValue()]) {
               this.generateBox(p_228743_, p_228747_, 2, $$9, $$10, 2, $$9 + 2, $$10, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228743_, p_228747_, 5, $$9, $$10, 5, $$9 + 2, $$10, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228743_, p_228747_, 3, $$9 + 2, $$10, 4, $$9 + 2, $$10, BASE_LIGHT, BASE_LIGHT, false);
            } else {
               this.generateBox(p_228743_, p_228747_, 0, $$9, $$10, 7, $$9 + 2, $$10, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228743_, p_228747_, 0, $$9 + 1, $$10, 7, $$9 + 1, $$10, BASE_GRAY, BASE_GRAY, false);
            }

            int var13 = 7;
            if ($$8.hasOpening[Direction.NORTH.get3DDataValue()]) {
               this.generateBox(p_228743_, p_228747_, 2, $$9, var13, 2, $$9 + 2, var13, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228743_, p_228747_, 5, $$9, var13, 5, $$9 + 2, var13, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228743_, p_228747_, 3, $$9 + 2, var13, 4, $$9 + 2, var13, BASE_LIGHT, BASE_LIGHT, false);
            } else {
               this.generateBox(p_228743_, p_228747_, 0, $$9, var13, 7, $$9 + 2, var13, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228743_, p_228747_, 0, $$9 + 1, var13, 7, $$9 + 1, var13, BASE_GRAY, BASE_GRAY, false);
            }

            int $$11 = 0;
            if ($$8.hasOpening[Direction.WEST.get3DDataValue()]) {
               this.generateBox(p_228743_, p_228747_, $$11, $$9, 2, $$11, $$9 + 2, 2, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228743_, p_228747_, $$11, $$9, 5, $$11, $$9 + 2, 5, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228743_, p_228747_, $$11, $$9 + 2, 3, $$11, $$9 + 2, 4, BASE_LIGHT, BASE_LIGHT, false);
            } else {
               this.generateBox(p_228743_, p_228747_, $$11, $$9, 0, $$11, $$9 + 2, 7, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228743_, p_228747_, $$11, $$9 + 1, 0, $$11, $$9 + 1, 7, BASE_GRAY, BASE_GRAY, false);
            }

            int var14 = 7;
            if ($$8.hasOpening[Direction.EAST.get3DDataValue()]) {
               this.generateBox(p_228743_, p_228747_, var14, $$9, 2, var14, $$9 + 2, 2, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228743_, p_228747_, var14, $$9, 5, var14, $$9 + 2, 5, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228743_, p_228747_, var14, $$9 + 2, 3, var14, $$9 + 2, 4, BASE_LIGHT, BASE_LIGHT, false);
            } else {
               this.generateBox(p_228743_, p_228747_, var14, $$9, 0, var14, $$9 + 2, 7, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228743_, p_228747_, var14, $$9 + 1, 0, var14, $$9 + 1, 7, BASE_GRAY, BASE_GRAY, false);
            }

            $$8 = $$7;
         }
      }
   }

   public static class OceanMonumentDoubleYZRoom extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentDoubleYZRoom(Direction p_228751_, OceanMonumentPieces.RoomDefinition p_228752_) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_YZ_ROOM, 1, p_228751_, p_228752_, 1, 2, 2);
      }

      public OceanMonumentDoubleYZRoom(CompoundTag p_228754_) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_YZ_ROOM, p_228754_);
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228756_,
         StructureManager p_228757_,
         ChunkGenerator p_228758_,
         RandomSource p_228759_,
         BoundingBox p_228760_,
         ChunkPos p_228761_,
         BlockPos p_228762_
      ) {
         OceanMonumentPieces.RoomDefinition $$7 = this.roomDefinition.connections[Direction.NORTH.get3DDataValue()];
         OceanMonumentPieces.RoomDefinition $$8 = this.roomDefinition;
         OceanMonumentPieces.RoomDefinition $$9 = $$7.connections[Direction.UP.get3DDataValue()];
         OceanMonumentPieces.RoomDefinition $$10 = $$8.connections[Direction.UP.get3DDataValue()];
         if (this.roomDefinition.index / 25 > 0) {
            this.generateDefaultFloor(p_228756_, p_228760_, 0, 8, $$7.hasOpening[Direction.DOWN.get3DDataValue()]);
            this.generateDefaultFloor(p_228756_, p_228760_, 0, 0, $$8.hasOpening[Direction.DOWN.get3DDataValue()]);
         }

         if ($$10.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(p_228756_, p_228760_, 1, 8, 1, 6, 8, 7, BASE_GRAY);
         }

         if ($$9.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(p_228756_, p_228760_, 1, 8, 8, 6, 8, 14, BASE_GRAY);
         }

         for (int $$11 = 1; $$11 <= 7; $$11++) {
            BlockState $$12 = BASE_LIGHT;
            if ($$11 == 2 || $$11 == 6) {
               $$12 = BASE_GRAY;
            }

            this.generateBox(p_228756_, p_228760_, 0, $$11, 0, 0, $$11, 15, $$12, $$12, false);
            this.generateBox(p_228756_, p_228760_, 7, $$11, 0, 7, $$11, 15, $$12, $$12, false);
            this.generateBox(p_228756_, p_228760_, 1, $$11, 0, 6, $$11, 0, $$12, $$12, false);
            this.generateBox(p_228756_, p_228760_, 1, $$11, 15, 6, $$11, 15, $$12, $$12, false);
         }

         for (int $$13 = 1; $$13 <= 7; $$13++) {
            BlockState $$14 = BASE_BLACK;
            if ($$13 == 2 || $$13 == 6) {
               $$14 = LAMP_BLOCK;
            }

            this.generateBox(p_228756_, p_228760_, 3, $$13, 7, 4, $$13, 8, $$14, $$14, false);
         }

         if ($$8.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(p_228756_, p_228760_, 3, 1, 0, 4, 2, 0);
         }

         if ($$8.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(p_228756_, p_228760_, 7, 1, 3, 7, 2, 4);
         }

         if ($$8.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(p_228756_, p_228760_, 0, 1, 3, 0, 2, 4);
         }

         if ($$7.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(p_228756_, p_228760_, 3, 1, 15, 4, 2, 15);
         }

         if ($$7.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(p_228756_, p_228760_, 0, 1, 11, 0, 2, 12);
         }

         if ($$7.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(p_228756_, p_228760_, 7, 1, 11, 7, 2, 12);
         }

         if ($$10.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(p_228756_, p_228760_, 3, 5, 0, 4, 6, 0);
         }

         if ($$10.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(p_228756_, p_228760_, 7, 5, 3, 7, 6, 4);
            this.generateBox(p_228756_, p_228760_, 5, 4, 2, 6, 4, 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228756_, p_228760_, 6, 1, 2, 6, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228756_, p_228760_, 6, 1, 5, 6, 3, 5, BASE_LIGHT, BASE_LIGHT, false);
         }

         if ($$10.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(p_228756_, p_228760_, 0, 5, 3, 0, 6, 4);
            this.generateBox(p_228756_, p_228760_, 1, 4, 2, 2, 4, 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228756_, p_228760_, 1, 1, 2, 1, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228756_, p_228760_, 1, 1, 5, 1, 3, 5, BASE_LIGHT, BASE_LIGHT, false);
         }

         if ($$9.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(p_228756_, p_228760_, 3, 5, 15, 4, 6, 15);
         }

         if ($$9.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(p_228756_, p_228760_, 0, 5, 11, 0, 6, 12);
            this.generateBox(p_228756_, p_228760_, 1, 4, 10, 2, 4, 13, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228756_, p_228760_, 1, 1, 10, 1, 3, 10, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228756_, p_228760_, 1, 1, 13, 1, 3, 13, BASE_LIGHT, BASE_LIGHT, false);
         }

         if ($$9.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(p_228756_, p_228760_, 7, 5, 11, 7, 6, 12);
            this.generateBox(p_228756_, p_228760_, 5, 4, 10, 6, 4, 13, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228756_, p_228760_, 6, 1, 10, 6, 3, 10, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228756_, p_228760_, 6, 1, 13, 6, 3, 13, BASE_LIGHT, BASE_LIGHT, false);
         }
      }
   }

   public static class OceanMonumentDoubleZRoom extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentDoubleZRoom(Direction p_228764_, OceanMonumentPieces.RoomDefinition p_228765_) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_Z_ROOM, 1, p_228764_, p_228765_, 1, 1, 2);
      }

      public OceanMonumentDoubleZRoom(CompoundTag p_228767_) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_Z_ROOM, p_228767_);
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228769_,
         StructureManager p_228770_,
         ChunkGenerator p_228771_,
         RandomSource p_228772_,
         BoundingBox p_228773_,
         ChunkPos p_228774_,
         BlockPos p_228775_
      ) {
         OceanMonumentPieces.RoomDefinition $$7 = this.roomDefinition.connections[Direction.NORTH.get3DDataValue()];
         OceanMonumentPieces.RoomDefinition $$8 = this.roomDefinition;
         if (this.roomDefinition.index / 25 > 0) {
            this.generateDefaultFloor(p_228769_, p_228773_, 0, 8, $$7.hasOpening[Direction.DOWN.get3DDataValue()]);
            this.generateDefaultFloor(p_228769_, p_228773_, 0, 0, $$8.hasOpening[Direction.DOWN.get3DDataValue()]);
         }

         if ($$8.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(p_228769_, p_228773_, 1, 4, 1, 6, 4, 7, BASE_GRAY);
         }

         if ($$7.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(p_228769_, p_228773_, 1, 4, 8, 6, 4, 14, BASE_GRAY);
         }

         this.generateBox(p_228769_, p_228773_, 0, 3, 0, 0, 3, 15, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 7, 3, 0, 7, 3, 15, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 1, 3, 0, 7, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 1, 3, 15, 6, 3, 15, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 0, 2, 0, 0, 2, 15, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(p_228769_, p_228773_, 7, 2, 0, 7, 2, 15, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(p_228769_, p_228773_, 1, 2, 0, 7, 2, 0, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(p_228769_, p_228773_, 1, 2, 15, 6, 2, 15, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(p_228769_, p_228773_, 0, 1, 0, 0, 1, 15, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 7, 1, 0, 7, 1, 15, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 1, 1, 0, 7, 1, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 1, 1, 15, 6, 1, 15, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 1, 1, 1, 1, 1, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 6, 1, 1, 6, 1, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 1, 3, 1, 1, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 6, 3, 1, 6, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 1, 1, 13, 1, 1, 14, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 6, 1, 13, 6, 1, 14, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 1, 3, 13, 1, 3, 14, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 6, 3, 13, 6, 3, 14, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 2, 1, 6, 2, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 5, 1, 6, 5, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 2, 1, 9, 2, 3, 9, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 5, 1, 9, 5, 3, 9, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 3, 2, 6, 4, 2, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 3, 2, 9, 4, 2, 9, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 2, 2, 7, 2, 2, 8, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 5, 2, 7, 5, 2, 8, BASE_LIGHT, BASE_LIGHT, false);
         this.placeBlock(p_228769_, LAMP_BLOCK, 2, 2, 5, p_228773_);
         this.placeBlock(p_228769_, LAMP_BLOCK, 5, 2, 5, p_228773_);
         this.placeBlock(p_228769_, LAMP_BLOCK, 2, 2, 10, p_228773_);
         this.placeBlock(p_228769_, LAMP_BLOCK, 5, 2, 10, p_228773_);
         this.placeBlock(p_228769_, BASE_LIGHT, 2, 3, 5, p_228773_);
         this.placeBlock(p_228769_, BASE_LIGHT, 5, 3, 5, p_228773_);
         this.placeBlock(p_228769_, BASE_LIGHT, 2, 3, 10, p_228773_);
         this.placeBlock(p_228769_, BASE_LIGHT, 5, 3, 10, p_228773_);
         if ($$8.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(p_228769_, p_228773_, 3, 1, 0, 4, 2, 0);
         }

         if ($$8.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(p_228769_, p_228773_, 7, 1, 3, 7, 2, 4);
         }

         if ($$8.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(p_228769_, p_228773_, 0, 1, 3, 0, 2, 4);
         }

         if ($$7.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(p_228769_, p_228773_, 3, 1, 15, 4, 2, 15);
         }

         if ($$7.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(p_228769_, p_228773_, 0, 1, 11, 0, 2, 12);
         }

         if ($$7.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(p_228769_, p_228773_, 7, 1, 11, 7, 2, 12);
         }
      }
   }

   public static class OceanMonumentEntryRoom extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentEntryRoom(Direction p_228777_, OceanMonumentPieces.RoomDefinition p_228778_) {
         super(StructurePieceType.OCEAN_MONUMENT_ENTRY_ROOM, 1, p_228777_, p_228778_, 1, 1, 1);
      }

      public OceanMonumentEntryRoom(CompoundTag p_228780_) {
         super(StructurePieceType.OCEAN_MONUMENT_ENTRY_ROOM, p_228780_);
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228782_,
         StructureManager p_228783_,
         ChunkGenerator p_228784_,
         RandomSource p_228785_,
         BoundingBox p_228786_,
         ChunkPos p_228787_,
         BlockPos p_228788_
      ) {
         this.generateBox(p_228782_, p_228786_, 0, 3, 0, 2, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228782_, p_228786_, 5, 3, 0, 7, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228782_, p_228786_, 0, 2, 0, 1, 2, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228782_, p_228786_, 6, 2, 0, 7, 2, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228782_, p_228786_, 0, 1, 0, 0, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228782_, p_228786_, 7, 1, 0, 7, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228782_, p_228786_, 0, 1, 7, 7, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228782_, p_228786_, 1, 1, 0, 2, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228782_, p_228786_, 5, 1, 0, 6, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
         if (this.roomDefinition.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(p_228782_, p_228786_, 3, 1, 7, 4, 2, 7);
         }

         if (this.roomDefinition.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(p_228782_, p_228786_, 0, 1, 3, 1, 2, 4);
         }

         if (this.roomDefinition.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(p_228782_, p_228786_, 6, 1, 3, 7, 2, 4);
         }
      }
   }

   public static class OceanMonumentPenthouse extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentPenthouse(Direction p_228790_, BoundingBox p_228791_) {
         super(StructurePieceType.OCEAN_MONUMENT_PENTHOUSE, p_228790_, 1, p_228791_);
      }

      public OceanMonumentPenthouse(CompoundTag p_228793_) {
         super(StructurePieceType.OCEAN_MONUMENT_PENTHOUSE, p_228793_);
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228795_,
         StructureManager p_228796_,
         ChunkGenerator p_228797_,
         RandomSource p_228798_,
         BoundingBox p_228799_,
         ChunkPos p_228800_,
         BlockPos p_228801_
      ) {
         this.generateBox(p_228795_, p_228799_, 2, -1, 2, 11, -1, 11, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228795_, p_228799_, 0, -1, 0, 1, -1, 11, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(p_228795_, p_228799_, 12, -1, 0, 13, -1, 11, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(p_228795_, p_228799_, 2, -1, 0, 11, -1, 1, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(p_228795_, p_228799_, 2, -1, 12, 11, -1, 13, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(p_228795_, p_228799_, 0, 0, 0, 0, 0, 13, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228795_, p_228799_, 13, 0, 0, 13, 0, 13, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228795_, p_228799_, 1, 0, 0, 12, 0, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228795_, p_228799_, 1, 0, 13, 12, 0, 13, BASE_LIGHT, BASE_LIGHT, false);

         for (int $$7 = 2; $$7 <= 11; $$7 += 3) {
            this.placeBlock(p_228795_, LAMP_BLOCK, 0, 0, $$7, p_228799_);
            this.placeBlock(p_228795_, LAMP_BLOCK, 13, 0, $$7, p_228799_);
            this.placeBlock(p_228795_, LAMP_BLOCK, $$7, 0, 0, p_228799_);
         }

         this.generateBox(p_228795_, p_228799_, 2, 0, 3, 4, 0, 9, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228795_, p_228799_, 9, 0, 3, 11, 0, 9, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228795_, p_228799_, 4, 0, 9, 9, 0, 11, BASE_LIGHT, BASE_LIGHT, false);
         this.placeBlock(p_228795_, BASE_LIGHT, 5, 0, 8, p_228799_);
         this.placeBlock(p_228795_, BASE_LIGHT, 8, 0, 8, p_228799_);
         this.placeBlock(p_228795_, BASE_LIGHT, 10, 0, 10, p_228799_);
         this.placeBlock(p_228795_, BASE_LIGHT, 3, 0, 10, p_228799_);
         this.generateBox(p_228795_, p_228799_, 3, 0, 3, 3, 0, 7, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(p_228795_, p_228799_, 10, 0, 3, 10, 0, 7, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(p_228795_, p_228799_, 6, 0, 10, 7, 0, 10, BASE_BLACK, BASE_BLACK, false);
         int $$8 = 3;

         for (int $$9 = 0; $$9 < 2; $$9++) {
            for (int $$10 = 2; $$10 <= 8; $$10 += 3) {
               this.generateBox(p_228795_, p_228799_, $$8, 0, $$10, $$8, 2, $$10, BASE_LIGHT, BASE_LIGHT, false);
            }

            $$8 = 10;
         }

         this.generateBox(p_228795_, p_228799_, 5, 0, 10, 5, 2, 10, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228795_, p_228799_, 8, 0, 10, 8, 2, 10, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228795_, p_228799_, 6, -1, 7, 7, -1, 8, BASE_BLACK, BASE_BLACK, false);
         this.generateWaterBox(p_228795_, p_228799_, 6, -1, 3, 7, -1, 4);
         this.spawnElder(p_228795_, p_228799_, 6, 1, 6);
      }
   }

   protected abstract static class OceanMonumentPiece extends StructurePiece {
      protected static final BlockState BASE_GRAY = Blocks.PRISMARINE.defaultBlockState();
      protected static final BlockState BASE_LIGHT = Blocks.PRISMARINE_BRICKS.defaultBlockState();
      protected static final BlockState BASE_BLACK = Blocks.DARK_PRISMARINE.defaultBlockState();
      protected static final BlockState DOT_DECO_DATA = BASE_LIGHT;
      protected static final BlockState LAMP_BLOCK = Blocks.SEA_LANTERN.defaultBlockState();
      protected static final boolean DO_FILL = true;
      protected static final BlockState FILL_BLOCK = Blocks.WATER.defaultBlockState();
      protected static final Set<Block> FILL_KEEP = ImmutableSet.builder()
         .add(Blocks.ICE)
         .add(Blocks.PACKED_ICE)
         .add(Blocks.BLUE_ICE)
         .add(FILL_BLOCK.getBlock())
         .build();
      protected static final int GRIDROOM_WIDTH = 8;
      protected static final int GRIDROOM_DEPTH = 8;
      protected static final int GRIDROOM_HEIGHT = 4;
      protected static final int GRID_WIDTH = 5;
      protected static final int GRID_DEPTH = 5;
      protected static final int GRID_HEIGHT = 3;
      protected static final int GRID_FLOOR_COUNT = 25;
      protected static final int GRID_SIZE = 75;
      protected static final int GRIDROOM_SOURCE_INDEX = getRoomIndex(2, 0, 0);
      protected static final int GRIDROOM_TOP_CONNECT_INDEX = getRoomIndex(2, 2, 0);
      protected static final int GRIDROOM_LEFTWING_CONNECT_INDEX = getRoomIndex(0, 1, 0);
      protected static final int GRIDROOM_RIGHTWING_CONNECT_INDEX = getRoomIndex(4, 1, 0);
      protected static final int LEFTWING_INDEX = 1001;
      protected static final int RIGHTWING_INDEX = 1002;
      protected static final int PENTHOUSE_INDEX = 1003;
      protected OceanMonumentPieces.RoomDefinition roomDefinition;

      protected static int getRoomIndex(int p_228890_, int p_228891_, int p_228892_) {
         return p_228891_ * 25 + p_228892_ * 5 + p_228890_;
      }

      public OceanMonumentPiece(StructurePieceType p_228836_, Direction p_228837_, int p_228838_, BoundingBox p_228839_) {
         super(p_228836_, p_228838_, p_228839_);
         this.setOrientation(p_228837_);
      }

      protected OceanMonumentPiece(
         StructurePieceType p_228828_,
         int p_228829_,
         Direction p_228830_,
         OceanMonumentPieces.RoomDefinition p_228831_,
         int p_228832_,
         int p_228833_,
         int p_228834_
      ) {
         super(p_228828_, p_228829_, makeBoundingBox(p_228830_, p_228831_, p_228832_, p_228833_, p_228834_));
         this.setOrientation(p_228830_);
         this.roomDefinition = p_228831_;
      }

      private static BoundingBox makeBoundingBox(Direction p_228875_, OceanMonumentPieces.RoomDefinition p_228876_, int p_228877_, int p_228878_, int p_228879_) {
         int $$5 = p_228876_.index;
         int $$6 = $$5 % 5;
         int $$7 = $$5 / 5 % 5;
         int $$8 = $$5 / 25;
         BoundingBox $$9 = makeBoundingBox(0, 0, 0, p_228875_, p_228877_ * 8, p_228878_ * 4, p_228879_ * 8);
         switch (p_228875_) {
            case NORTH:
               $$9.move($$6 * 8, $$8 * 4, -($$7 + p_228879_) * 8 + 1);
               break;
            case SOUTH:
               $$9.move($$6 * 8, $$8 * 4, $$7 * 8);
               break;
            case WEST:
               $$9.move(-($$7 + p_228879_) * 8 + 1, $$8 * 4, $$6 * 8);
               break;
            case EAST:
            default:
               $$9.move($$7 * 8, $$8 * 4, $$6 * 8);
         }

         return $$9;
      }

      public OceanMonumentPiece(StructurePieceType p_228841_, CompoundTag p_228842_) {
         super(p_228841_, p_228842_);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext p_228872_, CompoundTag p_228873_) {
      }

      protected void generateWaterBox(
         WorldGenLevel p_228881_, BoundingBox p_228882_, int p_228883_, int p_228884_, int p_228885_, int p_228886_, int p_228887_, int p_228888_
      ) {
         for (int $$8 = p_228884_; $$8 <= p_228887_; $$8++) {
            for (int $$9 = p_228883_; $$9 <= p_228886_; $$9++) {
               for (int $$10 = p_228885_; $$10 <= p_228888_; $$10++) {
                  BlockState $$11 = this.getBlock(p_228881_, $$9, $$8, $$10, p_228882_);
                  if (!FILL_KEEP.contains($$11.getBlock())) {
                     if (this.getWorldY($$8) >= p_228881_.getSeaLevel() && $$11 != FILL_BLOCK) {
                        this.placeBlock(p_228881_, Blocks.AIR.defaultBlockState(), $$9, $$8, $$10, p_228882_);
                     } else {
                        this.placeBlock(p_228881_, FILL_BLOCK, $$9, $$8, $$10, p_228882_);
                     }
                  }
               }
            }
         }
      }

      protected void generateDefaultFloor(WorldGenLevel p_228860_, BoundingBox p_228861_, int p_228862_, int p_228863_, boolean p_228864_) {
         if (p_228864_) {
            this.generateBox(p_228860_, p_228861_, p_228862_ + 0, 0, p_228863_ + 0, p_228862_ + 2, 0, p_228863_ + 8 - 1, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228860_, p_228861_, p_228862_ + 5, 0, p_228863_ + 0, p_228862_ + 8 - 1, 0, p_228863_ + 8 - 1, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228860_, p_228861_, p_228862_ + 3, 0, p_228863_ + 0, p_228862_ + 4, 0, p_228863_ + 2, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228860_, p_228861_, p_228862_ + 3, 0, p_228863_ + 5, p_228862_ + 4, 0, p_228863_ + 8 - 1, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228860_, p_228861_, p_228862_ + 3, 0, p_228863_ + 2, p_228862_ + 4, 0, p_228863_ + 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228860_, p_228861_, p_228862_ + 3, 0, p_228863_ + 5, p_228862_ + 4, 0, p_228863_ + 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228860_, p_228861_, p_228862_ + 2, 0, p_228863_ + 3, p_228862_ + 2, 0, p_228863_ + 4, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228860_, p_228861_, p_228862_ + 5, 0, p_228863_ + 3, p_228862_ + 5, 0, p_228863_ + 4, BASE_LIGHT, BASE_LIGHT, false);
         } else {
            this.generateBox(p_228860_, p_228861_, p_228862_ + 0, 0, p_228863_ + 0, p_228862_ + 8 - 1, 0, p_228863_ + 8 - 1, BASE_GRAY, BASE_GRAY, false);
         }
      }

      protected void generateBoxOnFillOnly(
         WorldGenLevel p_228850_,
         BoundingBox p_228851_,
         int p_228852_,
         int p_228853_,
         int p_228854_,
         int p_228855_,
         int p_228856_,
         int p_228857_,
         BlockState p_228858_
      ) {
         for (int $$9 = p_228853_; $$9 <= p_228856_; $$9++) {
            for (int $$10 = p_228852_; $$10 <= p_228855_; $$10++) {
               for (int $$11 = p_228854_; $$11 <= p_228857_; $$11++) {
                  if (this.getBlock(p_228850_, $$10, $$9, $$11, p_228851_) == FILL_BLOCK) {
                     this.placeBlock(p_228850_, p_228858_, $$10, $$9, $$11, p_228851_);
                  }
               }
            }
         }
      }

      protected boolean chunkIntersects(BoundingBox p_228866_, int p_228867_, int p_228868_, int p_228869_, int p_228870_) {
         int $$5 = this.getWorldX(p_228867_, p_228868_);
         int $$6 = this.getWorldZ(p_228867_, p_228868_);
         int $$7 = this.getWorldX(p_228869_, p_228870_);
         int $$8 = this.getWorldZ(p_228869_, p_228870_);
         return p_228866_.intersects(Math.min($$5, $$7), Math.min($$6, $$8), Math.max($$5, $$7), Math.max($$6, $$8));
      }

      protected void spawnElder(WorldGenLevel p_251919_, BoundingBox p_248944_, int p_251311_, int p_249326_, int p_252095_) {
         BlockPos $$5 = this.getWorldPos(p_251311_, p_249326_, p_252095_);
         if (p_248944_.isInside($$5)) {
            ElderGuardian $$6 = EntityType.ELDER_GUARDIAN.create(p_251919_.getLevel());
            if ($$6 != null) {
               $$6.heal($$6.getMaxHealth());
               $$6.moveTo($$5.getX() + 0.5, $$5.getY(), $$5.getZ() + 0.5, 0.0F, 0.0F);
               $$6.finalizeSpawn(p_251919_, p_251919_.getCurrentDifficultyAt($$6.blockPosition()), MobSpawnType.STRUCTURE, null);
               p_251919_.addFreshEntityWithPassengers($$6);
            }
         }
      }
   }

   public static class OceanMonumentSimpleRoom extends OceanMonumentPieces.OceanMonumentPiece {
      private int mainDesign;

      public OceanMonumentSimpleRoom(Direction p_228895_, OceanMonumentPieces.RoomDefinition p_228896_, RandomSource p_228897_) {
         super(StructurePieceType.OCEAN_MONUMENT_SIMPLE_ROOM, 1, p_228895_, p_228896_, 1, 1, 1);
         this.mainDesign = p_228897_.nextInt(3);
      }

      public OceanMonumentSimpleRoom(CompoundTag p_228899_) {
         super(StructurePieceType.OCEAN_MONUMENT_SIMPLE_ROOM, p_228899_);
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228901_,
         StructureManager p_228902_,
         ChunkGenerator p_228903_,
         RandomSource p_228904_,
         BoundingBox p_228905_,
         ChunkPos p_228906_,
         BlockPos p_228907_
      ) {
         if (this.roomDefinition.index / 25 > 0) {
            this.generateDefaultFloor(p_228901_, p_228905_, 0, 0, this.roomDefinition.hasOpening[Direction.DOWN.get3DDataValue()]);
         }

         if (this.roomDefinition.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(p_228901_, p_228905_, 1, 4, 1, 6, 4, 6, BASE_GRAY);
         }

         boolean $$7 = this.mainDesign != 0
            && p_228904_.nextBoolean()
            && !this.roomDefinition.hasOpening[Direction.DOWN.get3DDataValue()]
            && !this.roomDefinition.hasOpening[Direction.UP.get3DDataValue()]
            && this.roomDefinition.countOpenings() > 1;
         if (this.mainDesign == 0) {
            this.generateBox(p_228901_, p_228905_, 0, 1, 0, 2, 1, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 0, 3, 0, 2, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 0, 2, 0, 0, 2, 2, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228901_, p_228905_, 1, 2, 0, 2, 2, 0, BASE_GRAY, BASE_GRAY, false);
            this.placeBlock(p_228901_, LAMP_BLOCK, 1, 2, 1, p_228905_);
            this.generateBox(p_228901_, p_228905_, 5, 1, 0, 7, 1, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 5, 3, 0, 7, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 7, 2, 0, 7, 2, 2, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228901_, p_228905_, 5, 2, 0, 6, 2, 0, BASE_GRAY, BASE_GRAY, false);
            this.placeBlock(p_228901_, LAMP_BLOCK, 6, 2, 1, p_228905_);
            this.generateBox(p_228901_, p_228905_, 0, 1, 5, 2, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 0, 3, 5, 2, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 0, 2, 5, 0, 2, 7, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228901_, p_228905_, 1, 2, 7, 2, 2, 7, BASE_GRAY, BASE_GRAY, false);
            this.placeBlock(p_228901_, LAMP_BLOCK, 1, 2, 6, p_228905_);
            this.generateBox(p_228901_, p_228905_, 5, 1, 5, 7, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 5, 3, 5, 7, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 7, 2, 5, 7, 2, 7, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228901_, p_228905_, 5, 2, 7, 6, 2, 7, BASE_GRAY, BASE_GRAY, false);
            this.placeBlock(p_228901_, LAMP_BLOCK, 6, 2, 6, p_228905_);
            if (this.roomDefinition.hasOpening[Direction.SOUTH.get3DDataValue()]) {
               this.generateBox(p_228901_, p_228905_, 3, 3, 0, 4, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
            } else {
               this.generateBox(p_228901_, p_228905_, 3, 3, 0, 4, 3, 1, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228901_, p_228905_, 3, 2, 0, 4, 2, 0, BASE_GRAY, BASE_GRAY, false);
               this.generateBox(p_228901_, p_228905_, 3, 1, 0, 4, 1, 1, BASE_LIGHT, BASE_LIGHT, false);
            }

            if (this.roomDefinition.hasOpening[Direction.NORTH.get3DDataValue()]) {
               this.generateBox(p_228901_, p_228905_, 3, 3, 7, 4, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            } else {
               this.generateBox(p_228901_, p_228905_, 3, 3, 6, 4, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228901_, p_228905_, 3, 2, 7, 4, 2, 7, BASE_GRAY, BASE_GRAY, false);
               this.generateBox(p_228901_, p_228905_, 3, 1, 6, 4, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            }

            if (this.roomDefinition.hasOpening[Direction.WEST.get3DDataValue()]) {
               this.generateBox(p_228901_, p_228905_, 0, 3, 3, 0, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
            } else {
               this.generateBox(p_228901_, p_228905_, 0, 3, 3, 1, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228901_, p_228905_, 0, 2, 3, 0, 2, 4, BASE_GRAY, BASE_GRAY, false);
               this.generateBox(p_228901_, p_228905_, 0, 1, 3, 1, 1, 4, BASE_LIGHT, BASE_LIGHT, false);
            }

            if (this.roomDefinition.hasOpening[Direction.EAST.get3DDataValue()]) {
               this.generateBox(p_228901_, p_228905_, 7, 3, 3, 7, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
            } else {
               this.generateBox(p_228901_, p_228905_, 6, 3, 3, 7, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228901_, p_228905_, 7, 2, 3, 7, 2, 4, BASE_GRAY, BASE_GRAY, false);
               this.generateBox(p_228901_, p_228905_, 6, 1, 3, 7, 1, 4, BASE_LIGHT, BASE_LIGHT, false);
            }
         } else if (this.mainDesign == 1) {
            this.generateBox(p_228901_, p_228905_, 2, 1, 2, 2, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 2, 1, 5, 2, 3, 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 5, 1, 5, 5, 3, 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 5, 1, 2, 5, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.placeBlock(p_228901_, LAMP_BLOCK, 2, 2, 2, p_228905_);
            this.placeBlock(p_228901_, LAMP_BLOCK, 2, 2, 5, p_228905_);
            this.placeBlock(p_228901_, LAMP_BLOCK, 5, 2, 5, p_228905_);
            this.placeBlock(p_228901_, LAMP_BLOCK, 5, 2, 2, p_228905_);
            this.generateBox(p_228901_, p_228905_, 0, 1, 0, 1, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 0, 1, 1, 0, 3, 1, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 0, 1, 7, 1, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 0, 1, 6, 0, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 6, 1, 7, 7, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 7, 1, 6, 7, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 6, 1, 0, 7, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 7, 1, 1, 7, 3, 1, BASE_LIGHT, BASE_LIGHT, false);
            this.placeBlock(p_228901_, BASE_GRAY, 1, 2, 0, p_228905_);
            this.placeBlock(p_228901_, BASE_GRAY, 0, 2, 1, p_228905_);
            this.placeBlock(p_228901_, BASE_GRAY, 1, 2, 7, p_228905_);
            this.placeBlock(p_228901_, BASE_GRAY, 0, 2, 6, p_228905_);
            this.placeBlock(p_228901_, BASE_GRAY, 6, 2, 7, p_228905_);
            this.placeBlock(p_228901_, BASE_GRAY, 7, 2, 6, p_228905_);
            this.placeBlock(p_228901_, BASE_GRAY, 6, 2, 0, p_228905_);
            this.placeBlock(p_228901_, BASE_GRAY, 7, 2, 1, p_228905_);
            if (!this.roomDefinition.hasOpening[Direction.SOUTH.get3DDataValue()]) {
               this.generateBox(p_228901_, p_228905_, 1, 3, 0, 6, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228901_, p_228905_, 1, 2, 0, 6, 2, 0, BASE_GRAY, BASE_GRAY, false);
               this.generateBox(p_228901_, p_228905_, 1, 1, 0, 6, 1, 0, BASE_LIGHT, BASE_LIGHT, false);
            }

            if (!this.roomDefinition.hasOpening[Direction.NORTH.get3DDataValue()]) {
               this.generateBox(p_228901_, p_228905_, 1, 3, 7, 6, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228901_, p_228905_, 1, 2, 7, 6, 2, 7, BASE_GRAY, BASE_GRAY, false);
               this.generateBox(p_228901_, p_228905_, 1, 1, 7, 6, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            }

            if (!this.roomDefinition.hasOpening[Direction.WEST.get3DDataValue()]) {
               this.generateBox(p_228901_, p_228905_, 0, 3, 1, 0, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228901_, p_228905_, 0, 2, 1, 0, 2, 6, BASE_GRAY, BASE_GRAY, false);
               this.generateBox(p_228901_, p_228905_, 0, 1, 1, 0, 1, 6, BASE_LIGHT, BASE_LIGHT, false);
            }

            if (!this.roomDefinition.hasOpening[Direction.EAST.get3DDataValue()]) {
               this.generateBox(p_228901_, p_228905_, 7, 3, 1, 7, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228901_, p_228905_, 7, 2, 1, 7, 2, 6, BASE_GRAY, BASE_GRAY, false);
               this.generateBox(p_228901_, p_228905_, 7, 1, 1, 7, 1, 6, BASE_LIGHT, BASE_LIGHT, false);
            }
         } else if (this.mainDesign == 2) {
            this.generateBox(p_228901_, p_228905_, 0, 1, 0, 0, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 7, 1, 0, 7, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 1, 1, 0, 6, 1, 0, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 1, 1, 7, 6, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 0, 2, 0, 0, 2, 7, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(p_228901_, p_228905_, 7, 2, 0, 7, 2, 7, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(p_228901_, p_228905_, 1, 2, 0, 6, 2, 0, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(p_228901_, p_228905_, 1, 2, 7, 6, 2, 7, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(p_228901_, p_228905_, 0, 3, 0, 0, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 7, 3, 0, 7, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 1, 3, 0, 6, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 1, 3, 7, 6, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 0, 1, 3, 0, 2, 4, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(p_228901_, p_228905_, 7, 1, 3, 7, 2, 4, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(p_228901_, p_228905_, 3, 1, 0, 4, 2, 0, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(p_228901_, p_228905_, 3, 1, 7, 4, 2, 7, BASE_BLACK, BASE_BLACK, false);
            if (this.roomDefinition.hasOpening[Direction.SOUTH.get3DDataValue()]) {
               this.generateWaterBox(p_228901_, p_228905_, 3, 1, 0, 4, 2, 0);
            }

            if (this.roomDefinition.hasOpening[Direction.NORTH.get3DDataValue()]) {
               this.generateWaterBox(p_228901_, p_228905_, 3, 1, 7, 4, 2, 7);
            }

            if (this.roomDefinition.hasOpening[Direction.WEST.get3DDataValue()]) {
               this.generateWaterBox(p_228901_, p_228905_, 0, 1, 3, 0, 2, 4);
            }

            if (this.roomDefinition.hasOpening[Direction.EAST.get3DDataValue()]) {
               this.generateWaterBox(p_228901_, p_228905_, 7, 1, 3, 7, 2, 4);
            }
         }

         if ($$7) {
            this.generateBox(p_228901_, p_228905_, 3, 1, 3, 4, 1, 4, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 3, 2, 3, 4, 2, 4, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228901_, p_228905_, 3, 3, 3, 4, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
         }
      }
   }

   public static class OceanMonumentSimpleTopRoom extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentSimpleTopRoom(Direction p_228909_, OceanMonumentPieces.RoomDefinition p_228910_) {
         super(StructurePieceType.OCEAN_MONUMENT_SIMPLE_TOP_ROOM, 1, p_228909_, p_228910_, 1, 1, 1);
      }

      public OceanMonumentSimpleTopRoom(CompoundTag p_228912_) {
         super(StructurePieceType.OCEAN_MONUMENT_SIMPLE_TOP_ROOM, p_228912_);
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228914_,
         StructureManager p_228915_,
         ChunkGenerator p_228916_,
         RandomSource p_228917_,
         BoundingBox p_228918_,
         ChunkPos p_228919_,
         BlockPos p_228920_
      ) {
         if (this.roomDefinition.index / 25 > 0) {
            this.generateDefaultFloor(p_228914_, p_228918_, 0, 0, this.roomDefinition.hasOpening[Direction.DOWN.get3DDataValue()]);
         }

         if (this.roomDefinition.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(p_228914_, p_228918_, 1, 4, 1, 6, 4, 6, BASE_GRAY);
         }

         for (int $$7 = 1; $$7 <= 6; $$7++) {
            for (int $$8 = 1; $$8 <= 6; $$8++) {
               if (p_228917_.nextInt(3) != 0) {
                  int $$9 = 2 + (p_228917_.nextInt(4) == 0 ? 0 : 1);
                  BlockState $$10 = Blocks.WET_SPONGE.defaultBlockState();
                  this.generateBox(p_228914_, p_228918_, $$7, $$9, $$8, $$7, 3, $$8, $$10, $$10, false);
               }
            }
         }

         this.generateBox(p_228914_, p_228918_, 0, 1, 0, 0, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228914_, p_228918_, 7, 1, 0, 7, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228914_, p_228918_, 1, 1, 0, 6, 1, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228914_, p_228918_, 1, 1, 7, 6, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228914_, p_228918_, 0, 2, 0, 0, 2, 7, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(p_228914_, p_228918_, 7, 2, 0, 7, 2, 7, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(p_228914_, p_228918_, 1, 2, 0, 6, 2, 0, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(p_228914_, p_228918_, 1, 2, 7, 6, 2, 7, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(p_228914_, p_228918_, 0, 3, 0, 0, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228914_, p_228918_, 7, 3, 0, 7, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228914_, p_228918_, 1, 3, 0, 6, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228914_, p_228918_, 1, 3, 7, 6, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228914_, p_228918_, 0, 1, 3, 0, 2, 4, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(p_228914_, p_228918_, 7, 1, 3, 7, 2, 4, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(p_228914_, p_228918_, 3, 1, 0, 4, 2, 0, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(p_228914_, p_228918_, 3, 1, 7, 4, 2, 7, BASE_BLACK, BASE_BLACK, false);
         if (this.roomDefinition.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(p_228914_, p_228918_, 3, 1, 0, 4, 2, 0);
         }
      }
   }

   public static class OceanMonumentWingRoom extends OceanMonumentPieces.OceanMonumentPiece {
      private int mainDesign;

      public OceanMonumentWingRoom(Direction p_228923_, BoundingBox p_228924_, int p_228925_) {
         super(StructurePieceType.OCEAN_MONUMENT_WING_ROOM, p_228923_, 1, p_228924_);
         this.mainDesign = p_228925_ & 1;
      }

      public OceanMonumentWingRoom(CompoundTag p_228927_) {
         super(StructurePieceType.OCEAN_MONUMENT_WING_ROOM, p_228927_);
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228929_,
         StructureManager p_228930_,
         ChunkGenerator p_228931_,
         RandomSource p_228932_,
         BoundingBox p_228933_,
         ChunkPos p_228934_,
         BlockPos p_228935_
      ) {
         if (this.mainDesign == 0) {
            for (int $$7 = 0; $$7 < 4; $$7++) {
               this.generateBox(p_228929_, p_228933_, 10 - $$7, 3 - $$7, 20 - $$7, 12 + $$7, 3 - $$7, 20, BASE_LIGHT, BASE_LIGHT, false);
            }

            this.generateBox(p_228929_, p_228933_, 7, 0, 6, 15, 0, 16, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228929_, p_228933_, 6, 0, 6, 6, 3, 20, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228929_, p_228933_, 16, 0, 6, 16, 3, 20, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228929_, p_228933_, 7, 1, 7, 7, 1, 20, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228929_, p_228933_, 15, 1, 7, 15, 1, 20, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228929_, p_228933_, 7, 1, 6, 9, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228929_, p_228933_, 13, 1, 6, 15, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228929_, p_228933_, 8, 1, 7, 9, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228929_, p_228933_, 13, 1, 7, 14, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228929_, p_228933_, 9, 0, 5, 13, 0, 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228929_, p_228933_, 10, 0, 7, 12, 0, 7, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(p_228929_, p_228933_, 8, 0, 10, 8, 0, 12, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(p_228929_, p_228933_, 14, 0, 10, 14, 0, 12, BASE_BLACK, BASE_BLACK, false);

            for (int $$8 = 18; $$8 >= 7; $$8 -= 3) {
               this.placeBlock(p_228929_, LAMP_BLOCK, 6, 3, $$8, p_228933_);
               this.placeBlock(p_228929_, LAMP_BLOCK, 16, 3, $$8, p_228933_);
            }

            this.placeBlock(p_228929_, LAMP_BLOCK, 10, 0, 10, p_228933_);
            this.placeBlock(p_228929_, LAMP_BLOCK, 12, 0, 10, p_228933_);
            this.placeBlock(p_228929_, LAMP_BLOCK, 10, 0, 12, p_228933_);
            this.placeBlock(p_228929_, LAMP_BLOCK, 12, 0, 12, p_228933_);
            this.placeBlock(p_228929_, LAMP_BLOCK, 8, 3, 6, p_228933_);
            this.placeBlock(p_228929_, LAMP_BLOCK, 14, 3, 6, p_228933_);
            this.placeBlock(p_228929_, BASE_LIGHT, 4, 2, 4, p_228933_);
            this.placeBlock(p_228929_, LAMP_BLOCK, 4, 1, 4, p_228933_);
            this.placeBlock(p_228929_, BASE_LIGHT, 4, 0, 4, p_228933_);
            this.placeBlock(p_228929_, BASE_LIGHT, 18, 2, 4, p_228933_);
            this.placeBlock(p_228929_, LAMP_BLOCK, 18, 1, 4, p_228933_);
            this.placeBlock(p_228929_, BASE_LIGHT, 18, 0, 4, p_228933_);
            this.placeBlock(p_228929_, BASE_LIGHT, 4, 2, 18, p_228933_);
            this.placeBlock(p_228929_, LAMP_BLOCK, 4, 1, 18, p_228933_);
            this.placeBlock(p_228929_, BASE_LIGHT, 4, 0, 18, p_228933_);
            this.placeBlock(p_228929_, BASE_LIGHT, 18, 2, 18, p_228933_);
            this.placeBlock(p_228929_, LAMP_BLOCK, 18, 1, 18, p_228933_);
            this.placeBlock(p_228929_, BASE_LIGHT, 18, 0, 18, p_228933_);
            this.placeBlock(p_228929_, BASE_LIGHT, 9, 7, 20, p_228933_);
            this.placeBlock(p_228929_, BASE_LIGHT, 13, 7, 20, p_228933_);
            this.generateBox(p_228929_, p_228933_, 6, 0, 21, 7, 4, 21, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228929_, p_228933_, 15, 0, 21, 16, 4, 21, BASE_LIGHT, BASE_LIGHT, false);
            this.spawnElder(p_228929_, p_228933_, 11, 2, 16);
         } else if (this.mainDesign == 1) {
            this.generateBox(p_228929_, p_228933_, 9, 3, 18, 13, 3, 20, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228929_, p_228933_, 9, 0, 18, 9, 2, 18, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228929_, p_228933_, 13, 0, 18, 13, 2, 18, BASE_LIGHT, BASE_LIGHT, false);
            int $$9 = 9;
            int $$10 = 20;
            int $$11 = 5;

            for (int $$12 = 0; $$12 < 2; $$12++) {
               this.placeBlock(p_228929_, BASE_LIGHT, $$9, 6, 20, p_228933_);
               this.placeBlock(p_228929_, LAMP_BLOCK, $$9, 5, 20, p_228933_);
               this.placeBlock(p_228929_, BASE_LIGHT, $$9, 4, 20, p_228933_);
               $$9 = 13;
            }

            this.generateBox(p_228929_, p_228933_, 7, 3, 7, 15, 3, 14, BASE_LIGHT, BASE_LIGHT, false);
            int var14 = 10;

            for (int $$13 = 0; $$13 < 2; $$13++) {
               this.generateBox(p_228929_, p_228933_, var14, 0, 10, var14, 6, 10, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228929_, p_228933_, var14, 0, 12, var14, 6, 12, BASE_LIGHT, BASE_LIGHT, false);
               this.placeBlock(p_228929_, LAMP_BLOCK, var14, 0, 10, p_228933_);
               this.placeBlock(p_228929_, LAMP_BLOCK, var14, 0, 12, p_228933_);
               this.placeBlock(p_228929_, LAMP_BLOCK, var14, 4, 10, p_228933_);
               this.placeBlock(p_228929_, LAMP_BLOCK, var14, 4, 12, p_228933_);
               var14 = 12;
            }

            var14 = 8;

            for (int $$14 = 0; $$14 < 2; $$14++) {
               this.generateBox(p_228929_, p_228933_, var14, 0, 7, var14, 2, 7, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228929_, p_228933_, var14, 0, 14, var14, 2, 14, BASE_LIGHT, BASE_LIGHT, false);
               var14 = 14;
            }

            this.generateBox(p_228929_, p_228933_, 8, 3, 8, 8, 3, 13, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(p_228929_, p_228933_, 14, 3, 8, 14, 3, 13, BASE_BLACK, BASE_BLACK, false);
            this.spawnElder(p_228929_, p_228933_, 11, 5, 13);
         }
      }
   }

   static class RoomDefinition {
      final int index;
      final OceanMonumentPieces.RoomDefinition[] connections = new OceanMonumentPieces.RoomDefinition[6];
      final boolean[] hasOpening = new boolean[6];
      boolean claimed;
      boolean isSource;
      private int scanIndex;

      public RoomDefinition(int p_228943_) {
         this.index = p_228943_;
      }

      public void setConnection(Direction p_228948_, OceanMonumentPieces.RoomDefinition p_228949_) {
         this.connections[p_228948_.get3DDataValue()] = p_228949_;
         p_228949_.connections[p_228948_.getOpposite().get3DDataValue()] = this;
      }

      public void updateOpenings() {
         for (int $$0 = 0; $$0 < 6; $$0++) {
            this.hasOpening[$$0] = this.connections[$$0] != null;
         }
      }

      public boolean findSource(int p_228946_) {
         if (this.isSource) {
            return true;
         } else {
            this.scanIndex = p_228946_;

            for (int $$1 = 0; $$1 < 6; $$1++) {
               if (this.connections[$$1] != null
                  && this.hasOpening[$$1]
                  && this.connections[$$1].scanIndex != p_228946_
                  && this.connections[$$1].findSource(p_228946_)) {
                  return true;
               }
            }

            return false;
         }
      }

      public boolean isSpecial() {
         return this.index >= 75;
      }

      public int countOpenings() {
         int $$0 = 0;

         for (int $$1 = 0; $$1 < 6; $$1++) {
            if (this.hasOpening[$$1]) {
               $$0++;
            }
         }

         return $$0;
      }
   }
}
