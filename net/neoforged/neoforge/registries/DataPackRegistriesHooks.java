package net.neoforged.neoforge.registries;

import java.lang.StackWalker.Option;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

@Internal
public final class DataPackRegistriesHooks {
   private static final List<RegistryDataLoader.RegistryData<?>> NETWORKABLE_REGISTRIES = new ArrayList<>();
   private static final List<RegistryDataLoader.RegistryData<?>> DATA_PACK_REGISTRIES = new ArrayList<>(RegistryDataLoader.WORLDGEN_REGISTRIES);
   private static final List<RegistryDataLoader.RegistryData<?>> DATA_PACK_REGISTRIES_VIEW = Collections.unmodifiableList(DATA_PACK_REGISTRIES);
   private static final Set<ResourceKey<? extends Registry<?>>> SYNCED_CUSTOM_REGISTRIES = new HashSet<>();
   private static final Set<ResourceKey<? extends Registry<?>>> SYNCED_CUSTOM_REGISTRIES_VIEW = Collections.unmodifiableSet(SYNCED_CUSTOM_REGISTRIES);

   private DataPackRegistriesHooks() {
   }

   public static List<RegistryDataLoader.RegistryData<?>> grabNetworkableRegistries(List<RegistryDataLoader.RegistryData<?>> list) {
      if (!StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE).getCallerClass().equals(RegistryDataLoader.class)) {
         throw new IllegalCallerException("Attempted to call DataPackRegistriesHooks#grabNetworkableRegistries!");
      } else {
         List<RegistryDataLoader.RegistryData<?>> builder = new ArrayList<>(list);
         builder.addAll(NETWORKABLE_REGISTRIES);
         NETWORKABLE_REGISTRIES.clear();
         NETWORKABLE_REGISTRIES.addAll(builder);
         return Collections.unmodifiableList(NETWORKABLE_REGISTRIES);
      }
   }

   static <T> void addRegistryCodec(DataPackRegistryEvent.DataPackRegistryData<T> data) {
      RegistryDataLoader.RegistryData<T> loaderData = data.loaderData();
      DATA_PACK_REGISTRIES.add(loaderData);
      if (data.networkCodec() != null) {
         SYNCED_CUSTOM_REGISTRIES.add(loaderData.key());
         NETWORKABLE_REGISTRIES.add(new RegistryDataLoader.RegistryData<>(loaderData.key(), data.networkCodec(), false));
      }
   }

   public static List<RegistryDataLoader.RegistryData<?>> getDataPackRegistries() {
      return DATA_PACK_REGISTRIES_VIEW;
   }

   public static Stream<RegistryDataLoader.RegistryData<?>> getDataPackRegistriesWithDimensions() {
      return Stream.concat(DATA_PACK_REGISTRIES_VIEW.stream(), RegistryDataLoader.DIMENSION_REGISTRIES.stream());
   }

   public static Set<ResourceKey<? extends Registry<?>>> getSyncedCustomRegistries() {
      return SYNCED_CUSTOM_REGISTRIES_VIEW;
   }

   @Internal
   @Nullable
   public static <T> RegistryDataLoader.RegistryData<T> getSyncedRegistry(ResourceKey<? extends Registry<T>> registry) {
      return (RegistryDataLoader.RegistryData<T>)NETWORKABLE_REGISTRIES.stream().filter(data -> data.key().equals(registry)).findFirst().orElse(null);
   }
}
