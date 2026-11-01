package net.neoforged.neoforge.client.loading;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.world.level.DataPackConfig;
import net.neoforged.fml.Logging;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.ModLoadingException;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.ModWorkManager;
import net.neoforged.fml.VersionChecker;
import net.neoforged.fml.VersionChecker.CheckResult;
import net.neoforged.fml.VersionChecker.Status;
import net.neoforged.fml.loading.EarlyLoadingScreenController;
import net.neoforged.neoforge.client.config.NeoForgeClientConfig;
import net.neoforged.neoforge.client.gui.LoadingErrorScreen;
import net.neoforged.neoforge.client.network.registration.ClientNetworkRegistry;
import net.neoforged.neoforge.internal.CommonModLoader;
import net.neoforged.neoforge.logging.CrashReportExtender;
import net.neoforged.neoforge.resource.ResourcePackLoader;
import net.neoforged.neoforge.server.LanguageHook;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

@Internal
public class ClientModLoader extends CommonModLoader {
   private static final Logger LOGGER = LogManager.getLogger();
   private static boolean loading;
   private static boolean loadingComplete;
   @Nullable
   private static ModLoadingException error;

   public static void begin() {
      Runtime.getRuntime().addShutdownHook(new Thread(LogManager::shutdown));
      EarlyLoadingScreenController earlyLoadingScreen = EarlyLoadingScreenController.current();
      if (earlyLoadingScreen != null) {
         earlyLoadingScreen.updateProgress("Loading mods");
      }

      loading = true;
      LanguageHook.loadBuiltinLanguages();

      try {
         Runnable periodicTick = earlyLoadingScreen != null ? earlyLoadingScreen::periodicTick : () -> {};
         begin(periodicTick, false);
      } catch (ModLoadingException var2) {
         error = var2;
      }
   }

   public static void finish(PackRepository defaultResourcePacks, ReloadableResourceManager mcResourceManager) {
      if (error == null) {
         ResourcePackLoader.populatePackRepository(defaultResourcePacks, PackType.CLIENT_RESOURCES, false);
         DataPackConfig.DEFAULT.addModPacks(ResourcePackLoader.getPackNames(PackType.SERVER_DATA));
      }
   }

   public static CompletableFuture<Void> onResourceReload(
      PreparableReloadListener.SharedState sharedState, Executor asyncExecutor, PreparableReloadListener.PreparationBarrier stage, Executor syncExecutor
   ) {
      return CompletableFuture.runAsync(() -> startModLoading(syncExecutor, asyncExecutor), ModWorkManager.parallelExecutor())
         .thenCompose(stage::wait)
         .thenRunAsync(() -> finishModLoading(syncExecutor, asyncExecutor), ModWorkManager.parallelExecutor());
   }

   private static void catchLoadingException(Runnable r) {
      if (!loadingComplete) {
         if (!ModLoader.hasErrors()) {
            try {
               r.run();
            } catch (ModLoadingException var2) {
               if (error == null) {
                  error = var2;
               }
            }
         }
      }
   }

   private static void startModLoading(Executor syncExecutor, Executor parallelExecutor) {
      catchLoadingException(() -> load(syncExecutor, parallelExecutor));
   }

   private static void finishModLoading(Executor syncExecutor, Executor parallelExecutor) {
      catchLoadingException(() -> {
         finish(syncExecutor, parallelExecutor);
         ModLoader.runInitTask("Client network registry lock", syncExecutor, () -> {}, ClientNetworkRegistry::setup);
      });
      loading = false;
      loadingComplete = true;
   }

   public static Status checkForUpdates() {
      boolean anyOutdated = ModList.get()
         .getMods()
         .stream()
         .<CheckResult>map(VersionChecker::getResult)
         .map(result -> result.status())
         .anyMatch(status -> status == Status.OUTDATED || status == Status.BETA_OUTDATED);
      return anyOutdated ? Status.OUTDATED : null;
   }

   public static Runnable completeModLoading(Runnable initialScreensTask) {
      List<ModLoadingIssue> warnings = ModLoader.getLoadingIssues();
      boolean showWarnings = true;

      try {
         showWarnings = NeoForgeClientConfig.INSTANCE.showLoadWarnings.get();
      } catch (IllegalStateException | NullPointerException var5) {
      }

      if (error != null) {
         LanguageHook.loadBuiltinLanguages();
         File dumpedLocation = CrashReportExtender.dumpModLoadingCrashReport(LOGGER, error.getIssues(), Minecraft.getInstance().gameDirectory);
         return () -> Minecraft.getInstance().setScreen(new LoadingErrorScreen(error.getIssues(), dumpedLocation, () -> {}));
      } else {
         if (!warnings.isEmpty()) {
            if (showWarnings) {
               return () -> Minecraft.getInstance().setScreen(new LoadingErrorScreen(warnings, null, initialScreensTask));
            }

            LOGGER.warn(Logging.LOADING, "Mods loaded with {} warning(s)", warnings.size());

            for (ModLoadingIssue warning : warnings) {
               LOGGER.warn(Logging.LOADING, "{} [{}]", warning.translationKey(), warning.translationArgs());
            }
         }

         return initialScreensTask;
      }
   }

   public static boolean isLoading() {
      return loading;
   }
}
