package net.minecraft.server.packs;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.FileUtil;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class VanillaPackResources implements PackResources {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final PackLocationInfo location;
   private final BuiltInMetadata metadata;
   private final Set<String> namespaces;
   private final List<Path> rootPaths;
   private final Map<PackType, List<Path>> pathsForType;

   VanillaPackResources(
      PackLocationInfo location, BuiltInMetadata metadata, Set<String> namespaces, List<Path> rootPaths, Map<PackType, List<Path>> pathsForType
   ) {
      this.location = location;
      this.metadata = metadata;
      this.namespaces = namespaces;
      this.rootPaths = rootPaths;
      this.pathsForType = pathsForType;
   }

   @Nullable
   @Override
   public IoSupplier<InputStream> getRootResource(String... elements) {
      FileUtil.validatePath(elements);
      List<String> list = List.of(elements);

      for (Path path : this.rootPaths) {
         Path path2 = FileUtil.resolvePath(path, list);
         if (Files.exists(path2) && PathPackResources.validatePath(path2)) {
            return IoSupplier.create(path2);
         }
      }

      return null;
   }

   public void listRawPaths(PackType packType, Identifier packLocation, Consumer<Path> output) {
      FileUtil.decomposePath(packLocation.getPath()).ifSuccess(list -> {
         String string = packLocation.getNamespace();

         for (Path path : this.pathsForType.get(packType)) {
            Path path2 = path.resolve(string);
            output.accept(FileUtil.resolvePath(path2, list));
         }
      }).ifError(error -> LOGGER.error("Invalid path {}: {}", packLocation, error.message()));
   }

   @Override
   public void listResources(PackType packType, String namespace, String path, PackResources.ResourceOutput resourceOutput) {
      FileUtil.decomposePath(path).ifSuccess(list -> {
         List<Path> list2 = this.pathsForType.get(packType);
         int i = list2.size();
         if (i == 1) {
            getResources(resourceOutput, namespace, list2.get(0), list);
         } else if (i > 1) {
            Map<Identifier, IoSupplier<InputStream>> map = new HashMap<>();

            for (int j = 0; j < i - 1; j++) {
               getResources(map::putIfAbsent, namespace, list2.get(j), list);
            }

            Path pathx = list2.get(i - 1);
            if (map.isEmpty()) {
               getResources(resourceOutput, namespace, pathx, list);
            } else {
               getResources(map::putIfAbsent, namespace, pathx, list);
               map.forEach(resourceOutput);
            }
         }
      }).ifError(error -> LOGGER.error("Invalid path {}: {}", path, error.message()));
   }

   private static void getResources(PackResources.ResourceOutput resourceOutput, String namespace, Path root, List<String> paths) {
      Path path = root.resolve(namespace);
      PathPackResources.listPath(namespace, path, paths, resourceOutput);
   }

   @Nullable
   @Override
   public IoSupplier<InputStream> getResource(PackType packType, Identifier location) {
      return (IoSupplier<InputStream>)FileUtil.decomposePath(location.getPath()).mapOrElse(list -> {
         String string = location.getNamespace();

         for (Path path : this.pathsForType.get(packType)) {
            Path path2 = FileUtil.resolvePath(path.resolve(string), list);
            if (Files.exists(path2) && PathPackResources.validatePath(path2)) {
               return IoSupplier.create(path2);
            }
         }

         return null;
      }, error -> {
         LOGGER.error("Invalid path {}: {}", location, error.message());
         return null;
      });
   }

   @Override
   public Set<String> getNamespaces(PackType type) {
      return this.namespaces;
   }

   @Nullable
   @Override
   public <T> T getMetadataSection(MetadataSectionType<T> type) {
      IoSupplier<InputStream> ioSupplier = this.getRootResource("pack.mcmeta");
      if (ioSupplier != null) {
         try (InputStream inputStream = ioSupplier.get()) {
            T object = AbstractPackResources.getMetadataFromStream(type, inputStream, this.location);
            if (object != null) {
               return object;
            }

            return this.metadata.get(type);
         } catch (IOException var8) {
         }
      }

      return this.metadata.get(type);
   }

   @Override
   public PackLocationInfo location() {
      return this.location;
   }

   @Override
   public void close() {
   }

   public ResourceProvider asProvider() {
      return identifier -> Optional.ofNullable(this.getResource(PackType.CLIENT_RESOURCES, identifier))
         .map(ioSupplier -> new Resource(this, (IoSupplier<InputStream>)ioSupplier));
   }
}
