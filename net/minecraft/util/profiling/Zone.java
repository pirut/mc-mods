package net.minecraft.util.profiling;

import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public class Zone implements AutoCloseable {
   public static final Zone INACTIVE = new Zone(null);
   @Nullable
   private final ProfilerFiller profiler;

   Zone(@Nullable ProfilerFiller p_372834_) {
      this.profiler = p_372834_;
   }

   public Zone addText(String p_373072_) {
      if (this.profiler != null) {
         this.profiler.addZoneText(p_373072_);
      }

      return this;
   }

   public Zone addText(Supplier<String> p_372934_) {
      if (this.profiler != null) {
         this.profiler.addZoneText(p_372934_.get());
      }

      return this;
   }

   public Zone addValue(long p_372891_) {
      if (this.profiler != null) {
         this.profiler.addZoneValue(p_372891_);
      }

      return this;
   }

   public Zone setColor(int p_373057_) {
      if (this.profiler != null) {
         this.profiler.setZoneColor(p_373057_);
      }

      return this;
   }

   @Override
   public void close() {
      if (this.profiler != null) {
         this.profiler.pop();
      }
   }
}
