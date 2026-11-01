package net.minecraft.world.level.chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BulkSectionAccess implements AutoCloseable {
   private final LevelAccessor level;
   private final Long2ObjectMap<LevelChunkSection> acquiredSections = new Long2ObjectOpenHashMap();
   @Nullable
   private LevelChunkSection lastSection;
   private long lastSectionKey;

   public BulkSectionAccess(LevelAccessor level) {
      this.level = level;
   }

   @Nullable
   public LevelChunkSection getSection(BlockPos pos) {
      int i = this.level.getSectionIndex(pos.getY());
      if (i >= 0 && i < this.level.getSectionsCount()) {
         long l = SectionPos.asLong(pos);
         if (this.lastSection == null || this.lastSectionKey != l) {
            this.lastSection = (LevelChunkSection)this.acquiredSections.computeIfAbsent(l, lx -> {
               ChunkAccess chunkAccess = this.level.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
               LevelChunkSection levelChunkSection = chunkAccess.getSection(i);
               levelChunkSection.acquire();
               return levelChunkSection;
            });
            this.lastSectionKey = l;
         }

         return this.lastSection;
      } else {
         return null;
      }
   }

   public BlockState getBlockState(BlockPos pos) {
      LevelChunkSection levelChunkSection = this.getSection(pos);
      if (levelChunkSection == null) {
         return Blocks.AIR.defaultBlockState();
      } else {
         int i = SectionPos.sectionRelative(pos.getX());
         int j = SectionPos.sectionRelative(pos.getY());
         int k = SectionPos.sectionRelative(pos.getZ());
         return levelChunkSection.getBlockState(i, j, k);
      }
   }

   @Override
   public void close() {
      ObjectIterator var1 = this.acquiredSections.values().iterator();

      while (var1.hasNext()) {
         LevelChunkSection levelChunkSection = (LevelChunkSection)var1.next();
         levelChunkSection.release();
      }
   }
}
