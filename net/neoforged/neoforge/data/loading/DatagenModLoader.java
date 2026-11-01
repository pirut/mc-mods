package net.neoforged.neoforge.data.loading;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.entity.animation.json.AnimationTypeManager;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.internal.CommonModLoader;
import net.neoforged.neoforge.internal.RegistrationEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;

public class DatagenModLoader extends CommonModLoader {
   private static final Logger LOGGER = LogManager.getLogger();
   private static GatherDataEvent.DataGeneratorConfig dataGeneratorConfig;
   private static ExistingFileHelper existingFileHelper;
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
      Set<String> existingMods,
      boolean serverGenerators,
      boolean clientGenerators,
      boolean devToolGenerators,
      boolean reportsGenerator,
      boolean structureValidator,
      boolean flat,
      String assetIndex,
      File assetsDir
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
            serverGenerators,
            clientGenerators,
            devToolGenerators,
            reportsGenerator,
            structureValidator,
            flat,
            assetIndex,
            assetsDir,
            existingPacks
         );
         if (!mods.contains("neoforge")) {
            existingMods.add("neoforge");
         }

         if (clientGenerators) {
            ClientHooks.registerSpriteSourceTypes();
            AnimationTypeManager.init();
         }

         existingFileHelper = new ExistingFileHelper(existingPacks, existingMods, structureValidator, assetIndex, assetsDir);
         ModLoader.runEventGenerator(
            mc -> new GatherDataEvent(
               mc,
               dataGeneratorConfig.makeGenerator(
                  p -> dataGeneratorConfig.isFlat() ? p : p.resolve(mc.getModId()), dataGeneratorConfig.getMods().contains(mc.getModId())
               ),
               dataGeneratorConfig,
               existingFileHelper
            )
         );
         dataGeneratorConfig.runAll();
      }
   }
}
