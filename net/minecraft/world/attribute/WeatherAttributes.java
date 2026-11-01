package net.minecraft.world.attribute;

import com.google.common.collect.Sets;
import java.util.Set;
import net.minecraft.util.ARGB;
import net.minecraft.world.attribute.modifier.ColorModifier;
import net.minecraft.world.attribute.modifier.FloatModifier;
import net.minecraft.world.attribute.modifier.FloatWithAlpha;
import net.minecraft.world.level.Level;
import net.minecraft.world.timeline.Timelines;

public class WeatherAttributes {
   public static final EnvironmentAttributeMap RAIN = EnvironmentAttributeMap.builder()
      .modify(EnvironmentAttributes.SKY_COLOR, ColorModifier.BLEND_TO_GRAY, new ColorModifier.BlendToGray(0.6F, 0.75F))
      .modify(EnvironmentAttributes.FOG_COLOR, ColorModifier.MULTIPLY_RGB, ARGB.colorFromFloat(1.0F, 0.5F, 0.5F, 0.6F))
      .modify(EnvironmentAttributes.CLOUD_COLOR, ColorModifier.BLEND_TO_GRAY, new ColorModifier.BlendToGray(0.24F, 0.5F))
      .modify(EnvironmentAttributes.SKY_LIGHT_LEVEL, FloatModifier.ALPHA_BLEND, new FloatWithAlpha(4.0F, 0.3125F))
      .modify(EnvironmentAttributes.SKY_LIGHT_COLOR, ColorModifier.ALPHA_BLEND, ARGB.color(0.3125F, Timelines.NIGHT_SKY_LIGHT_COLOR))
      .modify(EnvironmentAttributes.SKY_LIGHT_FACTOR, FloatModifier.ALPHA_BLEND, new FloatWithAlpha(0.24F, 0.3125F))
      .set(EnvironmentAttributes.STAR_BRIGHTNESS, 0.0F)
      .modify(EnvironmentAttributes.SUNRISE_SUNSET_COLOR, ColorModifier.MULTIPLY_ARGB, ARGB.colorFromFloat(1.0F, 0.5F, 0.5F, 0.6F))
      .set(EnvironmentAttributes.BEES_STAY_IN_HIVE, true)
      .build();
   public static final EnvironmentAttributeMap THUNDER = EnvironmentAttributeMap.builder()
      .modify(EnvironmentAttributes.SKY_COLOR, ColorModifier.BLEND_TO_GRAY, new ColorModifier.BlendToGray(0.24F, 0.94F))
      .modify(EnvironmentAttributes.FOG_COLOR, ColorModifier.MULTIPLY_RGB, ARGB.colorFromFloat(1.0F, 0.25F, 0.25F, 0.3F))
      .modify(EnvironmentAttributes.CLOUD_COLOR, ColorModifier.BLEND_TO_GRAY, new ColorModifier.BlendToGray(0.095F, 0.94F))
      .modify(EnvironmentAttributes.SKY_LIGHT_LEVEL, FloatModifier.ALPHA_BLEND, new FloatWithAlpha(4.0F, 0.52734375F))
      .modify(EnvironmentAttributes.SKY_LIGHT_COLOR, ColorModifier.ALPHA_BLEND, ARGB.color(0.52734375F, Timelines.NIGHT_SKY_LIGHT_COLOR))
      .modify(EnvironmentAttributes.SKY_LIGHT_FACTOR, FloatModifier.ALPHA_BLEND, new FloatWithAlpha(0.24F, 0.52734375F))
      .set(EnvironmentAttributes.STAR_BRIGHTNESS, 0.0F)
      .modify(EnvironmentAttributes.SUNRISE_SUNSET_COLOR, ColorModifier.MULTIPLY_ARGB, ARGB.colorFromFloat(1.0F, 0.25F, 0.25F, 0.3F))
      .set(EnvironmentAttributes.BEES_STAY_IN_HIVE, true)
      .build();
   private static final Set<EnvironmentAttribute<?>> WEATHER_ATTRIBUTES = Sets.union(RAIN.keySet(), THUNDER.keySet());

   public static void addBuiltinLayers(EnvironmentAttributeSystem.Builder p_467664_, WeatherAttributes.WeatherAccess p_469548_) {
      for (EnvironmentAttribute<?> environmentattribute : WEATHER_ATTRIBUTES) {
         addLayer(p_467664_, p_469548_, environmentattribute);
      }
   }

   private static <Value> void addLayer(
      EnvironmentAttributeSystem.Builder p_468185_, WeatherAttributes.WeatherAccess p_469995_, EnvironmentAttribute<Value> p_469711_
   ) {
      EnvironmentAttributeMap.Entry<Value, ?> entry = RAIN.get(p_469711_);
      EnvironmentAttributeMap.Entry<Value, ?> entry1 = THUNDER.get(p_469711_);
      p_468185_.addTimeBasedLayer(p_469711_, (p_469894_, p_468621_) -> {
         float f = p_469995_.thunderLevel();
         float f1 = p_469995_.rainLevel() - f;
         if (entry != null && f1 > 0.0F) {
            Value value = entry.applyModifier(p_469894_);
            p_469894_ = p_469711_.type().stateChangeLerp().apply(f1, p_469894_, value);
         }

         if (entry1 != null && f > 0.0F) {
            Value value1 = entry1.applyModifier(p_469894_);
            p_469894_ = p_469711_.type().stateChangeLerp().apply(f, p_469894_, value1);
         }

         return p_469894_;
      });
   }

   public interface WeatherAccess {
      static WeatherAttributes.WeatherAccess from(final Level p_467002_) {
         return new WeatherAttributes.WeatherAccess() {
            @Override
            public float rainLevel() {
               return p_467002_.getRainLevel(1.0F);
            }

            @Override
            public float thunderLevel() {
               return p_467002_.getThunderLevel(1.0F);
            }
         };
      }

      float rainLevel();

      float thunderLevel();
   }
}
