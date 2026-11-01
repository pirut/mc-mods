package net.neoforged.neoforge.common.data;

import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableList.Builder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.WithConditions;
import org.slf4j.Logger;

public abstract class JsonCodecProvider<T> implements DataProvider {
   private static final Logger LOGGER = LogUtils.getLogger();
   protected final PackOutput.PathProvider pathProvider;
   protected final CompletableFuture<HolderLookup.Provider> lookupProvider;
   protected final String modid;
   protected final String directory;
   protected final Codec<T> codec;
   protected final Map<Identifier, WithConditions<T>> conditions = Maps.newHashMap();

   public JsonCodecProvider(
      PackOutput output, PackOutput.Target target, String directory, Codec<T> codec, CompletableFuture<HolderLookup.Provider> lookupProvider, String modId
   ) {
      this.pathProvider = output.createPathProvider(target, directory);
      this.modid = modId;
      this.directory = directory;
      this.codec = codec;
      this.lookupProvider = lookupProvider;
   }

   @Override
   public CompletableFuture<?> run(CachedOutput cache) {
      Builder<CompletableFuture<?>> futuresBuilder = new Builder();
      this.gather();
      return this.lookupProvider
         .thenCompose(
            provider -> {
               DynamicOps<JsonElement> dynamicOps = new ConditionalOps<>(RegistryOps.create(JsonOps.INSTANCE, provider), ICondition.IContext.EMPTY);
               this.conditions
                  .forEach(
                     (id, withConditions) -> {
                        Path path = this.pathProvider.json(id);
                        futuresBuilder.add(
                           CompletableFuture.<JsonElement>supplyAsync(
                                 () -> {
                                    Codec<Optional<WithConditions<T>>> withConditionsCodec = ConditionalOps.createConditionalCodecWithConditions(this.codec);
                                    return (JsonElement)withConditionsCodec.encodeStart(dynamicOps, Optional.of(withConditions))
                                       .getOrThrow(msg -> new RuntimeException("Failed to encode %s: %s".formatted(path, msg)));
                                 }
                              )
                              .thenComposeAsync(encoded -> DataProvider.saveStable(cache, encoded, path))
                        );
                     }
                  );
               return CompletableFuture.allOf((CompletableFuture<?>[])futuresBuilder.build().toArray(CompletableFuture[]::new));
            }
         );
   }

   protected abstract void gather();

   @Override
   public String getName() {
      return String.format("%s generator for %s", this.directory, this.modid);
   }

   public void unconditional(Identifier id, T value) {
      this.process(id, new WithConditions<>(List.of(), value));
   }

   public void conditionally(Identifier id, Consumer<WithConditions.Builder<T>> configurator) {
      WithConditions.Builder<T> builder = new WithConditions.Builder<>();
      configurator.accept(builder);
      WithConditions<T> withConditions = builder.build();
      this.process(id, withConditions);
   }

   private void process(Identifier id, WithConditions<T> withConditions) {
      this.conditions.put(id, withConditions);
   }
}
