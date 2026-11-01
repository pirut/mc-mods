package net.minecraft.data.structures;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.mojang.logging.LogUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.FastBufferedInputStream;
import org.slf4j.Logger;

public class NbtToSnbt implements DataProvider {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Iterable<Path> inputFolders;
   private final PackOutput output;

   public NbtToSnbt(PackOutput p_250442_, Collection<Path> p_249158_) {
      this.inputFolders = p_249158_;
      this.output = p_250442_;
   }

   @Override
   public CompletableFuture<?> run(CachedOutput p_254274_) {
      Path $$1 = this.output.getOutputFolder();
      List<CompletableFuture<?>> $$2 = new ArrayList<>();

      for (Path $$3 : this.inputFolders) {
         $$2.add(
            CompletableFuture.<CompletableFuture>supplyAsync(
                  () -> {
                     try {
                        CompletableFuture var4;
                        try (Stream<Path> $$3x = Files.walk($$3)) {
                           var4 = CompletableFuture.allOf(
                              $$3x.filter(p_126430_ -> p_126430_.toString().endsWith(".nbt"))
                                 .map(
                                    p_253418_ -> CompletableFuture.runAsync(
                                       () -> convertStructure(p_254274_, p_253418_, getName($$3, p_253418_), $$1), Util.ioPool()
                                    )
                                 )
                                 .toArray(CompletableFuture[]::new)
                           );
                        }

                        return var4;
                     } catch (IOException var8) {
                        LOGGER.error("Failed to read structure input directory", var8);
                        return CompletableFuture.completedFuture(null);
                     }
                  },
                  Util.backgroundExecutor()
               )
               .thenCompose(p_253420_ -> p_253420_)
         );
      }

      return CompletableFuture.allOf($$2.toArray(CompletableFuture[]::new));
   }

   @Override
   public final String getName() {
      return "NBT -> SNBT";
   }

   private static String getName(Path p_126436_, Path p_126437_) {
      String $$2 = p_126436_.relativize(p_126437_).toString().replaceAll("\\\\", "/");
      return $$2.substring(0, $$2.length() - ".nbt".length());
   }

   @Nullable
   public static Path convertStructure(CachedOutput p_236382_, Path p_236383_, String p_236384_, Path p_236385_) {
      try {
         Path var7;
         try (
            InputStream $$4 = Files.newInputStream(p_236383_);
            InputStream $$5 = new FastBufferedInputStream($$4);
         ) {
            Path $$6 = p_236385_.resolve(p_236384_ + ".snbt");
            writeSnbt(p_236382_, $$6, NbtUtils.structureToSnbt(NbtIo.readCompressed($$5, NbtAccounter.unlimitedHeap())));
            LOGGER.info("Converted {} from NBT to SNBT", p_236384_);
            var7 = $$6;
         }

         return var7;
      } catch (IOException var12) {
         LOGGER.error("Couldn't convert {} from NBT to SNBT at {}", new Object[]{p_236384_, p_236383_, var12});
         return null;
      }
   }

   public static void writeSnbt(CachedOutput p_236378_, Path p_236379_, String p_236380_) throws IOException {
      ByteArrayOutputStream $$3 = new ByteArrayOutputStream();
      HashingOutputStream $$4 = new HashingOutputStream(Hashing.sha1(), $$3);
      $$4.write(p_236380_.getBytes(StandardCharsets.UTF_8));
      $$4.write(10);
      p_236378_.writeIfNeeded(p_236379_, $$3.toByteArray(), $$4.hash());
   }
}
