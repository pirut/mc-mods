package net.minecraft.world.level.chunk.storage;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.util.ExceptionCollector;
import net.minecraft.world.level.ChunkPos;

public final class RegionFileStorage implements AutoCloseable {
   public static final String ANVIL_EXTENSION = ".mca";
   private static final int MAX_CACHE_SIZE = 256;
   private final Long2ObjectLinkedOpenHashMap<RegionFile> regionCache = new Long2ObjectLinkedOpenHashMap();
   private final RegionStorageInfo info;
   private final Path folder;
   private final boolean sync;

   RegionFileStorage(RegionStorageInfo p_326161_, Path p_196954_, boolean p_196955_) {
      this.folder = p_196954_;
      this.sync = p_196955_;
      this.info = p_326161_;
   }

   private RegionFile getRegionFile(ChunkPos p_63712_) throws IOException {
      long $$1 = ChunkPos.asLong(p_63712_.getRegionX(), p_63712_.getRegionZ());
      RegionFile $$2 = (RegionFile)this.regionCache.getAndMoveToFirst($$1);
      if ($$2 != null) {
         return $$2;
      } else {
         if (this.regionCache.size() >= 256) {
            ((RegionFile)this.regionCache.removeLast()).close();
         }

         FileUtil.createDirectoriesSafe(this.folder);
         Path $$3 = this.folder.resolve("r." + p_63712_.getRegionX() + "." + p_63712_.getRegionZ() + ".mca");
         RegionFile $$4 = new RegionFile(this.info, $$3, this.folder, this.sync);
         this.regionCache.putAndMoveToFirst($$1, $$4);
         return $$4;
      }
   }

   @Nullable
   public CompoundTag read(ChunkPos p_63707_) throws IOException {
      RegionFile $$1 = this.getRegionFile(p_63707_);

      CompoundTag var4;
      try (DataInputStream $$2 = $$1.getChunkDataInputStream(p_63707_)) {
         if ($$2 == null) {
            return null;
         }

         var4 = NbtIo.read($$2);
      }

      return var4;
   }

   public void scanChunk(ChunkPos p_196957_, StreamTagVisitor p_196958_) throws IOException {
      RegionFile $$2 = this.getRegionFile(p_196957_);

      try (DataInputStream $$3 = $$2.getChunkDataInputStream(p_196957_)) {
         if ($$3 != null) {
            NbtIo.parse($$3, p_196958_, NbtAccounter.unlimitedHeap());
         }
      }
   }

   protected void write(ChunkPos p_63709_, @Nullable CompoundTag p_63710_) throws IOException {
      RegionFile $$2 = this.getRegionFile(p_63709_);
      if (p_63710_ == null) {
         $$2.clear(p_63709_);
      } else {
         try (DataOutputStream $$3 = $$2.getChunkDataOutputStream(p_63709_)) {
            NbtIo.write(p_63710_, $$3);
         }
      }
   }

   @Override
   public void close() throws IOException {
      ExceptionCollector<IOException> $$0 = new ExceptionCollector<>();
      ObjectIterator var2 = this.regionCache.values().iterator();

      while (var2.hasNext()) {
         RegionFile $$1 = (RegionFile)var2.next();

         try {
            $$1.close();
         } catch (IOException var5) {
            $$0.add(var5);
         }
      }

      $$0.throwIfPresent();
   }

   public void flush() throws IOException {
      ObjectIterator var1 = this.regionCache.values().iterator();

      while (var1.hasNext()) {
         RegionFile $$0 = (RegionFile)var1.next();
         $$0.flush();
      }
   }

   public RegionStorageInfo info() {
      return this.info;
   }
}
