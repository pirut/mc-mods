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

   public SpatialAttributeInterpolator accumulate(double p_457741_, EnvironmentAttributeMap p_458075_) {
      this.weightsBySource.mergeDouble(p_458075_, p_457741_, Double::sum);
      return this;
   }

   public <Value> Value applyAttributeLayer(EnvironmentAttribute<Value> p_457685_, Value p_458195_) {
      if (this.weightsBySource.isEmpty()) {
         return p_458195_;
      } else if (this.weightsBySource.size() == 1) {
         EnvironmentAttributeMap environmentattributemap1 = (EnvironmentAttributeMap)this.weightsBySource.keySet().iterator().next();
         return environmentattributemap1.applyModifier(p_457685_, p_458195_);
      } else {
         LerpFunction<Value> lerpfunction = p_457685_.type().spatialLerp();
         Value value = null;
         double d0 = 0.0;
         ObjectIterator var7 = Reference2DoubleMaps.fastIterable(this.weightsBySource).iterator();

         while (var7.hasNext()) {
            Entry<EnvironmentAttributeMap> entry = (Entry<EnvironmentAttributeMap>)var7.next();
            EnvironmentAttributeMap environmentattributemap = (EnvironmentAttributeMap)entry.getKey();
            double d1 = entry.getDoubleValue();
            Value value1 = environmentattributemap.applyModifier(p_457685_, p_458195_);
            d0 += d1;
            if (value == null) {
               value = value1;
            } else {
               float f = (float)(d1 / d0);
               value = lerpfunction.apply(f, value, value1);
            }
         }

         return Objects.requireNonNull(value);
      }
   }
}
