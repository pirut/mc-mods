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
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.world.level.ChunkPos;
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

   public RegionFile(RegionStorageInfo p_326174_, Path p_196950_, Path p_196951_, boolean p_196952_) throws IOException {
      this(p_326174_, p_196950_, p_196951_, RegionFileVersion.getSelected(), p_196952_);
   }

   public RegionFile(RegionStorageInfo p_326221_, Path p_63633_, Path p_63634_, RegionFileVersion p_63635_, boolean p_63636_) throws IOException {
      this.info = p_326221_;
      this.path = p_63633_;
      this.version = p_63635_;
      if (!Files.isDirectory(p_63634_)) {
         throw new IllegalArgumentException("Expected directory, got " + p_63634_.toAbsolutePath());
      } else {
         this.externalFileDir = p_63634_;
         this.offsets = this.header.asIntBuffer();
         this.offsets.limit(1024);
         this.header.position(4096);
         this.timestamps = this.header.asIntBuffer();
         if (p_63636_) {
            this.file = FileChannel.open(p_63633_, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.DSYNC);
         } else {
            this.file = FileChannel.open(p_63633_, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
         }

         this.usedSectors.force(0, 2);
         this.header.position(0);
         int $$5 = this.file.read(this.header, 0L);
         if ($$5 != -1) {
            if ($$5 != 8192) {
               LOGGER.warn("Region file {} has truncated header: {}", p_63633_, $$5);
            }

            long $$6 = Files.size(p_63633_);

            for (int $$7 = 0; $$7 < 1024; $$7++) {
               int $$8 = this.offsets.get($$7);
               if ($$8 != 0) {
                  int $$9 = getSectorNumber($$8);
                  int $$10 = getNumSectors($$8);
                  if ($$9 < 2) {
                     LOGGER.warn("Region file {} has invalid sector at index: {}; sector {} overlaps with header", new Object[]{p_63633_, $$7, $$9});
                     this.offsets.put($$7, 0);
                  } else if ($$10 == 0) {
                     LOGGER.warn("Region file {} has an invalid sector at index: {}; size has to be > 0", p_63633_, $$7);
                     this.offsets.put($$7, 0);
                  } else if ($$9 * 4096L > $$6) {
                     LOGGER.warn("Region file {} has an invalid sector at index: {}; sector {} is out of bounds", new Object[]{p_63633_, $$7, $$9});
                     this.offsets.put($$7, 0);
                  } else {
                     this.usedSectors.force($$9, $$10);
                  }
               }
            }
         }
      }
   }

   public Path getPath() {
      return this.path;
   }

   private Path getExternalChunkPath(ChunkPos p_63685_) {
      String $$1 = "c." + p_63685_.x + "." + p_63685_.z + ".mcc";
      return this.externalFileDir.resolve($$1);
   }

   @Nullable
   public synchronized DataInputStream getChunkDataInputStream(ChunkPos p_63646_) throws IOException {
      int $$1 = this.getOffset(p_63646_);
      if ($$1 == 0) {
         return null;
      } else {
         int $$2 = getSectorNumber($$1);
         int $$3 = getNumSectors($$1);
         int $$4 = $$3 * 4096;
         ByteBuffer $$5 = ByteBuffer.allocate($$4);
         this.file.read($$5, $$2 * 4096);
         $$5.flip();
         if ($$5.remaining() < 5) {
            LOGGER.error("Chunk {} header is truncated: expected {} but read {}", new Object[]{p_63646_, $$4, $$5.remaining()});
            return null;
         } else {
            int $$6 = $$5.getInt();
            byte $$7 = $$5.get();
            if ($$6 == 0) {
               LOGGER.warn("Chunk {} is allocated, but stream is missing", p_63646_);
               return null;
            } else {
               int $$8 = $$6 - 1;
               if (isExternalStreamChunk($$7)) {
                  if ($$8 != 0) {
                     LOGGER.warn("Chunk has both internal and external streams");
                  }

                  return this.createExternalChunkInputStream(p_63646_, getExternalChunkVersion($$7));
               } else if ($$8 > $$5.remaining()) {
                  LOGGER.error("Chunk {} stream is truncated: expected {} but read {}", new Object[]{p_63646_, $$8, $$5.remaining()});
                  return null;
               } else if ($$8 < 0) {
                  LOGGER.error("Declared size {} of chunk {} is negative", $$6, p_63646_);
                  return null;
               } else {
                  JvmProfiler.INSTANCE.onRegionFileRead(this.info, p_63646_, this.version, $$8);
                  return this.createChunkInputStream(p_63646_, $$7, createStream($$5, $$8));
               }
            }
         }
      }
   }

   private static int getTimestamp() {
      return (int)(Util.getEpochMillis() / 1000L);
   }

   private static boolean isExternalStreamChunk(byte p_63639_) {
      return (p_63639_ & 128) != 0;
   }

   private static byte getExternalChunkVersion(byte p_63670_) {
      return (byte)(p_63670_ & -129);
   }

   @Nullable
   private DataInputStream createChunkInputStream(ChunkPos p_63651_, byte p_63652_, InputStream p_63653_) throws IOException {
      RegionFileVersion $$3 = RegionFileVersion.fromId(p_63652_);
      if ($$3 == RegionFileVersion.VERSION_CUSTOM) {
         String $$4 = new DataInputStream(p_63653_).readUTF();
         ResourceLocation $$5 = ResourceLocation.tryParse($$4);
         if ($$5 != null) {
            LOGGER.error("Unrecognized custom compression {}", $$5);
            return null;
         } else {
            LOGGER.error("Invalid custom compression id {}", $$4);
            return null;
         }
      } else if ($$3 == null) {
         LOGGER.error("Chunk {} has invalid chunk stream version {}", p_63651_, p_63652_);
         return null;
      } else {
         return new DataInputStream($$3.wrap(p_63653_));
      }
   }

   @Nullable
   private DataInputStream createExternalChunkInputStream(ChunkPos p_63648_, byte p_63649_) throws IOException {
      Path $$2 = this.getExternalChunkPath(p_63648_);
      if (!Files.isRegularFile($$2)) {
         LOGGER.error("External chunk path {} is not file", $$2);
         return null;
      } else {
         return this.createChunkInputStream(p_63648_, p_63649_, Files.newInputStream($$2));
      }
   }

   private static ByteArrayInputStream createStream(ByteBuffer p_63660_, int p_63661_) {
      return new ByteArrayInputStream(p_63660_.array(), p_63660_.position(), p_63661_);
   }

   private int packSectorOffset(int p_63643_, int p_63644_) {
      return p_63643_ << 8 | p_63644_;
   }

   private static int getNumSectors(int p_63641_) {
      return p_63641_ & 0xFF;
   }

   private static int getSectorNumber(int p_63672_) {
      return p_63672_ >> 8 & 16777215;
   }

   private static int sizeToSectors(int p_63677_) {
      return (p_63677_ + 4096 - 1) / 4096;
   }

   public boolean doesChunkExist(ChunkPos p_63674_) {
      int $$1 = this.getOffset(p_63674_);
      if ($$1 == 0) {
         return false;
      } else {
         int $$2 = getSectorNumber($$1);
         int $$3 = getNumSectors($$1);
         ByteBuffer $$4 = ByteBuffer.allocate(5);

         try {
            this.file.read($$4, $$2 * 4096);
            $$4.flip();
            if ($$4.remaining() != 5) {
               return false;
            } else {
               int $$5 = $$4.getInt();
               byte $$6 = $$4.get();
               if (isExternalStreamChunk($$6)) {
                  if (!RegionFileVersion.isValidVersion(getExternalChunkVersion($$6))) {
                     return false;
                  }

                  if (!Files.isRegularFile(this.getExternalChunkPath(p_63674_))) {
                     return false;
                  }
               } else {
                  if (!RegionFileVersion.isValidVersion($$6)) {
                     return false;
                  }

                  if ($$5 == 0) {
                     return false;
                  }

                  int $$7 = $$5 - 1;
                  if ($$7 < 0 || $$7 > 4096 * $$3) {
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

   public DataOutputStream getChunkDataOutputStream(ChunkPos p_63679_) throws IOException {
      return new DataOutputStream(this.version.wrap(new RegionFile.ChunkBuffer(p_63679_)));
   }

   public void flush() throws IOException {
      this.file.force(true);
   }

   public void clear(ChunkPos p_156614_) throws IOException {
      int $$1 = getOffsetIndex(p_156614_);
      int $$2 = this.offsets.get($$1);
      if ($$2 != 0) {
         this.offsets.put($$1, 0);
         this.timestamps.put($$1, getTimestamp());
         this.writeHeader();
         Files.deleteIfExists(this.getExternalChunkPath(p_156614_));
         this.usedSectors.free(getSectorNumber($$2), getNumSectors($$2));
      }
   }

   protected synchronized void write(ChunkPos p_63655_, ByteBuffer p_63656_) throws IOException {
      int $$2 = getOffsetIndex(p_63655_);
      int $$3 = this.offsets.get($$2);
      int $$4 = getSectorNumber($$3);
      int $$5 = getNumSectors($$3);
      int $$6 = p_63656_.remaining();
      int $$7 = sizeToSectors($$6);
      int $$9;
      RegionFile.CommitOp $$10;
      if ($$7 >= 256) {
         Path $$8 = this.getExternalChunkPath(p_63655_);
         LOGGER.warn("Saving oversized chunk {} ({} bytes} to external file {}", new Object[]{p_63655_, $$6, $$8});
         $$7 = 1;
         $$9 = this.usedSectors.allocate($$7);
         $$10 = this.writeToExternalFile($$8, p_63656_);
         ByteBuffer $$11 = this.createExternalStub();
         this.file.write($$11, $$9 * 4096);
      } else {
         $$9 = this.usedSectors.allocate($$7);
         $$10 = () -> Files.deleteIfExists(this.getExternalChunkPath(p_63655_));
         this.file.write(p_63656_, $$9 * 4096);
      }

      this.offsets.put($$2, this.packSectorOffset($$9, $$7));
      this.timestamps.put($$2, getTimestamp());
      this.writeHeader();
      $$10.run();
      if ($$4 != 0) {
         this.usedSectors.free($$4, $$5);
      }
   }

   private ByteBuffer createExternalStub() {
      ByteBuffer $$0 = ByteBuffer.allocate(5);
      $$0.putInt(1);
      $$0.put((byte)(this.version.getId() | 128));
      $$0.flip();
      return $$0;
   }

   private RegionFile.CommitOp writeToExternalFile(Path p_63663_, ByteBuffer p_63664_) throws IOException {
      Path $$2 = Files.createTempFile(this.externalFileDir, "tmp", null);

      try (FileChannel $$3 = FileChannel.open($$2, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
         p_63664_.position(5);
         $$3.write(p_63664_);
      }

      return () -> Files.move($$2, p_63663_, StandardCopyOption.REPLACE_EXISTING);
   }

   private void writeHeader() throws IOException {
      this.header.position(0);
      this.file.write(this.header, 0L);
   }

   private int getOffset(ChunkPos p_63687_) {
      return this.offsets.get(getOffsetIndex(p_63687_));
   }

   public boolean hasChunk(ChunkPos p_63683_) {
      return this.getOffset(p_63683_) != 0;
   }

   private static int getOffsetIndex(ChunkPos p_63689_) {
      return p_63689_.getRegionLocalX() + p_63689_.getRegionLocalZ() * 32;
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
      int $$0 = (int)this.file.size();
      int $$1 = sizeToSectors($$0) * 4096;
      if ($$0 != $$1) {
         ByteBuffer $$2 = PADDING_BUFFER.duplicate();
         $$2.position(0);
         this.file.write($$2, $$1 - 1);
      }
   }

   class ChunkBuffer extends ByteArrayOutputStream {
      private final ChunkPos pos;

      public ChunkBuffer(ChunkPos p_63696_) {
         super(8096);
         super.write(0);
         super.write(0);
         super.write(0);
         super.write(0);
         super.write(RegionFile.this.version.getId());
         this.pos = p_63696_;
      }

      @Override
      public void close() throws IOException {
         ByteBuffer $$0 = ByteBuffer.wrap(this.buf, 0, this.count);
         int $$1 = this.count - 5 + 1;
         JvmProfiler.INSTANCE.onRegionFileWrite(RegionFile.this.info, this.pos, RegionFile.this.version, $$1);
         $$0.putInt(0, $$1);
         RegionFile.this.write(this.pos, $$0);
      }
   }

   interface CommitOp {
      void run() throws IOException;
   }
}
