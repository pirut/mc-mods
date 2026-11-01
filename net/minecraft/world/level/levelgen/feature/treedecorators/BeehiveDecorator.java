package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BeehiveDecorator extends TreeDecorator {
   public static final MapCodec<BeehiveDecorator> CODEC = Codec.floatRange(0.0F, 1.0F)
      .fieldOf("probability")
      .xmap(BeehiveDecorator::new, p_69971_ -> p_69971_.probability);
   private static final Direction WORLDGEN_FACING = Direction.SOUTH;
   private static final Direction[] SPAWN_DIRECTIONS = Direction.Plane.HORIZONTAL
      .stream()
      .filter(p_202307_ -> p_202307_ != WORLDGEN_FACING.getOpposite())
      .toArray(Direction[]::new);
   private final float probability;

   public BeehiveDecorator(float p_69958_) {
      this.probability = p_69958_;
   }

   @Override
   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.BEEHIVE;
   }

   @Override
   public void place(TreeDecorator.Context p_226019_) {
      RandomSource $$1 = p_226019_.random();
      if (!($$1.nextFloat() >= this.probability)) {
         List<BlockPos> $$2 = p_226019_.leaves();
         List<BlockPos> $$3 = p_226019_.logs();
         int $$4 = !$$2.isEmpty()
            ? Math.max($$2.get(0).getY() - 1, $$3.get(0).getY() + 1)
            : Math.min($$3.get(0).getY() + 1 + $$1.nextInt(3), $$3.get($$3.size() - 1).getY());
         List<BlockPos> $$5 = $$3.stream()
            .filter(p_202300_ -> p_202300_.getY() == $$4)
            .flatMap(p_202305_ -> Stream.of(SPAWN_DIRECTIONS).map(p_202305_::relative))
            .collect(Collectors.toList());
         if (!$$5.isEmpty()) {
            Collections.shuffle($$5);
            Optional<BlockPos> $$6 = $$5.stream()
               .filter(p_226022_ -> p_226019_.isAir(p_226022_) && p_226019_.isAir(p_226022_.relative(WORLDGEN_FACING)))
               .findFirst();
            if (!$$6.isEmpty()) {
               p_226019_.setBlock($$6.get(), Blocks.BEE_NEST.defaultBlockState().setValue(BeehiveBlock.FACING, WORLDGEN_FACING));
               p_226019_.level().getBlockEntity($$6.get(), BlockEntityType.BEEHIVE).ifPresent(p_330149_ -> {
                  int $$2x = 2 + $$1.nextInt(2);

                  for (int $$3x = 0; $$3x < $$2x; $$3x++) {
                     p_330149_.storeBee(BeehiveBlockEntity.Occupant.create($$1.nextInt(599)));
                  }
               });
            }
         }
      }
   }
}
