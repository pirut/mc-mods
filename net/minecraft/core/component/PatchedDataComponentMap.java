package net.minecraft.core.component;

import com.google.common.math.IntMath;
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
import net.neoforged.neoforge.common.CommonHooks;
import org.jspecify.annotations.Nullable;

public final class PatchedDataComponentMap implements DataComponentMap {
   private final DataComponentMap prototype;
   private Reference2ObjectMap<DataComponentType<?>, Optional<?>> patch;
   private boolean copyOnWrite;

   public PatchedDataComponentMap(DataComponentMap p_332070_) {
      this(p_332070_, Reference2ObjectMaps.emptyMap(), true);
   }

   private PatchedDataComponentMap(DataComponentMap p_331644_, Reference2ObjectMap<DataComponentType<?>, Optional<?>> p_331707_, boolean p_331170_) {
      this.prototype = p_331644_;
      this.patch = p_331707_;
      this.copyOnWrite = p_331170_;
   }

   public static PatchedDataComponentMap fromPatch(DataComponentMap p_331807_, DataComponentPatch p_331890_) {
      if (isPatchSanitized(p_331807_, p_331890_.map)) {
         return new PatchedDataComponentMap(p_331807_, p_331890_.map, true);
      } else {
         PatchedDataComponentMap patcheddatacomponentmap = new PatchedDataComponentMap(p_331807_);
         patcheddatacomponentmap.applyPatch(p_331890_);
         return patcheddatacomponentmap;
      }
   }

   private static boolean isPatchSanitized(DataComponentMap p_331938_, Reference2ObjectMap<DataComponentType<?>, Optional<?>> p_330946_) {
      ObjectIterator var2 = Reference2ObjectMaps.fastIterable(p_330946_).iterator();

      while (var2.hasNext()) {
         Entry<DataComponentType<?>, Optional<?>> entry = (Entry<DataComponentType<?>, Optional<?>>)var2.next();
         Object object = p_331938_.get(entry.getKey());
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
   public <T> T get(DataComponentType<? extends T> p_331587_) {
      Optional<? extends T> optional = (Optional<? extends T>)this.patch.get(p_331587_);
      return (T)(optional != null ? optional.orElse(null) : this.prototype.get(p_331587_));
   }

   public boolean hasNonDefault(DataComponentType<?> p_389544_) {
      return this.patch.containsKey(p_389544_);
   }

   @Nullable
   public <T> T set(DataComponentType<T> p_330791_, @Nullable T p_330369_) {
      CommonHooks.validateComponent(p_330369_);
      this.ensureMapOwnership();
      T t = this.prototype.get(p_330791_);
      Optional<T> optional;
      if (Objects.equals(p_330369_, t)) {
         optional = (Optional<T>)this.patch.remove(p_330791_);
      } else {
         optional = (Optional<T>)this.patch.put(p_330791_, Optional.ofNullable(p_330369_));
      }

      return optional != null ? optional.orElse(t) : t;
   }

   @Nullable
   public <T> T set(TypedDataComponent<T> p_451093_) {
      return this.set(p_451093_.type(), p_451093_.value());
   }

   @Nullable
   public <T> T remove(DataComponentType<? extends T> p_330831_) {
      this.ensureMapOwnership();
      T t = this.prototype.get(p_330831_);
      Optional<? extends T> optional;
      if (t != null) {
         optional = (Optional<? extends T>)this.patch.put(p_330831_, Optional.empty());
      } else {
         optional = (Optional<? extends T>)this.patch.remove(p_330831_);
      }

      return (T)(optional != null ? optional.orElse(null) : t);
   }

   public void applyPatch(DataComponentPatch p_331964_) {
      this.ensureMapOwnership();
      ObjectIterator var2 = Reference2ObjectMaps.fastIterable(p_331964_.map).iterator();

      while (var2.hasNext()) {
         Entry<DataComponentType<?>, Optional<?>> entry = (Entry<DataComponentType<?>, Optional<?>>)var2.next();
         this.applyPatch(entry.getKey(), entry.getValue());
      }
   }

   private void applyPatch(DataComponentType<?> p_331724_, Optional<?> p_331550_) {
      Object object = this.prototype.get(p_331724_);
      if (p_331550_.isPresent()) {
         if (p_331550_.get().equals(object)) {
            this.patch.remove(p_331724_);
         } else {
            this.patch.put(p_331724_, p_331550_);
         }
      } else if (object != null) {
         this.patch.put(p_331724_, Optional.empty());
      } else {
         this.patch.remove(p_331724_);
      }
   }

   public void restorePatch(DataComponentPatch p_341355_) {
      this.ensureMapOwnership();
      this.patch.clear();
      this.patch.putAll(p_341355_.map);
   }

   public void clearPatch() {
      this.ensureMapOwnership();
      this.patch.clear();
   }

   public void setAll(DataComponentMap p_331652_) {
      for (TypedDataComponent<?> typeddatacomponent : p_331652_) {
         typeddatacomponent.applyTo(this);
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

         for (TypedDataComponent<?> typeddatacomponent : this.prototype) {
            if (!this.patch.containsKey(typeddatacomponent.type())) {
               list.add(typeddatacomponent);
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
         boolean flag = ((Optional)entry.getValue()).isPresent();
         boolean flag1 = this.prototype.has((DataComponentType<?>)entry.getKey());
         if (flag != flag1) {
            i += flag ? 1 : -1;
         }
      }

      return i;
   }

   public boolean isPatchEmpty() {
      return this.patch.isEmpty();
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
   public boolean equals(Object p_332106_) {
      return this == p_332106_
         ? true
         : p_332106_ instanceof PatchedDataComponentMap patcheddatacomponentmap
            && this.prototype.equals(patcheddatacomponentmap.prototype)
            && this.patch.equals(patcheddatacomponentmap.patch);
   }

   @Override
   public int hashCode() {
      return this.prototype.hashCode() + hashPatch(this.patch) * 31;
   }

   private static int hashPatch(Reference2ObjectMap<DataComponentType<?>, Optional<?>> patch) {
      int h = 0;
      int n = patch.size();
      ObjectIterator<it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>>> iterator = Reference2ObjectMaps.fastIterator(
         patch
      );

      while (n-- != 0) {
         it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>> entry = (it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>>)iterator.next();
         int exponent = System.identityHashCode(entry.getKey()) & 0xFF;
         int entryHash = IntMath.pow(31, exponent) * ((Optional)entry.getValue()).hashCode();
         h += entryHash;
      }

      return h;
   }

   @Override
   public String toString() {
      return "{" + this.stream().map(TypedDataComponent::toString).collect(Collectors.joining(", ")) + "}";
   }
}
