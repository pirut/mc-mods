package net.minecraft.client.renderer;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ViewArea {
   protected final LevelRenderer levelRenderer;
   protected final Level level;
   protected int sectionGridSizeY;
   protected int sectionGridSizeX;
   protected int sectionGridSizeZ;
   private int viewDistance;
   public SectionRenderDispatcher.RenderSection[] sections;

   public ViewArea(SectionRenderDispatcher p_296460_, Level p_110846_, int p_110847_, LevelRenderer p_110848_) {
      this.levelRenderer = p_110848_;
      this.level = p_110846_;
      this.setViewDistance(p_110847_);
      this.createSections(p_296460_);
   }

   protected void createSections(SectionRenderDispatcher p_294825_) {
      if (!Minecraft.getInstance().isSameThread()) {
         throw new IllegalStateException("createSections called from wrong thread: " + Thread.currentThread().getName());
      } else {
         int $$1 = this.sectionGridSizeX * this.sectionGridSizeY * this.sectionGridSizeZ;
         this.sections = new SectionRenderDispatcher.RenderSection[$$1];

         for (int $$2 = 0; $$2 < this.sectionGridSizeX; $$2++) {
            for (int $$3 = 0; $$3 < this.sectionGridSizeY; $$3++) {
               for (int $$4 = 0; $$4 < this.sectionGridSizeZ; $$4++) {
                  int $$5 = this.getSectionIndex($$2, $$3, $$4);
                  this.sections[$$5] = p_294825_.new RenderSection($$5, $$2 * 16, this.level.getMinBuildHeight() + $$3 * 16, $$4 * 16);
               }
            }
         }
      }
   }

   public void releaseAllBuffers() {
      for (SectionRenderDispatcher.RenderSection $$0 : this.sections) {
         $$0.releaseBuffers();
      }
   }

   private int getSectionIndex(int p_295999_, int p_294097_, int p_294966_) {
      return (p_294966_ * this.sectionGridSizeY + p_294097_) * this.sectionGridSizeX + p_295999_;
   }

   protected void setViewDistance(int p_110854_) {
      int $$1 = p_110854_ * 2 + 1;
      this.sectionGridSizeX = $$1;
      this.sectionGridSizeY = this.level.getSectionsCount();
      this.sectionGridSizeZ = $$1;
      this.viewDistance = p_110854_;
   }

   public int getViewDistance() {
      return this.viewDistance;
   }

   public LevelHeightAccessor getLevelHeightAccessor() {
      return this.level;
   }

   public void repositionCamera(double p_110851_, double p_110852_) {
      int $$2 = Mth.ceil(p_110851_);
      int $$3 = Mth.ceil(p_110852_);

      for (int $$4 = 0; $$4 < this.sectionGridSizeX; $$4++) {
         int $$5 = this.sectionGridSizeX * 16;
         int $$6 = $$2 - 8 - $$5 / 2;
         int $$7 = $$6 + Math.floorMod($$4 * 16 - $$6, $$5);

         for (int $$8 = 0; $$8 < this.sectionGridSizeZ; $$8++) {
            int $$9 = this.sectionGridSizeZ * 16;
            int $$10 = $$3 - 8 - $$9 / 2;
            int $$11 = $$10 + Math.floorMod($$8 * 16 - $$10, $$9);

            for (int $$12 = 0; $$12 < this.sectionGridSizeY; $$12++) {
               int $$13 = this.level.getMinBuildHeight() + $$12 * 16;
               SectionRenderDispatcher.RenderSection $$14 = this.sections[this.getSectionIndex($$4, $$12, $$8)];
               BlockPos $$15 = $$14.getOrigin();
               if ($$7 != $$15.getX() || $$13 != $$15.getY() || $$11 != $$15.getZ()) {
                  $$14.setOrigin($$7, $$13, $$11);
               }
            }
         }
      }
   }

   public void setDirty(int p_110860_, int p_110861_, int p_110862_, boolean p_110863_) {
      int $$4 = Math.floorMod(p_110860_, this.sectionGridSizeX);
      int $$5 = Math.floorMod(p_110861_ - this.level.getMinSection(), this.sectionGridSizeY);
      int $$6 = Math.floorMod(p_110862_, this.sectionGridSizeZ);
      SectionRenderDispatcher.RenderSection $$7 = this.sections[this.getSectionIndex($$4, $$5, $$6)];
      $$7.setDirty(p_110863_);
   }

   @Nullable
   protected SectionRenderDispatcher.RenderSection getRenderSectionAt(BlockPos p_294239_) {
      int $$1 = Mth.floorDiv(p_294239_.getY() - this.level.getMinBuildHeight(), 16);
      if ($$1 >= 0 && $$1 < this.sectionGridSizeY) {
         int $$2 = Mth.positiveModulo(Mth.floorDiv(p_294239_.getX(), 16), this.sectionGridSizeX);
         int $$3 = Mth.positiveModulo(Mth.floorDiv(p_294239_.getZ(), 16), this.sectionGridSizeZ);
         return this.sections[this.getSectionIndex($$2, $$1, $$3)];
      } else {
         return null;
      }
   }
}
