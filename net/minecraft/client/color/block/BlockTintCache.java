package net.minecraft.client.color.block;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.ToIntFunction;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockTintCache {
   private static final int MAX_CACHE_ENTRIES = 256;
   private final ThreadLocal<BlockTintCache.LatestCacheInfo> latestChunkOnThread = ThreadLocal.withInitial(BlockTintCache.LatestCacheInfo::new);
   private final Long2ObjectLinkedOpenHashMap<BlockTintCache.CacheData> cache = new Long2ObjectLinkedOpenHashMap(256, 0.25F);
   private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
   private final ToIntFunction<BlockPos> source;

   public BlockTintCache(ToIntFunction<BlockPos> p_193811_) {
      this.source = p_193811_;
   }

   public int getColor(BlockPos p_193813_) {
      int $$1 = SectionPos.blockToSectionCoord(p_193813_.getX());
      int $$2 = SectionPos.blockToSectionCoord(p_193813_.getZ());
      BlockTintCache.LatestCacheInfo $$3 = this.latestChunkOnThread.get();
      if ($$3.x != $$1 || $$3.z != $$2 || $$3.cache == null || $$3.cache.isInvalidated()) {
         $$3.x = $$1;
         $$3.z = $$2;
         $$3.cache = this.findOrCreateChunkCache($$1, $$2);
      }

      int[] $$4 = $$3.cache.getLayer(p_193813_.getY());
      int $$5 = p_193813_.getX() & 15;
      int $$6 = p_193813_.getZ() & 15;
      int $$7 = $$6 << 4 | $$5;
      int $$8 = $$4[$$7];
      if ($$8 != -1) {
         return $$8;
      } else {
         int $$9 = this.source.applyAsInt(p_193813_);
         $$4[$$7] = $$9;
         return $$9;
      }
   }

   public void invalidateForChunk(int p_92656_, int p_92657_) {
      try {
         this.lock.writeLock().lock();

         for (int $$2 = -1; $$2 <= 1; $$2++) {
            for (int $$3 = -1; $$3 <= 1; $$3++) {
               long $$4 = ChunkPos.asLong(p_92656_ + $$2, p_92657_ + $$3);
               BlockTintCache.CacheData $$5 = (BlockTintCache.CacheData)this.cache.remove($$4);
               if ($$5 != null) {
                  $$5.invalidate();
               }
            }
         }
      } finally {
         this.lock.writeLock().unlock();
      }
   }

   public void invalidateAll() {
      try {
         this.lock.writeLock().lock();
         this.cache.values().forEach(BlockTintCache.CacheData::invalidate);
         this.cache.clear();
      } finally {
         this.lock.writeLock().unlock();
      }
   }

   private BlockTintCache.CacheData findOrCreateChunkCache(int p_193815_, int p_193816_) {
      long $$2 = ChunkPos.asLong(p_193815_, p_193816_);
      this.lock.readLock().lock();

      try {
         BlockTintCache.CacheData $$3 = (BlockTintCache.CacheData)this.cache.get($$2);
         if ($$3 != null) {
            return $$3;
         }
      } finally {
         this.lock.readLock().unlock();
      }

      this.lock.writeLock().lock();

      BlockTintCache.CacheData $$5;
      try {
         BlockTintCache.CacheData $$4 = (BlockTintCache.CacheData)this.cache.get($$2);
         if ($$4 == null) {
            $$5 = new BlockTintCache.CacheData();
            if (this.cache.size() >= 256) {
               BlockTintCache.CacheData $$6 = (BlockTintCache.CacheData)this.cache.removeFirst();
               if ($$6 != null) {
                  $$6.invalidate();
               }
            }

            this.cache.put($$2, $$5);
            return $$5;
         }

         $$5 = $$4;
      } finally {
         this.lock.writeLock().unlock();
      }

      return $$5;
   }

   @OnlyIn(Dist.CLIENT)
   static class CacheData {
      private final Int2ObjectArrayMap<int[]> cache = new Int2ObjectArrayMap(16);
      private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
      private static final int BLOCKS_PER_LAYER = Mth.square(16);
      private volatile boolean invalidated;

      public int[] getLayer(int p_193824_) {
         this.lock.readLock().lock();

         try {
            int[] $$1 = (int[])this.cache.get(p_193824_);
            if ($$1 != null) {
               return $$1;
            }
         } finally {
            this.lock.readLock().unlock();
         }

         this.lock.writeLock().lock();

         int[] var12;
         try {
            var12 = (int[])this.cache.computeIfAbsent(p_193824_, p_193826_ -> this.allocateLayer());
         } finally {
            this.lock.writeLock().unlock();
         }

         return var12;
      }

      private int[] allocateLayer() {
         int[] $$0 = new int[BLOCKS_PER_LAYER];
         Arrays.fill($$0, -1);
         return $$0;
      }

      public boolean isInvalidated() {
         return this.invalidated;
      }

      public void invalidate() {
         this.invalidated = true;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class LatestCacheInfo {
      public int x = Integer.MIN_VALUE;
      public int z = Integer.MIN_VALUE;
      @Nullable
      BlockTintCache.CacheData cache;

      private LatestCacheInfo() {
      }
   }
}
