package net.neoforged.neoforge.event.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.neoforged.bus.api.ICancellableEvent;
import org.jspecify.annotations.Nullable;

public class BlockGrowFeatureEvent extends LevelEvent implements ICancellableEvent {
   private final RandomSource rand;
   private final BlockPos pos;
   @Nullable
   private Holder<ConfiguredFeature<?, ?>> feature;

   public BlockGrowFeatureEvent(LevelAccessor level, RandomSource rand, BlockPos pos, @Nullable Holder<ConfiguredFeature<?, ?>> feature) {
      super(level);
      this.rand = rand;
      this.pos = pos;
      this.feature = feature;
   }

   public RandomSource getRandom() {
      return this.rand;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   @Nullable
   public Holder<ConfiguredFeature<?, ?>> getFeature() {
      return this.feature;
   }

   public void setFeature(@Nullable Holder<ConfiguredFeature<?, ?>> feature) {
      this.feature = feature;
   }

   public void setFeature(ResourceKey<ConfiguredFeature<?, ?>> featureKey) {
      this.feature = this.getLevel().registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).get(featureKey).orElse(null);
   }

   public void setCanceled(boolean canceled) {
      super.setCanceled(canceled);
   }
}
