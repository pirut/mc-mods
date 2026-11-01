package net.minecraft.world.entity.variant;

import com.mojang.serialization.Codec;
import java.util.List;

public record SpawnPrioritySelectors(List<PriorityProvider.Selector<SpawnContext, SpawnCondition>> selectors) {
   public static final SpawnPrioritySelectors EMPTY = new SpawnPrioritySelectors(List.of());
   public static final Codec<SpawnPrioritySelectors> CODEC = PriorityProvider.Selector.codec(SpawnCondition.CODEC)
      .listOf()
      .xmap(SpawnPrioritySelectors::new, SpawnPrioritySelectors::selectors);

   public static SpawnPrioritySelectors single(SpawnCondition p_400226_, int p_400154_) {
      return new SpawnPrioritySelectors(PriorityProvider.single(p_400226_, p_400154_));
   }

   public static SpawnPrioritySelectors fallback(int p_400301_) {
      return new SpawnPrioritySelectors(PriorityProvider.alwaysTrue(p_400301_));
   }
}
