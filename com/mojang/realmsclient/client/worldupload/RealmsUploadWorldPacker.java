package com.mojang.realmsclient.client.worldupload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;
import java.util.zip.GZIPOutputStream;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

@OnlyIn(Dist.CLIENT)
public class RealmsUploadWorldPacker {
   private static final long SIZE_LIMIT = 5368709120L;
   private static final String WORLD_FOLDER_NAME = "world";
   private final BooleanSupplier isCanceled;
   private final Path directoryToPack;

   public static File pack(Path p_374057_, BooleanSupplier p_374422_) throws IOException {
      return new RealmsUploadWorldPacker(p_374057_, p_374422_).tarGzipArchive();
   }

   private RealmsUploadWorldPacker(Path p_374425_, BooleanSupplier p_374350_) {
      this.isCanceled = p_374350_;
      this.directoryToPack = p_374425_;
   }

   private File tarGzipArchive() throws IOException {
      TarArchiveOutputStream tararchiveoutputstream = null;

      File file2;
      try {
         File file1 = File.createTempFile("realms-upload-file", ".tar.gz");
         tararchiveoutputstream = new TarArchiveOutputStream(new GZIPOutputStream(new FileOutputStream(file1)));
         tararchiveoutputstream.setLongFileMode(3);
         this.addFileToTarGz(tararchiveoutputstream, this.directoryToPack, "world", true);
         if (this.isCanceled.getAsBoolean()) {
            throw new RealmsUploadCanceledException();
         }

         tararchiveoutputstream.finish();
         this.verifyBelowSizeLimit(file1.length());
         file2 = file1;
      } finally {
         if (tararchiveoutputstream != null) {
            tararchiveoutputstream.close();
         }
      }

      return file2;
   }

   private void addFileToTarGz(TarArchiveOutputStream p_374061_, Path p_374216_, String p_374206_, boolean p_374384_) throws IOException {
      if (this.isCanceled.getAsBoolean()) {
         throw new RealmsUploadCanceledException();
      } else {
         this.verifyBelowSizeLimit(p_374061_.getBytesWritten());
         File file1 = p_374216_.toFile();
         String s = p_374384_ ? p_374206_ : p_374206_ + file1.getName();
         TarArchiveEntry tararchiveentry = new TarArchiveEntry(file1, s);
         p_374061_.putArchiveEntry(tararchiveentry);
         if (file1.isFile()) {
            try (InputStream inputstream = new FileInputStream(file1)) {
               inputstream.transferTo(p_374061_);
            }

            p_374061_.closeArchiveEntry();
         } else {
            p_374061_.closeArchiveEntry();
            File[] afile = file1.listFiles();
            if (afile != null) {
               for (File file2 : afile) {
                  this.addFileToTarGz(p_374061_, file2.toPath(), s + "/", false);
               }
            }
         }
      }
   }

   private void verifyBelowSizeLimit(long p_374584_) {
      if (p_374584_ > 5368709120L) {
         throw new RealmsUploadTooLargeException(5368709120L);
      }
   }
}
