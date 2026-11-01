package net.minecraft.client.renderer.chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

@Environment(EnvType.CLIENT)
public class RenderRegionCache {
   private final Long2ObjectMap<SectionCopy> sectionCopyCache = new Long2ObjectOpenHashMap();

   public RenderSectionRegion createRegion(Level level, long chunkPos) {
      int i = SectionPos.x(chunkPos);
      int j = SectionPos.y(chunkPos);
      int k = SectionPos.z(chunkPos);
      int l = i - 1;
      int m = j - 1;
      int n = k - 1;
      int o = i + 1;
      int p = j + 1;
      int q = k + 1;
      SectionCopy[] sectionCopys = new SectionCopy[27];

      for (int r = n; r <= q; r++) {
         for (int s = m; s <= p; s++) {
            for (int t = l; t <= o; t++) {
               int u = RenderSectionRegion.index(l, m, n, t, s, r);
               sectionCopys[u] = this.getSectionDataCopy(level, t, s, r);
            }
         }
      }

      return new RenderSectionRegion(level, l, m, n, sectionCopys);
   }

   private SectionCopy getSectionDataCopy(Level level, int x, int y, int z) {
      return (SectionCopy)this.sectionCopyCache.computeIfAbsent(SectionPos.asLong(x, y, z), l -> {
         LevelChunk levelChunk = level.getChunk(x, z);
         return new SectionCopy(levelChunk, levelChunk.getSectionIndexFromSectionY(y));
      });
   }
}
