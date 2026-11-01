package net.minecraft.util;

import com.google.common.hash.Funnels;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Map;
import java.util.OptionalLong;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class HttpUtil {
   private static final Logger LOGGER = LogUtils.getLogger();

   private HttpUtil() {
   }

   public static Path downloadFile(
      Path p_314509_,
      URL p_314583_,
      Map<String, String> p_314414_,
      HashFunction p_314462_,
      @Nullable HashCode p_314495_,
      int p_314514_,
      Proxy p_314631_,
      HttpUtil.DownloadProgressListener p_314610_
   ) {
      HttpURLConnection $$8 = null;
      InputStream $$9 = null;
      p_314610_.requestStart();
      Path $$10;
      if (p_314495_ != null) {
         $$10 = cachedFilePath(p_314509_, p_314495_);

         try {
            if (checkExistingFile($$10, p_314462_, p_314495_)) {
               LOGGER.info("Returning cached file since actual hash matches requested");
               p_314610_.requestFinished(true);
               updateModificationTime($$10);
               return $$10;
            }
         } catch (IOException var35) {
            LOGGER.warn("Failed to check cached file {}", $$10, var35);
         }

         try {
            LOGGER.warn("Existing file {} not found or had mismatched hash", $$10);
            Files.deleteIfExists($$10);
         } catch (IOException var34) {
            p_314610_.requestFinished(false);
            throw new UncheckedIOException("Failed to remove existing file " + $$10, var34);
         }
      } else {
         $$10 = null;
      }

      Path $$18;
      try {
         $$8 = (HttpURLConnection)p_314583_.openConnection(p_314631_);
         $$8.setInstanceFollowRedirects(true);
         p_314414_.forEach($$8::setRequestProperty);
         $$9 = $$8.getInputStream();
         long $$14 = $$8.getContentLengthLong();
         OptionalLong $$15 = $$14 != -1L ? OptionalLong.of($$14) : OptionalLong.empty();
         FileUtil.createDirectoriesSafe(p_314509_);
         p_314610_.downloadStart($$15);
         if ($$15.isPresent() && $$15.getAsLong() > p_314514_) {
            throw new IOException("Filesize is bigger than maximum allowed (file is " + $$15 + ", limit is " + p_314514_ + ")");
         }

         if ($$10 == null) {
            Path $$17 = Files.createTempFile(p_314509_, "download", ".tmp");

            try {
               HashCode $$18x = downloadAndHash(p_314462_, p_314514_, p_314610_, $$9, $$17);
               Path $$19 = cachedFilePath(p_314509_, $$18x);
               if (!checkExistingFile($$19, p_314462_, $$18x)) {
                  Files.move($$17, $$19, StandardCopyOption.REPLACE_EXISTING);
               } else {
                  updateModificationTime($$19);
               }

               p_314610_.requestFinished(true);
               return $$19;
            } finally {
               Files.deleteIfExists($$17);
            }
         }

         HashCode $$16 = downloadAndHash(p_314462_, p_314514_, p_314610_, $$9, $$10);
         if (!$$16.equals(p_314495_)) {
            throw new IOException("Hash of downloaded file (" + $$16 + ") did not match requested (" + p_314495_ + ")");
         }

         p_314610_.requestFinished(true);
         $$18 = $$10;
      } catch (Throwable var36) {
         if ($$8 != null) {
            InputStream $$21 = $$8.getErrorStream();
            if ($$21 != null) {
               try {
                  LOGGER.error("HTTP response error: {}", IOUtils.toString($$21, StandardCharsets.UTF_8));
               } catch (Exception var32) {
                  LOGGER.error("Failed to read response from server");
               }
            }
         }

         p_314610_.requestFinished(false);
         throw new IllegalStateException("Failed to download file " + p_314583_, var36);
      } finally {
         IOUtils.closeQuietly($$9);
      }

      return $$18;
   }

   private static void updateModificationTime(Path p_314996_) {
      try {
         Files.setLastModifiedTime(p_314996_, FileTime.from(Instant.now()));
      } catch (IOException var2) {
         LOGGER.warn("Failed to update modification time of {}", p_314996_, var2);
      }
   }

   private static HashCode hashFile(Path p_314478_, HashFunction p_314630_) throws IOException {
      Hasher $$2 = p_314630_.newHasher();

      try (
         OutputStream $$3 = Funnels.asOutputStream($$2);
         InputStream $$4 = Files.newInputStream(p_314478_);
      ) {
         $$4.transferTo($$3);
      }

      return $$2.hash();
   }

   private static boolean checkExistingFile(Path p_314420_, HashFunction p_314503_, HashCode p_314584_) throws IOException {
      if (Files.exists(p_314420_)) {
         HashCode $$3 = hashFile(p_314420_, p_314503_);
         if ($$3.equals(p_314584_)) {
            return true;
         }

         LOGGER.warn("Mismatched hash of file {}, expected {} but found {}", new Object[]{p_314420_, p_314584_, $$3});
      }

      return false;
   }

   private static Path cachedFilePath(Path p_314479_, HashCode p_314627_) {
      return p_314479_.resolve(p_314627_.toString());
   }

   private static HashCode downloadAndHash(
      HashFunction p_314429_, int p_314497_, HttpUtil.DownloadProgressListener p_314419_, InputStream p_314557_, Path p_314618_
   ) throws IOException {
      HashCode var11;
      try (OutputStream $$5 = Files.newOutputStream(p_314618_, StandardOpenOption.CREATE)) {
         Hasher $$6 = p_314429_.newHasher();
         byte[] $$7 = new byte[8196];
         long $$8 = 0L;

         int $$9;
         while (($$9 = p_314557_.read($$7)) >= 0) {
            $$8 += $$9;
            p_314419_.downloadedBytes($$8);
            if ($$8 > p_314497_) {
               throw new IOException("Filesize was bigger than maximum allowed (got >= " + $$8 + ", limit was " + p_314497_ + ")");
            }

            if (Thread.interrupted()) {
               LOGGER.error("INTERRUPTED");
               throw new IOException("Download interrupted");
            }

            $$5.write($$7, 0, $$9);
            $$6.putBytes($$7, 0, $$9);
         }

         var11 = $$6.hash();
      }

      return var11;
   }

   public static int getAvailablePort() {
      try {
         int var1;
         try (ServerSocket $$0 = new ServerSocket(0)) {
            var1 = $$0.getLocalPort();
         }

         return var1;
      } catch (IOException var5) {
         return 25564;
      }
   }

   public static boolean isPortAvailable(int p_259872_) {
      if (p_259872_ >= 0 && p_259872_ <= 65535) {
         try {
            boolean var2;
            try (ServerSocket $$1 = new ServerSocket(p_259872_)) {
               var2 = $$1.getLocalPort() == p_259872_;
            }

            return var2;
         } catch (IOException var6) {
            return false;
         }
      } else {
         return false;
      }
   }

   public interface DownloadProgressListener {
      void requestStart();

      void downloadStart(OptionalLong var1);

      void downloadedBytes(long var1);

      void requestFinished(boolean var1);
   }
}
