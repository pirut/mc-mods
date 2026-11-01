package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;

public abstract class LayerLightSectionStorage<M extends DataLayerStorageMap<M>> {
   private final LightLayer layer;
   protected final LightChunkGetter chunkSource;
   protected final Long2ByteMap sectionStates = new Long2ByteOpenHashMap();
   private final LongSet columnsWithSources = new LongOpenHashSet();
   protected volatile M visibleSectionData;
   protected final M updatingSectionData;
   protected final LongSet changedSections = new LongOpenHashSet();
   protected final LongSet sectionsAffectedByLightUpdates = new LongOpenHashSet();
   protected final Long2ObjectMap<DataLayer> queuedSections = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap());
   private final LongSet columnsToRetainQueuedDataFor = new LongOpenHashSet();
   private final LongSet toRemove = new LongOpenHashSet();
   protected volatile boolean hasInconsistencies;

   protected LayerLightSectionStorage(LightLayer p_75745_, LightChunkGetter p_75746_, M p_75747_) {
      this.layer = p_75745_;
      this.chunkSource = p_75746_;
      this.updatingSectionData = p_75747_;
      this.visibleSectionData = p_75747_.copy();
      this.visibleSectionData.disableCache();
      this.sectionStates.defaultReturnValue((byte)0);
   }

   protected boolean storingLightForSection(long p_75792_) {
      return this.getDataLayer(p_75792_, true) != null;
   }

   @Nullable
   protected DataLayer getDataLayer(long p_75759_, boolean p_75760_) {
      return this.getDataLayer(p_75760_ ? this.updatingSectionData : this.visibleSectionData, p_75759_);
   }

   @Nullable
   protected DataLayer getDataLayer(M p_75762_, long p_75763_) {
      return p_75762_.getLayer(p_75763_);
   }

   @Nullable
   protected DataLayer getDataLayerToWrite(long p_285278_) {
      DataLayer $$1 = this.updatingSectionData.getLayer(p_285278_);
      if ($$1 == null) {
         return null;
      } else {
         if (this.changedSections.add(p_285278_)) {
            $$1 = $$1.copy();
            this.updatingSectionData.setLayer(p_285278_, $$1);
            this.updatingSectionData.clearCache();
         }

         return $$1;
      }
   }

   @Nullable
   public DataLayer getDataLayerData(long p_75794_) {
      DataLayer $$1 = (DataLayer)this.queuedSections.get(p_75794_);
      return $$1 != null ? $$1 : this.getDataLayer(p_75794_, false);
   }

   protected abstract int getLightValue(long var1);

   protected int getStoredLevel(long p_75796_) {
      long $$1 = SectionPos.blockToSection(p_75796_);
      DataLayer $$2 = this.getDataLayer($$1, true);
      return $$2.get(
         SectionPos.sectionRelative(BlockPos.getX(p_75796_)),
         SectionPos.sectionRelative(BlockPos.getY(p_75796_)),
         SectionPos.sectionRelative(BlockPos.getZ(p_75796_))
      );
   }

   protected void setStoredLevel(long p_75773_, int p_75774_) {
      long $$2 = SectionPos.blockToSection(p_75773_);
      DataLayer $$3;
      if (this.changedSections.add($$2)) {
         $$3 = this.updatingSectionData.copyDataLayer($$2);
      } else {
         $$3 = this.getDataLayer($$2, true);
      }

      $$3.set(
         SectionPos.sectionRelative(BlockPos.getX(p_75773_)),
         SectionPos.sectionRelative(BlockPos.getY(p_75773_)),
         SectionPos.sectionRelative(BlockPos.getZ(p_75773_)),
         p_75774_
      );
      SectionPos.aroundAndAtBlockPos(p_75773_, this.sectionsAffectedByLightUpdates::add);
   }

   protected void markSectionAndNeighborsAsAffected(long p_281610_) {
      int $$1 = SectionPos.x(p_281610_);
      int $$2 = SectionPos.y(p_281610_);
      int $$3 = SectionPos.z(p_281610_);

      for (int $$4 = -1; $$4 <= 1; $$4++) {
         for (int $$5 = -1; $$5 <= 1; $$5++) {
            for (int $$6 = -1; $$6 <= 1; $$6++) {
               this.sectionsAffectedByLightUpdates.add(SectionPos.asLong($$1 + $$5, $$2 + $$6, $$3 + $$4));
            }
         }
      }
   }

   protected DataLayer createDataLayer(long p_75797_) {
      DataLayer $$1 = (DataLayer)this.queuedSections.get(p_75797_);
      return $$1 != null ? $$1 : new DataLayer();
   }

   protected boolean hasInconsistencies() {
      return this.hasInconsistencies;
   }

   protected void markNewInconsistencies(LightEngine<M, ?> p_285081_) {
      if (this.hasInconsistencies) {
         this.hasInconsistencies = false;
         LongIterator $$5 = this.toRemove.iterator();

         while ($$5.hasNext()) {
            long $$1 = (Long)$$5.next();
            DataLayer $$2 = (DataLayer)this.queuedSections.remove($$1);
            DataLayer $$3 = this.updatingSectionData.removeLayer($$1);
            if (this.columnsToRetainQueuedDataFor.contains(SectionPos.getZeroNode($$1))) {
               if ($$2 != null) {
                  this.queuedSections.put($$1, $$2);
               } else if ($$3 != null) {
                  this.queuedSections.put($$1, $$3);
               }
            }
         }

         this.updatingSectionData.clearCache();
         $$5 = this.toRemove.iterator();

         while ($$5.hasNext()) {
            long $$4 = (Long)$$5.next();
            this.onNodeRemoved($$4);
            this.changedSections.add($$4);
         }

         this.toRemove.clear();
         ObjectIterator<Entry<DataLayer>> $$5x = Long2ObjectMaps.fastIterator(this.queuedSections);

         while ($$5x.hasNext()) {
            Entry<DataLayer> $$6 = (Entry<DataLayer>)$$5x.next();
            long $$7 = $$6.getLongKey();
            if (this.storingLightForSection($$7)) {
               DataLayer $$8 = (DataLayer)$$6.getValue();
               if (this.updatingSectionData.getLayer($$7) != $$8) {
                  this.updatingSectionData.setLayer($$7, $$8);
                  this.changedSections.add($$7);
               }

               $$5x.remove();
            }
         }

         this.updatingSectionData.clearCache();
      }
   }

   protected void onNodeAdded(long p_75798_) {
   }

   protected void onNodeRemoved(long p_75799_) {
   }

   protected void setLightEnabled(long p_285065_, boolean p_284938_) {
      if (p_284938_) {
         this.columnsWithSources.add(p_285065_);
      } else {
         this.columnsWithSources.remove(p_285065_);
      }
   }

   protected boolean lightOnInSection(long p_285433_) {
      long $$1 = SectionPos.getZeroNode(p_285433_);
      return this.columnsWithSources.contains($$1);
   }

   public void retainData(long p_75783_, boolean p_75784_) {
      if (p_75784_) {
         this.columnsToRetainQueuedDataFor.add(p_75783_);
      } else {
         this.columnsToRetainQueuedDataFor.remove(p_75783_);
      }
   }

   protected void queueSectionData(long p_285403_, @Nullable DataLayer p_285498_) {
      if (p_285498_ != null) {
         this.queuedSections.put(p_285403_, p_285498_);
         this.hasInconsistencies = true;
      } else {
         this.queuedSections.remove(p_285403_);
      }
   }

   protected void updateSectionStatus(long p_75788_, boolean p_75789_) {
      byte $$2 = this.sectionStates.get(p_75788_);
      byte $$3 = LayerLightSectionStorage.SectionState.hasData($$2, !p_75789_);
      if ($$2 != $$3) {
         this.putSectionState(p_75788_, $$3);
         int $$4 = p_75789_ ? -1 : 1;

         for (int $$5 = -1; $$5 <= 1; $$5++) {
            for (int $$6 = -1; $$6 <= 1; $$6++) {
               for (int $$7 = -1; $$7 <= 1; $$7++) {
                  if ($$5 != 0 || $$6 != 0 || $$7 != 0) {
                     long $$8 = SectionPos.offset(p_75788_, $$5, $$6, $$7);
                     byte $$9 = this.sectionStates.get($$8);
                     this.putSectionState(
                        $$8, LayerLightSectionStorage.SectionState.neighborCount($$9, LayerLightSectionStorage.SectionState.neighborCount($$9) + $$4)
                     );
                  }
               }
            }
         }
      }
   }

   protected void putSectionState(long p_285451_, byte p_285078_) {
      if (p_285078_ != 0) {
         if (this.sectionStates.put(p_285451_, p_285078_) == 0) {
            this.initializeSection(p_285451_);
         }
      } else if (this.sectionStates.remove(p_285451_) != 0) {
         this.removeSection(p_285451_);
      }
   }

   private void initializeSection(long p_285124_) {
      if (!this.toRemove.remove(p_285124_)) {
         this.updatingSectionData.setLayer(p_285124_, this.createDataLayer(p_285124_));
         this.changedSections.add(p_285124_);
         this.onNodeAdded(p_285124_);
         this.markSectionAndNeighborsAsAffected(p_285124_);
         this.hasInconsistencies = true;
      }
   }

   private void removeSection(long p_285477_) {
      this.toRemove.add(p_285477_);
      this.hasInconsistencies = true;
   }

   protected void swapSectionMap() {
      if (!this.changedSections.isEmpty()) {
         M $$0 = this.updatingSectionData.copy();
         $$0.disableCache();
         this.visibleSectionData = $$0;
         this.changedSections.clear();
      }

      if (!this.sectionsAffectedByLightUpdates.isEmpty()) {
         LongIterator $$1 = this.sectionsAffectedByLightUpdates.iterator();

         while ($$1.hasNext()) {
            long $$2 = $$1.nextLong();
            this.chunkSource.onLightUpdate(this.layer, SectionPos.of($$2));
         }

         this.sectionsAffectedByLightUpdates.clear();
      }
   }

   public LayerLightSectionStorage.SectionType getDebugSectionType(long p_285114_) {
      return LayerLightSectionStorage.SectionState.type(this.sectionStates.get(p_285114_));
   }

   protected static class SectionState {
      public static final byte EMPTY = 0;
      private static final int MIN_NEIGHBORS = 0;
      private static final int MAX_NEIGHBORS = 26;
      private static final byte HAS_DATA_BIT = 32;
      private static final byte NEIGHBOR_COUNT_BITS = 31;

      public static byte hasData(byte p_284954_, boolean p_285420_) {
         return (byte)(p_285420_ ? p_284954_ | 32 : p_284954_ & -33);
      }

      public static byte neighborCount(byte p_285516_, int p_285426_) {
         if (p_285426_ >= 0 && p_285426_ <= 26) {
            return (byte)(p_285516_ & -32 | p_285426_ & 31);
         } else {
            throw new IllegalArgumentException("Neighbor count was not within range [0; 26]");
         }
      }

      public static boolean hasData(byte p_285105_) {
         return (p_285105_ & 32) != 0;
      }

      public static int neighborCount(byte p_285437_) {
         return p_285437_ & 31;
      }

      public static LayerLightSectionStorage.SectionType type(byte p_285064_) {
         if (p_285064_ == 0) {
            return LayerLightSectionStorage.SectionType.EMPTY;
         } else {
            return hasData(p_285064_) ? LayerLightSectionStorage.SectionType.LIGHT_AND_DATA : LayerLightSectionStorage.SectionType.LIGHT_ONLY;
         }
      }
   }

   public static enum SectionType {
      EMPTY("2"),
      LIGHT_ONLY("1"),
      LIGHT_AND_DATA("0");

      private final String display;

      private SectionType(String p_285063_) {
         this.display = p_285063_;
      }

      public String display() {
         return this.display;
      }
   }
}
