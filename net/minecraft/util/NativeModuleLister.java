package net.minecraft.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.logging.LogUtils;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.Version;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.Tlhelp32.MODULEENTRY32W;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import net.minecraft.CrashReportCategory;
import org.slf4j.Logger;

public class NativeModuleLister {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int LANG_MASK = 65535;
   private static final int DEFAULT_LANG = 1033;
   private static final int CODEPAGE_MASK = -65536;
   private static final int DEFAULT_CODEPAGE = 78643200;

   public static List<NativeModuleLister.NativeModuleInfo> listModules() {
      if (!Platform.isWindows()) {
         return ImmutableList.of();
      } else {
         int $$0 = Kernel32.INSTANCE.GetCurrentProcessId();
         Builder<NativeModuleLister.NativeModuleInfo> $$1 = ImmutableList.builder();

         for (MODULEENTRY32W $$3 : Kernel32Util.getModules($$0)) {
            String $$4 = $$3.szModule();
            Optional<NativeModuleLister.NativeModuleVersion> $$5 = tryGetVersion($$3.szExePath());
            $$1.add(new NativeModuleLister.NativeModuleInfo($$4, $$5));
         }

         return $$1.build();
      }
   }

   private static Optional<NativeModuleLister.NativeModuleVersion> tryGetVersion(String p_184674_) {
      try {
         IntByReference $$1 = new IntByReference();
         int $$2 = Version.INSTANCE.GetFileVersionInfoSize(p_184674_, $$1);
         if ($$2 == 0) {
            int $$3 = Native.getLastError();
            if ($$3 != 1813 && $$3 != 1812) {
               throw new Win32Exception($$3);
            } else {
               return Optional.empty();
            }
         } else {
            Pointer $$4 = new Memory($$2);
            if (!Version.INSTANCE.GetFileVersionInfo(p_184674_, 0, $$2, $$4)) {
               throw new Win32Exception(Native.getLastError());
            } else {
               IntByReference $$5 = new IntByReference();
               Pointer $$6 = queryVersionValue($$4, "\\VarFileInfo\\Translation", $$5);
               int[] $$7 = $$6.getIntArray(0L, $$5.getValue() / 4);
               OptionalInt $$8 = findLangAndCodepage($$7);
               if ($$8.isEmpty()) {
                  return Optional.empty();
               } else {
                  int $$9 = $$8.getAsInt();
                  int $$10 = $$9 & 65535;
                  int $$11 = ($$9 & -65536) >> 16;
                  String $$12 = queryVersionString($$4, langTableKey("FileDescription", $$10, $$11), $$5);
                  String $$13 = queryVersionString($$4, langTableKey("CompanyName", $$10, $$11), $$5);
                  String $$14 = queryVersionString($$4, langTableKey("FileVersion", $$10, $$11), $$5);
                  return Optional.of(new NativeModuleLister.NativeModuleVersion($$12, $$14, $$13));
               }
            }
         }
      } catch (Exception var14) {
         LOGGER.info("Failed to find module info for {}", p_184674_, var14);
         return Optional.empty();
      }
   }

   private static String langTableKey(String p_184676_, int p_184677_, int p_184678_) {
      return String.format(Locale.ROOT, "\\StringFileInfo\\%04x%04x\\%s", p_184677_, p_184678_, p_184676_);
   }

   private static OptionalInt findLangAndCodepage(int[] p_184682_) {
      OptionalInt $$1 = OptionalInt.empty();

      for (int $$2 : p_184682_) {
         if (($$2 & -65536) == 78643200 && ($$2 & 65535) == 1033) {
            return OptionalInt.of($$2);
         }

         $$1 = OptionalInt.of($$2);
      }

      return $$1;
   }

   private static Pointer queryVersionValue(Pointer p_184670_, String p_184671_, IntByReference p_184672_) {
      PointerByReference $$3 = new PointerByReference();
      if (!Version.INSTANCE.VerQueryValue(p_184670_, p_184671_, $$3, p_184672_)) {
         throw new UnsupportedOperationException("Can't get version value " + p_184671_);
      } else {
         return $$3.getValue();
      }
   }

   private static String queryVersionString(Pointer p_184687_, String p_184688_, IntByReference p_184689_) {
      try {
         Pointer $$3 = queryVersionValue(p_184687_, p_184688_, p_184689_);
         byte[] $$4 = $$3.getByteArray(0L, (p_184689_.getValue() - 1) * 2);
         return new String($$4, StandardCharsets.UTF_16LE);
      } catch (Exception var5) {
         return "";
      }
   }

   public static void addCrashSection(CrashReportCategory p_184680_) {
      p_184680_.setDetail(
         "Modules",
         () -> listModules()
            .stream()
            .sorted(Comparator.comparing(p_184685_ -> p_184685_.name))
            .map(p_339486_ -> "\n\t\t" + p_339486_)
            .collect(Collectors.joining())
      );
   }

   public static class NativeModuleInfo {
      public final String name;
      public final Optional<NativeModuleLister.NativeModuleVersion> version;

      public NativeModuleInfo(String p_184693_, Optional<NativeModuleLister.NativeModuleVersion> p_184694_) {
         this.name = p_184693_;
         this.version = p_184694_;
      }

      @Override
      public String toString() {
         return this.version.<String>map(p_339487_ -> this.name + ":" + p_339487_).orElse(this.name);
      }
   }

   public static class NativeModuleVersion {
      public final String description;
      public final String version;
      public final String company;

      public NativeModuleVersion(String p_184702_, String p_184703_, String p_184704_) {
         this.description = p_184702_;
         this.version = p_184703_;
         this.company = p_184704_;
      }

      @Override
      public String toString() {
         return this.description + ":" + this.version + ":" + this.company;
      }
   }
}
