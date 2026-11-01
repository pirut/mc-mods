package net.neoforged.neoforge.common;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.apache.commons.io.output.CloseShieldOutputStream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IOUtilities {
   private static final String TEMP_FILE_SUFFIX = ".neoforge-tmp";
   private static final OpenOption[] OPEN_OPTIONS = new OpenOption[]{StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING};
   private static final Logger LOGGER = LoggerFactory.getLogger(IOUtilities.class);

   private IOUtilities() {
   }

   public static void tryCleanupTempFiles(Path targetPath, @Nullable String prefix) {
      for (Path file : tryListTempFiles(targetPath, prefix)) {
         try {
            Files.deleteIfExists(file);
         } catch (IOException var5) {
            LOGGER.error("Could not delete temp file {}: {}", file, var5.toString());
         }
      }
   }

   private static List<Path> tryListTempFiles(Path targetPath, @Nullable String prefix) {
      try {
         List var3;
         try (Stream<Path> stream = Files.find(targetPath, 1, createPredicate(prefix))) {
            var3 = stream.toList();
         }

         return var3;
      } catch (IOException var7) {
         LOGGER.error("Failed to list temporary files in {}", targetPath, var7);
         return List.of();
      }
   }

   private static BiPredicate<Path, BasicFileAttributes> createPredicate(@Nullable String prefix) {
      return (file, attributes) -> {
         String fileName = file.getFileName().toString();
         return fileName.endsWith(".neoforge-tmp") && (prefix == null || fileName.startsWith(prefix));
      };
   }

   public static void writeNbtCompressed(CompoundTag tag, Path path) throws IOException {
      atomicWrite(path, stream -> {
         try (BufferedOutputStream bufferedStream = new BufferedOutputStream(stream)) {
            NbtIo.writeCompressed(tag, bufferedStream);
         }
      });
   }

   public static void writeNbt(CompoundTag tag, Path path) throws IOException {
      atomicWrite(path, stream -> {
         try (
            BufferedOutputStream bufferedStream = new BufferedOutputStream(stream);
            DataOutputStream dataStream = new DataOutputStream(bufferedStream);
         ) {
            NbtIo.write(tag, dataStream);
         }
      });
   }

   public static void atomicWrite(Path targetPath, IOUtilities.WriteCallback writeCallback) throws IOException {
      Path tempPath = Files.createTempFile(targetPath.getParent(), targetPath.getFileName().toString(), ".neoforge-tmp");

      try {
         try (FileChannel channel = FileChannel.open(tempPath, OPEN_OPTIONS)) {
            CloseShieldOutputStream stream = CloseShieldOutputStream.wrap(Channels.newOutputStream(channel));
            writeCallback.write(stream);
            channel.force(true);
         }

         try {
            Files.move(tempPath, targetPath, StandardCopyOption.ATOMIC_MOVE);
         } catch (AtomicMoveNotSupportedException var8) {
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
         }
      } catch (Exception var10) {
         try {
            Files.deleteIfExists(tempPath);
         } catch (Exception var6) {
            var10.addSuppressed(var6);
         }

         throw var10;
      }
   }

   public interface WriteCallback {
      void write(OutputStream var1) throws IOException;
   }
}
