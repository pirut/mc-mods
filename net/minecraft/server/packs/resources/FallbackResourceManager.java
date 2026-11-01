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
   public final List<FallbackResourceManager.PackEntry> fallbacks = Lists.newArrayList();
   private final PackType type;
   private final String namespace;

   public FallbackResourceManager(PackType p_10605_, String p_10606_) {
      this.type = p_10605_;
      this.namespace = p_10606_;
   }

   public void push(PackResources p_215378_) {
      this.pushInternal(p_215378_.packId(), p_215378_, null);
   }

   public void push(PackResources p_215383_, Predicate<Identifier> p_215384_) {
      this.pushInternal(p_215383_.packId(), p_215383_, p_215384_);
   }

   public void pushFilterOnly(String p_215400_, Predicate<Identifier> p_215401_) {
      this.pushInternal(p_215400_, null, p_215401_);
   }

   private void pushInternal(String p_215396_, @Nullable PackResources p_215397_, @Nullable Predicate<Identifier> p_215398_) {
      this.fallbacks.add(new FallbackResourceManager.PackEntry(p_215396_, p_215397_, p_215398_));
   }

   @Override
   public Set<String> getNamespaces() {
      return ImmutableSet.of(this.namespace);
   }

   @Override
   public Optional<Resource> getResource(Identifier p_468360_) {
      for (int i = this.fallbacks.size() - 1; i >= 0; i--) {
         FallbackResourceManager.PackEntry fallbackresourcemanager$packentry = this.fallbacks.get(i);
         PackResources packresources = fallbackresourcemanager$packentry.resources;
         if (packresources != null) {
            IoSupplier<InputStream> iosupplier = packresources.getResource(this.type, p_468360_);
            if (iosupplier != null) {
               IoSupplier<ResourceMetadata> iosupplier1 = this.createStackMetadataFinder(p_468360_, i);
               return Optional.of(createResource(packresources, p_468360_, iosupplier, iosupplier1));
            }
         }

         if (fallbackresourcemanager$packentry.isFiltered(p_468360_)) {
            LOGGER.warn("Resource {} not found, but was filtered by pack {}", p_468360_, fallbackresourcemanager$packentry.name);
            return Optional.empty();
         }
      }

      return Optional.empty();
   }

   private static Resource createResource(
      PackResources p_249946_, Identifier p_469159_, IoSupplier<InputStream> p_250514_, IoSupplier<ResourceMetadata> p_251676_
   ) {
      return new Resource(p_249946_, wrapForDebug(p_469159_, p_249946_, p_250514_), p_251676_);
   }

   private static IoSupplier<InputStream> wrapForDebug(Identifier p_468218_, PackResources p_251740_, IoSupplier<InputStream> p_249116_) {
      return LOGGER.isDebugEnabled()
         ? () -> new FallbackResourceManager.LeakedResourceWarningInputStream(p_249116_.get(), p_468218_, p_251740_.packId())
         : p_249116_;
   }

   @Override
   public List<Resource> getResourceStack(Identifier p_469116_) {
      Identifier identifier = getMetadataLocation(p_469116_);
      List<Resource> list = new ArrayList<>();
      boolean flag = false;
      String s = null;

      for (int i = this.fallbacks.size() - 1; i >= 0; i--) {
         FallbackResourceManager.PackEntry fallbackresourcemanager$packentry = this.fallbacks.get(i);
         PackResources packresources = fallbackresourcemanager$packentry.resources;
         if (packresources != null) {
            IoSupplier<InputStream> iosupplier = packresources.getResource(this.type, p_469116_);
            if (iosupplier != null) {
               IoSupplier<ResourceMetadata> iosupplier1;
               if (flag) {
                  iosupplier1 = ResourceMetadata.EMPTY_SUPPLIER;
               } else {
                  iosupplier1 = () -> {
                     IoSupplier<InputStream> iosupplier2 = packresources.getResource(this.type, identifier);
                     return iosupplier2 != null ? parseMetadata(iosupplier2) : ResourceMetadata.EMPTY;
                  };
               }

               list.add(new Resource(packresources, iosupplier, iosupplier1));
            }
         }

         if (fallbackresourcemanager$packentry.isFiltered(p_469116_)) {
            s = fallbackresourcemanager$packentry.name;
            break;
         }

         if (fallbackresourcemanager$packentry.isFiltered(identifier)) {
            flag = true;
         }
      }

      if (list.isEmpty() && s != null) {
         LOGGER.warn("Resource {} not found, but was filtered by pack {}", p_469116_, s);
      }

      return Lists.reverse(list);
   }

   private static boolean isMetadata(Identifier p_466832_) {
      return p_466832_.getPath().endsWith(".mcmeta");
   }

   private static Identifier getIdentifierFromMetadata(Identifier p_467011_) {
      String s = p_467011_.getPath().substring(0, p_467011_.getPath().length() - ".mcmeta".length());
      return p_467011_.withPath(s);
   }

   static Identifier getMetadataLocation(Identifier p_467646_) {
      return p_467646_.withPath(p_467646_.getPath() + ".mcmeta");
   }

   @Override
   public Map<Identifier, Resource> listResources(String p_215413_, Predicate<Identifier> p_215414_) {
      record ResourceWithSourceAndIndex(PackResources packResources, IoSupplier<InputStream> resource, int packIndex) {
      }

      Map<Identifier, ResourceWithSourceAndIndex> map = new HashMap<>();
      Map<Identifier, ResourceWithSourceAndIndex> map1 = new HashMap<>();
      int i = this.fallbacks.size();

      for (int j = 0; j < i; j++) {
         FallbackResourceManager.PackEntry fallbackresourcemanager$packentry = this.fallbacks.get(j);
         fallbackresourcemanager$packentry.filterAll(map.keySet());
         fallbackresourcemanager$packentry.filterAll(map1.keySet());
         PackResources packresources = fallbackresourcemanager$packentry.resources;
         if (packresources != null) {
            int k = j;
            packresources.listResources(this.type, this.namespace, p_215413_, (p_466366_, p_466367_) -> {
               if (isMetadata(p_466366_)) {
                  if (p_215414_.test(getIdentifierFromMetadata(p_466366_))) {
                     map1.put(p_466366_, new ResourceWithSourceAndIndex(packresources, p_466367_, k));
                  }
               } else if (p_215414_.test(p_466366_)) {
                  map.put(p_466366_, new ResourceWithSourceAndIndex(packresources, p_466367_, k));
               }
            });
         }
      }

      Map<Identifier, Resource> map2 = Maps.newTreeMap();
      map.forEach(
         (p_466372_, p_466373_) -> {
            Identifier identifier = getMetadataLocation(p_466372_);
            ResourceWithSourceAndIndex fallbackresourcemanager$1resourcewithsourceandindex = map1.get(identifier);
            IoSupplier<ResourceMetadata> iosupplier;
            if (fallbackresourcemanager$1resourcewithsourceandindex != null
               && fallbackresourcemanager$1resourcewithsourceandindex.packIndex >= p_466373_.packIndex) {
               iosupplier = convertToMetadata(fallbackresourcemanager$1resourcewithsourceandindex.resource);
            } else {
               iosupplier = ResourceMetadata.EMPTY_SUPPLIER;
            }

            map2.put(p_466372_, createResource(p_466373_.packResources, p_466372_, p_466373_.resource, iosupplier));
         }
      );
      return map2;
   }

   private IoSupplier<ResourceMetadata> createStackMetadataFinder(Identifier p_467840_, int p_215370_) {
      return () -> {
         Identifier identifier = getMetadataLocation(p_467840_);

         for (int i = this.fallbacks.size() - 1; i >= p_215370_; i--) {
            FallbackResourceManager.PackEntry fallbackresourcemanager$packentry = this.fallbacks.get(i);
            PackResources packresources = fallbackresourcemanager$packentry.resources;
            if (packresources != null) {
               IoSupplier<InputStream> iosupplier = packresources.getResource(this.type, identifier);
               if (iosupplier != null) {
                  return parseMetadata(iosupplier);
               }
            }

            if (fallbackresourcemanager$packentry.isFiltered(identifier)) {
               break;
            }
         }

         return ResourceMetadata.EMPTY;
      };
   }

   private static IoSupplier<ResourceMetadata> convertToMetadata(IoSupplier<InputStream> p_250827_) {
      return () -> parseMetadata(p_250827_);
   }

   private static ResourceMetadata parseMetadata(IoSupplier<InputStream> p_250103_) throws IOException {
      ResourceMetadata resourcemetadata;
      try (InputStream inputstream = p_250103_.get()) {
         resourcemetadata = ResourceMetadata.fromJsonStream(inputstream);
      }

      return resourcemetadata;
   }

   private static void applyPackFiltersToExistingResources(
      FallbackResourceManager.PackEntry p_215393_, Map<Identifier, FallbackResourceManager.EntryStack> p_215394_
   ) {
      for (FallbackResourceManager.EntryStack fallbackresourcemanager$entrystack : p_215394_.values()) {
         if (p_215393_.isFiltered(fallbackresourcemanager$entrystack.fileLocation)) {
            fallbackresourcemanager$entrystack.fileSources.clear();
         } else if (p_215393_.isFiltered(fallbackresourcemanager$entrystack.metadataLocation())) {
            fallbackresourcemanager$entrystack.metaSources.clear();
         }
      }
   }

   private void listPackResources(
      FallbackResourceManager.PackEntry p_215388_,
      String p_215389_,
      Predicate<Identifier> p_215390_,
      Map<Identifier, FallbackResourceManager.EntryStack> p_215391_
   ) {
      PackResources packresources = p_215388_.resources;
      if (packresources != null) {
         packresources.listResources(
            this.type,
            this.namespace,
            p_215389_,
            (p_466377_, p_466378_) -> {
               if (isMetadata(p_466377_)) {
                  Identifier identifier = getIdentifierFromMetadata(p_466377_);
                  if (!p_215390_.test(identifier)) {
                     return;
                  }

                  p_215391_.computeIfAbsent(identifier, FallbackResourceManager.EntryStack::new).metaSources.put(packresources, p_466378_);
               } else {
                  if (!p_215390_.test(p_466377_)) {
                     return;
                  }

                  p_215391_.computeIfAbsent(p_466377_, FallbackResourceManager.EntryStack::new)
                     .fileSources
                     .add(new FallbackResourceManager.ResourceWithSource(packresources, p_466378_));
               }
            }
         );
      }
   }

   @Override
   public Map<Identifier, List<Resource>> listResourceStacks(String p_215416_, Predicate<Identifier> p_215417_) {
      Map<Identifier, FallbackResourceManager.EntryStack> map = Maps.newHashMap();

      for (FallbackResourceManager.PackEntry fallbackresourcemanager$packentry : this.fallbacks) {
         applyPackFiltersToExistingResources(fallbackresourcemanager$packentry, map);
         this.listPackResources(fallbackresourcemanager$packentry, p_215416_, p_215417_, map);
      }

      TreeMap<Identifier, List<Resource>> treemap = Maps.newTreeMap();

      for (FallbackResourceManager.EntryStack fallbackresourcemanager$entrystack : map.values()) {
         if (!fallbackresourcemanager$entrystack.fileSources.isEmpty()) {
            List<Resource> list = new ArrayList<>();

            for (FallbackResourceManager.ResourceWithSource fallbackresourcemanager$resourcewithsource : fallbackresourcemanager$entrystack.fileSources) {
               PackResources packresources = fallbackresourcemanager$resourcewithsource.source;
               IoSupplier<InputStream> iosupplier = fallbackresourcemanager$entrystack.metaSources.get(packresources);
               IoSupplier<ResourceMetadata> iosupplier1 = iosupplier != null ? convertToMetadata(iosupplier) : ResourceMetadata.EMPTY_SUPPLIER;
               list.add(
                  createResource(
                     packresources, fallbackresourcemanager$entrystack.fileLocation, fallbackresourcemanager$resourcewithsource.resource, iosupplier1
                  )
               );
            }

            treemap.put(fallbackresourcemanager$entrystack.fileLocation, list);
         }
      }

      return treemap;
   }

   @Override
   public Stream<PackResources> listPacks() {
      return this.fallbacks.stream().map(p_215386_ -> p_215386_.resources).filter(Objects::nonNull);
   }

   record EntryStack(
      Identifier fileLocation,
      Identifier metadataLocation,
      List<FallbackResourceManager.ResourceWithSource> fileSources,
      Map<PackResources, IoSupplier<InputStream>> metaSources
   ) {
      EntryStack(Identifier p_467793_) {
         this(p_467793_, FallbackResourceManager.getMetadataLocation(p_467793_), new ArrayList<>(), new Object2ObjectArrayMap());
      }
   }

   static class LeakedResourceWarningInputStream extends FilterInputStream {
      private final Supplier<String> message;
      private boolean closed;

      public LeakedResourceWarningInputStream(InputStream p_10633_, Identifier p_468465_, String p_10635_) {
         super(p_10633_);
         Exception exception = new Exception("Stacktrace");
         this.message = () -> {
            StringWriter stringwriter = new StringWriter();
            exception.printStackTrace(new PrintWriter(stringwriter));
            return "Leaked resource: '" + p_468465_ + "' loaded from pack: '" + p_10635_ + "'\n" + stringwriter;
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
      public void filterAll(Collection<Identifier> p_215443_) {
         if (this.filter != null) {
            p_215443_.removeIf(this.filter);
         }
      }

      public boolean isFiltered(Identifier p_467798_) {
         return this.filter != null && this.filter.test(p_467798_);
      }
   }

   record ResourceWithSource(PackResources source, IoSupplier<InputStream> resource) {
   }
}
