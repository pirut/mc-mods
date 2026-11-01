package net.minecraft.client.renderer;

import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SectionBufferBuilderPool {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Queue<SectionBufferBuilderPack> freeBuffers;
   private volatile int freeBufferCount;

   private SectionBufferBuilderPool(List<SectionBufferBuilderPack> p_307506_) {
      this.freeBuffers = Queues.newArrayDeque(p_307506_);
      this.freeBufferCount = this.freeBuffers.size();
   }

   public static SectionBufferBuilderPool allocate(int p_307250_) {
      int $$1 = Math.max(1, (int)(Runtime.getRuntime().maxMemory() * 0.3) / SectionBufferBuilderPack.TOTAL_BUFFERS_SIZE);
      int $$2 = Math.max(1, Math.min(p_307250_, $$1));
      List<SectionBufferBuilderPack> $$3 = new ArrayList<>($$2);

      try {
         for (int $$4 = 0; $$4 < $$2; $$4++) {
            $$3.add(new SectionBufferBuilderPack());
         }
      } catch (OutOfMemoryError var7) {
         LOGGER.warn("Allocated only {}/{} buffers", $$3.size(), $$2);
         int $$6 = Math.min($$3.size() * 2 / 3, $$3.size() - 1);

         for (int $$7 = 0; $$7 < $$6; $$7++) {
            $$3.remove($$3.size() - 1).close();
         }
      }

      return new SectionBufferBuilderPool($$3);
   }

   @Nullable
   public SectionBufferBuilderPack acquire() {
      SectionBufferBuilderPack $$0 = this.freeBuffers.poll();
      if ($$0 != null) {
         this.freeBufferCount = this.freeBuffers.size();
         return $$0;
      } else {
         return null;
      }
   }

   public void release(SectionBufferBuilderPack p_307626_) {
      this.freeBuffers.add(p_307626_);
      this.freeBufferCount = this.freeBuffers.size();
   }

   public boolean isEmpty() {
      return this.freeBuffers.isEmpty();
   }

   public int getFreeBufferCount() {
      return this.freeBufferCount;
   }
}
