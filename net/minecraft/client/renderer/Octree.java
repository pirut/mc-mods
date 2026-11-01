package net.minecraft.client.renderer;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Octree {
   private final Octree.Branch root;
   final BlockPos cameraSectionCenter;

   public Octree(SectionPos p_366555_, int p_366900_, int p_366696_, int p_366804_) {
      int i = p_366900_ * 2 + 1;
      int j = Mth.smallestEncompassingPowerOfTwo(i);
      int k = p_366900_ * 16;
      BlockPos blockpos = p_366555_.origin();
      this.cameraSectionCenter = p_366555_.center();
      int l = blockpos.getX() - k;
      int i1 = l + j * 16 - 1;
      int j1 = j >= p_366696_ ? p_366804_ : blockpos.getY() - k;
      int k1 = j1 + j * 16 - 1;
      int l1 = blockpos.getZ() - k;
      int i2 = l1 + j * 16 - 1;
      this.root = new Octree.Branch(new BoundingBox(l, j1, l1, i1, k1, i2));
   }

   public boolean add(SectionRenderDispatcher.RenderSection p_366660_) {
      return this.root.add(p_366660_);
   }

   public void visitNodes(Octree.OctreeVisitor p_366457_, Frustum p_366899_, int p_371758_) {
      this.root.visitNodes(p_366457_, false, p_366899_, 0, p_371758_, true);
   }

   boolean isClose(double p_371839_, double p_371388_, double p_371925_, double p_371916_, double p_371235_, double p_371502_, int p_371637_) {
      int i = this.cameraSectionCenter.getX();
      int j = this.cameraSectionCenter.getY();
      int k = this.cameraSectionCenter.getZ();
      return i > p_371839_ - p_371637_
         && i < p_371916_ + p_371637_
         && j > p_371388_ - p_371637_
         && j < p_371235_ + p_371637_
         && k > p_371925_ - p_371637_
         && k < p_371502_ + p_371637_;
   }

   @OnlyIn(Dist.CLIENT)
   static enum AxisSorting {
      XYZ(4, 2, 1),
      XZY(4, 1, 2),
      YXZ(2, 4, 1),
      YZX(1, 4, 2),
      ZXY(2, 1, 4),
      ZYX(1, 2, 4);

      final int xShift;
      final int yShift;
      final int zShift;

      private AxisSorting(int p_366534_, int p_366465_, int p_366528_) {
         this.xShift = p_366534_;
         this.yShift = p_366465_;
         this.zShift = p_366528_;
      }

      public static Octree.AxisSorting getAxisSorting(int p_366469_, int p_366599_, int p_366616_) {
         if (p_366469_ > p_366599_ && p_366469_ > p_366616_) {
            return p_366599_ > p_366616_ ? XYZ : XZY;
         } else if (p_366599_ > p_366469_ && p_366599_ > p_366616_) {
            return p_366469_ > p_366616_ ? YXZ : YZX;
         } else {
            return p_366469_ > p_366599_ ? ZXY : ZYX;
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   class Branch implements Octree.Node {
      private final Octree.Node[] nodes = new Octree.Node[8];
      private final BoundingBox boundingBox;
      private final int bbCenterX;
      private final int bbCenterY;
      private final int bbCenterZ;
      private final Octree.AxisSorting sorting;
      private final boolean cameraXDiffNegative;
      private final boolean cameraYDiffNegative;
      private final boolean cameraZDiffNegative;

      public Branch(BoundingBox p_366797_) {
         this.boundingBox = p_366797_;
         this.bbCenterX = this.boundingBox.minX() + this.boundingBox.getXSpan() / 2;
         this.bbCenterY = this.boundingBox.minY() + this.boundingBox.getYSpan() / 2;
         this.bbCenterZ = this.boundingBox.minZ() + this.boundingBox.getZSpan() / 2;
         int i = Octree.this.cameraSectionCenter.getX() - this.bbCenterX;
         int j = Octree.this.cameraSectionCenter.getY() - this.bbCenterY;
         int k = Octree.this.cameraSectionCenter.getZ() - this.bbCenterZ;
         this.sorting = Octree.AxisSorting.getAxisSorting(Math.abs(i), Math.abs(j), Math.abs(k));
         this.cameraXDiffNegative = i < 0;
         this.cameraYDiffNegative = j < 0;
         this.cameraZDiffNegative = k < 0;
      }

      public boolean add(SectionRenderDispatcher.RenderSection p_366697_) {
         long i = p_366697_.getSectionNode();
         boolean flag = SectionPos.sectionToBlockCoord(SectionPos.x(i)) - this.bbCenterX < 0;
         boolean flag1 = SectionPos.sectionToBlockCoord(SectionPos.y(i)) - this.bbCenterY < 0;
         boolean flag2 = SectionPos.sectionToBlockCoord(SectionPos.z(i)) - this.bbCenterZ < 0;
         boolean flag3 = flag != this.cameraXDiffNegative;
         boolean flag4 = flag1 != this.cameraYDiffNegative;
         boolean flag5 = flag2 != this.cameraZDiffNegative;
         int j = getNodeIndex(this.sorting, flag3, flag4, flag5);
         if (this.areChildrenLeaves()) {
            boolean flag6 = this.nodes[j] != null;
            this.nodes[j] = Octree.this.new Leaf(p_366697_);
            return !flag6;
         } else if (this.nodes[j] != null) {
            Octree.Branch octree$branch1 = (Octree.Branch)this.nodes[j];
            return octree$branch1.add(p_366697_);
         } else {
            BoundingBox boundingbox = this.createChildBoundingBox(flag, flag1, flag2);
            Octree.Branch octree$branch = Octree.this.new Branch(boundingbox);
            this.nodes[j] = octree$branch;
            return octree$branch.add(p_366697_);
         }
      }

      private static int getNodeIndex(Octree.AxisSorting p_366829_, boolean p_366579_, boolean p_366439_, boolean p_366724_) {
         int i = 0;
         if (p_366579_) {
            i += p_366829_.xShift;
         }

         if (p_366439_) {
            i += p_366829_.yShift;
         }

         if (p_366724_) {
            i += p_366829_.zShift;
         }

         return i;
      }

      private boolean areChildrenLeaves() {
         return this.boundingBox.getXSpan() == 32;
      }

      private BoundingBox createChildBoundingBox(boolean p_366433_, boolean p_366456_, boolean p_366831_) {
         int i;
         int j;
         if (p_366433_) {
            i = this.boundingBox.minX();
            j = this.bbCenterX - 1;
         } else {
            i = this.bbCenterX;
            j = this.boundingBox.maxX();
         }

         int k;
         int l;
         if (p_366456_) {
            k = this.boundingBox.minY();
            l = this.bbCenterY - 1;
         } else {
            k = this.bbCenterY;
            l = this.boundingBox.maxY();
         }

         int i1;
         int j1;
         if (p_366831_) {
            i1 = this.boundingBox.minZ();
            j1 = this.bbCenterZ - 1;
         } else {
            i1 = this.bbCenterZ;
            j1 = this.boundingBox.maxZ();
         }

         return new BoundingBox(i, k, i1, j, l, j1);
      }

      @Override
      public void visitNodes(Octree.OctreeVisitor p_366879_, boolean p_366787_, Frustum p_366666_, int p_366756_, int p_371411_, boolean p_371398_) {
         boolean flag = p_366787_;
         if (!p_366787_) {
            int i = p_366666_.cubeInFrustum(this.boundingBox);
            p_366787_ = i == -2;
            flag = i == -2 || i == -1;
         }

         if (flag) {
            p_371398_ = p_371398_
               && Octree.this.isClose(
                  this.boundingBox.minX(),
                  this.boundingBox.minY(),
                  this.boundingBox.minZ(),
                  this.boundingBox.maxX(),
                  this.boundingBox.maxY(),
                  this.boundingBox.maxZ(),
                  p_371411_
               );
            p_366879_.visit(this, p_366787_, p_366756_, p_371398_);

            for (Octree.Node octree$node : this.nodes) {
               if (octree$node != null) {
                  octree$node.visitNodes(p_366879_, p_366787_, p_366666_, p_366756_ + 1, p_371411_, p_371398_);
               }
            }
         }
      }

      @Override
      public SectionRenderDispatcher.RenderSection getSection() {
         return null;
      }

      @Override
      public AABB getAABB() {
         return new AABB(
            this.boundingBox.minX(),
            this.boundingBox.minY(),
            this.boundingBox.minZ(),
            this.boundingBox.maxX() + 1,
            this.boundingBox.maxY() + 1,
            this.boundingBox.maxZ() + 1
         );
      }
   }

   @OnlyIn(Dist.CLIENT)
   final class Leaf implements Octree.Node {
      private final SectionRenderDispatcher.RenderSection section;

      Leaf(SectionRenderDispatcher.RenderSection p_366798_) {
         this.section = p_366798_;
      }

      @Override
      public void visitNodes(Octree.OctreeVisitor p_366617_, boolean p_366572_, Frustum p_366767_, int p_366729_, int p_371551_, boolean p_371193_) {
         AABB aabb = this.section.getBoundingBox();
         if (p_366572_ || p_366767_.isVisible(this.getSection().getBoundingBox())) {
            p_371193_ = p_371193_ && Octree.this.isClose(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ, p_371551_);
            p_366617_.visit(this, p_366572_, p_366729_, p_371193_);
         }
      }

      @Override
      public SectionRenderDispatcher.RenderSection getSection() {
         return this.section;
      }

      @Override
      public AABB getAABB() {
         return this.section.getBoundingBox();
      }
   }

   @OnlyIn(Dist.CLIENT)
   public interface Node {
      void visitNodes(Octree.OctreeVisitor var1, boolean var2, Frustum var3, int var4, int var5, boolean var6);

      SectionRenderDispatcher.RenderSection getSection();

      AABB getAABB();
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface OctreeVisitor {
      void visit(Octree.Node var1, boolean var2, int var3, boolean var4);
   }
}
