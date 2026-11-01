package net.minecraft.util.profiling.metrics.storage;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CsvOutput;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.minecraft.util.profiling.metrics.MetricSampler;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class MetricsPersister {
   public static final Path PROFILING_RESULTS_DIR = Paths.get("debug/profiling");
   public static final String METRICS_DIR_NAME = "metrics";
   public static final String DEVIATIONS_DIR_NAME = "deviations";
   public static final String PROFILING_RESULT_FILENAME = "profiling.txt";
   private static final Logger LOGGER = LogUtils.getLogger();
   private final String rootFolderName;

   public MetricsPersister(String p_146217_) {
      this.rootFolderName = p_146217_;
   }

   public Path saveReports(Set<MetricSampler> p_146251_, Map<MetricSampler, List<RecordedDeviation>> p_146252_, ProfileResults p_146253_) {
      try {
         Files.createDirectories(PROFILING_RESULTS_DIR);
      } catch (IOException var8) {
         throw new UncheckedIOException(var8);
      }

      try {
         Path $$4 = Files.createTempDirectory("minecraft-profiling");
         $$4.toFile().deleteOnExit();
         Files.createDirectories(PROFILING_RESULTS_DIR);
         Path $$5 = $$4.resolve(this.rootFolderName);
         Path $$6 = $$5.resolve("metrics");
         this.saveMetrics(p_146251_, $$6);
         if (!p_146252_.isEmpty()) {
            this.saveDeviations(p_146252_, $$5.resolve("deviations"));
         }

         this.saveProfilingTaskExecutionResult(p_146253_, $$5);
         return $$4;
      } catch (IOException var7) {
         throw new UncheckedIOException(var7);
      }
   }

   private void saveMetrics(Set<MetricSampler> p_146248_, Path p_146249_) {
      if (p_146248_.isEmpty()) {
         throw new IllegalArgumentException("Expected at least one sampler to persist");
      } else {
         Map<MetricCategory, List<MetricSampler>> $$2 = p_146248_.stream().collect(Collectors.groupingBy(MetricSampler::getCategory));
         $$2.forEach((p_146232_, p_146233_) -> this.saveCategory(p_146232_, (List<MetricSampler>)p_146233_, p_146249_));
      }
   }

   private void saveCategory(MetricCategory p_146227_, List<MetricSampler> p_146228_, Path p_146229_) {
      Path $$3 = p_146229_.resolve(Util.sanitizeName(p_146227_.getDescription(), ResourceLocation::validPathChar) + ".csv");
      Writer $$4 = null;

      try {
         Files.createDirectories($$3.getParent());
         $$4 = Files.newBufferedWriter($$3, StandardCharsets.UTF_8);
         CsvOutput.Builder $$5 = CsvOutput.builder();
         $$5.addColumn("@tick");

         for (MetricSampler $$6 : p_146228_) {
            $$5.addColumn($$6.getName());
         }

         CsvOutput $$7 = $$5.build($$4);
         List<MetricSampler.SamplerResult> $$8 = p_146228_.stream().map(MetricSampler::result).collect(Collectors.toList());
         int $$9 = $$8.stream().mapToInt(MetricSampler.SamplerResult::getFirstTick).summaryStatistics().getMin();
         int $$10 = $$8.stream().mapToInt(MetricSampler.SamplerResult::getLastTick).summaryStatistics().getMax();

         for (int $$11 = $$9; $$11 <= $$10; $$11++) {
            int $$12 = $$11;
            Stream<String> $$13 = $$8.stream().map(p_146222_ -> String.valueOf(p_146222_.valueAtTick($$12)));
            Object[] $$14 = Stream.concat(Stream.of(String.valueOf($$11)), $$13).toArray(String[]::new);
            $$7.writeRow($$14);
         }

         LOGGER.info("Flushed metrics to {}", $$3);
      } catch (Exception var18) {
         LOGGER.error("Could not save profiler results to {}", $$3, var18);
      } finally {
         IOUtils.closeQuietly($$4);
      }
   }

   private void saveDeviations(Map<MetricSampler, List<RecordedDeviation>> p_146245_, Path p_146246_) {
      DateTimeFormatter $$2 = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss.SSS", Locale.UK).withZone(ZoneId.systemDefault());
      p_146245_.forEach(
         (p_146242_, p_146243_) -> p_146243_.forEach(
            p_146238_ -> {
               String $$4 = $$2.format(p_146238_.timestamp);
               Path $$5 = p_146246_.resolve(Util.sanitizeName(p_146242_.getName(), ResourceLocation::validPathChar))
                  .resolve(String.format(Locale.ROOT, "%d@%s.txt", p_146238_.tick, $$4));
               p_146238_.profilerResultAtTick.saveResults($$5);
            }
         )
      );
   }

   private void saveProfilingTaskExecutionResult(ProfileResults p_146224_, Path p_146225_) {
      p_146224_.saveResults(p_146225_.resolve("profiling.txt"));
   }
}
