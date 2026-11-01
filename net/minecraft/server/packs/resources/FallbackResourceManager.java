package net.minecraft.server.packs.resources;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class FallbackResourceManager implements ResourceManager {
   static final Logger LOGGER = LogUtils.getLogger();
   protected final List<FallbackResourceManager.PackEntry> fallbacks = Lists.newArrayList();
   private final PackType type;
   private final String namespace;

   public FallbackResourceManager(PackType type, String namespace) {
      this.type = type;
      this.namespace = namespace;
   }

   public void push(PackResources resources) {
      this.pushInternal(resources.packId(), resources, null);
   }

   public void push(PackResources resources, Predicate<Identifier> filter) {
      this.pushInternal(resources.packId(), resources, filter);
   }

   public void pushFilterOnly(String name, Predicate<Identifier> filter) {
      this.pushInternal(name, null, filter);
   }

   private void pushInternal(String name, @Nullable PackResources resources, @Nullable Predicate<Identifier> filter) {
      this.fallbacks.add(new FallbackResourceManager.PackEntry(name, resources, filter));
   }

   @Override
   public Set<String> getNamespaces() {
      return ImmutableSet.of(this.namespace);
   }

   @Override
   public Optional<Resource> getResource(Identifier identifier) {
      for (int i = this.fallbacks.size() - 1; i >= 0; i--) {
         FallbackResourceManager.PackEntry packEntry = this.fallbacks.get(i);
         PackResources packResources = packEntry.resources;
         if (packResources != null) {
            IoSupplier<InputStream> ioSupplier = packResources.getResource(this.type, identifier);
            if (ioSupplier != null) {
               IoSupplier<ResourceMetadata> ioSupplier2 = this.createStackMetadataFinder(identifier, i);
               return Optional.of(createResource(packResources, identifier, ioSupplier, ioSupplier2));
            }
         }

         if (packEntry.isFiltered(identifier)) {
            LOGGER.warn("Resource {} not found, but was filtered by pack {}", identifier, packEntry.name);
            return Optional.empty();
         }
      }

      return Optional.empty();
   }

   private static Resource createResource(
      PackResources source, Identifier location, IoSupplier<InputStream> streamSupplier, IoSupplier<ResourceMetadata> metadataSupplier
   ) {
      return new Resource(source, wrapForDebug(location, source, streamSupplier), metadataSupplier);
   }

   private static IoSupplier<InputStream> wrapForDebug(Identifier location, PackResources packResources, IoSupplier<InputStream> stream) {
      return LOGGER.isDebugEnabled()
         ? () -> new FallbackResourceManager.LeakedResourceWarningInputStream(stream.get(), location, packResources.packId())
         : stream;
   }

   @Override
   public List<Resource> getResourceStack(Identifier location) {
      Identifier identifier = getMetadataLocation(location);
      List<Resource> list = new ArrayList<>();
      boolean bl = false;
      String string = null;

      for (int i = this.fallbacks.size() - 1; i >= 0; i--) {
         FallbackResourceManager.PackEntry packEntry = this.fallbacks.get(i);
         PackResources packResources = packEntry.resources;
         if (packResources != null) {
            IoSupplier<InputStream> ioSupplier = packResources.getResource(this.type, location);
            if (ioSupplier != null) {
               IoSupplier<ResourceMetadata> ioSupplier2;
               if (bl) {
                  ioSupplier2 = ResourceMetadata.EMPTY_SUPPLIER;
               } else {
                  ioSupplier2 = () -> {
                     IoSupplier<InputStream> ioSupplierx = packResources.getResource(this.type, identifier);
                     return ioSupplierx != null ? parseMetadata(ioSupplierx) : ResourceMetadata.EMPTY;
                  };
               }

               list.add(new Resource(packResources, ioSupplier, ioSupplier2));
            }
         }

         if (packEntry.isFiltered(location)) {
            string = packEntry.name;
            break;
         }

         if (packEntry.isFiltered(identifier)) {
            bl = true;
         }
      }

      if (list.isEmpty() && string != null) {
         LOGGER.warn("Resource {} not found, but was filtered by pack {}", location, string);
      }

      return Lists.reverse(list);
   }

   private static boolean isMetadata(Identifier location) {
      return location.getPath().endsWith(".mcmeta");
   }

   private static Identifier getIdentifierFromMetadata(Identifier location) {
      String string = location.getPath().substring(0, location.getPath().length() - ".mcmeta".length());
      return location.withPath(string);
   }

   static Identifier getMetadataLocation(Identifier location) {
      return location.withPath(location.getPath() + ".mcmeta");
   }

   @Override
   public Map<Identifier, Resource> listResources(String path, Predicate<Identifier> filter) {
      record ResourceWithSourceAndIndex(PackResources packResources, IoSupplier<InputStream> resource, int packIndex) {
      }

      Map<Identifier, ResourceWithSourceAndIndex> map = new HashMap<>();
      Map<Identifier, ResourceWithSourceAndIndex> map2 = new HashMap<>();
      int i = this.fallbacks.size();

      for (int j = 0; j < i; j++) {
         FallbackResourceManager.PackEntry packEntry = this.fallbacks.get(j);
         packEntry.filterAll(map.keySet());
         packEntry.filterAll(map2.keySet());
         PackResources packResources = packEntry.resources;
         if (packResources != null) {
            int k = j;
            packResources.listResources(this.type, this.namespace, path, (identifier, ioSupplier) -> {
               if (isMetadata(identifier)) {
                  if (filter.test(getIdentifierFromMetadata(identifier))) {
                     map2.put(identifier, new ResourceWithSourceAndIndex(packResources, ioSupplier, k));
                  }
               } else if (filter.test(identifier)) {
                  map.put(identifier, new ResourceWithSourceAndIndex(packResources, ioSupplier, k));
               }
            });
         }
      }

      Map<Identifier, Resource> map3 = Maps.newTreeMap();
      map.forEach((identifier, arg) -> {
         Identifier identifier2 = getMetadataLocation(identifier);
         ResourceWithSourceAndIndex lv = map2.get(identifier2);
         IoSupplier<ResourceMetadata> ioSupplier;
         if (lv != null && lv.packIndex >= arg.packIndex) {
            ioSupplier = convertToMetadata(lv.resource);
         } else {
            ioSupplier = ResourceMetadata.EMPTY_SUPPLIER;
         }

         map3.put(identifier, createResource(arg.packResources, identifier, arg.resource, ioSupplier));
      });
      return map3;
   }

   private IoSupplier<ResourceMetadata> createStackMetadataFinder(Identifier location, int fallbackIndex) {
      return () -> {
         Identifier identifier2 = getMetadataLocation(location);

         for (int j = this.fallbacks.size() - 1; j >= fallbackIndex; j--) {
            FallbackResourceManager.PackEntry packEntry = this.fallbacks.get(j);
            PackResources packResources = packEntry.resources;
            if (packResources != null) {
               IoSupplier<InputStream> ioSupplier = packResources.getResource(this.type, identifier2);
               if (ioSupplier != null) {
                  return parseMetadata(ioSupplier);
               }
            }

            if (packEntry.isFiltered(identifier2)) {
               break;
            }
         }

         return ResourceMetadata.EMPTY;
      };
   }

   private static IoSupplier<ResourceMetadata> convertToMetadata(IoSupplier<InputStream> streamSupplier) {
      return () -> parseMetadata(streamSupplier);
   }

   private static ResourceMetadata parseMetadata(IoSupplier<InputStream> streamSupplier) throws IOException {
      ResourceMetadata var2;
      try (InputStream inputStream = streamSupplier.get()) {
         var2 = ResourceMetadata.fromJsonStream(inputStream);
      }

      return var2;
   }

   private static void applyPackFiltersToExistingResources(
      FallbackResourceManager.PackEntry packEntry, Map<Identifier, FallbackResourceManager.EntryStack> resources
   ) {
      for (FallbackResourceManager.EntryStack entryStack : resources.values()) {
         if (packEntry.isFiltered(entryStack.fileLocation)) {
            entryStack.fileSources.clear();
         } else if (packEntry.isFiltered(entryStack.metadataLocation())) {
            entryStack.metaSources.clear();
         }
      }
   }

   private void listPackResources(
      FallbackResourceManager.PackEntry entry, String path, Predicate<Identifier> filter, Map<Identifier, FallbackResourceManager.EntryStack> output
   ) {
      PackResources packResources = entry.resources;
      if (packResources != null) {
         packResources.listResources(
            this.type,
            this.namespace,
            path,
            (identifier, ioSupplier) -> {
               if (isMetadata(identifier)) {
                  Identifier identifier2 = getIdentifierFromMetadata(identifier);
                  if (!filter.test(identifier2)) {
                     return;
                  }

                  output.computeIfAbsent(identifier2, FallbackResourceManager.EntryStack::new).metaSources.put(packResources, ioSupplier);
               } else {
                  if (!filter.test(identifier)) {
                     return;
                  }

                  output.computeIfAbsent(identifier, FallbackResourceManager.EntryStack::new)
                     .fileSources
                     .add(new FallbackResourceManager.ResourceWithSource(packResources, ioSupplier));
               }
            }
         );
      }
   }

   @Override
   public Map<Identifier, List<Resource>> listResourceStacks(String path, Predicate<Identifier> filter) {
      Map<Identifier, FallbackResourceManager.EntryStack> map = Maps.newHashMap();

      for (FallbackResourceManager.PackEntry packEntry : this.fallbacks) {
         applyPackFiltersToExistingResources(packEntry, map);
         this.listPackResources(packEntry, path, filter, map);
      }

      TreeMap<Identifier, List<Resource>> treeMap = Maps.newTreeMap();

      for (FallbackResourceManager.EntryStack entryStack : map.values()) {
         if (!entryStack.fileSources.isEmpty()) {
            List<Resource> list = new ArrayList<>();

            for (FallbackResourceManager.ResourceWithSource resourceWithSource : entryStack.fileSources) {
               PackResources packResources = resourceWithSource.source;
               IoSupplier<InputStream> ioSupplier = entryStack.metaSources.get(packResources);
               IoSupplier<ResourceMetadata> ioSupplier2 = ioSupplier != null ? convertToMetadata(ioSupplier) : ResourceMetadata.EMPTY_SUPPLIER;
               list.add(createResource(packResources, entryStack.fileLocation, resourceWithSource.resource, ioSupplier2));
            }

            treeMap.put(entryStack.fileLocation, list);
         }
      }

      return treeMap;
   }

   @Override
   public Stream<PackResources> listPacks() {
      return this.fallbacks.stream().map(packEntry -> packEntry.resources).filter(Objects::nonNull);
   }

   record EntryStack(
      Identifier fileLocation,
      Identifier metadataLocation,
      List<FallbackResourceManager.ResourceWithSource> fileSources,
      Map<PackResources, IoSupplier<InputStream>> metaSources
   ) {

      EntryStack(Identifier fileLocation) {
         this(fileLocation, FallbackResourceManager.getMetadataLocation(fileLocation), new ArrayList<>(), new Object2ObjectArrayMap());
      }
   }

   static class LeakedResourceWarningInputStream extends FilterInputStream {
      private final Supplier<String> message;
      private boolean closed;

      public LeakedResourceWarningInputStream(InputStream inputStream, Identifier resourceLocation, String packName) {
         super(inputStream);
         Exception exception = new Exception("Stacktrace");
         this.message = () -> {
            StringWriter stringWriter = new StringWriter();
            exception.printStackTrace(new PrintWriter(stringWriter));
            return "Leaked resource: '" + resourceLocation + "' loaded from pack: '" + packName + "'\n" + stringWriter;
         };
      }

      @Override
      public void close() throws IOException {
         super.close();
         this.closed = true;
      }

      @Override
      protected void finalize() throws Throwable {
         if (!this.closed) {
            FallbackResourceManager.LOGGER.warn("{}", this.message.get());
         }

         super.finalize();
      }
   }

   record PackEntry(String name, @Nullable PackResources resources, @Nullable Predicate<Identifier> filter) {

      public void filterAll(Collection<Identifier> locations) {
         if (this.filter != null) {
            locations.removeIf(this.filter);
         }
      }

      public boolean isFiltered(Identifier location) {
         return this.filter != null && this.filter.test(location);
      }
   }

   record ResourceWithSource(PackResources source, IoSupplier<InputStream> resource) {
   }
}
