package net.neoforged.neoforge.registries.datamaps;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.common.conditions.WithConditions;
import net.neoforged.neoforge.common.util.NeoForgeExtraCodecs;

public record DataMapFile<T, R>(
   boolean replace, Map<Either<TagKey<R>, ResourceKey<R>>, Optional<WithConditions<DataMapEntry<T>>>> values, List<DataMapEntry.Removal<T, R>> removals
) {
   public static <T, R> Codec<DataMapFile<T, R>> codec(ResourceKey<Registry<R>> registryKey, DataMapType<R, T> dataMap) {
      Codec<Either<TagKey<R>, ResourceKey<R>>> tagOrValue = ExtraCodecs.TAG_OR_ELEMENT_ID
         .xmap(
            l -> l.tag() ? Either.left(TagKey.create(registryKey, l.id())) : Either.right(ResourceKey.create(registryKey, l.id())),
            e -> (ExtraCodecs.TagOrElementLocation)e.map(
               t -> new ExtraCodecs.TagOrElementLocation(t.location(), true), r -> new ExtraCodecs.TagOrElementLocation(r.location(), false)
            )
         );
      Codec<List<DataMapEntry.Removal<T, R>>> removalsCodec;
      if (dataMap instanceof AdvancedDataMapType) {
         Codec<DataMapEntry.Removal<T, R>> removalCodec = DataMapEntry.Removal.codec(tagOrValue, dataMap);
         AdvancedDataMapType<R, T, DataMapValueRemover<R, T>> advanced = (AdvancedDataMapType<R, T, DataMapValueRemover<R, T>>)dataMap;
         removalsCodec = NeoForgeExtraCodecs.withAlternative(
            NeoForgeExtraCodecs.withAlternative(
               removalCodec.listOf(),
               NeoForgeExtraCodecs.decodeOnly(
                  tagOrValue.listOf().map(l -> l.stream().map(k -> new DataMapEntry.Removal((Either<TagKey<R>, ResourceKey<R>>)k, Optional.empty())).toList())
               )
            ),
            NeoForgeExtraCodecs.decodeOnly(
               ExtraCodecs.strictUnboundedMap(tagOrValue, advanced.remover())
                  .map(
                     map -> map.entrySet()
                        .stream()
                        .map(
                           entry -> new DataMapEntry.Removal(
                              (Either<TagKey<R>, ResourceKey<R>>)entry.getKey(), Optional.of((DataMapValueRemover<R, T>)entry.getValue())
                           )
                        )
                        .toList()
                  )
            )
         );
      } else {
         removalsCodec = tagOrValue.listOf()
            .xmap(
               l -> l.stream().map(k -> new DataMapEntry.Removal((Either<TagKey<R>, ResourceKey<R>>)k, Optional.empty())).toList(),
               rm -> rm.stream().map(DataMapEntry.Removal::key).toList()
            );
      }

      return RecordCodecBuilder.create(
         in -> in.group(
               Codec.BOOL.optionalFieldOf("replace", false).forGetter(DataMapFile::replace),
               ExtraCodecs.strictUnboundedMap(tagOrValue, ConditionalOps.createConditionalCodecWithConditions(DataMapEntry.codec(dataMap)))
                  .fieldOf("values")
                  .forGetter(DataMapFile::values),
               removalsCodec.optionalFieldOf("remove", List.of()).forGetter(DataMapFile::removals)
            )
            .apply(in, DataMapFile::new)
      );
   }
}
