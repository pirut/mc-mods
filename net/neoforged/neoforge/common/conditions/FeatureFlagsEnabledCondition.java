package net.neoforged.neoforge.common.conditions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;

public record FeatureFlagsEnabledCondition(FeatureFlagSet flags) implements ICondition {
   public static final MapCodec<FeatureFlagsEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(FeatureFlags.CODEC.fieldOf("flags").forGetter(FeatureFlagsEnabledCondition::flags))
         .apply(instance, FeatureFlagsEnabledCondition::new)
   );

   public FeatureFlagsEnabledCondition(FeatureFlagSet flags) {
      if (flags.isEmpty()) {
         throw new IllegalArgumentException("FeatureFlagsEnabledCondition requires a non-empty feature flag set");
      } else {
         this.flags = flags;
      }
   }

   @Override
   public boolean test(ICondition.IContext context) {
      return this.flags.isSubsetOf(context.enabledFeatures());
   }

   @Override
   public MapCodec<? extends ICondition> codec() {
      return CODEC;
   }
}
