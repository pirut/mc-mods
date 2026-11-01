package net.minecraft.util;

import com.google.common.base.Charsets;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import net.minecraft.FileUtil;

public class DirectoryLock implements AutoCloseable {
   public static final String LOCK_FILE = "session.lock";
   private final FileChannel lockFile;
   private final FileLock lock;
   private static final ByteBuffer DUMMY;

   public static DirectoryLock create(Path p_13641_) throws IOException {
      Path $$1 = p_13641_.resolve("session.lock");
      FileUtil.createDirectoriesSafe(p_13641_);
      FileChannel $$2 = FileChannel.open($$1, StandardOpenOption.CREATE, StandardOpenOption.WRITE);

      try {
         $$2.write(DUMMY.duplicate());
         $$2.force(true);
         FileLock $$3 = $$2.tryLock();
         if ($$3 == null) {
            throw DirectoryLock.LockException.alreadyLocked($$1);
         } else {
            return new DirectoryLock($$2, $$3);
         }
      } catch (IOException var6) {
         try {
            $$2.close();
         } catch (IOException var5) {
            var6.addSuppressed(var5);
         }

         throw var6;
      }
   }

   private DirectoryLock(FileChannel p_13637_, FileLock p_13638_) {
      this.lockFile = p_13637_;
      this.lock = p_13638_;
   }

   @Override
   public void close() throws IOException {
      try {
         if (this.lock.isValid()) {
            this.lock.release();
         }
      } finally {
         if (this.lockFile.isOpen()) {
            this.lockFile.close();
         }
      }
   }

   public boolean isValid() {
      return this.lock.isValid();
   }

   public static boolean isLocked(Path p_13643_) throws IOException {
      Path $$1 = p_13643_.resolve("session.lock");

      try {
         boolean var4;
         try (
            FileChannel $$2 = FileChannel.open($$1, StandardOpenOption.WRITE);
            FileLock $$3 = $$2.tryLock();
         ) {
            var4 = $$3 == null;
         }

         return var4;
      } catch (AccessDeniedException var10) {
         return true;
      } catch (NoSuchFileException var11) {
         return false;
      }
   }

   static {
      byte[] $$0 = "☃".getBytes(Charsets.UTF_8);
      DUMMY = ByteBuffer.allocateDirect($$0.length);
      DUMMY.put($$0);
      DUMMY.flip();
   }

   public static class LockException extends IOException {
      private LockException(Path p_13646_, String p_13647_) {
         super(p_13646_.toAbsolutePath() + ": " + p_13647_);
      }

      public static DirectoryLock.LockException alreadyLocked(Path p_13649_) {
         return new DirectoryLock.LockException(p_13649_, "already locked (possibly by other Minecraft instance?)");
      }
   }
}
