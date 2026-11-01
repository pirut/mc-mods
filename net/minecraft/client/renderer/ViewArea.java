package net.minecraft.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;

@Environment(EnvType.CLIENT)
public class ViewArea {
   protected final LevelRenderer levelRenderer;
   protected final Level level;
   protected int sectionGridSizeY;
   protected int sectionGridSizeX;
   protected int sectionGridSizeZ;
   private int viewDistance;
   private SectionPos cameraSectionPos;
   public SectionRenderDispatcher.RenderSection[] sections;

   public ViewArea(SectionRenderDispatcher sectionRenderDispatcher, Level level, int viewDistance, LevelRenderer levelRenderer) {
      this.levelRenderer = levelRenderer;
      this.level = level;
      this.setViewDistance(viewDistance);
      this.createSections(sectionRenderDispatcher);
      this.cameraSectionPos = SectionPos.of(this.viewDistance + 1, 0, this.viewDistance + 1);
   }

   protected void createSections(SectionRenderDispatcher sectionRenderDispatcher) {
      if (!Minecraft.getInstance().isSameThread()) {
         throw new IllegalStateException("createSections called from wrong thread: " + Thread.currentThread().getName());
      } else {
         int i = this.sectionGridSizeX * this.sectionGridSizeY * this.sectionGridSizeZ;
         this.sections = new SectionRenderDispatcher.RenderSection[i];

         for (int j = 0; j < this.sectionGridSizeX; j++) {
            for (int k = 0; k < this.sectionGridSizeY; k++) {
               for (int l = 0; l < this.sectionGridSizeZ; l++) {
                  int m = this.getSectionIndex(j, k, l);
                  this.sections[m] = sectionRenderDispatcher.new RenderSection(m, SectionPos.asLong(j, k + this.level.getMinSectionY(), l));
               }
            }
         }
      }
   }

   public void releaseAllBuffers() {
      for (SectionRenderDispatcher.RenderSection renderSection : this.sections) {
         renderSection.reset();
      }
   }

   private int getSectionIndex(int x, int y, int z) {
      return (z * this.sectionGridSizeY + y) * this.sectionGridSizeX + x;
   }

   protected void setViewDistance(int renderDistanceChunks) {
      int i = renderDistanceChunks * 2 + 1;
      this.sectionGridSizeX = i;
      this.sectionGridSizeY = this.level.getSectionsCount();
      this.sectionGridSizeZ = i;
      this.viewDistance = renderDistanceChunks;
   }

   public int getViewDistance() {
      return this.viewDistance;
   }

   public LevelHeightAccessor getLevelHeightAccessor() {
      return this.level;
   }

   public void repositionCamera(SectionPos newSectionPos) {
      for (int i = 0; i < this.sectionGridSizeX; i++) {
         int j = newSectionPos.x() - this.viewDistance;
         int k = j + Math.floorMod(i - j, this.sectionGridSizeX);

         for (int l = 0; l < this.sectionGridSizeZ; l++) {
            int m = newSectionPos.z() - this.viewDistance;
            int n = m + Math.floorMod(l - m, this.sectionGridSizeZ);

            for (int o = 0; o < this.sectionGridSizeY; o++) {
               int p = this.level.getMinSectionY() + o;
               SectionRenderDispatcher.RenderSection renderSection = this.sections[this.getSectionIndex(i, o, l)];
               long q = renderSection.getSectionNode();
               if (q != SectionPos.asLong(k, p, n)) {
                  renderSection.setSectionNode(SectionPos.asLong(k, p, n));
               }
            }
         }
      }

      this.cameraSectionPos = newSectionPos;
      this.levelRenderer.getSectionOcclusionGraph().invalidate();
   }

   public SectionPos getCameraSectionPos() {
      return this.cameraSectionPos;
   }

   public void setDirty(int sectionX, int sectionY, int sectionZ, boolean reRenderOnMainThread) {
      SectionRenderDispatcher.RenderSection renderSection = this.getRenderSection(sectionX, sectionY, sectionZ);
      if (renderSection != null) {
         renderSection.setDirty(reRenderOnMainThread);
      }
   }

   protected SectionRenderDispatcher.RenderSection getRenderSectionAt(BlockPos pos) {
      return this.getRenderSection(SectionPos.asLong(pos));
   }

   protected SectionRenderDispatcher.RenderSection getRenderSection(long sectionPos) {
      int i = SectionPos.x(sectionPos);
      int j = SectionPos.y(sectionPos);
      int k = SectionPos.z(sectionPos);
      return this.getRenderSection(i, j, k);
   }

   private SectionRenderDispatcher.RenderSection getRenderSection(int x, int y, int z) {
      if (!this.containsSection(x, y, z)) {
         return null;
      } else {
         int i = y - this.level.getMinSectionY();
         int j = Math.floorMod(x, this.sectionGridSizeX);
         int k = Math.floorMod(z, this.sectionGridSizeZ);
         return this.sections[this.getSectionIndex(j, i, k)];
      }
   }

   private boolean containsSection(int x, int y, int z) {
      if (y >= this.level.getMinSectionY() && y <= this.level.getMaxSectionY()) {
         return x < this.cameraSectionPos.x() - this.viewDistance || x > this.cameraSectionPos.x() + this.viewDistance
            ? false
            : z >= this.cameraSectionPos.z() - this.viewDistance && z <= this.cameraSectionPos.z() + this.viewDistance;
      } else {
         return false;
      }
   }
}
