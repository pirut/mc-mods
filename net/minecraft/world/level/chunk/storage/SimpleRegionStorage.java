package net.minecraft.world.level.chunk.storage;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

public class SimpleRegionStorage implements AutoCloseable {
   private final IOWorker worker;
   private final DataFixer fixerUpper;
   private final DataFixTypes dataFixType;
   private final Supplier<LegacyTagFixer> legacyFixer;

   public SimpleRegionStorage(RegionStorageInfo p_326109_, Path p_321582_, DataFixer p_321815_, boolean p_321788_, DataFixTypes p_321522_) {
      this(p_326109_, p_321582_, p_321815_, p_321788_, p_321522_, LegacyTagFixer.EMPTY);
   }

   public SimpleRegionStorage(
      RegionStorageInfo p_459109_, Path p_459110_, DataFixer p_458998_, boolean p_459036_, DataFixTypes p_459167_, Supplier<LegacyTagFixer> p_459029_
   ) {
      this.fixerUpper = p_458998_;
      this.dataFixType = p_459167_;
      this.worker = new IOWorker(p_459109_, p_459110_, p_459036_);
      this.legacyFixer = Suppliers.memoize(p_459029_::get);
   }

   public boolean isOldChunkAround(ChunkPos p_459089_, int p_458946_) {
      return this.worker.isOldChunkAround(p_459089_, p_458946_);
   }

   public CompletableFuture<Optional<CompoundTag>> read(ChunkPos p_321653_) {
      return this.worker.loadAsync(p_321653_);
   }

   public CompletableFuture<Void> write(ChunkPos p_321715_, CompoundTag p_321816_) {
      return this.write(p_321715_, () -> p_321816_);
   }

   public CompletableFuture<Void> write(ChunkPos p_458987_, Supplier<CompoundTag> p_459197_) {
      this.markChunkDone(p_458987_);
      return this.worker.store(p_458987_, p_459197_);
   }

   public CompoundTag upgradeChunkTag(CompoundTag p_458990_, int p_458993_, @Nullable CompoundTag p_459066_) {
      int i = NbtUtils.getDataVersion(p_458990_, p_458993_);
      if (i == SharedConstants.getCurrentVersion().dataVersion().version()) {
         return p_458990_;
      } else {
         try {
            p_458990_ = this.legacyFixer.get().applyFix(p_458990_);
            injectDatafixingContext(p_458990_, p_459066_);
            p_458990_ = this.dataFixType.updateToCurrentVersion(this.fixerUpper, p_458990_, Math.max(this.legacyFixer.get().targetDataVersion(), i));
            removeDatafixingContext(p_458990_);
            NbtUtils.addCurrentDataVersion(p_458990_);
            return p_458990_;
         } catch (Exception var8) {
            CrashReport crashreport = CrashReport.forThrowable(var8, "Updated chunk");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Updated chunk details");
            crashreportcategory.setDetail("Data version", i);
            throw new ReportedException(crashreport);
         }
      }
   }

   public CompoundTag upgradeChunkTag(CompoundTag p_321601_, int p_321496_) {
      return this.upgradeChunkTag(p_321601_, p_321496_, null);
   }

   public Dynamic<Tag> upgradeChunkTag(Dynamic<Tag> p_321643_, int p_321759_) {
      return new Dynamic(p_321643_.getOps(), this.upgradeChunkTag((CompoundTag)p_321643_.getValue(), p_321759_, null));
   }

   public static void injectDatafixingContext(CompoundTag p_459208_, @Nullable CompoundTag p_459179_) {
      if (p_459179_ != null) {
         p_459208_.put("__context", p_459179_);
      }
   }

   private static void removeDatafixingContext(CompoundTag p_458955_) {
      p_458955_.remove("__context");
   }

   protected void markChunkDone(ChunkPos p_459105_) {
      this.legacyFixer.get().markChunkDone(p_459105_);
   }

   public CompletableFuture<Void> synchronize(boolean p_321682_) {
      return this.worker.synchronize(p_321682_);
   }

   @Override
   public void close() throws IOException {
      this.worker.close();
   }

   public ChunkScanAccess chunkScanner() {
      return this.worker;
   }

   public RegionStorageInfo storageInfo() {
      return this.worker.storageInfo();
   }
}
