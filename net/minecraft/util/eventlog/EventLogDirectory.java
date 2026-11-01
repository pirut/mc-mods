package net.minecraft.util.eventlog;

import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.annotation.Nullable;
import org.slf4j.Logger;

public class EventLogDirectory {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final int COMPRESS_BUFFER_SIZE = 4096;
   private static final String COMPRESSED_EXTENSION = ".gz";
   private final Path root;
   private final String extension;

   private EventLogDirectory(Path p_261546_, String p_261467_) {
      this.root = p_261546_;
      this.extension = p_261467_;
   }

   public static EventLogDirectory open(Path p_261743_, String p_261659_) throws IOException {
      Files.createDirectories(p_261743_);
      return new EventLogDirectory(p_261743_, p_261659_);
   }

   public EventLogDirectory.FileList listFiles() throws IOException {
      EventLogDirectory.FileList var2;
      try (Stream<Path> $$0 = Files.list(this.root)) {
         var2 = new EventLogDirectory.FileList($$0.filter(p_262170_ -> Files.isRegularFile(p_262170_)).map(this::parseFile).filter(Objects::nonNull).toList());
      }

      return var2;
   }

   @Nullable
   private EventLogDirectory.File parseFile(Path p_261985_) {
      String $$1 = p_261985_.getFileName().toString();
      int $$2 = $$1.indexOf(46);
      if ($$2 == -1) {
         return null;
      } else {
         EventLogDirectory.FileId $$3 = EventLogDirectory.FileId.parse($$1.substring(0, $$2));
         if ($$3 != null) {
            String $$4 = $$1.substring($$2);
            if ($$4.equals(this.extension)) {
               return new EventLogDirectory.RawFile(p_261985_, $$3);
            }

            if ($$4.equals(this.extension + ".gz")) {
               return new EventLogDirectory.CompressedFile(p_261985_, $$3);
            }
         }

         return null;
      }
   }

   static void tryCompress(Path p_261741_, Path p_262101_) throws IOException {
      if (Files.exists(p_262101_)) {
         throw new IOException("Compressed target file already exists: " + p_262101_);
      } else {
         try (FileChannel $$2 = FileChannel.open(p_261741_, StandardOpenOption.WRITE, StandardOpenOption.READ)) {
            FileLock $$3 = $$2.tryLock();
            if ($$3 == null) {
               throw new IOException("Raw log file is already locked, cannot compress: " + p_261741_);
            }

            writeCompressed($$2, p_262101_);
            $$2.truncate(0L);
         }

         Files.delete(p_261741_);
      }
   }

   private static void writeCompressed(ReadableByteChannel p_262066_, Path p_262054_) throws IOException {
      try (OutputStream $$2 = new GZIPOutputStream(Files.newOutputStream(p_262054_))) {
         byte[] $$3 = new byte[4096];
         ByteBuffer $$4 = ByteBuffer.wrap($$3);

         while (p_262066_.read($$4) >= 0) {
            $$4.flip();
            $$2.write($$3, 0, $$4.limit());
            $$4.clear();
         }
      }
   }

   public EventLogDirectory.RawFile createNewFile(LocalDate p_261865_) throws IOException {
      int $$1 = 1;
      Set<EventLogDirectory.FileId> $$2 = this.listFiles().ids();

      EventLogDirectory.FileId $$3;
      do {
         $$3 = new EventLogDirectory.FileId(p_261865_, $$1++);
      } while ($$2.contains($$3));

      EventLogDirectory.RawFile $$4 = new EventLogDirectory.RawFile(this.root.resolve($$3.toFileName(this.extension)), $$3);
      Files.createFile($$4.path());
      return $$4;
   }

   public record CompressedFile(Path path, EventLogDirectory.FileId id) implements EventLogDirectory.File {
      @Nullable
      @Override
      public Reader openReader() throws IOException {
         return !Files.exists(this.path) ? null : new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(this.path))));
      }

      @Override
      public EventLogDirectory.CompressedFile compress() {
         return this;
      }
   }

   public interface File {
      Path path();

      EventLogDirectory.FileId id();

      @Nullable
      Reader openReader() throws IOException;

      EventLogDirectory.CompressedFile compress() throws IOException;
   }

   public record FileId(LocalDate date, int index) {
      private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

      @Nullable
      public static EventLogDirectory.FileId parse(String p_261762_) {
         int $$1 = p_261762_.indexOf("-");
         if ($$1 == -1) {
            return null;
         } else {
            String $$2 = p_261762_.substring(0, $$1);
            String $$3 = p_261762_.substring($$1 + 1);

            try {
               return new EventLogDirectory.FileId(LocalDate.parse($$2, DATE_FORMATTER), Integer.parseInt($$3));
            } catch (DateTimeParseException | NumberFormatException var5) {
               return null;
            }
         }
      }

      @Override
      public String toString() {
         return DATE_FORMATTER.format(this.date) + "-" + this.index;
      }

      public String toFileName(String p_261982_) {
         return this + p_261982_;
      }
   }

   public static class FileList implements Iterable<EventLogDirectory.File> {
      private final List<EventLogDirectory.File> files;

      FileList(List<EventLogDirectory.File> p_261941_) {
         this.files = new ArrayList<>(p_261941_);
      }

      public EventLogDirectory.FileList prune(LocalDate p_261825_, int p_261918_) {
         this.files.removeIf(p_261494_ -> {
            EventLogDirectory.FileId $$3 = p_261494_.id();
            LocalDate $$4 = $$3.date().plusDays(p_261918_);
            if (!p_261825_.isBefore($$4)) {
               try {
                  Files.delete(p_261494_.path());
                  return true;
               } catch (IOException var6) {
                  EventLogDirectory.LOGGER.warn("Failed to delete expired event log file: {}", p_261494_.path(), var6);
               }
            }

            return false;
         });
         return this;
      }

      public EventLogDirectory.FileList compressAll() {
         ListIterator<EventLogDirectory.File> $$0 = this.files.listIterator();

         while ($$0.hasNext()) {
            EventLogDirectory.File $$1 = $$0.next();

            try {
               $$0.set($$1.compress());
            } catch (IOException var4) {
               EventLogDirectory.LOGGER.warn("Failed to compress event log file: {}", $$1.path(), var4);
            }
         }

         return this;
      }

      @Override
      public Iterator<EventLogDirectory.File> iterator() {
         return this.files.iterator();
      }

      public Stream<EventLogDirectory.File> stream() {
         return this.files.stream();
      }

      public Set<EventLogDirectory.FileId> ids() {
         return this.files.stream().map(EventLogDirectory.File::id).collect(Collectors.toSet());
      }
   }

   public record RawFile(Path path, EventLogDirectory.FileId id) implements EventLogDirectory.File {
      public FileChannel openChannel() throws IOException {
         return FileChannel.open(this.path, StandardOpenOption.WRITE, StandardOpenOption.READ);
      }

      @Nullable
      @Override
      public Reader openReader() throws IOException {
         return Files.exists(this.path) ? Files.newBufferedReader(this.path) : null;
      }

      @Override
      public EventLogDirectory.CompressedFile compress() throws IOException {
         Path $$0 = this.path.resolveSibling(this.path.getFileName().toString() + ".gz");
         EventLogDirectory.tryCompress(this.path, $$0);
         return new EventLogDirectory.CompressedFile($$0, this.id);
      }
   }
}
