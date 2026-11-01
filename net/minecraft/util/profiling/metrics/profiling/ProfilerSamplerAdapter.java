package net.minecraft.util.profiling.metrics.profiling;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.profiling.ActiveProfiler;
import net.minecraft.util.profiling.ProfileCollector;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.minecraft.util.profiling.metrics.MetricSampler;

public class ProfilerSamplerAdapter {
   private final Set<String> previouslyFoundSamplerNames = new ObjectOpenHashSet();

   public Set<MetricSampler> newSamplersFoundInProfiler(Supplier<ProfileCollector> p_146164_) {
      Set<MetricSampler> $$1 = p_146164_.get()
         .getChartedPaths()
         .stream()
         .filter(p_146176_ -> !this.previouslyFoundSamplerNames.contains(p_146176_.getLeft()))
         .map(p_146174_ -> samplerForProfilingPath(p_146164_, (String)p_146174_.getLeft(), (MetricCategory)p_146174_.getRight()))
         .collect(Collectors.toSet());

      for (MetricSampler $$2 : $$1) {
         this.previouslyFoundSamplerNames.add($$2.getName());
      }

      return $$1;
   }

   private static MetricSampler samplerForProfilingPath(Supplier<ProfileCollector> p_146169_, String p_146170_, MetricCategory p_146171_) {
      return MetricSampler.create(p_146170_, p_146171_, () -> {
         ActiveProfiler.PathEntry $$2 = p_146169_.get().getEntry(p_146170_);
         return $$2 == null ? 0.0 : (double)$$2.getMaxDuration() / TimeUtil.NANOSECONDS_PER_MILLISECOND;
      });
   }
}
