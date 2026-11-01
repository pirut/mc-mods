package net.neoforged.neoforge.logging;

import cpw.mods.modlauncher.log.TransformingThrowablePatternConverter;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import joptsimple.internal.Strings;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportType;
import net.minecraft.SystemReport;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.CrashReportCallables;
import net.neoforged.fml.ISystemReportExtender;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.i18n.FMLTranslations;
import net.neoforged.neoforge.forge.snapshots.ForgeSnapshotsMod;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import org.apache.logging.log4j.Logger;
import org.apache.maven.artifact.versioning.ArtifactVersion;

public class CrashReportExtender {
   private static final StackTraceElement[] BLANK_STACK_TRACE = new StackTraceElement[0];

   public static void extendSystemReport(SystemReport systemReport) {
      for (ISystemReportExtender call : CrashReportCallables.allCrashCallables()) {
         if (call.isActive()) {
            systemReport.setDetail(call.getLabel(), call);
         }
      }
   }

   public static void addCrashReportHeader(StringBuilder stringbuilder, CrashReport crashReport) {
      ForgeSnapshotsMod.addCrashReportHeader(stringbuilder, crashReport);
   }

   public static String generateEnhancedStackTrace(Throwable throwable) {
      return generateEnhancedStackTrace(throwable, true);
   }

   public static String generateEnhancedStackTrace(StackTraceElement[] stacktrace) {
      Throwable t = new Throwable();
      t.setStackTrace(stacktrace);
      return generateEnhancedStackTrace(t, false);
   }

   public static String generateEnhancedStackTrace(Throwable throwable, boolean header) {
      String s = TransformingThrowablePatternConverter.generateEnhancedStackTrace(throwable);
      return header ? s : s.substring(s.indexOf(Strings.LINE_SEPARATOR));
   }

   public static File dumpModLoadingCrashReport(Logger logger, List<ModLoadingIssue> issues, File topLevelDir) {
      CrashReport crashReport = CrashReport.forThrowable(
         new CrashReportExtender.ModLoadingCrashException("Mod loading has failed"),
         "Mod loading failures have occurred; consult the issue messages for more details"
      );

      for (ModLoadingIssue issue : issues) {
         Optional<IModInfo> modInfo = Optional.ofNullable(issue.affectedMod());
         CrashReportCategory category = crashReport.addCategory(
            modInfo.<String>map(iModInfo -> "Mod loading issue for: " + iModInfo.getModId()).orElse("Mod loading issue")
         );
         Throwable cause = issue.cause();

         for (int depth = 0; cause != null && cause.getCause() != null && cause.getCause() != cause; cause = cause.getCause()) {
            category.setDetail(
               "Caused by " + depth++, cause + generateEnhancedStackTrace(cause.getStackTrace()).replaceAll(Strings.LINE_SEPARATOR + "\t", "\n\t\t")
            );
         }

         if (cause != null) {
            category.setStackTrace(cause.getStackTrace());
         } else {
            category.setStackTrace(BLANK_STACK_TRACE);
         }

         category.setDetail(
            "Mod file",
            () -> modInfo.<IModFileInfo>map(IModInfo::getOwningFile)
               .map(t -> t.getFile().getFilePath().toUri().getPath())
               .orElse("<No mod information provided>")
         );

         try {
            category.setDetail("Failure message", () -> FMLTranslations.stripControlCodes(FMLTranslations.translateIssueEnglish(issue)).replace("\n", "\n\t\t"));
         } catch (Exception var13) {
            category.setDetail("Failure message", () -> issue.translationKey().replace("\n", "\n\t\t"));

            for (int i = 0; i < issue.translationArgs().size(); i++) {
               Object arg = issue.translationArgs().get(i);
               category.setDetail("Failure message arg " + (i + 1), () -> arg.toString().replace("\n", "\n\t\t"));
            }
         }

         category.setDetail(
            "Mod version", () -> modInfo.<ArtifactVersion>map(IModInfo::getVersion).map(Object::toString).orElse("<No mod information provided>")
         );
         category.setDetail(
            "Mod issues URL",
            () -> modInfo.<Object>map(IModInfo::getOwningFile)
               .map(IModFileInfo.class::cast)
               .flatMap(mfi -> mfi.getConfig().getConfigElement(new String[]{"issueTrackerURL"}))
               .orElse("<No issues URL found>")
         );
         category.setDetail("Exception message", Objects.toString(cause, "<No associated exception found>"));
      }

      File file1 = new File(topLevelDir, "crash-reports");
      File file2 = new File(file1, "crash-" + new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date()) + "-fml.txt");
      if (crashReport.saveToFile(file2.toPath(), ReportType.CRASH)) {
         logger.fatal("Crash report saved to {}", file2);
      } else {
         logger.fatal("Failed to save crash report");
      }

      Bootstrap.realStdoutPrintln(crashReport.getFriendlyReport(ReportType.CRASH));
      return file2;
   }

   private static class ModLoadingCrashException extends Exception {
      public ModLoadingCrashException(String message) {
         super(message);
      }

      @Override
      public synchronized Throwable fillInStackTrace() {
         return this;
      }
   }
}
