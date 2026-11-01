package net.minecraft.tags;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.DependencySorter;
import net.minecraft.util.StrictJsonParser;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class TagLoader<T> {
   private static final Logger LOGGER = LogUtils.getLogger();
   final TagLoader.ElementLookup<T> elementLookup;
   private final String directory;

   public TagLoader(TagLoader.ElementLookup<T> p_380077_, String p_144494_) {
      this.elementLookup = p_380077_;
      this.directory = p_144494_;
   }

   public Map<Identifier, List<TagLoader.EntryWithSource>> load(ResourceManager p_144496_) {
      Map<Identifier, List<TagLoader.EntryWithSource>> map = new HashMap<>();
      FileToIdConverter filetoidconverter = FileToIdConverter.json(this.directory);

      for (Entry<Identifier, List<Resource>> entry : filetoidconverter.listMatchingResourceStacks(p_144496_).entrySet()) {
         Identifier identifier = entry.getKey();
         Identifier identifier1 = filetoidconverter.fileToId(identifier);

         for (Resource resource : entry.getValue()) {
            try (Reader reader = resource.openAsReader()) {
               JsonElement jsonelement = StrictJsonParser.parse(reader);
               List<TagLoader.EntryWithSource> list = map.computeIfAbsent(identifier1, p_467206_ -> new ArrayList<>());
               TagFile tagfile = (TagFile)TagFile.CODEC.parse(new Dynamic(JsonOps.INSTANCE, jsonelement)).getOrThrow();
               if (tagfile.replace()) {
                  list.clear();
               }

               String s = resource.sourcePackId();
               tagfile.entries().forEach(p_215997_ -> list.add(new TagLoader.EntryWithSource(p_215997_, s)));
               tagfile.remove().forEach(e -> list.add(new TagLoader.EntryWithSource(e.withRequired(false), s, true)));
            } catch (Exception var17) {
               LOGGER.error("Couldn't read tag list {} from {} in data pack {}", new Object[]{identifier1, identifier, resource.sourcePackId(), var17});
            }
         }
      }

      return map;
   }

   private Either<List<TagLoader.EntryWithSource>, List<T>> tryBuildTag(TagEntry.Lookup<T> p_215979_, List<TagLoader.EntryWithSource> p_215980_) {
      SequencedSet<T> sequencedset = new LinkedHashSet<>();
      List<TagLoader.EntryWithSource> list = new ArrayList<>();

      for (TagLoader.EntryWithSource tagloader$entrywithsource : p_215980_) {
         if (!tagloader$entrywithsource.entry().build(p_215979_, tagloader$entrywithsource.remove() ? sequencedset::remove : sequencedset::add)) {
            list.add(tagloader$entrywithsource);
         }
      }

      return list.isEmpty() ? Either.right(List.copyOf(sequencedset)) : Either.left(list);
   }

   public Map<Identifier, List<T>> build(Map<Identifier, List<TagLoader.EntryWithSource>> p_203899_) {
      final Map<Identifier, List<T>> map = new HashMap<>();
      TagEntry.Lookup<T> lookup = new TagEntry.Lookup<T>() {
         @Nullable
         @Override
         public T element(Identifier p_468161_, boolean p_380359_) {
            return (T)TagLoader.this.elementLookup.get(p_468161_, p_380359_).orElse(null);
         }

         @Nullable
         @Override
         public Collection<T> tag(Identifier p_466850_) {
            return map.get(p_466850_);
         }
      };
      DependencySorter<Identifier, TagLoader.SortingEntry> dependencysorter = new DependencySorter<>();
      p_203899_.forEach((p_466937_, p_284686_) -> dependencysorter.addEntry(p_466937_, new TagLoader.SortingEntry((List<TagLoader.EntryWithSource>)p_284686_)));
      dependencysorter.orderByDependencies(
         (p_468743_, p_359646_) -> this.tryBuildTag(lookup, p_359646_.entries)
            .ifLeft(
               p_359633_ -> LOGGER.error(
                  "Couldn't load tag {} as it is missing following references: {}",
                  p_468743_,
                  p_359633_.stream().map(Objects::toString).collect(Collectors.joining("\n\t", "\n\t", ""))
               )
            )
            .ifRight(p_364232_ -> map.put(p_468743_, p_364232_))
      );
      return map;
   }

   public static <T> void loadTagsFromNetwork(TagNetworkSerialization.NetworkPayload p_362324_, WritableRegistry<T> p_363626_) {
      p_362324_.resolve(p_363626_).tags.forEach(p_363626_::bindTag);
   }

   public static List<Registry.PendingTags<?>> loadTagsForExistingRegistries(ResourceManager p_362119_, RegistryAccess p_363563_) {
      return p_363563_.registries()
         .map(p_359642_ -> loadPendingTags(p_362119_, p_359642_.value()))
         .flatMap(Optional::stream)
         .collect(Collectors.toUnmodifiableList());
   }

   public static <T> void loadTagsForRegistry(ResourceManager p_360949_, WritableRegistry<T> p_364371_) {
      ResourceKey<? extends Registry<T>> resourcekey = p_364371_.key();
      TagLoader<Holder<T>> tagloader = new TagLoader<>(TagLoader.ElementLookup.fromWritableRegistry(p_364371_), Registries.tagsDirPath(resourcekey));
      tagloader.build(tagloader.load(p_360949_))
         .forEach((p_466412_, p_466413_) -> p_364371_.bindTag(TagKey.create(resourcekey, p_466412_), (List<Holder<T>>)p_466413_));
   }

   private static <T> Map<TagKey<T>, List<Holder<T>>> wrapTags(ResourceKey<? extends Registry<T>> p_365157_, Map<Identifier, List<Holder<T>>> p_364335_) {
      return p_364335_.entrySet().stream().collect(Collectors.toUnmodifiableMap(p_466409_ -> TagKey.create(p_365157_, p_466409_.getKey()), Entry::getValue));
   }

   private static <T> Optional<Registry.PendingTags<T>> loadPendingTags(ResourceManager p_363343_, Registry<T> p_364997_) {
      ResourceKey<? extends Registry<T>> resourcekey = p_364997_.key();
      TagLoader<Holder<T>> tagloader = new TagLoader<>(
         (TagLoader.ElementLookup<Holder<T>>)TagLoader.ElementLookup.fromFrozenRegistry(p_364997_), Registries.tagsDirPath(resourcekey)
      );
      TagLoader.LoadResult<T> loadresult = new TagLoader.LoadResult<>(resourcekey, wrapTags(p_364997_.key(), tagloader.build(tagloader.load(p_363343_))));
      return loadresult.tags().isEmpty() ? Optional.empty() : Optional.of(p_364997_.prepareTagReload(loadresult));
   }

   public static List<HolderLookup.RegistryLookup<?>> buildUpdatedLookups(RegistryAccess.Frozen p_363335_, List<Registry.PendingTags<?>> p_364879_) {
      List<HolderLookup.RegistryLookup<?>> list = new ArrayList<>();
      p_363335_.registries().forEach(p_367916_ -> {
         Registry.PendingTags<?> pendingtags = findTagsForRegistry(p_364879_, p_367916_.key());
         list.add((HolderLookup.RegistryLookup<?>)(pendingtags != null ? pendingtags.lookup() : p_367916_.value()));
      });
      return list;
   }

   @Nullable
   private static Registry.PendingTags<?> findTagsForRegistry(List<Registry.PendingTags<?>> p_360320_, ResourceKey<? extends Registry<?>> p_360369_) {
      for (Registry.PendingTags<?> pendingtags : p_360320_) {
         if (pendingtags.key() == p_360369_) {
            return pendingtags;
         }
      }

      return null;
   }

   public interface ElementLookup<T> {
      Optional<? extends T> get(Identifier var1, boolean var2);

      static <T> TagLoader.ElementLookup<? extends Holder<T>> fromFrozenRegistry(Registry<T> p_379788_) {
         return (p_466419_, p_466420_) -> p_379788_.get(p_466419_);
      }

      static <T> TagLoader.ElementLookup<Holder<T>> fromWritableRegistry(WritableRegistry<T> p_379984_) {
         HolderGetter<T> holdergetter = p_379984_.createRegistrationLookup();
         return (p_466416_, p_466417_) -> ((HolderGetter<T>)(p_466417_ ? holdergetter : p_379984_)).get(ResourceKey.create(p_379984_.key(), p_466416_));
      }
   }

   public record EntryWithSource(TagEntry entry, String source, boolean remove) {
      public EntryWithSource(TagEntry entry, String source) {
         this(entry, source, false);
      }

      @Override
      public String toString() {
         return this.entry + " (from " + this.source + ")";
      }
   }

   public record LoadResult<T>(ResourceKey<? extends Registry<T>> key, Map<TagKey<T>, List<Holder<T>>> tags) {
   }

   record SortingEntry(List<TagLoader.EntryWithSource> entries) implements DependencySorter.Entry<Identifier> {
      @Override
      public void visitRequiredDependencies(Consumer<Identifier> p_285529_) {
         this.entries.forEach(p_285236_ -> p_285236_.entry.visitRequiredDependencies(p_285529_));
      }

      @Override
      public void visitOptionalDependencies(Consumer<Identifier> p_285469_) {
         this.entries.forEach(p_284943_ -> p_284943_.entry.visitOptionalDependencies(p_285469_));
      }
   }
}
