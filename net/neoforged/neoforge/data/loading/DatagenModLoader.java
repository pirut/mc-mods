package net.neoforged.neoforge.data.loading;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.packs.PackResources;
import net.minecraft.util.Util;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.internal.CommonModLoader;
import net.neoforged.neoforge.internal.RegistrationEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;

public class DatagenModLoader extends CommonModLoader {
   private static final Logger LOGGER = LogManager.getLogger();
   private static GatherDataEvent.DataGeneratorConfig dataGeneratorConfig;
   private static boolean runningDataGen;

   public static boolean isRunningDataGen() {
      return runningDataGen;
   }

   @Internal
   public static void begin(
      Set<String> mods,
      Path path,
      Collection<Path> inputs,
      Collection<Path> existingPacks,
      boolean devToolGenerators,
      boolean reportsGenerator,
      boolean structureValidator,
      boolean flat,
      Runnable setup,
      GatherDataEvent.GatherDataEventGenerator eventGenerator,
      DataGenerator vanillaGenerator,
      Consumer<Consumer<PackResources>> vanillaClientAssets
   ) {
      if (!mods.contains("minecraft") || mods.size() != 1) {
         LOGGER.info("Initializing Data Gatherer for mods {}", mods);
         runningDataGen = true;
         Bootstrap.bootStrap();
         begin(() -> {}, true);
         RegistrationEvents.modifyComponents();
         CompletableFuture<HolderLookup.Provider> lookupProvider = CompletableFuture.supplyAsync(VanillaRegistries::createLookup, Util.backgroundExecutor());
         dataGeneratorConfig = new GatherDataEvent.DataGeneratorConfig(
            mods,
            path,
            inputs,
            lookupProvider,
            devToolGenerators,
            reportsGenerator,
            structureValidator,
            flat,
            vanillaGenerator,
            existingPacks,
            vanillaClientAssets
         );
         setup.run();

         for (ModContainer mod : ModList.get().getSortedMods()) {
            if (dataGeneratorConfig.getMods().contains(mod.getModId())) {
               DataGenerator generator = dataGeneratorConfig.makeGenerator(p -> dataGeneratorConfig.isFlat() ? p : p.resolve(mod.getModId()));
               GatherDataEvent event = eventGenerator.create(mod, generator, dataGeneratorConfig);
               mod.acceptEvent(event);
            }
         }

         dataGeneratorConfig.runAll();
      }
   }
}
