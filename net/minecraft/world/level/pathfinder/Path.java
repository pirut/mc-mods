package net.minecraft.world.level.pathfinder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class Path {
   private final List<Node> nodes;
   @Nullable
   private Path.DebugData debugData;
   private int nextNodeIndex;
   private final BlockPos target;
   private final float distToTarget;
   private final boolean reached;

   public Path(List<Node> p_77371_, BlockPos p_77372_, boolean p_77373_) {
      this.nodes = p_77371_;
      this.target = p_77372_;
      this.distToTarget = p_77371_.isEmpty() ? Float.MAX_VALUE : this.nodes.get(this.nodes.size() - 1).distanceManhattan(this.target);
      this.reached = p_77373_;
   }

   public void advance() {
      this.nextNodeIndex++;
   }

   public boolean notStarted() {
      return this.nextNodeIndex <= 0;
   }

   public boolean isDone() {
      return this.nextNodeIndex >= this.nodes.size();
   }

   @Nullable
   public Node getEndNode() {
      return !this.nodes.isEmpty() ? this.nodes.get(this.nodes.size() - 1) : null;
   }

   public Node getNode(int p_77376_) {
      return this.nodes.get(p_77376_);
   }

   public void truncateNodes(int p_77389_) {
      if (this.nodes.size() > p_77389_) {
         this.nodes.subList(p_77389_, this.nodes.size()).clear();
      }
   }

   public void replaceNode(int p_77378_, Node p_77379_) {
      this.nodes.set(p_77378_, p_77379_);
   }

   public int getNodeCount() {
      return this.nodes.size();
   }

   public int getNextNodeIndex() {
      return this.nextNodeIndex;
   }

   public void setNextNodeIndex(int p_77394_) {
      this.nextNodeIndex = p_77394_;
   }

   public Vec3 getEntityPosAtNode(Entity p_77383_, int p_77384_) {
      Node $$2 = this.nodes.get(p_77384_);
      double $$3 = $$2.x + (int)(p_77383_.getBbWidth() + 1.0F) * 0.5;
      double $$4 = $$2.y;
      double $$5 = $$2.z + (int)(p_77383_.getBbWidth() + 1.0F) * 0.5;
      return new Vec3($$3, $$4, $$5);
   }

   public BlockPos getNodePos(int p_77397_) {
      return this.nodes.get(p_77397_).asBlockPos();
   }

   public Vec3 getNextEntityPos(Entity p_77381_) {
      return this.getEntityPosAtNode(p_77381_, this.nextNodeIndex);
   }

   public BlockPos getNextNodePos() {
      return this.nodes.get(this.nextNodeIndex).asBlockPos();
   }

   public Node getNextNode() {
      return this.nodes.get(this.nextNodeIndex);
   }

   @Nullable
   public Node getPreviousNode() {
      return this.nextNodeIndex > 0 ? this.nodes.get(this.nextNodeIndex - 1) : null;
   }

   public boolean sameAs(@Nullable Path p_77386_) {
      if (p_77386_ == null) {
         return false;
      } else if (p_77386_.nodes.size() != this.nodes.size()) {
         return false;
      } else {
         for (int $$1 = 0; $$1 < this.nodes.size(); $$1++) {
            Node $$2 = this.nodes.get($$1);
            Node $$3 = p_77386_.nodes.get($$1);
            if ($$2.x != $$3.x || $$2.y != $$3.y || $$2.z != $$3.z) {
               return false;
            }
         }

         return true;
      }
   }

   public boolean canReach() {
      return this.reached;
   }

   @VisibleForDebug
   void setDebug(Node[] p_164710_, Node[] p_164711_, Set<Target> p_164712_) {
      this.debugData = new Path.DebugData(p_164710_, p_164711_, p_164712_);
   }

   @Nullable
   public Path.DebugData debugData() {
      return this.debugData;
   }

   public void writeToStream(FriendlyByteBuf p_164705_) {
      if (this.debugData != null && !this.debugData.targetNodes.isEmpty()) {
         p_164705_.writeBoolean(this.reached);
         p_164705_.writeInt(this.nextNodeIndex);
         p_164705_.writeBlockPos(this.target);
         p_164705_.writeCollection(this.nodes, (p_294084_, p_294085_) -> p_294085_.writeToStream(p_294084_));
         this.debugData.write(p_164705_);
      }
   }

   public static Path createFromStream(FriendlyByteBuf p_77391_) {
      boolean $$1 = p_77391_.readBoolean();
      int $$2 = p_77391_.readInt();
      BlockPos $$3 = p_77391_.readBlockPos();
      List<Node> $$4 = p_77391_.readList(Node::createFromStream);
      Path.DebugData $$5 = Path.DebugData.read(p_77391_);
      Path $$6 = new Path($$4, $$3, $$1);
      $$6.debugData = $$5;
      $$6.nextNodeIndex = $$2;
      return $$6;
   }

   @Override
   public String toString() {
      return "Path(length=" + this.nodes.size() + ")";
   }

   public BlockPos getTarget() {
      return this.target;
   }

   public float getDistToTarget() {
      return this.distToTarget;
   }

   static Node[] readNodeArray(FriendlyByteBuf p_294398_) {
      Node[] $$1 = new Node[p_294398_.readVarInt()];

      for (int $$2 = 0; $$2 < $$1.length; $$2++) {
         $$1[$$2] = Node.createFromStream(p_294398_);
      }

      return $$1;
   }

   static void writeNodeArray(FriendlyByteBuf p_296066_, Node[] p_294231_) {
      p_296066_.writeVarInt(p_294231_.length);

      for (Node $$2 : p_294231_) {
         $$2.writeToStream(p_296066_);
      }
   }

   public Path copy() {
      Path $$0 = new Path(this.nodes, this.target, this.reached);
      $$0.debugData = this.debugData;
      $$0.nextNodeIndex = this.nextNodeIndex;
      return $$0;
   }

   public record DebugData(Node[] openSet, Node[] closedSet, Set<Target> targetNodes) {
      public void write(FriendlyByteBuf p_296345_) {
         p_296345_.writeCollection(this.targetNodes, (p_295084_, p_294361_) -> p_294361_.writeToStream(p_295084_));
         Path.writeNodeArray(p_296345_, this.openSet);
         Path.writeNodeArray(p_296345_, this.closedSet);
      }

      public static Path.DebugData read(FriendlyByteBuf p_295853_) {
         HashSet<Target> $$1 = p_295853_.readCollection(HashSet::new, Target::createFromStream);
         Node[] $$2 = Path.readNodeArray(p_295853_);
         Node[] $$3 = Path.readNodeArray(p_295853_);
         return new Path.DebugData($$2, $$3, $$1);
      }
   }
}
