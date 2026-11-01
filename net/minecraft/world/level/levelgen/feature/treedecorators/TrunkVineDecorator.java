package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.VineBlock;

public class TrunkVineDecorator extends TreeDecorator {
   public static final MapCodec<TrunkVineDecorator> CODEC = MapCodec.unit(() -> TrunkVineDecorator.INSTANCE);
   public static final TrunkVineDecorator INSTANCE = new TrunkVineDecorator();

   @Override
   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.TRUNK_VINE;
   }

   @Override
   public void place(TreeDecorator.Context p_226077_) {
      RandomSource $$1 = p_226077_.random();
      p_226077_.logs().forEach(p_226075_ -> {
         if ($$1.nextInt(3) > 0) {
            BlockPos $$3 = p_226075_.west();
            if (p_226077_.isAir($$3)) {
               p_226077_.placeVine($$3, VineBlock.EAST);
            }
         }

         if ($$1.nextInt(3) > 0) {
            BlockPos $$4 = p_226075_.east();
            if (p_226077_.isAir($$4)) {
               p_226077_.placeVine($$4, VineBlock.WEST);
            }
         }

         if ($$1.nextInt(3) > 0) {
            BlockPos $$5 = p_226075_.north();
            if (p_226077_.isAir($$5)) {
               p_226077_.placeVine($$5, VineBlock.SOUTH);
            }
         }

         if ($$1.nextInt(3) > 0) {
            BlockPos $$6 = p_226075_.south();
            if (p_226077_.isAir($$6)) {
               p_226077_.placeVine($$6, VineBlock.NORTH);
            }
         }
      });
   }
}
