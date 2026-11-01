package net.neoforged.neoforge.model.data;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

public final class ModelData {
   public static final ModelData EMPTY = new ModelData(new Reference2ReferenceArrayMap());
   private final Map<ModelProperty<?>, Object> properties;
   @Nullable
   private Set<ModelProperty<?>> propertySetView;

   private ModelData(Map<ModelProperty<?>, Object> properties) {
      this.properties = properties;
   }

   public Set<ModelProperty<?>> getProperties() {
      Set<ModelProperty<?>> view = this.propertySetView;
      if (view == null) {
         this.propertySetView = view = Collections.unmodifiableSet(this.properties.keySet());
      }

      return view;
   }

   public boolean has(ModelProperty<?> property) {
      return this.properties.containsKey(property);
   }

   @Nullable
   public <T> T get(ModelProperty<T> property) {
      return (T)this.properties.get(property);
   }

   public ModelData.Builder derive() {
      return new ModelData.Builder(this);
   }

   public static ModelData.Builder builder() {
      return new ModelData.Builder(null);
   }

   public static <T> ModelData of(ModelProperty<T> property, T value) {
      Preconditions.checkState(property.test(value), "The provided value is invalid for this property.");
      Reference2ReferenceArrayMap<ModelProperty<?>, Object> map = new Reference2ReferenceArrayMap(1);
      map.put(property, value);
      return new ModelData(map);
   }

   public static final class Builder {
      private static final int HASH_THRESHOLD = 4;
      private Map<ModelProperty<?>, Object> properties;

      private Builder(@Nullable ModelData parent) {
         if (parent != null) {
            this.properties = (Map<ModelProperty<?>, Object>)(parent.properties.size() >= 4
               ? new Reference2ReferenceOpenHashMap(parent.properties)
               : new Reference2ReferenceArrayMap(parent.properties));
         } else {
            this.properties = new Reference2ReferenceArrayMap(4);
         }
      }

      @Contract("_, _ -> this")
      public <T> ModelData.Builder with(ModelProperty<T> property, T value) {
         Preconditions.checkState(property.test(value), "The provided value is invalid for this property.");
         this.properties.put(property, value);
         if (this.properties.size() == 4 && this.properties instanceof Reference2ReferenceArrayMap) {
            this.properties = new Reference2ReferenceOpenHashMap(this.properties);
         }

         return this;
      }

      public ModelData build() {
         return this.properties.isEmpty() ? ModelData.EMPTY : new ModelData(this.properties);
      }
   }
}
