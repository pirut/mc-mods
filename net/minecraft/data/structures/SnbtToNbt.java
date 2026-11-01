package net.minecraft.data.structures;

import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class SnbtToNbt implements DataProvider {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final PackOutput output;
   private final Iterable<Path> inputFolders;
   private final List<SnbtToNbt.Filter> filters = Lists.newArrayList();

   public SnbtToNbt(PackOutput p_249104_, Iterable<Path> p_249523_) {
      this.output = p_249104_;
      this.inputFolders = p_249523_;
   }

   public SnbtToNbt addFilter(SnbtToNbt.Filter p_126476_) {
      this.filters.add(p_126476_);
      return this;
   }

   private CompoundTag applyFilters(String p_126461_, CompoundTag p_126462_) {
      CompoundTag $$2 = p_126462_;

      for (SnbtToNbt.Filter $$3 : this.filters) {
         $$2 = $$3.apply(p_126461_, $$2);
      }

      return $$2;
   }

   @Override
   public CompletableFuture<?> run(CachedOutput p_254336_) {
      Path $$1 = this.output.getOutputFolder();
      List<CompletableFuture<?>> $$2 = Lists.newArrayList();

      for (Path $$3 : this.inputFolders) {
         $$2.add(
            CompletableFuture.<CompletableFuture>supplyAsync(
                  () -> {
                     try {
                        CompletableFuture var5x;
                        try (Stream<Path> $$3x = Files.walk($$3)) {
                           var5x = CompletableFuture.allOf(
                              $$3x.filter(p_126464_ -> p_126464_.toString().endsWith(".snbt")).map(p_253432_ -> CompletableFuture.runAsync(() -> {
                                 SnbtToNbt.TaskResult $$4 = this.readStructure(p_253432_, this.getName($$3, p_253432_));
                                 this.storeStructureIfChanged(p_254336_, $$4, $$1);
                              }, Util.backgroundExecutor())).toArray(CompletableFuture[]::new)
                           );
                        }

                        return var5x;
                     } catch (Exception var9) {
                        throw new RuntimeException("Failed to read structure input directory, aborting", var9);
                     }
                  },
                  Util.backgroundExecutor()
               )
               .thenCompose(p_253441_ -> p_253441_)
         );
      }

      return Util.sequenceFailFast($$2);
   }

   @Override
   public final String getName() {
      return "SNBT -> NBT";
   }

   private String getName(Path p_126469_, Path p_126470_) {
      String $$2 = p_126469_.relativize(p_126470_).toString().replaceAll("\\\\", "/");
      return $$2.substring(0, $$2.length() - ".snbt".length());
   }

   private SnbtToNbt.TaskResult readStructure(Path p_126466_, String p_126467_) {
      try {
         SnbtToNbt.TaskResult var10;
         try (BufferedReader $$2 = Files.newBufferedReader(p_126466_)) {
            String $$3 = IOUtils.toString($$2);
            CompoundTag $$4 = this.applyFilters(p_126467_, NbtUtils.snbtToStructure($$3));
            ByteArrayOutputStream $$5 = new ByteArrayOutputStream();
            HashingOutputStream $$6 = new HashingOutputStream(Hashing.sha1(), $$5);
            NbtIo.writeCompressed($$4, $$6);
            byte[] $$7 = $$5.toByteArray();
            HashCode $$8 = $$6.hash();
            var10 = new SnbtToNbt.TaskResult(p_126467_, $$7, $$8);
         }

         return var10;
      } catch (Throwable var13) {
         throw new SnbtToNbt.StructureConversionException(p_126466_, var13);
      }
   }

   private void storeStructureIfChanged(CachedOutput p_236394_, SnbtToNbt.TaskResult p_236395_, Path p_236396_) {
      Path $$3 = p_236396_.resolve(p_236395_.name + ".nbt");

      try {
         p_236394_.writeIfNeeded($$3, p_236395_.payload, p_236395_.hash);
      } catch (IOException var6) {
         LOGGER.error("Couldn't write structure {} at {}", new Object[]{p_236395_.name, $$3, var6});
      }
   }

   @FunctionalInterface
   public interface Filter {
      CompoundTag apply(String var1, CompoundTag var2);
   }

   static class StructureConversionException extends RuntimeException {
      public StructureConversionException(Path p_176820_, Throwable p_176821_) {
         super(p_176820_.toAbsolutePath().toString(), p_176821_);
      }
   }

   record TaskResult(String name, byte[] payload, HashCode hash) {
   }
}
