package net.minecraft.world.level.chunk.storage;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.logging.LogUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class RegionFile implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int SECTOR_BYTES = 4096;
   @VisibleForTesting
   protected static final int SECTOR_INTS = 1024;
   private static final int CHUNK_HEADER_SIZE = 5;
   private static final int HEADER_OFFSET = 0;
   private static final ByteBuffer PADDING_BUFFER = ByteBuffer.allocateDirect(1);
   private static final String EXTERNAL_FILE_EXTENSION = ".mcc";
   private static final int EXTERNAL_STREAM_FLAG = 128;
   private static final int EXTERNAL_CHUNK_THRESHOLD = 256;
   private static final int CHUNK_NOT_PRESENT = 0;
   final RegionStorageInfo info;
   private final Path path;
   private final FileChannel file;
   private final Path externalFileDir;
   final RegionFileVersion version;
   private final ByteBuffer header = ByteBuffer.allocateDirect(8192);
   private final IntBuffer offsets;
   private final IntBuffer timestamps;
   @VisibleForTesting
   protected final RegionBitmap usedSectors = new RegionBitmap();

   public RegionFile(RegionStorageInfo info, Path path, Path externalFileDir, boolean sync) throws IOException {
      this(info, path, externalFileDir, RegionFileVersion.getSelected(), sync);
   }

   public RegionFile(RegionStorageInfo info, Path path, Path externalFileDir, RegionFileVersion version, boolean sync) throws IOException {
      this.info = info;
      this.path = path;
      this.version = version;
      if (!Files.isDirectory(externalFileDir)) {
         throw new IllegalArgumentException("Expected directory, got " + externalFileDir.toAbsolutePath());
      } else {
         this.externalFileDir = externalFileDir;
         this.offsets = this.header.asIntBuffer();
         this.offsets.limit(1024);
         this.header.position(4096);
         this.timestamps = this.header.asIntBuffer();
         if (sync) {
            this.file = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.DSYNC);
         } else {
            this.file = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
         }

         this.usedSectors.force(0, 2);
         this.header.position(0);
         int i = this.file.read(this.header, 0L);
         if (i != -1) {
            if (i != 8192) {
               LOGGER.warn("Region file {} has truncated header: {}", path, i);
            }

            long l = Files.size(path);

            for (int j = 0; j < 1024; j++) {
               int k = this.offsets.get(j);
               if (k != 0) {
                  int m = getSectorNumber(k);
                  int n = getNumSectors(k);
                  if (m < 2) {
                     LOGGER.warn("Region file {} has invalid sector at index: {}; sector {} overlaps with header", new Object[]{path, j, m});
                     this.offsets.put(j, 0);
                  } else if (n == 0) {
                     LOGGER.warn("Region file {} has an invalid sector at index: {}; size has to be > 0", path, j);
                     this.offsets.put(j, 0);
                  } else if (m * 4096L > l) {
                     LOGGER.warn("Region file {} has an invalid sector at index: {}; sector {} is out of bounds", new Object[]{path, j, m});
                     this.offsets.put(j, 0);
                  } else {
                     this.usedSectors.force(m, n);
                  }
               }
            }
         }
      }
   }

   public Path getPath() {
      return this.path;
   }

   private Path getExternalChunkPath(ChunkPos chunkPos) {
      String string = "c." + chunkPos.x + "." + chunkPos.z + ".mcc";
      return this.externalFileDir.resolve(string);
   }

   @Nullable
   public synchronized DataInputStream getChunkDataInputStream(ChunkPos chunkPos) throws IOException {
      int i = this.getOffset(chunkPos);
      if (i == 0) {
         return null;
      } else {
         int j = getSectorNumber(i);
         int k = getNumSectors(i);
         int l = k * 4096;
         ByteBuffer byteBuffer = ByteBuffer.allocate(l);
         this.file.read(byteBuffer, j * 4096);
         byteBuffer.flip();
         if (byteBuffer.remaining() < 5) {
            LOGGER.error("Chunk {} header is truncated: expected {} but read {}", new Object[]{chunkPos, l, byteBuffer.remaining()});
            return null;
         } else {
            int m = byteBuffer.getInt();
            byte b = byteBuffer.get();
            if (m == 0) {
               LOGGER.warn("Chunk {} is allocated, but stream is missing", chunkPos);
               return null;
            } else {
               int n = m - 1;
               if (isExternalStreamChunk(b)) {
                  if (n != 0) {
                     LOGGER.warn("Chunk has both internal and external streams");
                  }

                  return this.createExternalChunkInputStream(chunkPos, getExternalChunkVersion(b));
               } else if (n > byteBuffer.remaining()) {
                  LOGGER.error("Chunk {} stream is truncated: expected {} but read {}", new Object[]{chunkPos, n, byteBuffer.remaining()});
                  return null;
               } else if (n < 0) {
                  LOGGER.error("Declared size {} of chunk {} is negative", m, chunkPos);
                  return null;
               } else {
                  JvmProfiler.INSTANCE.onRegionFileRead(this.info, chunkPos, this.version, n);
                  return this.createChunkInputStream(chunkPos, b, createStream(byteBuffer, n));
               }
            }
         }
      }
   }

   private static int getTimestamp() {
      return (int)(Util.getEpochMillis() / 1000L);
   }

   private static boolean isExternalStreamChunk(byte versionByte) {
      return (versionByte & 128) != 0;
   }

   private static byte getExternalChunkVersion(byte versionByte) {
      return (byte)(versionByte & -129);
   }

   @Nullable
   private DataInputStream createChunkInputStream(ChunkPos chunkPos, byte versionByte, InputStream inputStream) throws IOException {
      RegionFileVersion regionFileVersion = RegionFileVersion.fromId(versionByte);
      if (regionFileVersion == RegionFileVersion.VERSION_CUSTOM) {
         String string = new DataInputStream(inputStream).readUTF();
         Identifier identifier = Identifier.tryParse(string);
         if (identifier != null) {
            LOGGER.error("Unrecognized custom compression {}", identifier);
            return null;
         } else {
            LOGGER.error("Invalid custom compression id {}", string);
            return null;
         }
      } else if (regionFileVersion == null) {
         LOGGER.error("Chunk {} has invalid chunk stream version {}", chunkPos, versionByte);
         return null;
      } else {
         return new DataInputStream(regionFileVersion.wrap(inputStream));
      }
   }

   @Nullable
   private DataInputStream createExternalChunkInputStream(ChunkPos chunkPos, byte versionByte) throws IOException {
      Path path = this.getExternalChunkPath(chunkPos);
      if (!Files.isRegularFile(path)) {
         LOGGER.error("External chunk path {} is not file", path);
         return null;
      } else {
         return this.createChunkInputStream(chunkPos, versionByte, Files.newInputStream(path));
      }
   }

   private static ByteArrayInputStream createStream(ByteBuffer sourceBuffer, int length) {
      return new ByteArrayInputStream(sourceBuffer.array(), sourceBuffer.position(), length);
   }

   private int packSectorOffset(int sectorOffset, int sectorCount) {
      return sectorOffset << 8 | sectorCount;
   }

   private static int getNumSectors(int packedSectorOffset) {
      return packedSectorOffset & 0xFF;
   }

   private static int getSectorNumber(int packedSectorOffset) {
      return packedSectorOffset >> 8 & 16777215;
   }

   private static int sizeToSectors(int size) {
      return (size + 4096 - 1) / 4096;
   }

   public boolean doesChunkExist(ChunkPos chunkPos) {
      int i = this.getOffset(chunkPos);
      if (i == 0) {
         return false;
      } else {
         int j = getSectorNumber(i);
         int k = getNumSectors(i);
         ByteBuffer byteBuffer = ByteBuffer.allocate(5);

         try {
            this.file.read(byteBuffer, j * 4096);
            byteBuffer.flip();
            if (byteBuffer.remaining() != 5) {
               return false;
            } else {
               int l = byteBuffer.getInt();
               byte b = byteBuffer.get();
               if (isExternalStreamChunk(b)) {
                  if (!RegionFileVersion.isValidVersion(getExternalChunkVersion(b))) {
                     return false;
                  }

                  if (!Files.isRegularFile(this.getExternalChunkPath(chunkPos))) {
                     return false;
                  }
               } else {
                  if (!RegionFileVersion.isValidVersion(b)) {
                     return false;
                  }

                  if (l == 0) {
                     return false;
                  }

                  int m = l - 1;
                  if (m < 0 || m > 4096 * k) {
                     return false;
                  }
               }

               return true;
            }
         } catch (IOException var9) {
            return false;
         }
      }
   }

   public DataOutputStream getChunkDataOutputStream(ChunkPos chunkPos) throws IOException {
      return new DataOutputStream(this.version.wrap(new RegionFile.ChunkBuffer(chunkPos)));
   }

   public void flush() throws IOException {
      this.file.force(true);
   }

   public void clear(ChunkPos chunkPos) throws IOException {
      int i = getOffsetIndex(chunkPos);
      int j = this.offsets.get(i);
      if (j != 0) {
         this.offsets.put(i, 0);
         this.timestamps.put(i, getTimestamp());
         this.writeHeader();
         Files.deleteIfExists(this.getExternalChunkPath(chunkPos));
         this.usedSectors.free(getSectorNumber(j), getNumSectors(j));
      }
   }

   protected synchronized void write(ChunkPos chunkPos, ByteBuffer chunkData) throws IOException {
      int i = getOffsetIndex(chunkPos);
      int j = this.offsets.get(i);
      int k = getSectorNumber(j);
      int l = getNumSectors(j);
      int m = chunkData.remaining();
      int n = sizeToSectors(m);
      int o;
      RegionFile.CommitOp commitOp;
      if (n >= 256) {
         Path path = this.getExternalChunkPath(chunkPos);
         LOGGER.warn("Saving oversized chunk {} ({} bytes} to external file {}", new Object[]{chunkPos, m, path});
         n = 1;
         o = this.usedSectors.allocate(n);
         commitOp = this.writeToExternalFile(path, chunkData);
         ByteBuffer byteBuffer = this.createExternalStub();
         this.file.write(byteBuffer, o * 4096);
      } else {
         o = this.usedSectors.allocate(n);
         commitOp = () -> Files.deleteIfExists(this.getExternalChunkPath(chunkPos));
         this.file.write(chunkData, o * 4096);
      }

      this.offsets.put(i, this.packSectorOffset(o, n));
      this.timestamps.put(i, getTimestamp());
      this.writeHeader();
      commitOp.run();
      if (k != 0) {
         this.usedSectors.free(k, l);
      }
   }

   private ByteBuffer createExternalStub() {
      ByteBuffer byteBuffer = ByteBuffer.allocate(5);
      byteBuffer.putInt(1);
      byteBuffer.put((byte)(this.version.getId() | 128));
      byteBuffer.flip();
      return byteBuffer;
   }

   private RegionFile.CommitOp writeToExternalFile(Path externalChunkFile, ByteBuffer chunkData) throws IOException {
      Path path = Files.createTempFile(this.externalFileDir, "tmp", null);

      try (FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
         chunkData.position(5);
         fileChannel.write(chunkData);
      }

      return () -> Files.move(path, externalChunkFile, StandardCopyOption.REPLACE_EXISTING);
   }

   private void writeHeader() throws IOException {
      this.header.position(0);
      this.file.write(this.header, 0L);
   }

   private int getOffset(ChunkPos chunkPos) {
      return this.offsets.get(getOffsetIndex(chunkPos));
   }

   public boolean hasChunk(ChunkPos chunkPos) {
      return this.getOffset(chunkPos) != 0;
   }

   private static int getOffsetIndex(ChunkPos chunkPos) {
      return chunkPos.getRegionLocalX() + chunkPos.getRegionLocalZ() * 32;
   }

   @Override
   public void close() throws IOException {
      try {
         this.padToFullSector();
      } finally {
         try {
            this.file.force(true);
         } finally {
            this.file.close();
         }
      }
   }

   private void padToFullSector() throws IOException {
      int i = (int)this.file.size();
      int j = sizeToSectors(i) * 4096;
      if (i != j) {
         ByteBuffer byteBuffer = PADDING_BUFFER.duplicate();
         byteBuffer.position(0);
         this.file.write(byteBuffer, j - 1);
      }
   }

   class ChunkBuffer extends ByteArrayOutputStream {
      private final ChunkPos pos;

      public ChunkBuffer(final ChunkPos pos) {
         super(8096);
         super.write(0);
         super.write(0);
         super.write(0);
         super.write(0);
         super.write(RegionFile.this.version.getId());
         this.pos = pos;
      }

      @Override
      public void close() throws IOException {
         ByteBuffer byteBuffer = ByteBuffer.wrap(this.buf, 0, this.count);
         int i = this.count - 5 + 1;
         JvmProfiler.INSTANCE.onRegionFileWrite(RegionFile.this.info, this.pos, RegionFile.this.version, i);
         byteBuffer.putInt(0, i);
         RegionFile.this.write(this.pos, byteBuffer);
      }
   }

   interface CommitOp {
      void run() throws IOException;
   }
}
