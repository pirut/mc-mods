package net.minecraft.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

@Environment(EnvType.CLIENT)
public class Octree {
   private final Octree.Branch root;
   final BlockPos cameraSectionCenter;

   public Octree(SectionPos cameraSectionPos, int viewDistance, int sectionGridSizeY, int minY) {
      int i = viewDistance * 2 + 1;
      int j = Mth.smallestEncompassingPowerOfTwo(i);
      int k = viewDistance * 16;
      BlockPos blockPos = cameraSectionPos.origin();
      this.cameraSectionCenter = cameraSectionPos.center();
      int l = blockPos.getX() - k;
      int m = l + j * 16 - 1;
      int n = j >= sectionGridSizeY ? minY : blockPos.getY() - k;
      int o = n + j * 16 - 1;
      int p = blockPos.getZ() - k;
      int q = p + j * 16 - 1;
      this.root = new Octree.Branch(new BoundingBox(l, n, p, m, o, q));
   }

   public boolean add(SectionRenderDispatcher.RenderSection section) {
      return this.root.add(section);
   }

   public void visitNodes(Octree.OctreeVisitor visitor, Frustum frustum, int nearbyRadius) {
      this.root.visitNodes(visitor, false, frustum, 0, nearbyRadius, true);
   }

   boolean isClose(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int radius) {
      int i = this.cameraSectionCenter.getX();
      int j = this.cameraSectionCenter.getY();
      int k = this.cameraSectionCenter.getZ();
      return i > minX - radius && i < maxX + radius && j > minY - radius && j < maxY + radius && k > minZ - radius && k < maxZ + radius;
   }

   @Environment(EnvType.CLIENT)
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

      private AxisSorting(final int xShift, final int yShift, final int zShift) {
         this.xShift = xShift;
         this.yShift = yShift;
         this.zShift = zShift;
      }

      public static Octree.AxisSorting getAxisSorting(int xDiff, int yDiff, int zDiff) {
         if (xDiff > yDiff && xDiff > zDiff) {
            return yDiff > zDiff ? XYZ : XZY;
         } else if (yDiff > xDiff && yDiff > zDiff) {
            return xDiff > zDiff ? YXZ : YZX;
         } else {
            return xDiff > yDiff ? ZXY : ZYX;
         }
      }
   }

   @Environment(EnvType.CLIENT)
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

      public Branch(final BoundingBox boundingBox) {
         this.boundingBox = boundingBox;
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

      public boolean add(SectionRenderDispatcher.RenderSection section) {
         long l = section.getSectionNode();
         boolean bl = SectionPos.sectionToBlockCoord(SectionPos.x(l)) - this.bbCenterX < 0;
         boolean bl2 = SectionPos.sectionToBlockCoord(SectionPos.y(l)) - this.bbCenterY < 0;
         boolean bl3 = SectionPos.sectionToBlockCoord(SectionPos.z(l)) - this.bbCenterZ < 0;
         boolean bl4 = bl != this.cameraXDiffNegative;
         boolean bl5 = bl2 != this.cameraYDiffNegative;
         boolean bl6 = bl3 != this.cameraZDiffNegative;
         int i = getNodeIndex(this.sorting, bl4, bl5, bl6);
         if (this.areChildrenLeaves()) {
            boolean bl7 = this.nodes[i] != null;
            this.nodes[i] = Octree.this.new Leaf(section);
            return !bl7;
         } else if (this.nodes[i] != null) {
            Octree.Branch branch = (Octree.Branch)this.nodes[i];
            return branch.add(section);
         } else {
            BoundingBox boundingBox = this.createChildBoundingBox(bl, bl2, bl3);
            Octree.Branch branch2 = Octree.this.new Branch(boundingBox);
            this.nodes[i] = branch2;
            return branch2.add(section);
         }
      }

      private static int getNodeIndex(Octree.AxisSorting sorting, boolean xDiffNegative, boolean yDiffNegative, boolean zDiffNegative) {
         int i = 0;
         if (xDiffNegative) {
            i += sorting.xShift;
         }

         if (yDiffNegative) {
            i += sorting.yShift;
         }

         if (zDiffNegative) {
            i += sorting.zShift;
         }

         return i;
      }

      private boolean areChildrenLeaves() {
         return this.boundingBox.getXSpan() == 32;
      }

      private BoundingBox createChildBoundingBox(boolean xDiffNegative, boolean yDiffNegative, boolean zDiffNegative) {
         int i;
         int j;
         if (xDiffNegative) {
            i = this.boundingBox.minX();
            j = this.bbCenterX - 1;
         } else {
            i = this.bbCenterX;
            j = this.boundingBox.maxX();
         }

         int k;
         int l;
         if (yDiffNegative) {
            k = this.boundingBox.minY();
            l = this.bbCenterY - 1;
         } else {
            k = this.bbCenterY;
            l = this.boundingBox.maxY();
         }

         int m;
         int n;
         if (zDiffNegative) {
            m = this.boundingBox.minZ();
            n = this.bbCenterZ - 1;
         } else {
            m = this.bbCenterZ;
            n = this.boundingBox.maxZ();
         }

         return new BoundingBox(i, k, m, j, l, n);
      }

      @Override
      public void visitNodes(Octree.OctreeVisitor visitor, boolean isLeafNode, Frustum frustum, int recursionDepth, int nearbyRadius, boolean isNearby) {
         boolean bl = isLeafNode;
         if (!isLeafNode) {
            int i = frustum.cubeInFrustum(this.boundingBox);
            isLeafNode = i == -2;
            bl = i == -2 || i == -1;
         }

         if (bl) {
            isNearby = isNearby
               && Octree.this.isClose(
                  this.boundingBox.minX(),
                  this.boundingBox.minY(),
                  this.boundingBox.minZ(),
                  this.boundingBox.maxX(),
                  this.boundingBox.maxY(),
                  this.boundingBox.maxZ(),
                  nearbyRadius
               );
            visitor.visit(this, isLeafNode, recursionDepth, isNearby);

            for (Octree.Node node : this.nodes) {
               if (node != null) {
                  node.visitNodes(visitor, isLeafNode, frustum, recursionDepth + 1, nearbyRadius, isNearby);
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

   @Environment(EnvType.CLIENT)
   final class Leaf implements Octree.Node {
      private final SectionRenderDispatcher.RenderSection section;

      Leaf(final SectionRenderDispatcher.RenderSection section) {
         this.section = section;
      }

      @Override
      public void visitNodes(Octree.OctreeVisitor visitor, boolean isLeafNode, Frustum frustum, int recursionDepth, int nearbyRadius, boolean isNearby) {
         AABB aABB = this.section.getBoundingBox();
         if (isLeafNode || frustum.isVisible(this.getSection().getBoundingBox())) {
            isNearby = isNearby && Octree.this.isClose(aABB.minX, aABB.minY, aABB.minZ, aABB.maxX, aABB.maxY, aABB.maxZ, nearbyRadius);
            visitor.visit(this, isLeafNode, recursionDepth, isNearby);
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

   @Environment(EnvType.CLIENT)
   public interface Node {
      void visitNodes(Octree.OctreeVisitor visitor, boolean isLeafNode, Frustum frustum, int recursionDepth, int nearbyRadius, boolean isNearby);

      SectionRenderDispatcher.RenderSection getSection();

      AABB getAABB();
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface OctreeVisitor {
      void visit(Octree.Node node, boolean bl, int i, boolean bl2);
   }
}
