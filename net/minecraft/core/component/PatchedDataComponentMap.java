package net.minecraft.core.component;

import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

public final class PatchedDataComponentMap implements DataComponentMap {
   private final DataComponentMap prototype;
   private Reference2ObjectMap<DataComponentType<?>, Optional<?>> patch;
   private boolean copyOnWrite;

   public PatchedDataComponentMap(DataComponentMap prototype) {
      this(prototype, Reference2ObjectMaps.emptyMap(), true);
   }

   private PatchedDataComponentMap(DataComponentMap prototype, Reference2ObjectMap<DataComponentType<?>, Optional<?>> patch, boolean copyOnWrite) {
      this.prototype = prototype;
      this.patch = patch;
      this.copyOnWrite = copyOnWrite;
   }

   public static PatchedDataComponentMap fromPatch(DataComponentMap prototype, DataComponentPatch patch) {
      if (isPatchSanitized(prototype, patch.map)) {
         return new PatchedDataComponentMap(prototype, patch.map, true);
      } else {
         PatchedDataComponentMap patchedDataComponentMap = new PatchedDataComponentMap(prototype);
         patchedDataComponentMap.applyPatch(patch);
         return patchedDataComponentMap;
      }
   }

   private static boolean isPatchSanitized(DataComponentMap prototype, Reference2ObjectMap<DataComponentType<?>, Optional<?>> map) {
      ObjectIterator var2 = Reference2ObjectMaps.fastIterable(map).iterator();

      while (var2.hasNext()) {
         Entry<DataComponentType<?>, Optional<?>> entry = (Entry<DataComponentType<?>, Optional<?>>)var2.next();
         Object object = prototype.get(entry.getKey());
         Optional<?> optional = entry.getValue();
         if (optional.isPresent() && optional.get().equals(object)) {
            return false;
         }

         if (optional.isEmpty() && object == null) {
            return false;
         }
      }

      return true;
   }

   @Nullable
   @Override
   public <T> T get(DataComponentType<? extends T> component) {
      Optional<? extends T> optional = (Optional<? extends T>)this.patch.get(component);
      return (T)(optional != null ? optional.orElse(null) : this.prototype.get(component));
   }

   public boolean hasNonDefault(DataComponentType<?> component) {
      return this.patch.containsKey(component);
   }

   @Nullable
   public <T> T set(DataComponentType<T> component, @Nullable T value) {
      this.ensureMapOwnership();
      T object = this.prototype.get(component);
      Optional<T> optional;
      if (Objects.equals(value, object)) {
         optional = (Optional<T>)this.patch.remove(component);
      } else {
         optional = (Optional<T>)this.patch.put(component, Optional.ofNullable(value));
      }

      return optional != null ? optional.orElse(object) : object;
   }

   @Nullable
   public <T> T set(TypedDataComponent<T> component) {
      return this.set(component.type(), component.value());
   }

   @Nullable
   public <T> T remove(DataComponentType<? extends T> component) {
      this.ensureMapOwnership();
      T object = this.prototype.get(component);
      Optional<? extends T> optional;
      if (object != null) {
         optional = (Optional<? extends T>)this.patch.put(component, Optional.empty());
      } else {
         optional = (Optional<? extends T>)this.patch.remove(component);
      }

      return (T)(optional != null ? optional.orElse(null) : object);
   }

   public void applyPatch(DataComponentPatch patch) {
      this.ensureMapOwnership();
      ObjectIterator var2 = Reference2ObjectMaps.fastIterable(patch.map).iterator();

      while (var2.hasNext()) {
         Entry<DataComponentType<?>, Optional<?>> entry = (Entry<DataComponentType<?>, Optional<?>>)var2.next();
         this.applyPatch(entry.getKey(), entry.getValue());
      }
   }

   private void applyPatch(DataComponentType<?> component, Optional<?> value) {
      Object object = this.prototype.get(component);
      if (value.isPresent()) {
         if (value.get().equals(object)) {
            this.patch.remove(component);
         } else {
            this.patch.put(component, value);
         }
      } else if (object != null) {
         this.patch.put(component, Optional.empty());
      } else {
         this.patch.remove(component);
      }
   }

   public void restorePatch(DataComponentPatch patch) {
      this.ensureMapOwnership();
      this.patch.clear();
      this.patch.putAll(patch.map);
   }

   public void clearPatch() {
      this.ensureMapOwnership();
      this.patch.clear();
   }

   public void setAll(DataComponentMap map) {
      for (TypedDataComponent<?> typedDataComponent : map) {
         typedDataComponent.applyTo(this);
      }
   }

   private void ensureMapOwnership() {
      if (this.copyOnWrite) {
         this.patch = new Reference2ObjectArrayMap(this.patch);
         this.copyOnWrite = false;
      }
   }

   @Override
   public Set<DataComponentType<?>> keySet() {
      if (this.patch.isEmpty()) {
         return this.prototype.keySet();
      } else {
         Set<DataComponentType<?>> set = new ReferenceArraySet(this.prototype.keySet());
         ObjectIterator var2 = Reference2ObjectMaps.fastIterable(this.patch).iterator();

         while (var2.hasNext()) {
            it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>> entry = (it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>>)var2.next();
            Optional<?> optional = (Optional<?>)entry.getValue();
            if (optional.isPresent()) {
               set.add((DataComponentType<?>)entry.getKey());
            } else {
               set.remove(entry.getKey());
            }
         }

         return set;
      }
   }

   @Override
   public Iterator<TypedDataComponent<?>> iterator() {
      if (this.patch.isEmpty()) {
         return this.prototype.iterator();
      } else {
         List<TypedDataComponent<?>> list = new ArrayList<>(this.patch.size() + this.prototype.size());
         ObjectIterator var2 = Reference2ObjectMaps.fastIterable(this.patch).iterator();

         while (var2.hasNext()) {
            it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>> entry = (it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>>)var2.next();
            if (((Optional)entry.getValue()).isPresent()) {
               list.add(TypedDataComponent.createUnchecked((DataComponentType)entry.getKey(), ((Optional)entry.getValue()).get()));
            }
         }

         for (TypedDataComponent<?> typedDataComponent : this.prototype) {
            if (!this.patch.containsKey(typedDataComponent.type())) {
               list.add(typedDataComponent);
            }
         }

         return list.iterator();
      }
   }

   @Override
   public int size() {
      int i = this.prototype.size();
      ObjectIterator var2 = Reference2ObjectMaps.fastIterable(this.patch).iterator();

      while (var2.hasNext()) {
         it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>> entry = (it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>>)var2.next();
         boolean bl = ((Optional)entry.getValue()).isPresent();
         boolean bl2 = this.prototype.has((DataComponentType<?>)entry.getKey());
         if (bl != bl2) {
            i += bl ? 1 : -1;
         }
      }

      return i;
   }

   public DataComponentPatch asPatch() {
      if (this.patch.isEmpty()) {
         return DataComponentPatch.EMPTY;
      } else {
         this.copyOnWrite = true;
         return new DataComponentPatch(this.patch);
      }
   }

   public PatchedDataComponentMap copy() {
      this.copyOnWrite = true;
      return new PatchedDataComponentMap(this.prototype, this.patch, true);
   }

   public DataComponentMap toImmutableMap() {
      return (DataComponentMap)(this.patch.isEmpty() ? this.prototype : this.copy());
   }

   @Override
   public boolean equals(Object object) {
      return this == object
         ? true
         : object instanceof PatchedDataComponentMap patchedDataComponentMap
            && this.prototype.equals(patchedDataComponentMap.prototype)
            && this.patch.equals(patchedDataComponentMap.patch);
   }

   @Override
   public int hashCode() {
      return this.prototype.hashCode() + this.patch.hashCode() * 31;
   }

   @Override
   public String toString() {
      return "{" + this.stream().map(TypedDataComponent::toString).collect(Collectors.joining(", ")) + "}";
   }
}
