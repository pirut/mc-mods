package net.minecraft.world.attribute;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EnvironmentAttributeProbe {
   private final Map<EnvironmentAttribute<?>, EnvironmentAttributeProbe.ValueProbe<?>> valueProbes = new Reference2ObjectOpenHashMap();
   private final Function<EnvironmentAttribute<?>, EnvironmentAttributeProbe.ValueProbe<?>> valueProbeFactory = p_457807_ -> new EnvironmentAttributeProbe.ValueProbe<>(
      p_457807_
   );
   @Nullable
   Level level;
   @Nullable
   Vec3 position;
   final SpatialAttributeInterpolator biomeInterpolator = new SpatialAttributeInterpolator();

   public void reset() {
      this.level = null;
      this.position = null;
      this.biomeInterpolator.clear();
      this.valueProbes.clear();
   }

   public void tick(Level p_457865_, Vec3 p_457570_) {
      this.level = p_457865_;
      this.position = p_457570_;
      this.valueProbes.values().removeIf(EnvironmentAttributeProbe.ValueProbe::tick);
      this.biomeInterpolator.clear();
      GaussianSampler.sample(
         p_457570_.scale(0.25),
         p_457865_.getBiomeManager()::getNoiseBiomeAtQuart,
         (p_458003_, p_458163_) -> this.biomeInterpolator.accumulate(p_458003_, p_458163_.value().getAttributes())
      );
   }

   public <Value> Value getValue(EnvironmentAttribute<Value> p_457589_, float p_457851_) {
      EnvironmentAttributeProbe.ValueProbe<Value> valueprobe = (EnvironmentAttributeProbe.ValueProbe<Value>)this.valueProbes
         .computeIfAbsent(p_457589_, this.valueProbeFactory);
      return valueprobe.get(p_457589_, p_457851_);
   }

   class ValueProbe<Value> {
      private Value lastValue;
      @Nullable
      private Value newValue;

      public ValueProbe(EnvironmentAttribute<Value> p_458283_) {
         Value value = this.getValueFromLevel(p_458283_);
         this.lastValue = value;
         this.newValue = value;
      }

      private Value getValueFromLevel(EnvironmentAttribute<Value> p_457739_) {
         return EnvironmentAttributeProbe.this.level != null && EnvironmentAttributeProbe.this.position != null
            ? EnvironmentAttributeProbe.this.level
               .environmentAttributes()
               .getValue(p_457739_, EnvironmentAttributeProbe.this.position, EnvironmentAttributeProbe.this.biomeInterpolator)
            : p_457739_.defaultValue();
      }

      public boolean tick() {
         if (this.newValue == null) {
            return true;
         } else {
            this.lastValue = this.newValue;
            this.newValue = null;
            return false;
         }
      }

      public Value get(EnvironmentAttribute<Value> p_457545_, float p_458042_) {
         if (this.newValue == null) {
            this.newValue = this.getValueFromLevel(p_457545_);
         }

         return p_457545_.type().partialTickLerp().apply(p_458042_, this.lastValue, this.newValue);
      }
   }
}
