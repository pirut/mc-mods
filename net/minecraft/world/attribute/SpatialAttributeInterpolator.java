package net.minecraft.world.attribute;

import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2DoubleMaps;
import it.unimi.dsi.fastutil.objects.Reference2DoubleMap.Entry;
import java.util.Objects;

public class SpatialAttributeInterpolator {
   private final Reference2DoubleArrayMap<EnvironmentAttributeMap> weightsBySource = new Reference2DoubleArrayMap();

   public void clear() {
      this.weightsBySource.clear();
   }

   public SpatialAttributeInterpolator accumulate(double weight, EnvironmentAttributeMap map) {
      this.weightsBySource.mergeDouble(map, weight, Double::sum);
      return this;
   }

   public <Value> Value applyAttributeLayer(EnvironmentAttribute<Value> attribute, Value value) {
      if (this.weightsBySource.isEmpty()) {
         return value;
      } else if (this.weightsBySource.size() == 1) {
         EnvironmentAttributeMap environmentAttributeMap = (EnvironmentAttributeMap)this.weightsBySource.keySet().iterator().next();
         return environmentAttributeMap.applyModifier(attribute, value);
      } else {
         LerpFunction<Value> lerpFunction = attribute.type().spatialLerp();
         Value object = null;
         double d = 0.0;
         ObjectIterator var7 = Reference2DoubleMaps.fastIterable(this.weightsBySource).iterator();

         while (var7.hasNext()) {
            Entry<EnvironmentAttributeMap> entry = (Entry<EnvironmentAttributeMap>)var7.next();
            EnvironmentAttributeMap environmentAttributeMap2 = (EnvironmentAttributeMap)entry.getKey();
            double e = entry.getDoubleValue();
            Value object2 = environmentAttributeMap2.applyModifier(attribute, value);
            d += e;
            if (object == null) {
               object = object2;
            } else {
               float f = (float)(e / d);
               object = lerpFunction.apply(f, object, object2);
            }
         }

         return Objects.requireNonNull(object);
      }
   }
}
