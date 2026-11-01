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

   EnvironmentAttributeSystem(Map<EnvironmentAttribute<?>, List<EnvironmentAttributeLayer<?>>> p_467074_) {
      p_467074_.forEach(
         (p_466523_, p_466524_) -> this.attributeSamplers
            .put(
               (EnvironmentAttribute<?>)p_466523_,
               this.bakeLayerSampler((EnvironmentAttribute<?>)p_466523_, (List<? extends EnvironmentAttributeLayer<?>>)p_466524_)
            )
      );
   }

   private <Value> EnvironmentAttributeSystem.ValueSampler<Value> bakeLayerSampler(
      EnvironmentAttribute<Value> p_468341_, List<? extends EnvironmentAttributeLayer<?>> p_466955_
   ) {
      List<EnvironmentAttributeLayer<Value>> list = new ArrayList<>((Collection<? extends EnvironmentAttributeLayer<Value>>)p_466955_);
      Value value = p_468341_.defaultValue();

      while (!list.isEmpty()) {
         if (!(list.getFirst() instanceof EnvironmentAttributeLayer.Constant<Value> constant)) {
            break;
         }

         value = constant.applyConstant(value);
         list.removeFirst();
      }

      boolean flag = list.stream().anyMatch(p_466522_ -> p_466522_ instanceof EnvironmentAttributeLayer.Positional);
      return new EnvironmentAttributeSystem.ValueSampler<>(p_468341_, value, List.copyOf(list), flag);
   }

   public static EnvironmentAttributeSystem.Builder builder() {
      return new EnvironmentAttributeSystem.Builder();
   }

   static void addDefaultLayers(EnvironmentAttributeSystem.Builder p_467092_, Level p_467915_) {
      RegistryAccess registryaccess = p_467915_.registryAccess();
      BiomeManager biomemanager = p_467915_.getBiomeManager();
      LongSupplier longsupplier = p_467915_::getDayTime;
      addDimensionLayer(p_467092_, p_467915_.dimensionType());
      addBiomeLayer(p_467092_, registryaccess.lookupOrThrow(Registries.BIOME), biomemanager);
      p_467915_.dimensionType().timelines().forEach(p_466516_ -> p_467092_.addTimelineLayer((Holder<Timeline>)p_466516_, longsupplier));
      if (p_467915_.canHaveWeather()) {
         WeatherAttributes.addBuiltinLayers(p_467092_, WeatherAttributes.WeatherAccess.from(p_467915_));
      }
   }

   private static void addDimensionLayer(EnvironmentAttributeSystem.Builder p_466934_, DimensionType p_466961_) {
      p_466934_.addConstantLayer(p_466961_.attributes());
   }

   private static void addBiomeLayer(EnvironmentAttributeSystem.Builder p_468014_, HolderLookup<Biome> p_468636_, BiomeManager p_469748_) {
      Stream<EnvironmentAttribute<?>> stream = p_468636_.listElements().flatMap(p_457819_ -> p_457819_.value().getAttributes().keySet().stream()).distinct();
      stream.forEach(p_466513_ -> addBiomeLayerForAttribute(p_468014_, (EnvironmentAttribute<?>)p_466513_, p_469748_));
   }

   private static <Value> void addBiomeLayerForAttribute(
      EnvironmentAttributeSystem.Builder p_468440_, EnvironmentAttribute<Value> p_469626_, BiomeManager p_469570_
   ) {
      p_468440_.addPositionalLayer(p_469626_, (p_466519_, p_466520_, p_466521_) -> {
         if (p_466521_ != null && p_469626_.isSpatiallyInterpolated()) {
            return p_466521_.applyAttributeLayer(p_469626_, p_466519_);
         } else {
            Holder<Biome> holder = p_469570_.getNoiseBiomeAtPosition(p_466520_.x, p_466520_.y, p_466520_.z);
            return holder.value().getAttributes().applyModifier(p_469626_, p_466519_);
         }
      });
   }

   public void invalidateTickCache() {
      this.attributeSamplers.values().forEach(EnvironmentAttributeSystem.ValueSampler::invalidateTickCache);
   }

   @Nullable
   private <Value> EnvironmentAttributeSystem.ValueSampler<Value> getValueSampler(EnvironmentAttribute<Value> p_468466_) {
      return (EnvironmentAttributeSystem.ValueSampler<Value>)this.attributeSamplers.get(p_468466_);
   }

   @Override
   public <Value> Value getDimensionValue(EnvironmentAttribute<Value> p_457805_) {
      if (SharedConstants.IS_RUNNING_IN_IDE && p_457805_.isPositional()) {
         throw new IllegalStateException("Position must always be provided for positional attribute " + p_457805_);
      } else {
         EnvironmentAttributeSystem.ValueSampler<Value> valuesampler = this.getValueSampler(p_457805_);
         return valuesampler == null ? p_457805_.defaultValue() : valuesampler.getDimensionValue();
      }
   }

   @Override
   public <Value> Value getValue(EnvironmentAttribute<Value> p_458091_, Vec3 p_458226_, @Nullable SpatialAttributeInterpolator p_458292_) {
      EnvironmentAttributeSystem.ValueSampler<Value> valuesampler = this.getValueSampler(p_458091_);
      return valuesampler == null ? p_458091_.defaultValue() : valuesampler.getValue(p_458226_, p_458292_);
   }

   @VisibleForTesting
   <Value> Value getConstantBaseValue(EnvironmentAttribute<Value> p_467381_) {
      EnvironmentAttributeSystem.ValueSampler<Value> valuesampler = this.getValueSampler(p_467381_);
      return valuesampler != null ? valuesampler.baseValue : p_467381_.defaultValue();
   }

   @VisibleForTesting
   boolean isAffectedByPosition(EnvironmentAttribute<?> p_469030_) {
      EnvironmentAttributeSystem.ValueSampler<?> valuesampler = this.getValueSampler(p_469030_);
      return valuesampler != null && valuesampler.isAffectedByPosition;
   }

   public static class Builder {
      private final Map<EnvironmentAttribute<?>, List<EnvironmentAttributeLayer<?>>> layersByAttribute = new HashMap<>();

      Builder() {
      }

      public EnvironmentAttributeSystem.Builder addDefaultLayers(Level p_466985_) {
         EnvironmentAttributeSystem.addDefaultLayers(this, p_466985_);
         return this;
      }

      public EnvironmentAttributeSystem.Builder addConstantLayer(EnvironmentAttributeMap p_469707_) {
         for (EnvironmentAttribute<?> environmentattribute : p_469707_.keySet()) {
            this.addConstantEntry(environmentattribute, p_469707_);
         }

         return this;
      }

      private <Value> EnvironmentAttributeSystem.Builder addConstantEntry(EnvironmentAttribute<Value> p_467116_, EnvironmentAttributeMap p_467308_) {
         EnvironmentAttributeMap.Entry<Value, ?> entry = p_467308_.get(p_467116_);
         if (entry == null) {
            throw new IllegalArgumentException("Missing attribute " + p_467116_);
         } else {
            return this.addConstantLayer(p_467116_, entry::applyModifier);
         }
      }

      public <Value> EnvironmentAttributeSystem.Builder addConstantLayer(
         EnvironmentAttribute<Value> p_467123_, EnvironmentAttributeLayer.Constant<Value> p_467867_
      ) {
         return this.addLayer(p_467123_, p_467867_);
      }

      public <Value> EnvironmentAttributeSystem.Builder addTimeBasedLayer(
         EnvironmentAttribute<Value> p_467475_, EnvironmentAttributeLayer.TimeBased<Value> p_467570_
      ) {
         return this.addLayer(p_467475_, p_467570_);
      }

      public <Value> EnvironmentAttributeSystem.Builder addPositionalLayer(
         EnvironmentAttribute<Value> p_467451_, EnvironmentAttributeLayer.Positional<Value> p_468842_
      ) {
         return this.addLayer(p_467451_, p_468842_);
      }

      private <Value> EnvironmentAttributeSystem.Builder addLayer(EnvironmentAttribute<Value> p_467411_, EnvironmentAttributeLayer<Value> p_466984_) {
         this.layersByAttribute.computeIfAbsent(p_467411_, p_467515_ -> new ArrayList<>()).add(p_466984_);
         return this;
      }

      public EnvironmentAttributeSystem.Builder addTimelineLayer(Holder<Timeline> p_467401_, LongSupplier p_466837_) {
         for (EnvironmentAttribute<?> environmentattribute : p_467401_.value().attributes()) {
            this.addTimelineLayerForAttribute(p_467401_, environmentattribute, p_466837_);
         }

         return this;
      }

      private <Value> void addTimelineLayerForAttribute(Holder<Timeline> p_469494_, EnvironmentAttribute<Value> p_467257_, LongSupplier p_468443_) {
         this.addTimeBasedLayer(p_467257_, p_469494_.value().createTrackSampler(p_467257_, p_468443_));
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

      ValueSampler(EnvironmentAttribute<Value> p_469170_, Value p_468216_, List<EnvironmentAttributeLayer<Value>> p_468328_, boolean p_469262_) {
         this.attribute = p_469170_;
         this.baseValue = p_468216_;
         this.layers = p_468328_;
         this.isAffectedByPosition = p_469262_;
      }

      public void invalidateTickCache() {
         this.cachedTickValue = null;
         this.cacheTickId++;
      }

      public Value getDimensionValue() {
         if (this.cachedTickValue != null) {
            return this.cachedTickValue;
         } else {
            Value value = this.computeValueNotPositional();
            this.cachedTickValue = value;
            return value;
         }
      }

      public Value getValue(Vec3 p_467228_, @Nullable SpatialAttributeInterpolator p_468134_) {
         return !this.isAffectedByPosition ? this.getDimensionValue() : this.computeValuePositional(p_467228_, p_468134_);
      }

      private Value computeValuePositional(Vec3 p_467943_, @Nullable SpatialAttributeInterpolator p_467052_) {
         Value value = this.baseValue;

         for (EnvironmentAttributeLayer<Value> environmentattributelayer : this.layers) {
            value = (Value)(switch (environmentattributelayer) {
               case EnvironmentAttributeLayer.Constant<Value> constant -> constant.applyConstant(value);
               case EnvironmentAttributeLayer.TimeBased<Value> timebased -> timebased.applyTimeBased(value, this.cacheTickId);
               case EnvironmentAttributeLayer.Positional<Value> positional -> positional.applyPositional(value, Objects.requireNonNull(p_467943_), p_467052_);
               default -> throw new MatchException(null, null);
            });
         }

         return this.attribute.sanitizeValue(value);
      }

      private Value computeValueNotPositional() {
         Value value = this.baseValue;

         for (EnvironmentAttributeLayer<Value> environmentattributelayer : this.layers) {
            value = (Value)(switch (environmentattributelayer) {
               case EnvironmentAttributeLayer.Constant<Value> constant -> constant.applyConstant(value);
               case EnvironmentAttributeLayer.TimeBased<Value> timebased -> timebased.applyTimeBased(value, this.cacheTickId);
               case EnvironmentAttributeLayer.Positional<Value> positional -> value;
               default -> throw new MatchException(null, null);
            });
         }

         return this.attribute.sanitizeValue(value);
      }
   }
}
