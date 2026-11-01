package net.minecraft.nbt;

import com.google.common.annotations.VisibleForTesting;
import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UTFDataFormatException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.util.DelegateDataOutput;
import net.minecraft.util.FastBufferedInputStream;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class NbtIo {
   private static final OpenOption[] SYNC_OUTPUT_OPTIONS = new OpenOption[]{
      StandardOpenOption.SYNC, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
   };

   public static CompoundTag readCompressed(Path path, NbtAccounter accounter) throws IOException {
      CompoundTag var4;
      try (
         InputStream inputStream = Files.newInputStream(path);
         InputStream inputStream2 = new FastBufferedInputStream(inputStream);
      ) {
         var4 = readCompressed(inputStream2, accounter);
      }

      return var4;
   }

   private static DataInputStream createDecompressorStream(InputStream zippedStream) throws IOException {
      return new DataInputStream(new FastBufferedInputStream(new GZIPInputStream(zippedStream)));
   }

   private static DataOutputStream createCompressorStream(OutputStream outputSteam) throws IOException {
      return new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(outputSteam)));
   }

   public static CompoundTag readCompressed(InputStream zippedStream, NbtAccounter accounter) throws IOException {
      CompoundTag var3;
      try (DataInputStream dataInputStream = createDecompressorStream(zippedStream)) {
         var3 = read(dataInputStream, accounter);
      }

      return var3;
   }

   public static void parseCompressed(Path path, StreamTagVisitor visitor, NbtAccounter accounter) throws IOException {
      try (
         InputStream inputStream = Files.newInputStream(path);
         InputStream inputStream2 = new FastBufferedInputStream(inputStream);
      ) {
         parseCompressed(inputStream2, visitor, accounter);
      }
   }

   public static void parseCompressed(InputStream zippedStream, StreamTagVisitor visitor, NbtAccounter accounter) throws IOException {
      try (DataInputStream dataInputStream = createDecompressorStream(zippedStream)) {
         parse(dataInputStream, visitor, accounter);
      }
   }

   public static void writeCompressed(CompoundTag compoundTag, Path path) throws IOException {
      try (
         OutputStream outputStream = Files.newOutputStream(path, SYNC_OUTPUT_OPTIONS);
         OutputStream outputStream2 = new BufferedOutputStream(outputStream);
      ) {
         writeCompressed(compoundTag, outputStream2);
      }
   }

   public static void writeCompressed(CompoundTag compoundTag, OutputStream outputStream) throws IOException {
      try (DataOutputStream dataOutputStream = createCompressorStream(outputStream)) {
         write(compoundTag, dataOutputStream);
      }
   }

   public static void write(CompoundTag compoundTag, Path path) throws IOException {
      try (
         OutputStream outputStream = Files.newOutputStream(path, SYNC_OUTPUT_OPTIONS);
         OutputStream outputStream2 = new BufferedOutputStream(outputStream);
         DataOutputStream dataOutputStream = new DataOutputStream(outputStream2);
      ) {
         write(compoundTag, dataOutputStream);
      }
   }

   @Nullable
   public static CompoundTag read(Path path) throws IOException {
      if (!Files.exists(path)) {
         return null;
      } else {
         CompoundTag var3;
         try (
            InputStream inputStream = Files.newInputStream(path);
            DataInputStream dataInputStream = new DataInputStream(inputStream);
         ) {
            var3 = read(dataInputStream, NbtAccounter.unlimitedHeap());
         }

         return var3;
      }
   }

   public static CompoundTag read(DataInput input) throws IOException {
      return read(input, NbtAccounter.unlimitedHeap());
   }

   public static CompoundTag read(DataInput input, NbtAccounter accounter) throws IOException {
      Tag tag = readUnnamedTag(input, accounter);
      if (tag instanceof CompoundTag) {
         return (CompoundTag)tag;
      } else {
         throw new IOException("Root tag must be a named compound tag");
      }
   }

   public static void write(CompoundTag compoundTag, DataOutput output) throws IOException {
      writeUnnamedTagWithFallback(compoundTag, output);
   }

   public static void parse(DataInput input, StreamTagVisitor visitor, NbtAccounter accounter) throws IOException {
      TagType<?> tagType = TagTypes.getType(input.readByte());
      if (tagType == EndTag.TYPE) {
         if (visitor.visitRootEntry(EndTag.TYPE) == StreamTagVisitor.ValueResult.CONTINUE) {
            visitor.visitEnd();
         }
      } else {
         switch (visitor.visitRootEntry(tagType)) {
            case HALT:
            default:
               break;
            case BREAK:
               StringTag.skipString(input);
               tagType.skip(input, accounter);
               break;
            case CONTINUE:
               StringTag.skipString(input);
               tagType.parse(input, visitor, accounter);
         }
      }
   }

   public static Tag readAnyTag(DataInput input, NbtAccounter accounter) throws IOException {
      byte b = input.readByte();
      return (Tag)(b == 0 ? EndTag.INSTANCE : readTagSafe(input, accounter, b));
   }

   public static void writeAnyTag(Tag tag, DataOutput output) throws IOException {
      output.writeByte(tag.getId());
      if (tag.getId() != 0) {
         tag.write(output);
      }
   }

   public static void writeUnnamedTag(Tag tag, DataOutput output) throws IOException {
      output.writeByte(tag.getId());
      if (tag.getId() != 0) {
         output.writeUTF("");
         tag.write(output);
      }
   }

   public static void writeUnnamedTagWithFallback(Tag tag, DataOutput output) throws IOException {
      writeUnnamedTag(tag, new NbtIo.StringFallbackDataOutput(output));
   }

   @VisibleForTesting
   public static Tag readUnnamedTag(DataInput input, NbtAccounter accounter) throws IOException {
      byte b = input.readByte();
      if (b == 0) {
         return EndTag.INSTANCE;
      } else {
         StringTag.skipString(input);
         return readTagSafe(input, accounter, b);
      }
   }

   private static Tag readTagSafe(DataInput input, NbtAccounter accounter, byte type) {
      try {
         return TagTypes.getType(type).load(input, accounter);
      } catch (IOException var6) {
         CrashReport crashReport = CrashReport.forThrowable(var6, "Loading NBT data");
         CrashReportCategory crashReportCategory = crashReport.addCategory("NBT Tag");
         crashReportCategory.setDetail("Tag type", type);
         throw new ReportedNbtException(crashReport);
      }
   }

   public static class StringFallbackDataOutput extends DelegateDataOutput {
      public StringFallbackDataOutput(DataOutput dataOutput) {
         super(dataOutput);
      }

      @Override
      public void writeUTF(String string) throws IOException {
         try {
            super.writeUTF(string);
         } catch (UTFDataFormatException var3) {
            Util.logAndPauseIfInIde("Failed to write NBT String", var3);
            super.writeUTF("");
         }
      }
   }
}
