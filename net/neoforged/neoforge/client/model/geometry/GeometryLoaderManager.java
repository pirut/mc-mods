package net.neoforged.neoforge.client.model.geometry;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

public final class GeometryLoaderManager {
   private static ImmutableMap<ResourceLocation, IGeometryLoader<?>> LOADERS;
   private static String LOADER_LIST;

   @Nullable
   public static IGeometryLoader<?> get(ResourceLocation name) {
      return (IGeometryLoader<?>)LOADERS.get(name);
   }

   public static String getLoaderList() {
      return LOADER_LIST;
   }

   @Internal
   public static void init() {
      HashMap<ResourceLocation, IGeometryLoader<?>> loaders = new HashMap<>();
      ModelEvent.RegisterGeometryLoaders event = new ModelEvent.RegisterGeometryLoaders(loaders);
      ModLoader.postEventWrapContainerInModOrder(event);
      LOADERS = ImmutableMap.copyOf(loaders);
      LOADER_LIST = loaders.keySet().stream().map(ResourceLocation::toString).collect(Collectors.joining(", "));
   }

   private GeometryLoaderManager() {
   }
}
