package net.minecraft.world.level.chunk.status;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.util.profiling.jfr.callback.ProfiledDuration;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;

public record ChunkStep(
   ChunkStatus targetStatus, ChunkDependencies directDependencies, ChunkDependencies accumulatedDependencies, int blockStateWriteRadius, ChunkStatusTask task
) {
   public int getAccumulatedRadiusOf(ChunkStatus p_347454_) {
      return p_347454_ == this.targetStatus ? 0 : this.accumulatedDependencies.getRadiusOf(p_347454_);
   }

   public CompletableFuture<ChunkAccess> apply(WorldGenContext p_347561_, StaticCache2D<GenerationChunkHolder> p_347614_, ChunkAccess p_347449_) {
      if (p_347449_.getPersistedStatus().isBefore(this.targetStatus)) {
         ProfiledDuration $$3 = JvmProfiler.INSTANCE.onChunkGenerate(p_347449_.getPos(), p_347561_.level().dimension(), this.targetStatus.getName());
         return this.task.doWork(p_347561_, this, p_347614_, p_347449_).thenApply(p_347625_ -> this.completeChunkGeneration(p_347625_, $$3));
      } else {
         return this.task.doWork(p_347561_, this, p_347614_, p_347449_);
      }
   }

   private ChunkAccess completeChunkGeneration(ChunkAccess p_347705_, @Nullable ProfiledDuration p_347691_) {
      if (p_347705_ instanceof ProtoChunk $$2 && $$2.getPersistedStatus().isBefore(this.targetStatus)) {
         $$2.setPersistedStatus(this.targetStatus);
      }

      if (p_347691_ != null) {
         p_347691_.finish();
      }

      return p_347705_;
   }

   public static class Builder {
      private final ChunkStatus status;
      @Nullable
      private final ChunkStep parent;
      private ChunkStatus[] directDependenciesByRadius;
      private int blockStateWriteRadius = -1;
      private ChunkStatusTask task = ChunkStatusTasks::passThrough;

      protected Builder(ChunkStatus p_347618_) {
         if (p_347618_.getParent() != p_347618_) {
            throw new IllegalArgumentException("Not starting with the first status: " + p_347618_);
         } else {
            this.status = p_347618_;
            this.parent = null;
            this.directDependenciesByRadius = new ChunkStatus[0];
         }
      }

      protected Builder(ChunkStatus p_347600_, ChunkStep p_347655_) {
         if (p_347655_.targetStatus.getIndex() != p_347600_.getIndex() - 1) {
            throw new IllegalArgumentException("Out of order status: " + p_347600_);
         } else {
            this.status = p_347600_;
            this.parent = p_347655_;
            this.directDependenciesByRadius = new ChunkStatus[]{p_347655_.targetStatus};
         }
      }

      public ChunkStep.Builder addRequirement(ChunkStatus p_347610_, int p_347602_) {
         if (p_347610_.isOrAfter(this.status)) {
            throw new IllegalArgumentException("Status " + p_347610_ + " can not be required by " + this.status);
         } else {
            ChunkStatus[] $$2 = this.directDependenciesByRadius;
            int $$3 = p_347602_ + 1;
            if ($$3 > $$2.length) {
               this.directDependenciesByRadius = new ChunkStatus[$$3];
               Arrays.fill(this.directDependenciesByRadius, p_347610_);
            }

            for (int $$4 = 0; $$4 < Math.min($$3, $$2.length); $$4++) {
               this.directDependenciesByRadius[$$4] = ChunkStatus.max($$2[$$4], p_347610_);
            }

            return this;
         }
      }

      public ChunkStep.Builder blockStateWriteRadius(int p_347450_) {
         this.blockStateWriteRadius = p_347450_;
         return this;
      }

      public ChunkStep.Builder setTask(ChunkStatusTask p_347502_) {
         this.task = p_347502_;
         return this;
      }

      public ChunkStep build() {
         return new ChunkStep(
            this.status,
            new ChunkDependencies(ImmutableList.copyOf(this.directDependenciesByRadius)),
            new ChunkDependencies(ImmutableList.copyOf(this.buildAccumulatedDependencies())),
            this.blockStateWriteRadius,
            this.task
         );
      }

      private ChunkStatus[] buildAccumulatedDependencies() {
         if (this.parent == null) {
            return this.directDependenciesByRadius;
         } else {
            int $$0 = this.getRadiusOfParent(this.parent.targetStatus);
            ChunkDependencies $$1 = this.parent.accumulatedDependencies;
            ChunkStatus[] $$2 = new ChunkStatus[Math.max($$0 + $$1.size(), this.directDependenciesByRadius.length)];

            for (int $$3 = 0; $$3 < $$2.length; $$3++) {
               int $$4 = $$3 - $$0;
               if ($$4 < 0 || $$4 >= $$1.size()) {
                  $$2[$$3] = this.directDependenciesByRadius[$$3];
               } else if ($$3 >= this.directDependenciesByRadius.length) {
                  $$2[$$3] = $$1.get($$4);
               } else {
                  $$2[$$3] = ChunkStatus.max(this.directDependenciesByRadius[$$3], $$1.get($$4));
               }
            }

            return $$2;
         }
      }

      private int getRadiusOfParent(ChunkStatus p_347567_) {
         for (int $$1 = this.directDependenciesByRadius.length - 1; $$1 >= 0; $$1--) {
            if (this.directDependenciesByRadius[$$1].isOrAfter(p_347567_)) {
               return $$1;
            }
         }

         return 0;
      }
   }
}
