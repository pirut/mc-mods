package net.minecraft.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.resources.ResourceKey;

public class LayeredRegistryAccess<T> {
   private final List<T> keys;
   private final List<RegistryAccess.Frozen> values;
   private final RegistryAccess.Frozen composite;

   public LayeredRegistryAccess(List<T> p_251225_) {
      this(p_251225_, Util.make(() -> {
         RegistryAccess.Frozen[] $$1 = new RegistryAccess.Frozen[p_251225_.size()];
         Arrays.fill($$1, RegistryAccess.EMPTY);
         return Arrays.asList($$1);
      }));
   }

   private LayeredRegistryAccess(List<T> p_250473_, List<RegistryAccess.Frozen> p_249320_) {
      this.keys = List.copyOf(p_250473_);
      this.values = List.copyOf(p_249320_);
      this.composite = new RegistryAccess.ImmutableRegistryAccess(collectRegistries(p_249320_.stream())).freeze();
   }

   private int getLayerIndexOrThrow(T p_250144_) {
      int $$1 = this.keys.indexOf(p_250144_);
      if ($$1 == -1) {
         throw new IllegalStateException("Can't find " + p_250144_ + " inside " + this.keys);
      } else {
         return $$1;
      }
   }

   public RegistryAccess.Frozen getLayer(T p_250826_) {
      int $$1 = this.getLayerIndexOrThrow(p_250826_);
      return this.values.get($$1);
   }

   public RegistryAccess.Frozen getAccessForLoading(T p_251335_) {
      int $$1 = this.getLayerIndexOrThrow(p_251335_);
      return this.getCompositeAccessForLayers(0, $$1);
   }

   public RegistryAccess.Frozen getAccessFrom(T p_250766_) {
      int $$1 = this.getLayerIndexOrThrow(p_250766_);
      return this.getCompositeAccessForLayers($$1, this.values.size());
   }

   private RegistryAccess.Frozen getCompositeAccessForLayers(int p_251526_, int p_251999_) {
      return new RegistryAccess.ImmutableRegistryAccess(collectRegistries(this.values.subList(p_251526_, p_251999_).stream())).freeze();
   }

   public LayeredRegistryAccess<T> replaceFrom(T p_252104_, RegistryAccess.Frozen... p_250492_) {
      return this.replaceFrom(p_252104_, Arrays.asList(p_250492_));
   }

   public LayeredRegistryAccess<T> replaceFrom(T p_249539_, List<RegistryAccess.Frozen> p_250124_) {
      int $$2 = this.getLayerIndexOrThrow(p_249539_);
      if (p_250124_.size() > this.values.size() - $$2) {
         throw new IllegalStateException("Too many values to replace");
      } else {
         List<RegistryAccess.Frozen> $$3 = new ArrayList<>();

         for (int $$4 = 0; $$4 < $$2; $$4++) {
            $$3.add(this.values.get($$4));
         }

         $$3.addAll(p_250124_);

         while ($$3.size() < this.values.size()) {
            $$3.add(RegistryAccess.EMPTY);
         }

         return new LayeredRegistryAccess<>(this.keys, $$3);
      }
   }

   public RegistryAccess.Frozen compositeAccess() {
      return this.composite;
   }

   private static Map<ResourceKey<? extends Registry<?>>, Registry<?>> collectRegistries(Stream<? extends RegistryAccess> p_248595_) {
      Map<ResourceKey<? extends Registry<?>>, Registry<?>> $$1 = new HashMap<>();
      p_248595_.forEach(p_252003_ -> p_252003_.registries().forEach(p_339330_ -> {
         if ($$1.put(p_339330_.key(), p_339330_.value()) != null) {
            throw new IllegalStateException("Duplicated registry " + p_339330_.key());
         }
      }));
      return $$1;
   }
}
