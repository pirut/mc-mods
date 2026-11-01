package net.neoforged.neoforge.client.model.standalone;

import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.util.thread.ParallelMapTransform;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class StandaloneModelLoader {
   private static final Logger LOGGER = LogUtils.getLogger();

   private StandaloneModelLoader() {
   }

   public static CompletableFuture<StandaloneModelLoader.LoadedModels> load(Executor executor) {
      return CompletableFuture.supplyAsync(() -> {
         Map<StandaloneModelKey<?>, UnbakedStandaloneModel<?>> models = new IdentityHashMap<>();
         ModLoader.postEvent(new ModelEvent.RegisterStandalone(models));
         return new StandaloneModelLoader.LoadedModels(models);
      }, executor);
   }

   public static CompletableFuture<StandaloneModelLoader.BakedModels> bake(
      StandaloneModelLoader.LoadedModels standaloneModels, ModelBaker baker, Executor executor
   ) {
      return ParallelMapTransform.schedule(standaloneModels.models, (key, model) -> {
         try {
            return model.bake(baker);
         } catch (Exception var4) {
            LOGGER.warn("Unable to bake standalone model: '{}': {}", key.getName(), var4);
            return null;
         }
      }, executor).thenApply(StandaloneModelLoader.BakedModels::new);
   }

   public record BakedModels(Map<StandaloneModelKey<?>, ?> models) {
      public static final StandaloneModelLoader.BakedModels EMPTY = new StandaloneModelLoader.BakedModels(Map.of());

      @Nullable
      public <T> T get(StandaloneModelKey<T> key) {
         return (T)this.models.get(key);
      }

      public StandaloneModelLoader.BakedModels unmodifiable() {
         return new StandaloneModelLoader.BakedModels(Collections.unmodifiableMap(this.models));
      }
   }

   public record LoadedModels(Map<StandaloneModelKey<?>, UnbakedStandaloneModel<?>> models) {
      public static final StandaloneModelLoader.LoadedModels EMPTY = new StandaloneModelLoader.LoadedModels(Map.of());
   }
}
