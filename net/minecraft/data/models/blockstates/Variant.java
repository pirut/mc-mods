package net.minecraft.data.models.blockstates;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Variant implements Supplier<JsonElement> {
   private final Map<VariantProperty<?>, VariantProperty<?>.Value> values = Maps.newLinkedHashMap();

   public <T> Variant with(VariantProperty<T> p_125512_, T p_125513_) {
      VariantProperty<?>.Value $$2 = this.values.put(p_125512_, p_125512_.withValue(p_125513_));
      if ($$2 != null) {
         throw new IllegalStateException("Replacing value of " + $$2 + " with " + p_125513_);
      } else {
         return this;
      }
   }

   public static Variant variant() {
      return new Variant();
   }

   public static Variant merge(Variant p_125509_, Variant p_125510_) {
      Variant $$2 = new Variant();
      $$2.values.putAll(p_125509_.values);
      $$2.values.putAll(p_125510_.values);
      return $$2;
   }

   public JsonElement get() {
      JsonObject $$0 = new JsonObject();
      this.values.values().forEach(p_125507_ -> p_125507_.addToVariant($$0));
      return $$0;
   }

   public static JsonElement convertList(List<Variant> p_125515_) {
      if (p_125515_.size() == 1) {
         return p_125515_.get(0).get();
      } else {
         JsonArray $$1 = new JsonArray();
         p_125515_.forEach(p_125504_ -> $$1.add(p_125504_.get()));
         return $$1;
      }
   }
}
