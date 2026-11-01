package net.neoforged.neoforge.client.entity.animation.json;

import com.google.common.collect.MapMaker;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.Util;
import net.neoforged.neoforge.resource.ContextAwareReloadListener;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class AnimationLoader extends ContextAwareReloadListener implements PreparableReloadListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final AnimationLoader INSTANCE = new AnimationLoader();
   public static final PreparableReloadListener.StateKey<AnimationLoader.PendingAnimations> STATE_KEY = new PreparableReloadListener.StateKey<>();
   private static final FileToIdConverter LISTER = FileToIdConverter.json("neoforge/animations/entity");
   private final Map<Identifier, AnimationHolder> animations = new MapMaker().weakValues().concurrencyLevel(1).makeMap();
   private final List<AnimationHolder> strongHolderReferences = new ArrayList<>();

   private AnimationLoader() {
   }

   @Nullable
   public AnimationDefinition getAnimation(Identifier key) {
      AnimationHolder holder = this.animations.get(key);
      return holder != null ? holder.getOrNull() : null;
   }

   public AnimationHolder getAnimationHolder(Identifier key) {
      return this.animations.computeIfAbsent(key, AnimationHolder::new);
   }

   @Override
   public void prepareSharedState(PreparableReloadListener.SharedState sharedState) {
      sharedState.set(STATE_KEY, new AnimationLoader.PendingAnimations());
   }

   @Override
   public CompletableFuture<Void> reload(
      PreparableReloadListener.SharedState sharedState, Executor prepareExecutor, PreparableReloadListener.PreparationBarrier barrier, Executor applyExecutor
   ) {
      ResourceManager resourceManager = sharedState.resourceManager();
      AnimationLoader.PendingAnimations pending = sharedState.get(STATE_KEY);
      return CompletableFuture.<Map<Identifier, AnimationDefinition>>supplyAsync(() -> this.prepare(resourceManager), prepareExecutor)
         .whenComplete((map, error) -> {
            if (map != null) {
               pending.future.complete((Map<Identifier, AnimationDefinition>)map);
            } else {
               pending.future.completeExceptionally(error);
            }
         })
         .thenCompose(barrier::wait)
         .thenAcceptAsync(this::apply, applyExecutor);
   }

   private Map<Identifier, AnimationDefinition> prepare(ResourceManager resourceManager) {
      Map<Identifier, AnimationDefinition> map = new HashMap<>();
      SimpleJsonResourceReloadListener.scanDirectory(resourceManager, LISTER, this.makeConditionalOps(JsonOps.INSTANCE), AnimationParser.CODEC, map);
      return map;
   }

   private void apply(Map<Identifier, AnimationDefinition> animationJsons) {
      this.animations.values().forEach(AnimationHolder::unbind);
      this.strongHolderReferences.clear();
      int loaded = 0;

      for (Entry<Identifier, AnimationDefinition> entry : animationJsons.entrySet()) {
         AnimationHolder holder = this.getAnimationHolder(entry.getKey());
         holder.bind(entry.getValue());
         this.strongHolderReferences.add(holder);
         loaded++;
      }

      LOGGER.info("Loaded {} entity animations", loaded);
   }

   public static final class PendingAnimations {
      public static final AnimationLoader.PendingAnimations EMPTY = Util.make(
         new AnimationLoader.PendingAnimations(), pending -> pending.future.complete(Map.of())
      );
      private final CompletableFuture<Map<Identifier, AnimationDefinition>> future = new CompletableFuture<>();

      private PendingAnimations() {
      }

      @Nullable
      public AnimationDefinition get(Identifier id) {
         return this.future.join().get(id);
      }
   }
}
