package net.minecraft.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class ServerFunctionLibrary implements PreparableReloadListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final ResourceKey<Registry<CommandFunction<CommandSourceStack>>> TYPE_KEY = ResourceKey.createRegistryKey(
      ResourceLocation.withDefaultNamespace("function")
   );
   private static final FileToIdConverter LISTER = new FileToIdConverter(Registries.elementsDirPath(TYPE_KEY), ".mcfunction");
   private volatile Map<ResourceLocation, CommandFunction<CommandSourceStack>> functions = ImmutableMap.of();
   private final TagLoader<CommandFunction<CommandSourceStack>> tagsLoader = new TagLoader<>(this::getFunction, Registries.tagsDirPath(TYPE_KEY));
   private volatile Map<ResourceLocation, Collection<CommandFunction<CommandSourceStack>>> tags = Map.of();
   private final int functionCompilationLevel;
   private final CommandDispatcher<CommandSourceStack> dispatcher;

   public Optional<CommandFunction<CommandSourceStack>> getFunction(ResourceLocation p_136090_) {
      return Optional.ofNullable(this.functions.get(p_136090_));
   }

   public Map<ResourceLocation, CommandFunction<CommandSourceStack>> getFunctions() {
      return this.functions;
   }

   public Collection<CommandFunction<CommandSourceStack>> getTag(ResourceLocation p_214328_) {
      return this.tags.getOrDefault(p_214328_, List.of());
   }

   public Iterable<ResourceLocation> getAvailableTags() {
      return this.tags.keySet();
   }

   public ServerFunctionLibrary(int p_136053_, CommandDispatcher<CommandSourceStack> p_136054_) {
      this.functionCompilationLevel = p_136053_;
      this.dispatcher = p_136054_;
   }

   @Override
   public CompletableFuture<Void> reload(
      PreparableReloadListener.PreparationBarrier p_136057_,
      ResourceManager p_136058_,
      ProfilerFiller p_136059_,
      ProfilerFiller p_136060_,
      Executor p_136061_,
      Executor p_136062_
   ) {
      CompletableFuture<Map<ResourceLocation, List<TagLoader.EntryWithSource>>> $$6 = CompletableFuture.supplyAsync(
         () -> this.tagsLoader.load(p_136058_), p_136061_
      );
      CompletableFuture<Map<ResourceLocation, CompletableFuture<CommandFunction<CommandSourceStack>>>> $$7 = CompletableFuture.<Map<ResourceLocation, Resource>>supplyAsync(
            () -> LISTER.listMatchingResources(p_136058_), p_136061_
         )
         .thenCompose(
            p_248095_ -> {
               Map<ResourceLocation, CompletableFuture<CommandFunction<CommandSourceStack>>> $$2 = Maps.newHashMap();
               CommandSourceStack $$3 = new CommandSourceStack(
                  CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, null, this.functionCompilationLevel, "", CommonComponents.EMPTY, null, null
               );

               for (Entry<ResourceLocation, Resource> $$4 : p_248095_.entrySet()) {
                  ResourceLocation $$5 = $$4.getKey();
                  ResourceLocation $$6x = LISTER.fileToId($$5);
                  $$2.put($$6x, CompletableFuture.supplyAsync(() -> {
                     List<String> $$3x = readLines($$4.getValue());
                     return CommandFunction.fromLines($$6x, this.dispatcher, $$3, $$3x);
                  }, p_136061_));
               }

               CompletableFuture<?>[] $$7x = $$2.values().toArray(new CompletableFuture[0]);
               return CompletableFuture.allOf($$7x).handle((p_179949_, p_179950_) -> $$2);
            }
         );
      return $$6.thenCombine($$7, Pair::of)
         .thenCompose(p_136057_::wait)
         .thenAcceptAsync(
            p_179944_ -> {
               Map<ResourceLocation, CompletableFuture<CommandFunction<CommandSourceStack>>> $$1 = (Map<ResourceLocation, CompletableFuture<CommandFunction<CommandSourceStack>>>)p_179944_.getSecond();
               Builder<ResourceLocation, CommandFunction<CommandSourceStack>> $$2 = ImmutableMap.builder();
               $$1.forEach((p_179941_, p_179942_) -> p_179942_.handle((p_306152_, p_179955_) -> {
                  if (p_179955_ != null) {
                     LOGGER.error("Failed to load function {}", p_179941_, p_179955_);
                  } else {
                     $$2.put(p_179941_, p_306152_);
                  }

                  return null;
               }).join());
               this.functions = $$2.build();
               this.tags = this.tagsLoader.build((Map<ResourceLocation, List<TagLoader.EntryWithSource>>)p_179944_.getFirst());
            },
            p_136062_
         );
   }

   private static List<String> readLines(Resource p_214317_) {
      try {
         List var2;
         try (BufferedReader $$1 = p_214317_.openAsReader()) {
            var2 = $$1.lines().toList();
         }

         return var2;
      } catch (IOException var6) {
         throw new CompletionException(var6);
      }
   }
}
