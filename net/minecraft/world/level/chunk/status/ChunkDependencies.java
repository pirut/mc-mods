package net.minecraft.world.level.chunk.status;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.util.Locale;

public final class ChunkDependencies {
   private final ImmutableList<ChunkStatus> dependencyByRadius;
   private final int[] radiusByDependency;

   public ChunkDependencies(ImmutableList<ChunkStatus> p_347595_) {
      this.dependencyByRadius = p_347595_;
      int $$1 = p_347595_.isEmpty() ? 0 : ((ChunkStatus)p_347595_.getFirst()).getIndex() + 1;
      this.radiusByDependency = new int[$$1];

      for (int $$2 = 0; $$2 < p_347595_.size(); $$2++) {
         ChunkStatus $$3 = (ChunkStatus)p_347595_.get($$2);
         int $$4 = $$3.getIndex();

         for (int $$5 = 0; $$5 <= $$4; $$5++) {
            this.radiusByDependency[$$5] = $$2;
         }
      }
   }

   @VisibleForTesting
   public ImmutableList<ChunkStatus> asList() {
      return this.dependencyByRadius;
   }

   public int size() {
      return this.dependencyByRadius.size();
   }

   public int getRadiusOf(ChunkStatus p_347685_) {
      int $$1 = p_347685_.getIndex();
      if ($$1 >= this.radiusByDependency.length) {
         throw new IllegalArgumentException(
            String.format(Locale.ROOT, "Requesting a ChunkStatus(%s) outside of dependency range(%s)", p_347685_, this.dependencyByRadius)
         );
      } else {
         return this.radiusByDependency[$$1];
      }
   }

   public int getRadius() {
      return Math.max(0, this.dependencyByRadius.size() - 1);
   }

   public ChunkStatus get(int p_347623_) {
      return (ChunkStatus)this.dependencyByRadius.get(p_347623_);
   }

   @Override
   public String toString() {
      return this.dependencyByRadius.toString();
   }
}
