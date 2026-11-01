package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.state.BlockState;

public class WeightedStateProvider extends BlockStateProvider {
   public static final MapCodec<WeightedStateProvider> CODEC = WeightedList.nonEmptyCodec(BlockState.CODEC)
      .comapFlatMap(WeightedStateProvider::create, p_393435_ -> p_393435_.weightedList)
      .fieldOf("entries");
   private final WeightedList<BlockState> weightedList;

   private static DataResult<WeightedStateProvider> create(WeightedList<BlockState> p_394378_) {
      return p_394378_.isEmpty() ? DataResult.error(() -> "WeightedStateProvider with no states") : DataResult.success(new WeightedStateProvider(p_394378_));
   }

   public WeightedStateProvider(WeightedList<BlockState> p_394592_) {
      this.weightedList = p_394592_;
   }

   public WeightedStateProvider(WeightedList.Builder<BlockState> p_393479_) {
      this(p_393479_.build());
   }

   @Override
   protected BlockStateProviderType<?> type() {
      return BlockStateProviderType.WEIGHTED_STATE_PROVIDER;
   }

   @Override
   public BlockState getState(RandomSource p_225966_, BlockPos p_225967_) {
      return this.weightedList.getRandomOrThrow(p_225966_);
   }
}
