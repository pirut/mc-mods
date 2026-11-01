package net.minecraft.world.level.pathfinder;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;

public class PathFinder {
   private static final float FUDGING = 1.5F;
   private final Node[] neighbors = new Node[32];
   private final int maxVisitedNodes;
   private final NodeEvaluator nodeEvaluator;
   private static final boolean DEBUG = false;
   private final BinaryHeap openSet = new BinaryHeap();

   public PathFinder(NodeEvaluator p_77425_, int p_77426_) {
      this.nodeEvaluator = p_77425_;
      this.maxVisitedNodes = p_77426_;
   }

   @Nullable
   public Path findPath(PathNavigationRegion p_77428_, Mob p_77429_, Set<BlockPos> p_77430_, float p_77431_, int p_77432_, float p_77433_) {
      this.openSet.clear();
      this.nodeEvaluator.prepare(p_77428_, p_77429_);
      Node $$6 = this.nodeEvaluator.getStart();
      if ($$6 == null) {
         return null;
      } else {
         Map<Target, BlockPos> $$7 = p_77430_.stream()
            .collect(Collectors.toMap(p_326774_ -> this.nodeEvaluator.getTarget(p_326774_.getX(), p_326774_.getY(), p_326774_.getZ()), Function.identity()));
         Path $$8 = this.findPath(p_77428_.getProfiler(), $$6, $$7, p_77431_, p_77432_, p_77433_);
         this.nodeEvaluator.done();
         return $$8;
      }
   }

   @Nullable
   private Path findPath(ProfilerFiller p_164717_, Node p_164718_, Map<Target, BlockPos> p_164719_, float p_164720_, int p_164721_, float p_164722_) {
      p_164717_.push("find_path");
      p_164717_.markForCharting(MetricCategory.PATH_FINDING);
      Set<Target> $$6 = p_164719_.keySet();
      p_164718_.g = 0.0F;
      p_164718_.h = this.getBestH(p_164718_, $$6);
      p_164718_.f = p_164718_.h;
      this.openSet.clear();
      this.openSet.insert(p_164718_);
      Set<Node> $$7 = ImmutableSet.of();
      int $$8 = 0;
      Set<Target> $$9 = Sets.newHashSetWithExpectedSize($$6.size());
      int $$10 = (int)(this.maxVisitedNodes * p_164722_);

      while (!this.openSet.isEmpty()) {
         if (++$$8 >= $$10) {
            break;
         }

         Node $$11 = this.openSet.pop();
         $$11.closed = true;

         for (Target $$12 : $$6) {
            if ($$11.distanceManhattan($$12) <= p_164721_) {
               $$12.setReached();
               $$9.add($$12);
            }
         }

         if (!$$9.isEmpty()) {
            break;
         }

         if (!($$11.distanceTo(p_164718_) >= p_164720_)) {
            int $$13 = this.nodeEvaluator.getNeighbors(this.neighbors, $$11);

            for (int $$14 = 0; $$14 < $$13; $$14++) {
               Node $$15 = this.neighbors[$$14];
               float $$16 = this.distance($$11, $$15);
               $$15.walkedDistance = $$11.walkedDistance + $$16;
               float $$17 = $$11.g + $$16 + $$15.costMalus;
               if ($$15.walkedDistance < p_164720_ && (!$$15.inOpenSet() || $$17 < $$15.g)) {
                  $$15.cameFrom = $$11;
                  $$15.g = $$17;
                  $$15.h = this.getBestH($$15, $$6) * 1.5F;
                  if ($$15.inOpenSet()) {
                     this.openSet.changeCost($$15, $$15.g + $$15.h);
                  } else {
                     $$15.f = $$15.g + $$15.h;
                     this.openSet.insert($$15);
                  }
               }
            }
         }
      }

      Optional<Path> $$18 = !$$9.isEmpty()
         ? $$9.stream()
            .map(p_77454_ -> this.reconstructPath(p_77454_.getBestNode(), p_164719_.get(p_77454_), true))
            .min(Comparator.comparingInt(Path::getNodeCount))
         : $$6.stream()
            .map(p_77451_ -> this.reconstructPath(p_77451_.getBestNode(), p_164719_.get(p_77451_), false))
            .min(Comparator.comparingDouble(Path::getDistToTarget).thenComparingInt(Path::getNodeCount));
      p_164717_.pop();
      return $$18.isEmpty() ? null : $$18.get();
   }

   protected float distance(Node p_230617_, Node p_230618_) {
      return p_230617_.distanceTo(p_230618_);
   }

   private float getBestH(Node p_77445_, Set<Target> p_77446_) {
      float $$2 = Float.MAX_VALUE;

      for (Target $$3 : p_77446_) {
         float $$4 = p_77445_.distanceTo($$3);
         $$3.updateBest($$4, p_77445_);
         $$2 = Math.min($$4, $$2);
      }

      return $$2;
   }

   private Path reconstructPath(Node p_77435_, BlockPos p_77436_, boolean p_77437_) {
      List<Node> $$3 = Lists.newArrayList();
      Node $$4 = p_77435_;
      $$3.add(0, p_77435_);

      while ($$4.cameFrom != null) {
         $$4 = $$4.cameFrom;
         $$3.add(0, $$4);
      }

      return new Path($$3, p_77436_, p_77437_);
   }
}
