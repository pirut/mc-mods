package net.neoforged.neoforge.server.loading;

import java.io.File;
import java.util.List;
import net.neoforged.fml.Logging;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.ModLoadingException;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.ModWorkManager;
import net.neoforged.fml.ModLoadingIssue.Severity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.LogicalSidedProvider;
import net.neoforged.neoforge.internal.CommonModLoader;
import net.neoforged.neoforge.logging.CrashReportExtender;
import net.neoforged.neoforge.server.LanguageHook;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class ServerModLoader extends CommonModLoader {
   private static final Logger LOGGER = LogManager.getLogger();
   private static boolean hasErrors = false;

   public static void load() {
      LogicalSidedProvider.setServer(() -> {
         throw new IllegalStateException("Unable to access server yet");
      });
      LanguageHook.loadBuiltinLanguages();

      try {
         begin(() -> {}, false);
         load(ModWorkManager.syncExecutor(), ModWorkManager.parallelExecutor());
         finish(ModWorkManager.syncExecutor(), ModWorkManager.parallelExecutor());
      } catch (ModLoadingException var3) {
         hasErrors = true;
         LanguageHook.loadBuiltinLanguages();
         CrashReportExtender.dumpModLoadingCrashReport(LOGGER, var3.getIssues(), new File("."));
         throw var3;
      }

      List<ModLoadingIssue> warnings = ModLoader.getLoadingIssues().stream().filter(i -> i.severity() == Severity.WARNING).toList();
      if (!warnings.isEmpty()) {
         LOGGER.warn(Logging.LOADING, "Mods loaded with {} issues", warnings.size());

         for (ModLoadingIssue issue : warnings) {
            LOGGER.warn(Logging.LOADING, "{} [{}]", issue.translationKey(), issue.translationArgs());
         }
      }

      NeoForge.EVENT_BUS.start();
   }

   public static boolean hasErrors() {
      return hasErrors;
   }
}
