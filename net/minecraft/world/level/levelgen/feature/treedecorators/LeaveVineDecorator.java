package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class LeaveVineDecorator extends TreeDecorator {
   public static final MapCodec<LeaveVineDecorator> CODEC = Codec.floatRange(0.0F, 1.0F)
      .fieldOf("probability")
      .xmap(LeaveVineDecorator::new, p_226037_ -> p_226037_.probability);
   private final float probability;

   @Override
   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.LEAVE_VINE;
   }

   public LeaveVineDecorator(float p_226031_) {
      this.probability = p_226031_;
   }

   @Override
   public void place(TreeDecorator.Context p_226039_) {
      RandomSource $$1 = p_226039_.random();
      p_226039_.leaves().forEach(p_226035_ -> {
         if ($$1.nextFloat() < this.probability) {
            BlockPos $$3 = p_226035_.west();
            if (p_226039_.isAir($$3)) {
               addHangingVine($$3, VineBlock.EAST, p_226039_);
            }
         }

         if ($$1.nextFloat() < this.probability) {
            BlockPos $$4 = p_226035_.east();
            if (p_226039_.isAir($$4)) {
               addHangingVine($$4, VineBlock.WEST, p_226039_);
            }
         }

         if ($$1.nextFloat() < this.probability) {
            BlockPos $$5 = p_226035_.north();
            if (p_226039_.isAir($$5)) {
               addHangingVine($$5, VineBlock.SOUTH, p_226039_);
            }
         }

         if ($$1.nextFloat() < this.probability) {
            BlockPos $$6 = p_226035_.south();
            if (p_226039_.isAir($$6)) {
               addHangingVine($$6, VineBlock.NORTH, p_226039_);
            }
         }
      });
   }

   private static void addHangingVine(BlockPos p_226041_, BooleanProperty p_226042_, TreeDecorator.Context p_226043_) {
      p_226043_.placeVine(p_226041_, p_226042_);
      int $$3 = 4;

      for (BlockPos var4 = p_226041_.below(); p_226043_.isAir(var4) && $$3 > 0; $$3--) {
         p_226043_.placeVine(var4, p_226042_);
         var4 = var4.below();
      }
   }
}
