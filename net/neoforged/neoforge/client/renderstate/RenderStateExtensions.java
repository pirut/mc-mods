package net.neoforged.neoforge.client.renderstate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.ApiStatus.Internal;

public final class RenderStateExtensions {
   private static final Map<Class<?>, Collection<BiConsumer<?, ?>>> ENTITY = new Reference2ObjectArrayMap();
   private static final Map<Class<?>, Collection<BiConsumer<?, ?>>> ENTITY_CACHE = new Reference2ObjectOpenHashMap();
   private static final List<BiConsumer<MapItemSavedData, MapRenderState>> MAP = new ObjectArrayList();
   private static final Map<ResourceKey<MapDecorationType>, Collection<MapDecorationRenderStateModifier>> MAP_DECORATION = new Reference2ObjectArrayMap();

   private RenderStateExtensions() {
   }

   @Internal
   public static <E extends Entity, S extends EntityRenderState> void onUpdateEntityRenderState(EntityRenderer<E, S> renderer, E entity, S renderState) {
      renderState.resetRenderData();

      for (BiConsumer<E, S> modifier : ENTITY_CACHE.computeIfAbsent(renderer.getClass(), aClass -> {
         Builder<BiConsumer<?, ?>> builder = ImmutableList.builder();

         for (Entry<Class<?>, Collection<BiConsumer<?, ?>>> entry : ENTITY.entrySet()) {
            if (entry.getKey().isAssignableFrom((Class<?>)aClass)) {
               builder.addAll(entry.getValue());
            }
         }

         return builder.build();
      })) {
         modifier.accept(entity, renderState);
      }
   }

   @Internal
   public static void onUpdateMapRenderState(MapItemSavedData mapItemSavedData, MapRenderState renderState) {
      renderState.resetRenderData();

      for (BiConsumer<MapItemSavedData, MapRenderState> modifier : MAP) {
         modifier.accept(mapItemSavedData, renderState);
      }
   }

   @Internal
   public static MapRenderState.MapDecorationRenderState onUpdateMapDecorationRenderState(
      Holder<MapDecorationType> mapDecorationTypeHolder,
      MapItemSavedData mapItemSavedData,
      MapRenderState mapRenderState,
      MapRenderState.MapDecorationRenderState mapDecorationRenderState
   ) {
      mapDecorationRenderState.resetRenderData();

      for (MapDecorationRenderStateModifier modifier : MAP_DECORATION.getOrDefault(mapDecorationTypeHolder.getKey(), List.of())) {
         modifier.accept(mapItemSavedData, mapRenderState, mapDecorationRenderState);
      }

      return mapDecorationRenderState;
   }

   static void registerEntity(Class<?> baseRenderer, BiConsumer<?, ?> modifier) {
      ENTITY.computeIfAbsent(baseRenderer, aClass -> new ObjectArrayList()).add(modifier);
   }

   static void registerMap(BiConsumer<MapItemSavedData, MapRenderState> modifier) {
      MAP.add(modifier);
   }

   static void registerMapDecoration(ResourceKey<MapDecorationType> mapDecorationTypeKey, MapDecorationRenderStateModifier modifier) {
      MAP_DECORATION.computeIfAbsent(mapDecorationTypeKey, aClass -> new ObjectArrayList()).add(modifier);
   }
}
