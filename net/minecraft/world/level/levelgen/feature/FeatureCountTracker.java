package net.minecraft.world.level.levelgen.feature;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;

public class FeatureCountTracker {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final LoadingCache<ServerLevel, FeatureCountTracker.LevelData> data = CacheBuilder.newBuilder()
      .weakKeys()
      .expireAfterAccess(5L, TimeUnit.MINUTES)
      .build(new CacheLoader<ServerLevel, FeatureCountTracker.LevelData>() {
         public FeatureCountTracker.LevelData load(ServerLevel p_190902_) {
            return new FeatureCountTracker.LevelData(Object2IntMaps.synchronize(new Object2IntOpenHashMap()), new MutableInt(0));
         }
      });

   public static void chunkDecorated(ServerLevel p_190882_) {
      try {
         ((FeatureCountTracker.LevelData)data.get(p_190882_)).chunksWithFeatures().increment();
      } catch (Exception var2) {
         LOGGER.error("Failed to increment chunk count", var2);
      }
   }

   public static void featurePlaced(ServerLevel p_190884_, ConfiguredFeature<?, ?> p_190885_, Optional<PlacedFeature> p_190886_) {
      try {
         ((FeatureCountTracker.LevelData)data.get(p_190884_))
            .featureData()
            .computeInt(new FeatureCountTracker.FeatureData(p_190885_, p_190886_), (p_190891_, p_190892_) -> p_190892_ == null ? 1 : p_190892_ + 1);
      } catch (Exception var4) {
         LOGGER.error("Failed to increment feature count", var4);
      }
   }

   public static void clearCounts() {
      data.invalidateAll();
      LOGGER.debug("Cleared feature counts");
   }

   public static void logCounts() {
      LOGGER.debug("Logging feature counts:");
      data.asMap()
         .forEach(
            (p_190888_, p_190889_) -> {
               String $$2 = p_190888_.dimension().location().toString();
               boolean $$3 = p_190888_.getServer().isRunning();
               Registry<PlacedFeature> $$4 = p_190888_.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
               String $$5 = ($$3 ? "running" : "dead") + " " + $$2;
               Integer $$6 = p_190889_.chunksWithFeatures().getValue();
               LOGGER.debug($$5 + " total_chunks: " + $$6);
               p_190889_.featureData()
                  .forEach(
                     (p_339550_, p_339551_) -> LOGGER.debug(
                        $$5
                           + " "
                           + String.format(Locale.ROOT, "%10d ", p_339551_)
                           + String.format(Locale.ROOT, "%10f ", (double)p_339551_.intValue() / $$6.intValue())
                           + p_339550_.topFeature().flatMap($$4::getResourceKey).<ResourceLocation>map(ResourceKey::location)
                           + " "
                           + p_339550_.feature().feature()
                           + " "
                           + p_339550_.feature()
                     )
                  );
            }
         );
   }

   record FeatureData(ConfiguredFeature<?, ?> feature, Optional<PlacedFeature> topFeature) {
   }

   record LevelData(Object2IntMap<FeatureCountTracker.FeatureData> featureData, MutableInt chunksWithFeatures) {
   }
}
