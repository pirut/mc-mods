package net.minecraft.world.level.storage;

import com.google.common.collect.Iterables;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FastBufferedInputStream;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.common.IOUtilities;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class DimensionDataStorage implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   @Nullable
   private final ServerLevel level;
   private final Map<SavedDataType<?>, Optional<SavedData>> cache = new HashMap<>();
   private final DataFixer fixerUpper;
   private final HolderLookup.Provider registries;
   private final Path dataFolder;
   private CompletableFuture<?> pendingWriteFuture = CompletableFuture.completedFuture(null);

   @Deprecated
   public DimensionDataStorage(Path p_362421_, DataFixer p_78150_, HolderLookup.Provider p_324180_) {
      this(null, p_362421_, p_78150_, p_324180_);
   }

   public DimensionDataStorage(@Nullable ServerLevel level, Path p_362421_, DataFixer p_78150_, HolderLookup.Provider p_324180_) {
      this.level = level;
      this.fixerUpper = p_78150_;
      this.dataFolder = p_362421_;
      this.registries = p_324180_;
   }

   private Path getDataFile(String p_78157_) {
      return this.dataFolder.resolve(p_78157_ + ".dat");
   }

   public <T extends SavedData> T computeIfAbsent(SavedDataType<T> p_401394_) {
      T t = this.get(p_401394_);
      if (t != null) {
         return t;
      } else {
         T t1 = (T)p_401394_.factory().create(this.level);
         this.set(p_401394_, t1);
         return t1;
      }
   }

   @Nullable
   public <T extends SavedData> T get(SavedDataType<T> p_401285_) {
      Optional<SavedData> optional = this.cache.get(p_401285_);
      if (optional == null) {
         optional = Optional.ofNullable(this.readSavedData(p_401285_));
         this.cache.put(p_401285_, optional);
      }

      return (T)optional.orElse(null);
   }

   @Nullable
   private <T extends SavedData> T readSavedData(SavedDataType<T> p_401056_) {
      try {
         Path path = this.getDataFile(p_401056_.id());
         if (Files.exists(path)) {
            CompoundTag compoundtag = this.readTagFromDisk(p_401056_.id(), p_401056_.dataFixType(), SharedConstants.getCurrentVersion().dataVersion().version());
            RegistryOps<Tag> registryops = this.registries.createSerializationContext(NbtOps.INSTANCE);
            return (T)p_401056_.codecFactory()
               .create(this.level)
               .parse(registryops, compoundtag.get("data"))
               .resultOrPartial(p_400985_ -> LOGGER.error("Failed to parse saved data for '{}': {}", p_401056_, p_400985_))
               .orElse(null);
         }
      } catch (Exception var5) {
         LOGGER.error("Error loading saved data: {}", p_401056_, var5);
      }

      return null;
   }

   public <T extends SavedData> void set(SavedDataType<T> p_401359_, T p_164857_) {
      this.cache.put(p_401359_, Optional.of(p_164857_));
      p_164857_.setDirty();
   }

   public CompoundTag readTagFromDisk(String p_78159_, @Nullable DataFixTypes p_295038_, int p_78160_) throws IOException {
      CompoundTag compoundtag1;
      try (
         InputStream inputstream = Files.newInputStream(this.getDataFile(p_78159_));
         PushbackInputStream pushbackinputstream = new PushbackInputStream(new FastBufferedInputStream(inputstream), 2);
      ) {
         CompoundTag compoundtag;
         if (this.isGzip(pushbackinputstream)) {
            compoundtag = NbtIo.readCompressed(pushbackinputstream, NbtAccounter.unlimitedHeap());
         } else {
            try (DataInputStream datainputstream = new DataInputStream(pushbackinputstream)) {
               compoundtag = NbtIo.read(datainputstream);
            }
         }

         if (p_295038_ != null) {
            int i = NbtUtils.getDataVersion(compoundtag, 1343);
            compoundtag1 = p_295038_.update(this.fixerUpper, compoundtag, i, p_78160_);
         } else {
            compoundtag1 = compoundtag;
         }
      }

      this.pendingWriteFuture = this.pendingWriteFuture.thenRunAsync(() -> IOUtilities.tryCleanupTempFiles(this.dataFolder, p_78159_), Util.ioPool());
      return compoundtag1;
   }

   private boolean isGzip(PushbackInputStream p_78155_) throws IOException {
      byte[] abyte = new byte[2];
      boolean flag = false;
      int i = p_78155_.read(abyte, 0, 2);
      if (i == 2) {
         int j = (abyte[1] & 255) << 8 | abyte[0] & 255;
         if (j == 35615) {
            flag = true;
         }
      }

      if (i != 0) {
         p_78155_.unread(abyte, 0, i);
      }

      return flag;
   }

   public CompletableFuture<?> scheduleSave() {
      Map<SavedDataType<?>, CompoundTag> map = this.collectDirtyTagsToSave();
      if (map.isEmpty()) {
         return CompletableFuture.completedFuture(null);
      } else {
         int i = Util.maxAllowedExecutorThreads();
         int j = map.size();
         if (j > i) {
            this.pendingWriteFuture = this.pendingWriteFuture.thenCompose(p_466777_ -> {
               List<CompletableFuture<?>> list = new ArrayList<>(i);
               int k = Mth.positiveCeilDiv(j, i);

               for (List<Entry<SavedDataType<?>, CompoundTag>> list1 : Iterables.partition(map.entrySet(), k)) {
                  list.add(CompletableFuture.runAsync(() -> {
                     for (Entry<SavedDataType<?>, CompoundTag> entry : list1) {
                        this.tryWrite(entry.getKey(), entry.getValue());
                     }
                  }, Util.ioPool()));
               }

               return CompletableFuture.allOf(list.toArray(CompletableFuture[]::new));
            });
         } else {
            this.pendingWriteFuture = this.pendingWriteFuture
               .thenCompose(
                  p_400974_ -> CompletableFuture.allOf(
                     map.entrySet()
                        .stream()
                        .map(p_466778_ -> CompletableFuture.runAsync(() -> this.tryWrite(p_466778_.getKey(), p_466778_.getValue()), Util.ioPool()))
                        .toArray(CompletableFuture[]::new)
                  )
               );
         }

         return this.pendingWriteFuture;
      }
   }

   private Map<SavedDataType<?>, CompoundTag> collectDirtyTagsToSave() {
      Map<SavedDataType<?>, CompoundTag> map = new Object2ObjectArrayMap();
      RegistryOps<Tag> registryops = this.registries.createSerializationContext(NbtOps.INSTANCE);
      this.cache.forEach((p_400971_, p_400972_) -> p_400972_.filter(SavedData::isDirty).ifPresent(p_400978_ -> {
         map.put(p_400971_, this.encodeUnchecked(p_400971_, p_400978_, registryops));
         p_400978_.setDirty(false);
      }));
      return map;
   }

   private <T extends SavedData> CompoundTag encodeUnchecked(SavedDataType<T> p_401078_, SavedData p_401244_, RegistryOps<Tag> p_401357_) {
      Codec<T> codec = p_401078_.codecFactory().create(this.level);
      CompoundTag compoundtag = new CompoundTag();
      compoundtag.put("data", (Tag)codec.encodeStart(p_401357_, p_401244_).getOrThrow());
      NbtUtils.addCurrentDataVersion(compoundtag);
      return compoundtag;
   }

   private void tryWrite(SavedDataType<?> p_401021_, CompoundTag p_390657_) {
      Path path = this.getDataFile(p_401021_.id());

      try {
         if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
         }

         IOUtilities.writeNbtCompressed(p_390657_, path);
      } catch (IOException var5) {
         LOGGER.error("Could not save data to {}", path.getFileName(), var5);
      }
   }

   public void saveAndJoin() {
      this.scheduleSave().join();
   }

   @Override
   public void close() {
      this.saveAndJoin();
   }
}
