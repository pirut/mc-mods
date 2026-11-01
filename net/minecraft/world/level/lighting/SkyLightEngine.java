package net.minecraft.world.level.lighting;

import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import org.jetbrains.annotations.VisibleForTesting;

public final class SkyLightEngine extends LightEngine<SkyLightSectionStorage.SkyDataLayerStorageMap, SkyLightSectionStorage> {
   private static final long REMOVE_TOP_SKY_SOURCE_ENTRY = LightEngine.QueueEntry.decreaseAllDirections(15);
   private static final long REMOVE_SKY_SOURCE_ENTRY = LightEngine.QueueEntry.decreaseSkipOneDirection(15, Direction.UP);
   private static final long ADD_SKY_SOURCE_ENTRY = LightEngine.QueueEntry.increaseSkipOneDirection(15, false, Direction.UP);
   private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
   private final ChunkSkyLightSources emptyChunkSources;

   public SkyLightEngine(LightChunkGetter p_75843_) {
      this(p_75843_, new SkyLightSectionStorage(p_75843_));
   }

   @VisibleForTesting
   protected SkyLightEngine(LightChunkGetter p_282215_, SkyLightSectionStorage p_282341_) {
      super(p_282215_, p_282341_);
      this.emptyChunkSources = new ChunkSkyLightSources(p_282215_.getLevel());
   }

   private static boolean isSourceLevel(int p_285004_) {
      return p_285004_ == 15;
   }

   private int getLowestSourceY(int p_285058_, int p_285191_, int p_285111_) {
      ChunkSkyLightSources $$3 = this.getChunkSources(SectionPos.blockToSectionCoord(p_285058_), SectionPos.blockToSectionCoord(p_285191_));
      return $$3 == null ? p_285111_ : $$3.getLowestSourceY(SectionPos.sectionRelative(p_285058_), SectionPos.sectionRelative(p_285191_));
   }

   @Nullable
   private ChunkSkyLightSources getChunkSources(int p_285270_, int p_285307_) {
      LightChunk $$2 = this.chunkSource.getChunkForLighting(p_285270_, p_285307_);
      return $$2 != null ? $$2.getSkyLightSources() : null;
   }

   @Override
   protected void checkNode(long p_75859_) {
      int $$1 = BlockPos.getX(p_75859_);
      int $$2 = BlockPos.getY(p_75859_);
      int $$3 = BlockPos.getZ(p_75859_);
      long $$4 = SectionPos.blockToSection(p_75859_);
      int $$5 = this.storage.lightOnInSection($$4) ? this.getLowestSourceY($$1, $$3, Integer.MAX_VALUE) : Integer.MAX_VALUE;
      if ($$5 != Integer.MAX_VALUE) {
         this.updateSourcesInColumn($$1, $$3, $$5);
      }

      if (this.storage.storingLightForSection($$4)) {
         boolean $$6 = $$2 >= $$5;
         if ($$6) {
            this.enqueueDecrease(p_75859_, REMOVE_SKY_SOURCE_ENTRY);
            this.enqueueIncrease(p_75859_, ADD_SKY_SOURCE_ENTRY);
         } else {
            int $$7 = this.storage.getStoredLevel(p_75859_);
            if ($$7 > 0) {
               this.storage.setStoredLevel(p_75859_, 0);
               this.enqueueDecrease(p_75859_, LightEngine.QueueEntry.decreaseAllDirections($$7));
            } else {
               this.enqueueDecrease(p_75859_, PULL_LIGHT_IN_ENTRY);
            }
         }
      }
   }

   private void updateSourcesInColumn(int p_285053_, int p_285140_, int p_285337_) {
      int $$3 = SectionPos.sectionToBlockCoord(this.storage.getBottomSectionY());
      this.removeSourcesBelow(p_285053_, p_285140_, p_285337_, $$3);
      this.addSourcesAbove(p_285053_, p_285140_, p_285337_, $$3);
   }

   private void removeSourcesBelow(int p_285475_, int p_285138_, int p_285130_, int p_285112_) {
      if (p_285130_ > p_285112_) {
         int $$4 = SectionPos.blockToSectionCoord(p_285475_);
         int $$5 = SectionPos.blockToSectionCoord(p_285138_);
         int $$6 = p_285130_ - 1;

         for (int $$7 = SectionPos.blockToSectionCoord($$6); this.storage.hasLightDataAtOrBelow($$7); $$7--) {
            if (this.storage.storingLightForSection(SectionPos.asLong($$4, $$7, $$5))) {
               int $$8 = SectionPos.sectionToBlockCoord($$7);
               int $$9 = $$8 + 15;

               for (int $$10 = Math.min($$9, $$6); $$10 >= $$8; $$10--) {
                  long $$11 = BlockPos.asLong(p_285475_, $$10, p_285138_);
                  if (!isSourceLevel(this.storage.getStoredLevel($$11))) {
                     return;
                  }

                  this.storage.setStoredLevel($$11, 0);
                  this.enqueueDecrease($$11, $$10 == p_285130_ - 1 ? REMOVE_TOP_SKY_SOURCE_ENTRY : REMOVE_SKY_SOURCE_ENTRY);
               }
            }
         }
      }
   }

   private void addSourcesAbove(int p_285241_, int p_285212_, int p_284972_, int p_285134_) {
      int $$4 = SectionPos.blockToSectionCoord(p_285241_);
      int $$5 = SectionPos.blockToSectionCoord(p_285212_);
      int $$6 = Math.max(
         Math.max(this.getLowestSourceY(p_285241_ - 1, p_285212_, Integer.MIN_VALUE), this.getLowestSourceY(p_285241_ + 1, p_285212_, Integer.MIN_VALUE)),
         Math.max(this.getLowestSourceY(p_285241_, p_285212_ - 1, Integer.MIN_VALUE), this.getLowestSourceY(p_285241_, p_285212_ + 1, Integer.MIN_VALUE))
      );
      int $$7 = Math.max(p_284972_, p_285134_);

      for (long $$8 = SectionPos.asLong($$4, SectionPos.blockToSectionCoord($$7), $$5);
         !this.storage.isAboveData($$8);
         $$8 = SectionPos.offset($$8, Direction.UP)
      ) {
         if (this.storage.storingLightForSection($$8)) {
            int $$9 = SectionPos.sectionToBlockCoord(SectionPos.y($$8));
            int $$10 = $$9 + 15;

            for (int $$11 = Math.max($$9, $$7); $$11 <= $$10; $$11++) {
               long $$12 = BlockPos.asLong(p_285241_, $$11, p_285212_);
               if (isSourceLevel(this.storage.getStoredLevel($$12))) {
                  return;
               }

               this.storage.setStoredLevel($$12, 15);
               if ($$11 < $$6 || $$11 == p_284972_) {
                  this.enqueueIncrease($$12, ADD_SKY_SOURCE_ENTRY);
               }
            }
         }
      }
   }

   @Override
   protected void propagateIncrease(long p_285341_, long p_285204_, int p_285003_) {
      BlockState $$3 = null;
      int $$4 = this.countEmptySectionsBelowIfAtBorder(p_285341_);

      for (Direction $$5 : PROPAGATION_DIRECTIONS) {
         if (LightEngine.QueueEntry.shouldPropagateInDirection(p_285204_, $$5)) {
            long $$6 = BlockPos.offset(p_285341_, $$5);
            if (this.storage.storingLightForSection(SectionPos.blockToSection($$6))) {
               int $$7 = this.storage.getStoredLevel($$6);
               int $$8 = p_285003_ - 1;
               if ($$8 > $$7) {
                  this.mutablePos.set($$6);
                  BlockState $$9 = this.getState(this.mutablePos);
                  int $$10 = p_285003_ - this.getOpacity($$9, this.mutablePos);
                  if ($$10 > $$7) {
                     if ($$3 == null) {
                        $$3 = LightEngine.QueueEntry.isFromEmptyShape(p_285204_)
                           ? Blocks.AIR.defaultBlockState()
                           : this.getState(this.mutablePos.set(p_285341_));
                     }

                     if (!this.shapeOccludes(p_285341_, $$3, $$6, $$9, $$5)) {
                        this.storage.setStoredLevel($$6, $$10);
                        if ($$10 > 1) {
                           this.enqueueIncrease($$6, LightEngine.QueueEntry.increaseSkipOneDirection($$10, isEmptyShape($$9), $$5.getOpposite()));
                        }

                        this.propagateFromEmptySections($$6, $$5, $$10, true, $$4);
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   protected void propagateDecrease(long p_285015_, long p_285395_) {
      int $$2 = this.countEmptySectionsBelowIfAtBorder(p_285015_);
      int $$3 = LightEngine.QueueEntry.getFromLevel(p_285395_);

      for (Direction $$4 : PROPAGATION_DIRECTIONS) {
         if (LightEngine.QueueEntry.shouldPropagateInDirection(p_285395_, $$4)) {
            long $$5 = BlockPos.offset(p_285015_, $$4);
            if (this.storage.storingLightForSection(SectionPos.blockToSection($$5))) {
               int $$6 = this.storage.getStoredLevel($$5);
               if ($$6 != 0) {
                  if ($$6 <= $$3 - 1) {
                     this.storage.setStoredLevel($$5, 0);
                     this.enqueueDecrease($$5, LightEngine.QueueEntry.decreaseSkipOneDirection($$6, $$4.getOpposite()));
                     this.propagateFromEmptySections($$5, $$4, $$6, false, $$2);
                  } else {
                     this.enqueueIncrease($$5, LightEngine.QueueEntry.increaseOnlyOneDirection($$6, false, $$4.getOpposite()));
                  }
               }
            }
         }
      }
   }

   private int countEmptySectionsBelowIfAtBorder(long p_285356_) {
      int $$1 = BlockPos.getY(p_285356_);
      int $$2 = SectionPos.sectionRelative($$1);
      if ($$2 != 0) {
         return 0;
      } else {
         int $$3 = BlockPos.getX(p_285356_);
         int $$4 = BlockPos.getZ(p_285356_);
         int $$5 = SectionPos.sectionRelative($$3);
         int $$6 = SectionPos.sectionRelative($$4);
         if ($$5 != 0 && $$5 != 15 && $$6 != 0 && $$6 != 15) {
            return 0;
         } else {
            int $$7 = SectionPos.blockToSectionCoord($$3);
            int $$8 = SectionPos.blockToSectionCoord($$1);
            int $$9 = SectionPos.blockToSectionCoord($$4);
            int $$10 = 0;

            while (!this.storage.storingLightForSection(SectionPos.asLong($$7, $$8 - $$10 - 1, $$9)) && this.storage.hasLightDataAtOrBelow($$8 - $$10 - 1)) {
               $$10++;
            }

            return $$10;
         }
      }
   }

   private void propagateFromEmptySections(long p_284965_, Direction p_285308_, int p_284977_, boolean p_285001_, int p_285052_) {
      if (p_285052_ != 0) {
         int $$5 = BlockPos.getX(p_284965_);
         int $$6 = BlockPos.getZ(p_284965_);
         if (crossedSectionEdge(p_285308_, SectionPos.sectionRelative($$5), SectionPos.sectionRelative($$6))) {
            int $$7 = BlockPos.getY(p_284965_);
            int $$8 = SectionPos.blockToSectionCoord($$5);
            int $$9 = SectionPos.blockToSectionCoord($$6);
            int $$10 = SectionPos.blockToSectionCoord($$7) - 1;
            int $$11 = $$10 - p_285052_ + 1;

            while ($$10 >= $$11) {
               if (!this.storage.storingLightForSection(SectionPos.asLong($$8, $$10, $$9))) {
                  $$10--;
               } else {
                  int $$12 = SectionPos.sectionToBlockCoord($$10);

                  for (int $$13 = 15; $$13 >= 0; $$13--) {
                     long $$14 = BlockPos.asLong($$5, $$12 + $$13, $$6);
                     if (p_285001_) {
                        this.storage.setStoredLevel($$14, p_284977_);
                        if (p_284977_ > 1) {
                           this.enqueueIncrease($$14, LightEngine.QueueEntry.increaseSkipOneDirection(p_284977_, true, p_285308_.getOpposite()));
                        }
                     } else {
                        this.storage.setStoredLevel($$14, 0);
                        this.enqueueDecrease($$14, LightEngine.QueueEntry.decreaseSkipOneDirection(p_284977_, p_285308_.getOpposite()));
                     }
                  }

                  $$10--;
               }
            }
         }
      }
   }

   private static boolean crossedSectionEdge(Direction p_285014_, int p_284991_, int p_285468_) {
      return switch (p_285014_) {
         case NORTH -> p_285468_ == 15;
         case SOUTH -> p_285468_ == 0;
         case WEST -> p_284991_ == 15;
         case EAST -> p_284991_ == 0;
         default -> false;
      };
   }

   @Override
   public void setLightEnabled(ChunkPos p_285459_, boolean p_285013_) {
      super.setLightEnabled(p_285459_, p_285013_);
      if (p_285013_) {
         ChunkSkyLightSources $$2 = Objects.requireNonNullElse(this.getChunkSources(p_285459_.x, p_285459_.z), this.emptyChunkSources);
         int $$3 = $$2.getHighestLowestSourceY() - 1;
         int $$4 = SectionPos.blockToSectionCoord($$3) + 1;
         long $$5 = SectionPos.getZeroNode(p_285459_.x, p_285459_.z);
         int $$6 = this.storage.getTopSectionY($$5);
         int $$7 = Math.max(this.storage.getBottomSectionY(), $$4);

         for (int $$8 = $$6 - 1; $$8 >= $$7; $$8--) {
            DataLayer $$9 = this.storage.getDataLayerToWrite(SectionPos.asLong(p_285459_.x, $$8, p_285459_.z));
            if ($$9 != null && $$9.isEmpty()) {
               $$9.fill(15);
            }
         }
      }
   }

   @Override
   public void propagateLightSources(ChunkPos p_285333_) {
      long $$1 = SectionPos.getZeroNode(p_285333_.x, p_285333_.z);
      this.storage.setLightEnabled($$1, true);
      ChunkSkyLightSources $$2 = Objects.requireNonNullElse(this.getChunkSources(p_285333_.x, p_285333_.z), this.emptyChunkSources);
      ChunkSkyLightSources $$3 = Objects.requireNonNullElse(this.getChunkSources(p_285333_.x, p_285333_.z - 1), this.emptyChunkSources);
      ChunkSkyLightSources $$4 = Objects.requireNonNullElse(this.getChunkSources(p_285333_.x, p_285333_.z + 1), this.emptyChunkSources);
      ChunkSkyLightSources $$5 = Objects.requireNonNullElse(this.getChunkSources(p_285333_.x - 1, p_285333_.z), this.emptyChunkSources);
      ChunkSkyLightSources $$6 = Objects.requireNonNullElse(this.getChunkSources(p_285333_.x + 1, p_285333_.z), this.emptyChunkSources);
      int $$7 = this.storage.getTopSectionY($$1);
      int $$8 = this.storage.getBottomSectionY();
      int $$9 = SectionPos.sectionToBlockCoord(p_285333_.x);
      int $$10 = SectionPos.sectionToBlockCoord(p_285333_.z);

      for (int $$11 = $$7 - 1; $$11 >= $$8; $$11--) {
         long $$12 = SectionPos.asLong(p_285333_.x, $$11, p_285333_.z);
         DataLayer $$13 = this.storage.getDataLayerToWrite($$12);
         if ($$13 != null) {
            int $$14 = SectionPos.sectionToBlockCoord($$11);
            int $$15 = $$14 + 15;
            boolean $$16 = false;

            for (int $$17 = 0; $$17 < 16; $$17++) {
               for (int $$18 = 0; $$18 < 16; $$18++) {
                  int $$19 = $$2.getLowestSourceY($$18, $$17);
                  if ($$19 <= $$15) {
                     int $$20 = $$17 == 0 ? $$3.getLowestSourceY($$18, 15) : $$2.getLowestSourceY($$18, $$17 - 1);
                     int $$21 = $$17 == 15 ? $$4.getLowestSourceY($$18, 0) : $$2.getLowestSourceY($$18, $$17 + 1);
                     int $$22 = $$18 == 0 ? $$5.getLowestSourceY(15, $$17) : $$2.getLowestSourceY($$18 - 1, $$17);
                     int $$23 = $$18 == 15 ? $$6.getLowestSourceY(0, $$17) : $$2.getLowestSourceY($$18 + 1, $$17);
                     int $$24 = Math.max(Math.max($$20, $$21), Math.max($$22, $$23));

                     for (int $$25 = $$15; $$25 >= Math.max($$14, $$19); $$25--) {
                        $$13.set($$18, SectionPos.sectionRelative($$25), $$17, 15);
                        if ($$25 == $$19 || $$25 < $$24) {
                           long $$26 = BlockPos.asLong($$9 + $$18, $$25, $$10 + $$17);
                           this.enqueueIncrease(
                              $$26, LightEngine.QueueEntry.increaseSkySourceInDirections($$25 == $$19, $$25 < $$20, $$25 < $$21, $$25 < $$22, $$25 < $$23)
                           );
                        }
                     }

                     if ($$19 < $$14) {
                        $$16 = true;
                     }
                  }
               }
            }

            if (!$$16) {
               break;
            }
         }
      }
   }
}
