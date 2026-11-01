package net.minecraft.data.tags;

import com.google.common.collect.Maps;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagFile;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public abstract class TagsProvider<T> implements DataProvider {
   protected final PackOutput.PathProvider pathProvider;
   private final CompletableFuture<HolderLookup.Provider> lookupProvider;
   private final CompletableFuture<Void> contentsDone = new CompletableFuture<>();
   private final CompletableFuture<TagsProvider.TagLookup<T>> parentProvider;
   protected final ResourceKey<? extends Registry<T>> registryKey;
   protected final Map<Identifier, TagBuilder> builders = Maps.newLinkedHashMap();
   protected final String modId;

   @Deprecated
   protected TagsProvider(PackOutput p_256596_, ResourceKey<? extends Registry<T>> p_255886_, CompletableFuture<HolderLookup.Provider> p_256513_) {
      this(p_256596_, p_255886_, p_256513_, "vanilla");
   }

   protected TagsProvider(PackOutput p_256596_, ResourceKey<? extends Registry<T>> p_255886_, CompletableFuture<HolderLookup.Provider> p_256513_, String modId) {
      this(p_256596_, p_255886_, p_256513_, CompletableFuture.completedFuture(TagsProvider.TagLookup.empty()), modId);
   }

   @Deprecated
   protected TagsProvider(
      PackOutput p_275432_,
      ResourceKey<? extends Registry<T>> p_275476_,
      CompletableFuture<HolderLookup.Provider> p_275222_,
      CompletableFuture<TagsProvider.TagLookup<T>> p_275565_
   ) {
      this(p_275432_, p_275476_, p_275222_, p_275565_, "vanilla");
   }

   protected TagsProvider(
      PackOutput p_275432_,
      ResourceKey<? extends Registry<T>> p_275476_,
      CompletableFuture<HolderLookup.Provider> p_275222_,
      CompletableFuture<TagsProvider.TagLookup<T>> p_275565_,
      String modId
   ) {
      this.pathProvider = p_275432_.createRegistryTagsPathProvider(p_275476_);
      this.registryKey = p_275476_;
      this.parentProvider = p_275565_;
      this.lookupProvider = p_275222_;
      this.modId = modId;
   }

   @Nullable
   protected Path getPath(Identifier id) {
      return this.pathProvider.json(id);
   }

   @Override
   public String getName() {
      return "Tags for " + this.registryKey.identifier() + " mod id " + this.modId;
   }

   protected abstract void addTags(HolderLookup.Provider var1);

   @Override
   public CompletableFuture<?> run(CachedOutput p_253684_) {
      record CombinedData<T>(HolderLookup.Provider contents, TagsProvider.TagLookup<T> parent) {
      }

      return this.createContentsProvider()
         .thenApply(p_275895_ -> {
            this.contentsDone.complete(null);
            return (HolderLookup.Provider)p_275895_;
         })
         .thenCombineAsync(
            this.parentProvider, (p_274778_, p_274779_) -> new CombinedData<>(p_274778_, (TagsProvider.TagLookup<T>)p_274779_), Util.backgroundExecutor()
         )
         .thenCompose(
            p_323140_ -> {
               HolderLookup.RegistryLookup<T> registrylookup = p_323140_.contents.lookupOrThrow(this.registryKey);
               Predicate<Identifier> predicate = p_466080_ -> registrylookup.get(ResourceKey.create(this.registryKey, p_466080_)).isPresent();
               Predicate<Identifier> predicate1 = p_466088_ -> this.builders.containsKey(p_466088_)
                  || p_323140_.parent.contains(TagKey.create(this.registryKey, p_466088_));
               return CompletableFuture.allOf(
                  this.builders
                     .entrySet()
                     .stream()
                     .map(
                        p_466086_ -> {
                           Identifier identifier = p_466086_.getKey();
                           TagBuilder tagbuilder = p_466086_.getValue();
                           List<TagEntry> list = tagbuilder.build();
                           List<TagEntry> list1 = Stream.concat(list.stream(), tagbuilder.getRemoveEntries())
                              .filter(p_274771_ -> p_274771_.getId().getNamespace().equals(this.modId) && !p_274771_.verifyIfPresent(predicate, predicate1))
                              .toList();
                           if (!list1.isEmpty()) {
                              throw new IllegalArgumentException(
                                 String.format(
                                    Locale.ROOT,
                                    "Couldn't define tag %s as it is missing following references: %s",
                                    identifier,
                                    list1.stream().map(Objects::toString).collect(Collectors.joining(","))
                                 )
                              );
                           } else {
                              Path path = this.getPath(identifier);
                              if (path == null) {
                                 return CompletableFuture.completedFuture(null);
                              } else {
                                 List<TagEntry> removed = tagbuilder.getRemoveEntries().toList();
                                 return DataProvider.saveStable(
                                    p_253684_, p_323140_.contents, TagFile.CODEC, new TagFile(list, tagbuilder.isReplace(), removed), path
                                 );
                              }
                           }
                        }
                     )
                     .toArray(CompletableFuture[]::new)
               );
            }
         );
   }

   protected TagBuilder getOrCreateRawBuilder(TagKey<T> p_236452_) {
      return this.builders.computeIfAbsent(p_236452_.location(), p_468482_ -> TagBuilder.create());
   }

   public CompletableFuture<TagsProvider.TagLookup<T>> contentsGetter() {
      return this.contentsDone.thenApply(p_276016_ -> p_466081_ -> Optional.ofNullable(this.builders.get(p_466081_.location())));
   }

   protected CompletableFuture<HolderLookup.Provider> createContentsProvider() {
      return this.lookupProvider.thenApply(p_274768_ -> {
         this.builders.clear();
         this.addTags(p_274768_);
         return (HolderLookup.Provider)p_274768_;
      });
   }

   @FunctionalInterface
   public interface TagLookup<T> extends Function<TagKey<T>, Optional<TagBuilder>> {
      static <T> TagsProvider.TagLookup<T> empty() {
         return p_275247_ -> Optional.empty();
      }

      default boolean contains(TagKey<T> p_275413_) {
         return this.apply(p_275413_).isPresent();
      }
   }
}
