package net.minecraft.world.attribute;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.timeline.Timeline;
import org.jspecify.annotations.Nullable;

public class EnvironmentAttributeSystem implements EnvironmentAttributeReader {
   private final Map<EnvironmentAttribute<?>, EnvironmentAttributeSystem.ValueSampler<?>> attributeSamplers = new Reference2ObjectOpenHashMap();

   EnvironmentAttributeSystem(Map<EnvironmentAttribute<?>, List<EnvironmentAttributeLayer<?>>> attributes) {
      attributes.forEach(
         (environmentAttribute, list) -> this.attributeSamplers
            .put(
               (EnvironmentAttribute<?>)environmentAttribute,
               this.bakeLayerSampler((EnvironmentAttribute<?>)environmentAttribute, (List<? extends EnvironmentAttributeLayer<?>>)list)
            )
      );
   }

   private <Value> EnvironmentAttributeSystem.ValueSampler<Value> bakeLayerSampler(
      EnvironmentAttribute<Value> attribute, List<? extends EnvironmentAttributeLayer<?>> layers
   ) {
      List<EnvironmentAttributeLayer<Value>> list = new ArrayList<>((Collection<? extends EnvironmentAttributeLayer<Value>>)layers);
      Value object = attribute.defaultValue();

      while (!list.isEmpty()) {
         if (!(list.getFirst() instanceof EnvironmentAttributeLayer.Constant<Value> constant)) {
            break;
         }

         object = constant.applyConstant(object);
         list.removeFirst();
      }

      boolean bl = list.stream().anyMatch(environmentAttributeLayer -> environmentAttributeLayer instanceof EnvironmentAttributeLayer.Positional);
      return new EnvironmentAttributeSystem.ValueSampler<>(attribute, object, List.copyOf(list), bl);
   }

   public static EnvironmentAttributeSystem.Builder builder() {
      return new EnvironmentAttributeSystem.Builder();
   }

   static void addDefaultLayers(EnvironmentAttributeSystem.Builder builder, Level level) {
      RegistryAccess registryAccess = level.registryAccess();
      BiomeManager biomeManager = level.getBiomeManager();
      LongSupplier longSupplier = level::getDayTime;
      addDimensionLayer(builder, level.dimensionType());
      addBiomeLayer(builder, registryAccess.lookupOrThrow(Registries.BIOME), biomeManager);
      level.dimensionType().timelines().forEach(holder -> builder.addTimelineLayer((Holder<Timeline>)holder, longSupplier));
      if (level.canHaveWeather()) {
         WeatherAttributes.addBuiltinLayers(builder, WeatherAttributes.WeatherAccess.from(level));
      }
   }

   private static void addDimensionLayer(EnvironmentAttributeSystem.Builder builder, DimensionType dimension) {
      builder.addConstantLayer(dimension.attributes());
   }

   private static void addBiomeLayer(EnvironmentAttributeSystem.Builder builder, HolderLookup<Biome> biome, BiomeManager biomeManager) {
      Stream<EnvironmentAttribute<?>> stream = biome.listElements().flatMap(reference -> reference.value().getAttributes().keySet().stream()).distinct();
      stream.forEach(environmentAttribute -> addBiomeLayerForAttribute(builder, (EnvironmentAttribute<?>)environmentAttribute, biomeManager));
   }

   private static <Value> void addBiomeLayerForAttribute(
      EnvironmentAttributeSystem.Builder builder, EnvironmentAttribute<Value> attribute, BiomeManager biomeManager
   ) {
      builder.addPositionalLayer(attribute, (object, vec3, spatialAttributeInterpolator) -> {
         if (spatialAttributeInterpolator != null && attribute.isSpatiallyInterpolated()) {
            return spatialAttributeInterpolator.applyAttributeLayer(attribute, object);
         } else {
            Holder<Biome> holder = biomeManager.getNoiseBiomeAtPosition(vec3.x, vec3.y, vec3.z);
            return holder.value().getAttributes().applyModifier(attribute, object);
         }
      });
   }

   public void invalidateTickCache() {
      this.attributeSamplers.values().forEach(EnvironmentAttributeSystem.ValueSampler::invalidateTickCache);
   }

   @Nullable
   private <Value> EnvironmentAttributeSystem.ValueSampler<Value> getValueSampler(EnvironmentAttribute<Value> attribute) {
      return (EnvironmentAttributeSystem.ValueSampler<Value>)this.attributeSamplers.get(attribute);
   }

   @Override
   public <Value> Value getDimensionValue(EnvironmentAttribute<Value> attribute) {
      if (SharedConstants.IS_RUNNING_IN_IDE && attribute.isPositional()) {
         throw new IllegalStateException("Position must always be provided for positional attribute " + attribute);
      } else {
         EnvironmentAttributeSystem.ValueSampler<Value> valueSampler = this.getValueSampler(attribute);
         return valueSampler == null ? attribute.defaultValue() : valueSampler.getDimensionValue();
      }
   }

   @Override
   public <Value> Value getValue(EnvironmentAttribute<Value> attribute, Vec3 pos, @Nullable SpatialAttributeInterpolator interpolator) {
      EnvironmentAttributeSystem.ValueSampler<Value> valueSampler = this.getValueSampler(attribute);
      return valueSampler == null ? attribute.defaultValue() : valueSampler.getValue(pos, interpolator);
   }

   @VisibleForTesting
   <Value> Value getConstantBaseValue(EnvironmentAttribute<Value> attribute) {
      EnvironmentAttributeSystem.ValueSampler<Value> valueSampler = this.getValueSampler(attribute);
      return valueSampler != null ? valueSampler.baseValue : attribute.defaultValue();
   }

   @VisibleForTesting
   boolean isAffectedByPosition(EnvironmentAttribute<?> attribute) {
      EnvironmentAttributeSystem.ValueSampler<?> valueSampler = this.getValueSampler(attribute);
      return valueSampler != null && valueSampler.isAffectedByPosition;
   }

   public static class Builder {
      private final Map<EnvironmentAttribute<?>, List<EnvironmentAttributeLayer<?>>> layersByAttribute = new HashMap<>();

      Builder() {
      }

      public EnvironmentAttributeSystem.Builder addDefaultLayers(Level level) {
         EnvironmentAttributeSystem.addDefaultLayers(this, level);
         return this;
      }

      public EnvironmentAttributeSystem.Builder addConstantLayer(EnvironmentAttributeMap map) {
         for (EnvironmentAttribute<?> environmentAttribute : map.keySet()) {
            this.addConstantEntry(environmentAttribute, map);
         }

         return this;
      }

      private <Value> EnvironmentAttributeSystem.Builder addConstantEntry(EnvironmentAttribute<Value> attribute, EnvironmentAttributeMap map) {
         EnvironmentAttributeMap.Entry<Value, ?> entry = map.get(attribute);
         if (entry == null) {
            throw new IllegalArgumentException("Missing attribute " + attribute);
         } else {
            return this.addConstantLayer(attribute, entry::applyModifier);
         }
      }

      public <Value> EnvironmentAttributeSystem.Builder addConstantLayer(EnvironmentAttribute<Value> attribute, EnvironmentAttributeLayer.Constant<Value> layer) {
         return this.addLayer(attribute, layer);
      }

      public <Value> EnvironmentAttributeSystem.Builder addTimeBasedLayer(
         EnvironmentAttribute<Value> attribute, EnvironmentAttributeLayer.TimeBased<Value> layer
      ) {
         return this.addLayer(attribute, layer);
      }

      public <Value> EnvironmentAttributeSystem.Builder addPositionalLayer(
         EnvironmentAttribute<Value> attribute, EnvironmentAttributeLayer.Positional<Value> layer
      ) {
         return this.addLayer(attribute, layer);
      }

      private <Value> EnvironmentAttributeSystem.Builder addLayer(EnvironmentAttribute<Value> attribute, EnvironmentAttributeLayer<Value> layer) {
         this.layersByAttribute.computeIfAbsent(attribute, environmentAttribute -> new ArrayList<>()).add(layer);
         return this;
      }

      public EnvironmentAttributeSystem.Builder addTimelineLayer(Holder<Timeline> timeline, LongSupplier dayTimeGetter) {
         for (EnvironmentAttribute<?> environmentAttribute : timeline.value().attributes()) {
            this.addTimelineLayerForAttribute(timeline, environmentAttribute, dayTimeGetter);
         }

         return this;
      }

      private <Value> void addTimelineLayerForAttribute(Holder<Timeline> timeline, EnvironmentAttribute<Value> attribute, LongSupplier dayTimeGetter) {
         this.addTimeBasedLayer(attribute, timeline.value().createTrackSampler(attribute, dayTimeGetter));
      }

      public EnvironmentAttributeSystem build() {
         return new EnvironmentAttributeSystem(this.layersByAttribute);
      }
   }

   static class ValueSampler<Value> {
      private final EnvironmentAttribute<Value> attribute;
      final Value baseValue;
      private final List<EnvironmentAttributeLayer<Value>> layers;
      final boolean isAffectedByPosition;
      @Nullable
      private Value cachedTickValue;
      private int cacheTickId;

      ValueSampler(EnvironmentAttribute<Value> attribute, Value baseValue, List<EnvironmentAttributeLayer<Value>> layers, boolean isAffectedByPosition) {
         this.attribute = attribute;
         this.baseValue = baseValue;
         this.layers = layers;
         this.isAffectedByPosition = isAffectedByPosition;
      }

      public void invalidateTickCache() {
         this.cachedTickValue = null;
         this.cacheTickId++;
      }

      public Value getDimensionValue() {
         if (this.cachedTickValue != null) {
            return this.cachedTickValue;
         } else {
            Value object = this.computeValueNotPositional();
            this.cachedTickValue = object;
            return object;
         }
      }

      public Value getValue(Vec3 pos, @Nullable SpatialAttributeInterpolator interpolator) {
         return !this.isAffectedByPosition ? this.getDimensionValue() : this.computeValuePositional(pos, interpolator);
      }

      private Value computeValuePositional(Vec3 pos, @Nullable SpatialAttributeInterpolator interpolator) {
         Value object = this.baseValue;

         for (EnvironmentAttributeLayer<Value> environmentAttributeLayer : this.layers) {
            object = (Value)(switch (environmentAttributeLayer) {
               case EnvironmentAttributeLayer.Constant<Value> constant -> (Object)constant.applyConstant(object);
               case EnvironmentAttributeLayer.TimeBased<Value> timeBased -> (Object)timeBased.applyTimeBased(object, this.cacheTickId);
               case EnvironmentAttributeLayer.Positional<Value> positional -> (Object)positional.applyPositional(
                  object, Objects.requireNonNull(pos), interpolator
               );
               default -> throw new MatchException(null, null);
            });
         }

         return this.attribute.sanitizeValue(object);
      }

      private Value computeValueNotPositional() {
         Value object = this.baseValue;

         for (EnvironmentAttributeLayer<Value> environmentAttributeLayer : this.layers) {
            object = (Value)(switch (environmentAttributeLayer) {
               case EnvironmentAttributeLayer.Constant<Value> constant -> (Object)constant.applyConstant(object);
               case EnvironmentAttributeLayer.TimeBased<Value> timeBased -> (Object)timeBased.applyTimeBased(object, this.cacheTickId);
               case EnvironmentAttributeLayer.Positional<Value> positional -> (Object)object;
               default -> throw new MatchException(null, null);
            });
         }

         return this.attribute.sanitizeValue(object);
      }
   }
}
