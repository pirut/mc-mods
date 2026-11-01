package net.minecraft.server.packs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.IoSupplier;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class FilePackResources extends AbstractPackResources {
   static final Logger LOGGER = LogUtils.getLogger();
   private final FilePackResources.SharedZipFileAccess zipFileAccess;
   private final String prefix;

   public FilePackResources(PackLocationInfo p_326506_, FilePackResources.SharedZipFileAccess p_296014_, String p_256076_) {
      super(p_326506_);
      this.zipFileAccess = p_296014_;
      this.prefix = p_256076_;
   }

   private static String getPathFromLocation(PackType p_250585_, ResourceLocation p_251470_) {
      return String.format(Locale.ROOT, "%s/%s/%s", p_250585_.getDirectory(), p_251470_.getNamespace(), p_251470_.getPath());
   }

   @Nullable
   @Override
   public IoSupplier<InputStream> getRootResource(String... p_248514_) {
      return this.getResource(String.join("/", p_248514_));
   }

   @Override
   public IoSupplier<InputStream> getResource(PackType p_249605_, ResourceLocation p_252147_) {
      return this.getResource(getPathFromLocation(p_249605_, p_252147_));
   }

   private String addPrefix(String p_295349_) {
      return this.prefix.isEmpty() ? p_295349_ : this.prefix + "/" + p_295349_;
   }

   @Nullable
   private IoSupplier<InputStream> getResource(String p_251795_) {
      ZipFile $$1 = this.zipFileAccess.getOrCreateZipFile();
      if ($$1 == null) {
         return null;
      } else {
         ZipEntry $$2 = $$1.getEntry(this.addPrefix(p_251795_));
         return $$2 == null ? null : IoSupplier.create($$1, $$2);
      }
   }

   @Override
   public Set<String> getNamespaces(PackType p_10238_) {
      ZipFile $$1 = this.zipFileAccess.getOrCreateZipFile();
      if ($$1 == null) {
         return Set.of();
      } else {
         Enumeration<? extends ZipEntry> $$2 = $$1.entries();
         Set<String> $$3 = Sets.newHashSet();
         String $$4 = this.addPrefix(p_10238_.getDirectory() + "/");

         while ($$2.hasMoreElements()) {
            ZipEntry $$5 = $$2.nextElement();
            String $$6 = $$5.getName();
            String $$7 = extractNamespace($$4, $$6);
            if (!$$7.isEmpty()) {
               if (ResourceLocation.isValidNamespace($$7)) {
                  $$3.add($$7);
               } else {
                  LOGGER.warn("Non [a-z0-9_.-] character in namespace {} in pack {}, ignoring", $$7, this.zipFileAccess.file);
               }
            }
         }

         return $$3;
      }
   }

   @VisibleForTesting
   public static String extractNamespace(String p_294953_, String p_294476_) {
      if (!p_294476_.startsWith(p_294953_)) {
         return "";
      } else {
         int $$2 = p_294953_.length();
         int $$3 = p_294476_.indexOf(47, $$2);
         return $$3 == -1 ? p_294476_.substring($$2) : p_294476_.substring($$2, $$3);
      }
   }

   @Override
   public void close() {
      this.zipFileAccess.close();
   }

   @Override
   public void listResources(PackType p_250500_, String p_249598_, String p_251613_, PackResources.ResourceOutput p_250655_) {
      ZipFile $$4 = this.zipFileAccess.getOrCreateZipFile();
      if ($$4 != null) {
         Enumeration<? extends ZipEntry> $$5 = $$4.entries();
         String $$6 = this.addPrefix(p_250500_.getDirectory() + "/" + p_249598_ + "/");
         String $$7 = $$6 + p_251613_ + "/";

         while ($$5.hasMoreElements()) {
            ZipEntry $$8 = $$5.nextElement();
            if (!$$8.isDirectory()) {
               String $$9 = $$8.getName();
               if ($$9.startsWith($$7)) {
                  String $$10 = $$9.substring($$6.length());
                  ResourceLocation $$11 = ResourceLocation.tryBuild(p_249598_, $$10);
                  if ($$11 != null) {
                     p_250655_.accept($$11, IoSupplier.create($$4, $$8));
                  } else {
                     LOGGER.warn("Invalid path in datapack: {}:{}, ignoring", p_249598_, $$10);
                  }
               }
            }
         }
      }
   }

   public static class FileResourcesSupplier implements Pack.ResourcesSupplier {
      private final File content;

      public FileResourcesSupplier(Path p_326243_) {
         this(p_326243_.toFile());
      }

      public FileResourcesSupplier(File p_326453_) {
         this.content = p_326453_;
      }

      @Override
      public PackResources openPrimary(PackLocationInfo p_326493_) {
         FilePackResources.SharedZipFileAccess $$1 = new FilePackResources.SharedZipFileAccess(this.content);
         return new FilePackResources(p_326493_, $$1, "");
      }

      @Override
      public PackResources openFull(PackLocationInfo p_326484_, Pack.Metadata p_326165_) {
         FilePackResources.SharedZipFileAccess $$2 = new FilePackResources.SharedZipFileAccess(this.content);
         PackResources $$3 = new FilePackResources(p_326484_, $$2, "");
         List<String> $$4 = p_326165_.overlays();
         if ($$4.isEmpty()) {
            return $$3;
         } else {
            List<PackResources> $$5 = new ArrayList<>($$4.size());

            for (String $$6 : $$4) {
               $$5.add(new FilePackResources(p_326484_, $$2, $$6));
            }

            return new CompositePackResources($$3, $$5);
         }
      }
   }

   public static class SharedZipFileAccess implements AutoCloseable {
      final File file;
      @Nullable
      private ZipFile zipFile;
      private boolean failedToLoad;

      public SharedZipFileAccess(File p_294132_) {
         this.file = p_294132_;
      }

      @Nullable
      ZipFile getOrCreateZipFile() {
         if (this.failedToLoad) {
            return null;
         } else {
            if (this.zipFile == null) {
               try {
                  this.zipFile = new ZipFile(this.file);
               } catch (IOException var2) {
                  FilePackResources.LOGGER.error("Failed to open pack {}", this.file, var2);
                  this.failedToLoad = true;
                  return null;
               }
            }

            return this.zipFile;
         }
      }

      @Override
      public void close() {
         if (this.zipFile != null) {
            IOUtils.closeQuietly(this.zipFile);
            this.zipFile = null;
         }
      }

      @Override
      protected void finalize() throws Throwable {
         this.close();
         super.finalize();
      }
   }
}
