package net.minecraft.world.level.block;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public interface BonemealableBlock {
   boolean isValidBonemealTarget(LevelReader var1, BlockPos var2, BlockState var3);

   boolean isBonemealSuccess(Level var1, RandomSource var2, BlockPos var3, BlockState var4);

   void performBonemeal(ServerLevel var1, RandomSource var2, BlockPos var3, BlockState var4);

   static boolean hasSpreadableNeighbourPos(LevelReader p_401785_, BlockPos p_401901_, BlockState p_401773_) {
      return getSpreadableNeighbourPos(Direction.Plane.HORIZONTAL.stream().toList(), p_401785_, p_401901_, p_401773_).isPresent();
   }

   static Optional<BlockPos> findSpreadableNeighbourPos(Level p_401928_, BlockPos p_401865_, BlockState p_401938_) {
      return getSpreadableNeighbourPos(Direction.Plane.HORIZONTAL.shuffledCopy(p_401928_.random), p_401928_, p_401865_, p_401938_);
   }

   private static Optional<BlockPos> getSpreadableNeighbourPos(List<Direction> p_401891_, LevelReader p_401878_, BlockPos p_401894_, BlockState p_401830_) {
      for (Direction direction : p_401891_) {
         BlockPos blockpos = p_401894_.relative(direction);
         if (p_401878_.isEmptyBlock(blockpos) && p_401830_.canSurvive(p_401878_, blockpos)) {
            return Optional.of(blockpos);
         }
      }

      return Optional.empty();
   }

   default BlockPos getParticlePos(BlockPos p_316301_) {
      return switch (this.getType()) {
         case NEIGHBOR_SPREADER -> p_316301_.above();
         case GROWER -> p_316301_;
      };
   }

   default BonemealableBlock.Type getType() {
      return BonemealableBlock.Type.GROWER;
   }

   public static enum Type {
      NEIGHBOR_SPREADER,
      GROWER;
   }
}
