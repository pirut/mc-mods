package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.world.level.chunk.DataLayer;

public abstract class DataLayerStorageMap<M extends DataLayerStorageMap<M>> {
   private static final int CACHE_SIZE = 2;
   private final long[] lastSectionKeys = new long[2];
   private final DataLayer[] lastSections = new DataLayer[2];
   private boolean cacheEnabled;
   protected final Long2ObjectOpenHashMap<DataLayer> map;

   protected DataLayerStorageMap(Long2ObjectOpenHashMap<DataLayer> p_75523_) {
      this.map = p_75523_;
      this.clearCache();
      this.cacheEnabled = true;
   }

   public abstract M copy();

   public DataLayer copyDataLayer(long p_281841_) {
      DataLayer $$1 = ((DataLayer)this.map.get(p_281841_)).copy();
      this.map.put(p_281841_, $$1);
      this.clearCache();
      return $$1;
   }

   public boolean hasLayer(long p_75530_) {
      return this.map.containsKey(p_75530_);
   }

   @Nullable
   public DataLayer getLayer(long p_75533_) {
      if (this.cacheEnabled) {
         for (int $$1 = 0; $$1 < 2; $$1++) {
            if (p_75533_ == this.lastSectionKeys[$$1]) {
               return this.lastSections[$$1];
            }
         }
      }

      DataLayer $$2 = (DataLayer)this.map.get(p_75533_);
      if ($$2 == null) {
         return null;
      } else {
         if (this.cacheEnabled) {
            for (int $$3 = 1; $$3 > 0; $$3--) {
               this.lastSectionKeys[$$3] = this.lastSectionKeys[$$3 - 1];
               this.lastSections[$$3] = this.lastSections[$$3 - 1];
            }

            this.lastSectionKeys[0] = p_75533_;
            this.lastSections[0] = $$2;
         }

         return $$2;
      }
   }

   @Nullable
   public DataLayer removeLayer(long p_75536_) {
      return (DataLayer)this.map.remove(p_75536_);
   }

   public void setLayer(long p_75527_, DataLayer p_75528_) {
      this.map.put(p_75527_, p_75528_);
   }

   public void clearCache() {
      for (int $$0 = 0; $$0 < 2; $$0++) {
         this.lastSectionKeys[$$0] = Long.MAX_VALUE;
         this.lastSections[$$0] = null;
      }
   }

   public void disableCache() {
      this.cacheEnabled = false;
   }
}
