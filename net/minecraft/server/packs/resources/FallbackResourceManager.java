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
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
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

   public void push(PackResources p_215383_, Predicate<ResourceLocation> p_215384_) {
      this.pushInternal(p_215383_.packId(), p_215383_, p_215384_);
   }

   public void pushFilterOnly(String p_215400_, Predicate<ResourceLocation> p_215401_) {
      this.pushInternal(p_215400_, null, p_215401_);
   }

   private void pushInternal(String p_215396_, @Nullable PackResources p_215397_, @Nullable Predicate<ResourceLocation> p_215398_) {
      this.fallbacks.add(new FallbackResourceManager.PackEntry(p_215396_, p_215397_, p_215398_));
   }

   @Override
   public Set<String> getNamespaces() {
      return ImmutableSet.of(this.namespace);
   }

   @Override
   public Optional<Resource> getResource(ResourceLocation p_215419_) {
      for (int $$1 = this.fallbacks.size() - 1; $$1 >= 0; $$1--) {
         FallbackResourceManager.PackEntry $$2 = this.fallbacks.get($$1);
         PackResources $$3 = $$2.resources;
         if ($$3 != null) {
            IoSupplier<InputStream> $$4 = $$3.getResource(this.type, p_215419_);
            if ($$4 != null) {
               IoSupplier<ResourceMetadata> $$5 = this.createStackMetadataFinder(p_215419_, $$1);
               return Optional.of(createResource($$3, p_215419_, $$4, $$5));
            }
         }

         if ($$2.isFiltered(p_215419_)) {
            LOGGER.warn("Resource {} not found, but was filtered by pack {}", p_215419_, $$2.name);
            return Optional.empty();
         }
      }

      return Optional.empty();
   }

   private static Resource createResource(
      PackResources p_249946_, ResourceLocation p_250632_, IoSupplier<InputStream> p_250514_, IoSupplier<ResourceMetadata> p_251676_
   ) {
      return new Resource(p_249946_, wrapForDebug(p_250632_, p_249946_, p_250514_), p_251676_);
   }

   private static IoSupplier<InputStream> wrapForDebug(ResourceLocation p_248639_, PackResources p_251740_, IoSupplier<InputStream> p_249116_) {
      return LOGGER.isDebugEnabled()
         ? () -> new FallbackResourceManager.LeakedResourceWarningInputStream(p_249116_.get(), p_248639_, p_251740_.packId())
         : p_249116_;
   }

   @Override
   public List<Resource> getResourceStack(ResourceLocation p_215367_) {
      ResourceLocation $$1 = getMetadataLocation(p_215367_);
      List<Resource> $$2 = new ArrayList<>();
      boolean $$3 = false;
      String $$4 = null;

      for (int $$5 = this.fallbacks.size() - 1; $$5 >= 0; $$5--) {
         FallbackResourceManager.PackEntry $$6 = this.fallbacks.get($$5);
         PackResources $$7 = $$6.resources;
         if ($$7 != null) {
            IoSupplier<InputStream> $$8 = $$7.getResource(this.type, p_215367_);
            if ($$8 != null) {
               IoSupplier<ResourceMetadata> $$9;
               if ($$3) {
                  $$9 = ResourceMetadata.EMPTY_SUPPLIER;
               } else {
                  $$9 = () -> {
                     IoSupplier<InputStream> $$2x = $$7.getResource(this.type, $$1);
                     return $$2x != null ? parseMetadata($$2x) : ResourceMetadata.EMPTY;
                  };
               }

               $$2.add(new Resource($$7, $$8, $$9));
            }
         }

         if ($$6.isFiltered(p_215367_)) {
            $$4 = $$6.name;
            break;
         }

         if ($$6.isFiltered($$1)) {
            $$3 = true;
         }
      }

      if ($$2.isEmpty() && $$4 != null) {
         LOGGER.warn("Resource {} not found, but was filtered by pack {}", p_215367_, $$4);
      }

      return Lists.reverse($$2);
   }

   private static boolean isMetadata(ResourceLocation p_249381_) {
      return p_249381_.getPath().endsWith(".mcmeta");
   }

   private static ResourceLocation getResourceLocationFromMetadata(ResourceLocation p_249669_) {
      String $$1 = p_249669_.getPath().substring(0, p_249669_.getPath().length() - ".mcmeta".length());
      return p_249669_.withPath($$1);
   }

   static ResourceLocation getMetadataLocation(ResourceLocation p_10625_) {
      return p_10625_.withPath(p_10625_.getPath() + ".mcmeta");
   }

   @Override
   public Map<ResourceLocation, Resource> listResources(String p_215413_, Predicate<ResourceLocation> p_215414_) {
      record ResourceWithSourceAndIndex(PackResources packResources, IoSupplier<InputStream> resource, int packIndex) {
      }

      Map<ResourceLocation, ResourceWithSourceAndIndex> $$2 = new HashMap<>();
      Map<ResourceLocation, ResourceWithSourceAndIndex> $$3 = new HashMap<>();
      int $$4 = this.fallbacks.size();

      for (int $$5 = 0; $$5 < $$4; $$5++) {
         FallbackResourceManager.PackEntry $$6 = this.fallbacks.get($$5);
         $$6.filterAll($$2.keySet());
         $$6.filterAll($$3.keySet());
         PackResources $$7 = $$6.resources;
         if ($$7 != null) {
            int $$8 = $$5;
            $$7.listResources(this.type, this.namespace, p_215413_, (p_248254_, p_248255_) -> {
               if (isMetadata(p_248254_)) {
                  if (p_215414_.test(getResourceLocationFromMetadata(p_248254_))) {
                     $$3.put(p_248254_, new ResourceWithSourceAndIndex($$7, p_248255_, $$8));
                  }
               } else if (p_215414_.test(p_248254_)) {
                  $$2.put(p_248254_, new ResourceWithSourceAndIndex($$7, p_248255_, $$8));
               }
            });
         }
      }

      Map<ResourceLocation, Resource> $$9 = Maps.newTreeMap();
      $$2.forEach((p_248258_, p_248259_) -> {
         ResourceLocation $$4x = getMetadataLocation(p_248258_);
         ResourceWithSourceAndIndex $$5x = $$3.get($$4x);
         IoSupplier<ResourceMetadata> $$6x;
         if ($$5x != null && $$5x.packIndex >= p_248259_.packIndex) {
            $$6x = convertToMetadata($$5x.resource);
         } else {
            $$6x = ResourceMetadata.EMPTY_SUPPLIER;
         }

         $$9.put(p_248258_, createResource(p_248259_.packResources, p_248258_, p_248259_.resource, $$6x));
      });
      return $$9;
   }

   private IoSupplier<ResourceMetadata> createStackMetadataFinder(ResourceLocation p_215369_, int p_215370_) {
      return () -> {
         ResourceLocation $$2 = getMetadataLocation(p_215369_);

         for (int $$3 = this.fallbacks.size() - 1; $$3 >= p_215370_; $$3--) {
            FallbackResourceManager.PackEntry $$4 = this.fallbacks.get($$3);
            PackResources $$5 = $$4.resources;
            if ($$5 != null) {
               IoSupplier<InputStream> $$6 = $$5.getResource(this.type, $$2);
               if ($$6 != null) {
                  return parseMetadata($$6);
               }
            }

            if ($$4.isFiltered($$2)) {
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
      ResourceMetadata var2;
      try (InputStream $$1 = p_250103_.get()) {
         var2 = ResourceMetadata.fromJsonStream($$1);
      }

      return var2;
   }

   private static void applyPackFiltersToExistingResources(
      FallbackResourceManager.PackEntry p_215393_, Map<ResourceLocation, FallbackResourceManager.EntryStack> p_215394_
   ) {
      for (FallbackResourceManager.EntryStack $$2 : p_215394_.values()) {
         if (p_215393_.isFiltered($$2.fileLocation)) {
            $$2.fileSources.clear();
         } else if (p_215393_.isFiltered($$2.metadataLocation())) {
            $$2.metaSources.clear();
         }
      }
   }

   private void listPackResources(
      FallbackResourceManager.PackEntry p_215388_,
      String p_215389_,
      Predicate<ResourceLocation> p_215390_,
      Map<ResourceLocation, FallbackResourceManager.EntryStack> p_215391_
   ) {
      PackResources $$4 = p_215388_.resources;
      if ($$4 != null) {
         $$4.listResources(
            this.type,
            this.namespace,
            p_215389_,
            (p_248266_, p_248267_) -> {
               if (isMetadata(p_248266_)) {
                  ResourceLocation $$5 = getResourceLocationFromMetadata(p_248266_);
                  if (!p_215390_.test($$5)) {
                     return;
                  }

                  p_215391_.computeIfAbsent($$5, FallbackResourceManager.EntryStack::new).metaSources.put($$4, p_248267_);
               } else {
                  if (!p_215390_.test(p_248266_)) {
                     return;
                  }

                  p_215391_.computeIfAbsent(p_248266_, FallbackResourceManager.EntryStack::new)
                     .fileSources
                     .add(new FallbackResourceManager.ResourceWithSource($$4, p_248267_));
               }
            }
         );
      }
   }

   @Override
   public Map<ResourceLocation, List<Resource>> listResourceStacks(String p_215416_, Predicate<ResourceLocation> p_215417_) {
      Map<ResourceLocation, FallbackResourceManager.EntryStack> $$2 = Maps.newHashMap();

      for (FallbackResourceManager.PackEntry $$3 : this.fallbacks) {
         applyPackFiltersToExistingResources($$3, $$2);
         this.listPackResources($$3, p_215416_, p_215417_, $$2);
      }

      TreeMap<ResourceLocation, List<Resource>> $$4 = Maps.newTreeMap();

      for (FallbackResourceManager.EntryStack $$5 : $$2.values()) {
         if (!$$5.fileSources.isEmpty()) {
            List<Resource> $$6 = new ArrayList<>();

            for (FallbackResourceManager.ResourceWithSource $$7 : $$5.fileSources) {
               PackResources $$8 = $$7.source;
               IoSupplier<InputStream> $$9 = $$5.metaSources.get($$8);
               IoSupplier<ResourceMetadata> $$10 = $$9 != null ? convertToMetadata($$9) : ResourceMetadata.EMPTY_SUPPLIER;
               $$6.add(createResource($$8, $$5.fileLocation, $$7.resource, $$10));
            }

            $$4.put($$5.fileLocation, $$6);
         }
      }

      return $$4;
   }

   @Override
   public Stream<PackResources> listPacks() {
      return this.fallbacks.stream().map(p_215386_ -> p_215386_.resources).filter(Objects::nonNull);
   }

   record EntryStack(
      ResourceLocation fileLocation,
      ResourceLocation metadataLocation,
      List<FallbackResourceManager.ResourceWithSource> fileSources,
      Map<PackResources, IoSupplier<InputStream>> metaSources
   ) {
      EntryStack(ResourceLocation p_251350_) {
         this(p_251350_, FallbackResourceManager.getMetadataLocation(p_251350_), new ArrayList<>(), new Object2ObjectArrayMap());
      }
   }

   static class LeakedResourceWarningInputStream extends FilterInputStream {
      private final Supplier<String> message;
      private boolean closed;

      public LeakedResourceWarningInputStream(InputStream p_10633_, ResourceLocation p_10634_, String p_10635_) {
         super(p_10633_);
         Exception $$3 = new Exception("Stacktrace");
         this.message = () -> {
            StringWriter $$3x = new StringWriter();
            $$3.printStackTrace(new PrintWriter($$3x));
            return "Leaked resource: '" + p_10634_ + "' loaded from pack: '" + p_10635_ + "'\n" + $$3x;
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

   record PackEntry(String name, @Nullable PackResources resources, @Nullable Predicate<ResourceLocation> filter) {
      public void filterAll(Collection<ResourceLocation> p_215443_) {
         if (this.filter != null) {
            p_215443_.removeIf(this.filter);
         }
      }

      public boolean isFiltered(ResourceLocation p_215441_) {
         return this.filter != null && this.filter.test(p_215441_);
      }
   }

   record ResourceWithSource(PackResources source, IoSupplier<InputStream> resource) {
   }
}
