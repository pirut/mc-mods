package net.neoforged.neoforge.client.entity.animation.json;

import com.google.common.collect.MapMaker;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public final class AnimationLoader extends SimpleJsonResourceReloadListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final AnimationLoader INSTANCE = new AnimationLoader();
   private final Map<ResourceLocation, AnimationHolder> animations = new MapMaker().weakValues().concurrencyLevel(1).makeMap();
   private final List<AnimationHolder> strongHolderReferences = new ArrayList<>();

   private AnimationLoader() {
      super(new Gson(), "neoforge/animations/entity");
   }

   @Nullable
   public AnimationDefinition getAnimation(ResourceLocation key) {
      AnimationHolder holder = this.animations.get(key);
      return holder != null ? holder.getOrNull() : null;
   }

   public AnimationHolder getAnimationHolder(ResourceLocation key) {
      return this.animations.computeIfAbsent(key, AnimationHolder::new);
   }

   protected void apply(Map<ResourceLocation, JsonElement> animationJsons, ResourceManager resourceManager, ProfilerFiller profiler) {
      this.animations.values().forEach(AnimationHolder::unbind);
      this.strongHolderReferences.clear();
      int loaded = 0;

      for (Entry<ResourceLocation, JsonElement> entry : animationJsons.entrySet()) {
         try {
            AnimationDefinition animation = (AnimationDefinition)AnimationParser.CODEC
               .parse(JsonOps.INSTANCE, entry.getValue())
               .getOrThrow(JsonParseException::new);
            AnimationHolder holder = this.getAnimationHolder(entry.getKey());
            holder.bind(animation);
            this.strongHolderReferences.add(holder);
            loaded++;
         } catch (Exception var9) {
            LOGGER.error("Failed to load animation {}", entry.getKey(), var9);
         }
      }

      LOGGER.info("Loaded {} entity animations", loaded);
   }
}
