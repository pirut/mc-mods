package net.minecraft.server.packs;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.IoSupplier;
import org.slf4j.Logger;

public class PathPackResources extends AbstractPackResources {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Joiner PATH_JOINER = Joiner.on("/");
   private final Path root;

   public PathPackResources(PackLocationInfo p_326080_, Path p_256025_) {
      super(p_326080_);
      this.root = p_256025_;
   }

   @Nullable
   @Override
   public IoSupplier<InputStream> getRootResource(String... p_249041_) {
      FileUtil.validatePath(p_249041_);
      Path $$1 = FileUtil.resolvePath(this.root, List.of(p_249041_));
      return Files.exists($$1) ? IoSupplier.create($$1) : null;
   }

   public static boolean validatePath(Path p_249579_) {
      return true;
   }

   @Nullable
   @Override
   public IoSupplier<InputStream> getResource(PackType p_249352_, ResourceLocation p_251715_) {
      Path $$2 = this.root.resolve(p_249352_.getDirectory()).resolve(p_251715_.getNamespace());
      return getResource(p_251715_, $$2);
   }

   @Nullable
   public static IoSupplier<InputStream> getResource(ResourceLocation p_250145_, Path p_251046_) {
      return (IoSupplier<InputStream>)FileUtil.decomposePath(p_250145_.getPath()).mapOrElse(p_251647_ -> {
         Path $$2 = FileUtil.resolvePath(p_251046_, p_251647_);
         return returnFileIfExists($$2);
      }, p_337558_ -> {
         LOGGER.error("Invalid path {}: {}", p_250145_, p_337558_.message());
         return null;
      });
   }

   @Nullable
   private static IoSupplier<InputStream> returnFileIfExists(Path p_250506_) {
      return Files.exists(p_250506_) && validatePath(p_250506_) ? IoSupplier.create(p_250506_) : null;
   }

   @Override
   public void listResources(PackType p_251452_, String p_249854_, String p_248650_, PackResources.ResourceOutput p_248572_) {
      FileUtil.decomposePath(p_248650_).ifSuccess(p_250225_ -> {
         Path $$4 = this.root.resolve(p_251452_.getDirectory()).resolve(p_249854_);
         listPath(p_249854_, $$4, p_250225_, p_248572_);
      }).ifError(p_337560_ -> LOGGER.error("Invalid path {}: {}", p_248650_, p_337560_.message()));
   }

   public static void listPath(String p_249455_, Path p_249514_, List<String> p_251918_, PackResources.ResourceOutput p_249964_) {
      Path $$4 = FileUtil.resolvePath(p_249514_, p_251918_);

      try (Stream<Path> $$5 = Files.find($$4, Integer.MAX_VALUE, (p_250060_, p_250796_) -> p_250796_.isRegularFile())) {
         $$5.forEach(p_249092_ -> {
            String $$4x = PATH_JOINER.join(p_249514_.relativize(p_249092_));
            ResourceLocation $$5x = ResourceLocation.tryBuild(p_249455_, $$4x);
            if ($$5x == null) {
               Util.logAndPauseIfInIde(String.format(Locale.ROOT, "Invalid path in pack: %s:%s, ignoring", p_249455_, $$4x));
            } else {
               p_249964_.accept($$5x, IoSupplier.create(p_249092_));
            }
         });
      } catch (NotDirectoryException | NoSuchFileException var10) {
      } catch (IOException var11) {
         LOGGER.error("Failed to list path {}", $$4, var11);
      }
   }

   @Override
   public Set<String> getNamespaces(PackType p_251896_) {
      Set<String> $$1 = Sets.newHashSet();
      Path $$2 = this.root.resolve(p_251896_.getDirectory());

      try (DirectoryStream<Path> $$3 = Files.newDirectoryStream($$2)) {
         for (Path $$4 : $$3) {
            String $$5 = $$4.getFileName().toString();
            if (ResourceLocation.isValidNamespace($$5)) {
               $$1.add($$5);
            } else {
               LOGGER.warn("Non [a-z0-9_.-] character in namespace {} in pack {}, ignoring", $$5, this.root);
            }
         }
      } catch (NotDirectoryException | NoSuchFileException var10) {
      } catch (IOException var11) {
         LOGGER.error("Failed to list path {}", $$2, var11);
      }

      return $$1;
   }

   @Override
   public void close() {
   }

   public static class PathResourcesSupplier implements Pack.ResourcesSupplier {
      private final Path content;

      public PathResourcesSupplier(Path p_294512_) {
         this.content = p_294512_;
      }

      @Override
      public PackResources openPrimary(PackLocationInfo p_325962_) {
         return new PathPackResources(p_325962_, this.content);
      }

      @Override
      public PackResources openFull(PackLocationInfo p_326066_, Pack.Metadata p_326237_) {
         PackResources $$2 = this.openPrimary(p_326066_);
         List<String> $$3 = p_326237_.overlays();
         if ($$3.isEmpty()) {
            return $$2;
         } else {
            List<PackResources> $$4 = new ArrayList<>($$3.size());

            for (String $$5 : $$3) {
               Path $$6 = this.content.resolve($$5);
               $$4.add(new PathPackResources(p_326066_, $$6));
            }

            return new CompositePackResources($$2, $$4);
         }
      }
   }
}
