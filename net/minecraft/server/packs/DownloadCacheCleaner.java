package net.minecraft.server.packs;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;

public class DownloadCacheCleaner {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static void vacuumCacheDir(Path p_314919_, int p_314928_) {
      try {
         List<DownloadCacheCleaner.PathAndTime> $$2 = listFilesWithModificationTimes(p_314919_);
         int $$3 = $$2.size() - p_314928_;
         if ($$3 <= 0) {
            return;
         }

         $$2.sort(DownloadCacheCleaner.PathAndTime.NEWEST_FIRST);
         List<DownloadCacheCleaner.PathAndPriority> $$4 = prioritizeFilesInDirs($$2);
         Collections.reverse($$4);
         $$4.sort(DownloadCacheCleaner.PathAndPriority.HIGHEST_PRIORITY_FIRST);
         Set<Path> $$5 = new HashSet<>();

         for (int $$6 = 0; $$6 < $$3; $$6++) {
            DownloadCacheCleaner.PathAndPriority $$7 = $$4.get($$6);
            Path $$8 = $$7.path;

            try {
               Files.delete($$8);
               if ($$7.removalPriority == 0) {
                  $$5.add($$8.getParent());
               }
            } catch (IOException var12) {
               LOGGER.warn("Failed to delete cache file {}", $$8, var12);
            }
         }

         $$5.remove(p_314919_);

         for (Path $$10 : $$5) {
            try {
               Files.delete($$10);
            } catch (DirectoryNotEmptyException var10) {
            } catch (IOException var11) {
               LOGGER.warn("Failed to delete empty(?) cache directory {}", $$10, var11);
            }
         }
      } catch (UncheckedIOException | IOException var13) {
         LOGGER.error("Failed to vacuum cache dir {}", p_314919_, var13);
      }
   }

   private static List<DownloadCacheCleaner.PathAndTime> listFilesWithModificationTimes(final Path p_314985_) throws IOException {
      try {
         final List<DownloadCacheCleaner.PathAndTime> $$1 = new ArrayList<>();
         Files.walkFileTree(p_314985_, new SimpleFileVisitor<Path>() {
            public FileVisitResult visitFile(Path p_314922_, BasicFileAttributes p_315004_) {
               if (p_315004_.isRegularFile() && !p_314922_.getParent().equals(p_314985_)) {
                  FileTime $$2 = p_315004_.lastModifiedTime();
                  $$1.add(new DownloadCacheCleaner.PathAndTime(p_314922_, $$2));
               }

               return FileVisitResult.CONTINUE;
            }
         });
         return $$1;
      } catch (NoSuchFileException var2) {
         return List.of();
      }
   }

   private static List<DownloadCacheCleaner.PathAndPriority> prioritizeFilesInDirs(List<DownloadCacheCleaner.PathAndTime> p_314924_) {
      List<DownloadCacheCleaner.PathAndPriority> $$1 = new ArrayList<>();
      Object2IntOpenHashMap<Path> $$2 = new Object2IntOpenHashMap();

      for (DownloadCacheCleaner.PathAndTime $$3 : p_314924_) {
         int $$4 = $$2.addTo($$3.path.getParent(), 1);
         $$1.add(new DownloadCacheCleaner.PathAndPriority($$3.path, $$4));
      }

      return $$1;
   }

   record PathAndPriority(Path path, int removalPriority) {
      public static final Comparator<DownloadCacheCleaner.PathAndPriority> HIGHEST_PRIORITY_FIRST = Comparator.comparing(
            DownloadCacheCleaner.PathAndPriority::removalPriority
         )
         .reversed();
   }

   record PathAndTime(Path path, FileTime modifiedTime) {
      public static final Comparator<DownloadCacheCleaner.PathAndTime> NEWEST_FIRST = Comparator.comparing(DownloadCacheCleaner.PathAndTime::modifiedTime)
         .reversed();
   }
}
