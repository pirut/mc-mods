package net.neoforged.neoforge.common.world;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class BiomeGenerationSettingsBuilder extends BiomeGenerationSettings.PlainBuilder {
   public BiomeGenerationSettingsBuilder(BiomeGenerationSettings orig) {
      orig.getCarvingStages().forEach(k -> {
         this.carvers.put(k, new ArrayList<>());
         orig.getCarvers(k).forEach(v -> this.carvers.get(k).add((Holder<ConfiguredWorldCarver<?>>)v));
      });
      orig.features().forEach(l -> {
         ArrayList<Holder<PlacedFeature>> featureList = new ArrayList<>();
         l.forEach(featureList::add);
         this.features.add(featureList);
      });
   }

   public List<Holder<PlacedFeature>> getFeatures(GenerationStep.Decoration stage) {
      this.addFeatureStepsUpTo(stage.ordinal());
      return this.features.get(stage.ordinal());
   }

   public List<Holder<ConfiguredWorldCarver<?>>> getCarvers(GenerationStep.Carving stage) {
      return this.carvers.computeIfAbsent(stage, key -> new ArrayList<>());
   }
}
