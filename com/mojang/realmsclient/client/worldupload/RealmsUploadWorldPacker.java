package com.mojang.realmsclient.client.worldupload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;
import java.util.zip.GZIPOutputStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

@Environment(EnvType.CLIENT)
public class RealmsUploadWorldPacker {
   private static final long SIZE_LIMIT = 5368709120L;
   private static final String WORLD_FOLDER_NAME = "world";
   private final BooleanSupplier isCanceled;
   private final Path directoryToPack;

   public static File pack(Path directoryToPack, BooleanSupplier isCanceled) throws IOException {
      return new RealmsUploadWorldPacker(directoryToPack, isCanceled).tarGzipArchive();
   }

   private RealmsUploadWorldPacker(Path directoryToPack, BooleanSupplier isCanceled) {
      this.isCanceled = isCanceled;
      this.directoryToPack = directoryToPack;
   }

   private File tarGzipArchive() throws IOException {
      TarArchiveOutputStream tarArchiveOutputStream = null;

      File var3;
      try {
         File file = File.createTempFile("realms-upload-file", ".tar.gz");
         tarArchiveOutputStream = new TarArchiveOutputStream(new GZIPOutputStream(new FileOutputStream(file)));
         tarArchiveOutputStream.setLongFileMode(3);
         this.addFileToTarGz(tarArchiveOutputStream, this.directoryToPack, "world", true);
         if (this.isCanceled.getAsBoolean()) {
            throw new RealmsUploadCanceledException();
         }

         tarArchiveOutputStream.finish();
         this.verifyBelowSizeLimit(file.length());
         var3 = file;
      } finally {
         if (tarArchiveOutputStream != null) {
            tarArchiveOutputStream.close();
         }
      }

      return var3;
   }

   private void addFileToTarGz(TarArchiveOutputStream stream, Path directory, String prefix, boolean isRootDirectory) throws IOException {
      if (this.isCanceled.getAsBoolean()) {
         throw new RealmsUploadCanceledException();
      } else {
         this.verifyBelowSizeLimit(stream.getBytesWritten());
         File file = directory.toFile();
         String string = isRootDirectory ? prefix : prefix + file.getName();
         TarArchiveEntry tarArchiveEntry = new TarArchiveEntry(file, string);
         stream.putArchiveEntry(tarArchiveEntry);
         if (file.isFile()) {
            try (InputStream inputStream = new FileInputStream(file)) {
               inputStream.transferTo(stream);
            }

            stream.closeArchiveEntry();
         } else {
            stream.closeArchiveEntry();
            File[] files = file.listFiles();
            if (files != null) {
               for (File file2 : files) {
                  this.addFileToTarGz(stream, file2.toPath(), string + "/", false);
               }
            }
         }
      }
   }

   private void verifyBelowSizeLimit(long size) {
      if (size > 5368709120L) {
         throw new RealmsUploadTooLargeException(5368709120L);
      }
   }
}
