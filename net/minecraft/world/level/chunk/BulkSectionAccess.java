package net.minecraft.world.level.chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class BulkSectionAccess implements AutoCloseable {
   private final LevelAccessor level;
   private final Long2ObjectMap<LevelChunkSection> acquiredSections = new Long2ObjectOpenHashMap();
   @Nullable
   private LevelChunkSection lastSection;
   private long lastSectionKey;

   public BulkSectionAccess(LevelAccessor p_156103_) {
      this.level = p_156103_;
   }

   @Nullable
   public LevelChunkSection getSection(BlockPos p_156105_) {
      int $$1 = this.level.getSectionIndex(p_156105_.getY());
      if ($$1 >= 0 && $$1 < this.level.getSectionsCount()) {
         long $$2 = SectionPos.asLong(p_156105_);
         if (this.lastSection == null || this.lastSectionKey != $$2) {
            this.lastSection = (LevelChunkSection)this.acquiredSections.computeIfAbsent($$2, p_156109_ -> {
               ChunkAccess $$3 = this.level.getChunk(SectionPos.blockToSectionCoord(p_156105_.getX()), SectionPos.blockToSectionCoord(p_156105_.getZ()));
               LevelChunkSection $$4 = $$3.getSection($$1);
               $$4.acquire();
               return $$4;
            });
            this.lastSectionKey = $$2;
         }

         return this.lastSection;
      } else {
         return null;
      }
   }

   public BlockState getBlockState(BlockPos p_156111_) {
      LevelChunkSection $$1 = this.getSection(p_156111_);
      if ($$1 == null) {
         return Blocks.AIR.defaultBlockState();
      } else {
         int $$2 = SectionPos.sectionRelative(p_156111_.getX());
         int $$3 = SectionPos.sectionRelative(p_156111_.getY());
         int $$4 = SectionPos.sectionRelative(p_156111_.getZ());
         return $$1.getBlockState($$2, $$3, $$4);
      }
   }

   @Override
   public void close() {
      ObjectIterator var1 = this.acquiredSections.values().iterator();

      while (var1.hasNext()) {
         LevelChunkSection $$0 = (LevelChunkSection)var1.next();
         $$0.release();
      }
   }
}
