package net.minecraft.server.packs;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.Util;
import org.slf4j.Logger;

public class VanillaPackResourcesBuilder {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static Consumer<VanillaPackResourcesBuilder> developmentConfig = p_251787_ -> {};
   private static final Map<PackType, Path> ROOT_DIR_BY_TYPE = Util.make(() -> {
      synchronized (VanillaPackResources.class) {
         Builder<PackType, Path> $$0 = ImmutableMap.builder();

         for (PackType $$1 : PackType.values()) {
            String $$2 = "/" + $$1.getDirectory() + "/.mcassetsroot";
            URL $$3 = VanillaPackResources.class.getResource($$2);
            if ($$3 == null) {
               LOGGER.error("File {} does not exist in classpath", $$2);
            } else {
               try {
                  URI $$4 = $$3.toURI();
                  String $$5 = $$4.getScheme();
                  if (!"jar".equals($$5) && !"file".equals($$5)) {
                     LOGGER.warn("Assets URL '{}' uses unexpected schema", $$4);
                  }

                  Path $$6 = safeGetPath($$4);
                  $$0.put($$1, $$6.getParent());
               } catch (Exception var12) {
                  LOGGER.error("Couldn't resolve path to vanilla assets", var12);
               }
            }
         }

         return $$0.build();
      }
   });
   private final Set<Path> rootPaths = new LinkedHashSet<>();
   private final Map<PackType, Set<Path>> pathsForType = new EnumMap<>(PackType.class);
   private BuiltInMetadata metadata = BuiltInMetadata.of();
   private final Set<String> namespaces = new HashSet<>();

   private static Path safeGetPath(URI p_248652_) throws IOException {
      try {
         return Paths.get(p_248652_);
      } catch (FileSystemNotFoundException var3) {
      } catch (Throwable var4) {
         LOGGER.warn("Unable to get path for: {}", p_248652_, var4);
      }

      try {
         FileSystems.newFileSystem(p_248652_, Collections.emptyMap());
      } catch (FileSystemAlreadyExistsException var2) {
      }

      return Paths.get(p_248652_);
   }

   private boolean validateDirPath(Path p_249112_) {
      if (!Files.exists(p_249112_)) {
         return false;
      } else if (!Files.isDirectory(p_249112_)) {
         throw new IllegalArgumentException("Path " + p_249112_.toAbsolutePath() + " is not directory");
      } else {
         return true;
      }
   }

   private void pushRootPath(Path p_251084_) {
      if (this.validateDirPath(p_251084_)) {
         this.rootPaths.add(p_251084_);
      }
   }

   private void pushPathForType(PackType p_250073_, Path p_252259_) {
      if (this.validateDirPath(p_252259_)) {
         this.pathsForType.computeIfAbsent(p_250073_, p_250639_ -> new LinkedHashSet<>()).add(p_252259_);
      }
   }

   public VanillaPackResourcesBuilder pushJarResources() {
      ROOT_DIR_BY_TYPE.forEach((p_251514_, p_251979_) -> {
         this.pushRootPath(p_251979_.getParent());
         this.pushPathForType(p_251514_, p_251979_);
      });
      return this;
   }

   public VanillaPackResourcesBuilder pushClasspathResources(PackType p_251987_, Class<?> p_249062_) {
      Enumeration<URL> $$2 = null;

      try {
         $$2 = p_249062_.getClassLoader().getResources(p_251987_.getDirectory() + "/");
      } catch (IOException var8) {
      }

      while ($$2 != null && $$2.hasMoreElements()) {
         URL $$3 = $$2.nextElement();

         try {
            URI $$4 = $$3.toURI();
            if ("file".equals($$4.getScheme())) {
               Path $$5 = Paths.get($$4);
               this.pushRootPath($$5.getParent());
               this.pushPathForType(p_251987_, $$5);
            }
         } catch (Exception var7) {
            LOGGER.error("Failed to extract path from {}", $$3, var7);
         }
      }

      return this;
   }

   public VanillaPackResourcesBuilder applyDevelopmentConfig() {
      developmentConfig.accept(this);
      return this;
   }

   public VanillaPackResourcesBuilder pushUniversalPath(Path p_249464_) {
      this.pushRootPath(p_249464_);

      for (PackType $$1 : PackType.values()) {
         this.pushPathForType($$1, p_249464_.resolve($$1.getDirectory()));
      }

      return this;
   }

   public VanillaPackResourcesBuilder pushAssetPath(PackType p_248623_, Path p_250065_) {
      this.pushRootPath(p_250065_);
      this.pushPathForType(p_248623_, p_250065_);
      return this;
   }

   public VanillaPackResourcesBuilder setMetadata(BuiltInMetadata p_249597_) {
      this.metadata = p_249597_;
      return this;
   }

   public VanillaPackResourcesBuilder exposeNamespace(String... p_250838_) {
      this.namespaces.addAll(Arrays.asList(p_250838_));
      return this;
   }

   public VanillaPackResources build(PackLocationInfo p_326441_) {
      Map<PackType, List<Path>> $$1 = new EnumMap<>(PackType.class);

      for (PackType $$2 : PackType.values()) {
         List<Path> $$3 = copyAndReverse(this.pathsForType.getOrDefault($$2, Set.of()));
         $$1.put($$2, $$3);
      }

      return new VanillaPackResources(p_326441_, this.metadata, Set.copyOf(this.namespaces), copyAndReverse(this.rootPaths), $$1);
   }

   private static List<Path> copyAndReverse(Collection<Path> p_252072_) {
      List<Path> $$1 = new ArrayList<>(p_252072_);
      Collections.reverse($$1);
      return List.copyOf($$1);
   }
}
