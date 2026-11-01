package net.minecraft;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.PhysicalMemory;
import oshi.hardware.VirtualMemory;
import oshi.hardware.CentralProcessor.ProcessorIdentifier;

public class SystemReport {
   public static final long BYTES_PER_MEBIBYTE = 1048576L;
   private static final long ONE_GIGA = 1000000000L;
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String OPERATING_SYSTEM = System.getProperty("os.name")
      + " ("
      + System.getProperty("os.arch")
      + ") version "
      + System.getProperty("os.version");
   private static final String JAVA_VERSION = System.getProperty("java.version") + ", " + System.getProperty("java.vendor");
   private static final String JAVA_VM_VERSION = System.getProperty("java.vm.name")
      + " ("
      + System.getProperty("java.vm.info")
      + "), "
      + System.getProperty("java.vm.vendor");
   private final Map<String, String> entries = Maps.newLinkedHashMap();

   public SystemReport() {
      this.setDetail("Minecraft Version", SharedConstants.getCurrentVersion().getName());
      this.setDetail("Minecraft Version ID", SharedConstants.getCurrentVersion().getId());
      this.setDetail("Operating System", OPERATING_SYSTEM);
      this.setDetail("Java Version", JAVA_VERSION);
      this.setDetail("Java VM Version", JAVA_VM_VERSION);
      this.setDetail("Memory", () -> {
         Runtime $$0 = Runtime.getRuntime();
         long $$1 = $$0.maxMemory();
         long $$2 = $$0.totalMemory();
         long $$3 = $$0.freeMemory();
         long $$4 = $$1 / 1048576L;
         long $$5 = $$2 / 1048576L;
         long $$6 = $$3 / 1048576L;
         return $$3 + " bytes (" + $$6 + " MiB) / " + $$2 + " bytes (" + $$5 + " MiB) up to " + $$1 + " bytes (" + $$4 + " MiB)";
      });
      this.setDetail("CPUs", () -> String.valueOf(Runtime.getRuntime().availableProcessors()));
      this.ignoreErrors("hardware", () -> this.putHardware(new SystemInfo()));
      this.setDetail("JVM Flags", () -> {
         List<String> $$0 = Util.getVmArguments().collect(Collectors.toList());
         return String.format(Locale.ROOT, "%d total; %s", $$0.size(), String.join(" ", $$0));
      });
   }

   public void setDetail(String p_143520_, String p_143521_) {
      this.entries.put(p_143520_, p_143521_);
   }

   public void setDetail(String p_143523_, Supplier<String> p_143524_) {
      try {
         this.setDetail(p_143523_, p_143524_.get());
      } catch (Exception var4) {
         LOGGER.warn("Failed to get system info for {}", p_143523_, var4);
         this.setDetail(p_143523_, "ERR");
      }
   }

   private void putHardware(SystemInfo p_143536_) {
      HardwareAbstractionLayer $$1 = p_143536_.getHardware();
      this.ignoreErrors("processor", () -> this.putProcessor($$1.getProcessor()));
      this.ignoreErrors("graphics", () -> this.putGraphics($$1.getGraphicsCards()));
      this.ignoreErrors("memory", () -> this.putMemory($$1.getMemory()));
      this.ignoreErrors("storage", this::putStorage);
   }

   private void ignoreErrors(String p_143517_, Runnable p_143518_) {
      try {
         p_143518_.run();
      } catch (Throwable var4) {
         LOGGER.warn("Failed retrieving info for group {}", p_143517_, var4);
      }
   }

   public static float sizeInMiB(long p_345366_) {
      return (float)p_345366_ / 1048576.0F;
   }

   private void putPhysicalMemory(List<PhysicalMemory> p_143532_) {
      int $$1 = 0;

      for (PhysicalMemory $$2 : p_143532_) {
         String $$3 = String.format(Locale.ROOT, "Memory slot #%d ", $$1++);
         this.setDetail($$3 + "capacity (MiB)", () -> String.format(Locale.ROOT, "%.2f", sizeInMiB($$2.getCapacity())));
         this.setDetail($$3 + "clockSpeed (GHz)", () -> String.format(Locale.ROOT, "%.2f", (float)$$2.getClockSpeed() / 1.0E9F));
         this.setDetail($$3 + "type", $$2::getMemoryType);
      }
   }

   private void putVirtualMemory(VirtualMemory p_143550_) {
      this.setDetail("Virtual memory max (MiB)", () -> String.format(Locale.ROOT, "%.2f", sizeInMiB(p_143550_.getVirtualMax())));
      this.setDetail("Virtual memory used (MiB)", () -> String.format(Locale.ROOT, "%.2f", sizeInMiB(p_143550_.getVirtualInUse())));
      this.setDetail("Swap memory total (MiB)", () -> String.format(Locale.ROOT, "%.2f", sizeInMiB(p_143550_.getSwapTotal())));
      this.setDetail("Swap memory used (MiB)", () -> String.format(Locale.ROOT, "%.2f", sizeInMiB(p_143550_.getSwapUsed())));
   }

   private void putMemory(GlobalMemory p_143542_) {
      this.ignoreErrors("physical memory", () -> this.putPhysicalMemory(p_143542_.getPhysicalMemory()));
      this.ignoreErrors("virtual memory", () -> this.putVirtualMemory(p_143542_.getVirtualMemory()));
   }

   private void putGraphics(List<GraphicsCard> p_143553_) {
      int $$1 = 0;

      for (GraphicsCard $$2 : p_143553_) {
         String $$3 = String.format(Locale.ROOT, "Graphics card #%d ", $$1++);
         this.setDetail($$3 + "name", $$2::getName);
         this.setDetail($$3 + "vendor", $$2::getVendor);
         this.setDetail($$3 + "VRAM (MiB)", () -> String.format(Locale.ROOT, "%.2f", sizeInMiB($$2.getVRam())));
         this.setDetail($$3 + "deviceId", $$2::getDeviceId);
         this.setDetail($$3 + "versionInfo", $$2::getVersionInfo);
      }
   }

   private void putProcessor(CentralProcessor p_143540_) {
      ProcessorIdentifier $$1 = p_143540_.getProcessorIdentifier();
      this.setDetail("Processor Vendor", $$1::getVendor);
      this.setDetail("Processor Name", $$1::getName);
      this.setDetail("Identifier", $$1::getIdentifier);
      this.setDetail("Microarchitecture", $$1::getMicroarchitecture);
      this.setDetail("Frequency (GHz)", () -> String.format(Locale.ROOT, "%.2f", (float)$$1.getVendorFreq() / 1.0E9F));
      this.setDetail("Number of physical packages", () -> String.valueOf(p_143540_.getPhysicalPackageCount()));
      this.setDetail("Number of physical CPUs", () -> String.valueOf(p_143540_.getPhysicalProcessorCount()));
      this.setDetail("Number of logical CPUs", () -> String.valueOf(p_143540_.getLogicalProcessorCount()));
   }

   private void putStorage() {
      this.putSpaceForProperty("jna.tmpdir");
      this.putSpaceForProperty("org.lwjgl.system.SharedLibraryExtractPath");
      this.putSpaceForProperty("io.netty.native.workdir");
      this.putSpaceForProperty("java.io.tmpdir");
      this.putSpaceForPath("workdir", () -> "");
   }

   private void putSpaceForProperty(String p_345533_) {
      this.putSpaceForPath(p_345533_, () -> System.getProperty(p_345533_));
   }

   private void putSpaceForPath(String p_344883_, Supplier<String> p_345715_) {
      String $$2 = "Space in storage for " + p_344883_ + " (MiB)";

      try {
         String $$3 = p_345715_.get();
         if ($$3 == null) {
            this.setDetail($$2, "<path not set>");
            return;
         }

         FileStore $$4 = Files.getFileStore(Path.of($$3));
         this.setDetail($$2, String.format(Locale.ROOT, "available: %.2f, total: %.2f", sizeInMiB($$4.getUsableSpace()), sizeInMiB($$4.getTotalSpace())));
      } catch (InvalidPathException var6) {
         LOGGER.warn("{} is not a path", p_344883_, var6);
         this.setDetail($$2, "<invalid path>");
      } catch (Exception var7) {
         LOGGER.warn("Failed retrieving storage space for {}", p_344883_, var7);
         this.setDetail($$2, "ERR");
      }
   }

   public void appendToCrashReportString(StringBuilder p_143526_) {
      p_143526_.append("-- ").append("System Details").append(" --\n");
      p_143526_.append("Details:");
      this.entries.forEach((p_143529_, p_143530_) -> {
         p_143526_.append("\n\t");
         p_143526_.append(p_143529_);
         p_143526_.append(": ");
         p_143526_.append(p_143530_);
      });
   }

   public String toLineSeparatedString() {
      return this.entries
         .entrySet()
         .stream()
         .map(p_143534_ -> p_143534_.getKey() + ": " + p_143534_.getValue())
         .collect(Collectors.joining(System.lineSeparator()));
   }
}
