package net.minecraft.server.packs;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.Util;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.HttpUtil;
import net.minecraft.util.eventlog.JsonEventLog;
import net.minecraft.util.thread.ProcessorMailbox;
import org.slf4j.Logger;

public class DownloadQueue implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_KEPT_PACKS = 20;
   private final Path cacheDir;
   private final JsonEventLog<DownloadQueue.LogEntry> eventLog;
   private final ProcessorMailbox<Runnable> tasks = ProcessorMailbox.create(Util.nonCriticalIoPool(), "download-queue");

   public DownloadQueue(Path p_314439_) throws IOException {
      this.cacheDir = p_314439_;
      FileUtil.createDirectoriesSafe(p_314439_);
      this.eventLog = JsonEventLog.open(DownloadQueue.LogEntry.CODEC, p_314439_.resolve("log.json"));
      DownloadCacheCleaner.vacuumCacheDir(p_314439_, 20);
   }

   private DownloadQueue.BatchResult runDownload(DownloadQueue.BatchConfig p_314482_, Map<UUID, DownloadQueue.DownloadRequest> p_314452_) {
      DownloadQueue.BatchResult $$2 = new DownloadQueue.BatchResult();
      p_314452_.forEach(
         (p_314645_, p_314573_) -> {
            Path $$4 = this.cacheDir.resolve(p_314645_.toString());
            Path $$5 = null;

            try {
               $$5 = HttpUtil.downloadFile(
                  $$4, p_314573_.url, p_314482_.headers, p_314482_.hashFunction, p_314573_.hash, p_314482_.maxSize, p_314482_.proxy, p_314482_.listener
               );
               $$2.downloaded.put(p_314645_, $$5);
            } catch (Exception var9) {
               LOGGER.error("Failed to download {}", p_314573_.url, var9);
               $$2.failed.add(p_314645_);
            }

            try {
               this.eventLog
                  .write(
                     new DownloadQueue.LogEntry(
                        p_314645_,
                        p_314573_.url.toString(),
                        Instant.now(),
                        Optional.ofNullable(p_314573_.hash).map(HashCode::toString),
                        $$5 != null ? this.getFileInfo($$5) : Either.left("download_failed")
                     )
                  );
            } catch (Exception var8) {
               LOGGER.error("Failed to log download of {}", p_314573_.url, var8);
            }
         }
      );
      return $$2;
   }

   private Either<String, DownloadQueue.FileInfoEntry> getFileInfo(Path p_314601_) {
      try {
         long $$1 = Files.size(p_314601_);
         Path $$2 = this.cacheDir.relativize(p_314601_);
         return Either.right(new DownloadQueue.FileInfoEntry($$2.toString(), $$1));
      } catch (IOException var5) {
         LOGGER.error("Failed to get file size of {}", p_314601_, var5);
         return Either.left("no_access");
      }
   }

   public CompletableFuture<DownloadQueue.BatchResult> downloadBatch(DownloadQueue.BatchConfig p_314536_, Map<UUID, DownloadQueue.DownloadRequest> p_314654_) {
      return CompletableFuture.supplyAsync(() -> this.runDownload(p_314536_, p_314654_), this.tasks::tell);
   }

   @Override
   public void close() throws IOException {
      this.tasks.close();
      this.eventLog.close();
   }

   public record BatchConfig(HashFunction hashFunction, int maxSize, Map<String, String> headers, Proxy proxy, HttpUtil.DownloadProgressListener listener) {
   }

   public record BatchResult(Map<UUID, Path> downloaded, Set<UUID> failed) {
      public BatchResult() {
         this(new HashMap<>(), new HashSet<>());
      }
   }

   public record DownloadRequest(URL url, @Nullable HashCode hash) {
   }

   record FileInfoEntry(String name, long size) {
      public static final Codec<DownloadQueue.FileInfoEntry> CODEC = RecordCodecBuilder.create(
         p_314458_ -> p_314458_.group(
               Codec.STRING.fieldOf("name").forGetter(DownloadQueue.FileInfoEntry::name),
               Codec.LONG.fieldOf("size").forGetter(DownloadQueue.FileInfoEntry::size)
            )
            .apply(p_314458_, DownloadQueue.FileInfoEntry::new)
      );
   }

   record LogEntry(UUID id, String url, Instant time, Optional<String> hash, Either<String, DownloadQueue.FileInfoEntry> errorOrFileInfo) {
      public static final Codec<DownloadQueue.LogEntry> CODEC = RecordCodecBuilder.create(
         p_314637_ -> p_314637_.group(
               UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(DownloadQueue.LogEntry::id),
               Codec.STRING.fieldOf("url").forGetter(DownloadQueue.LogEntry::url),
               ExtraCodecs.INSTANT_ISO8601.fieldOf("time").forGetter(DownloadQueue.LogEntry::time),
               Codec.STRING.optionalFieldOf("hash").forGetter(DownloadQueue.LogEntry::hash),
               Codec.mapEither(Codec.STRING.fieldOf("error"), DownloadQueue.FileInfoEntry.CODEC.fieldOf("file"))
                  .forGetter(DownloadQueue.LogEntry::errorOrFileInfo)
            )
            .apply(p_314637_, DownloadQueue.LogEntry::new)
      );
   }
}
