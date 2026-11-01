package net.minecraft.client.renderer.chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.model.data.ModelData;

@OnlyIn(Dist.CLIENT)
public class RenderRegionCache {
   private final Long2ObjectMap<SectionCopy> sectionCopyCache = new Long2ObjectOpenHashMap();

   public RenderSectionRegion createRegion(Level p_200466_, long p_427252_) {
      int i = SectionPos.x(p_427252_);
      int j = SectionPos.y(p_427252_);
      int k = SectionPos.z(p_427252_);
      int l = i - 1;
      int i1 = j - 1;
      int j1 = k - 1;
      int k1 = i + 1;
      int l1 = j + 1;
      int i2 = k + 1;
      SectionCopy[] asectioncopy = new SectionCopy[27];

      for (int j2 = j1; j2 <= i2; j2++) {
         for (int k2 = i1; k2 <= l1; k2++) {
            for (int l2 = l; l2 <= k1; l2++) {
               int i3 = RenderSectionRegion.index(l, i1, j1, l2, k2, j2);
               asectioncopy[i3] = this.getSectionDataCopy(p_200466_, l2, k2, j2);
            }
         }
      }

      Long2ObjectFunction<ModelData> modelDataManager = p_200466_.getModelDataManager().snapshotSectionRegion(l, i1, j1, k1, l1, i2);
      return new RenderSectionRegion(p_200466_, l, i1, j1, asectioncopy, modelDataManager);
   }

   private SectionCopy getSectionDataCopy(Level p_427240_, int p_427457_, int p_427278_, int p_427229_) {
      return (SectionCopy)this.sectionCopyCache.computeIfAbsent(SectionPos.asLong(p_427457_, p_427278_, p_427229_), p_426915_ -> {
         LevelChunk levelchunk = p_427240_.getChunk(p_427457_, p_427229_);
         return new SectionCopy(levelchunk, levelchunk.getSectionIndexFromSectionY(p_427278_));
      });
   }
}
