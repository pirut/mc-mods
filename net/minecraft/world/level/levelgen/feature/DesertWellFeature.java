package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class DesertWellFeature extends Feature<NoneFeatureConfiguration> {
   private static final BlockStatePredicate IS_SAND = BlockStatePredicate.forBlock(Blocks.SAND);
   private final BlockState sand = Blocks.SAND.defaultBlockState();
   private final BlockState sandSlab = Blocks.SANDSTONE_SLAB.defaultBlockState();
   private final BlockState sandstone = Blocks.SANDSTONE.defaultBlockState();
   private final BlockState water = Blocks.WATER.defaultBlockState();

   public DesertWellFeature(Codec<NoneFeatureConfiguration> p_65599_) {
      super(p_65599_);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> p_159571_) {
      WorldGenLevel $$1 = p_159571_.level();
      BlockPos $$2 = p_159571_.origin();
      $$2 = $$2.above();

      while ($$1.isEmptyBlock($$2) && $$2.getY() > $$1.getMinBuildHeight() + 2) {
         $$2 = $$2.below();
      }

      if (!IS_SAND.test($$1.getBlockState($$2))) {
         return false;
      } else {
         for (int $$3 = -2; $$3 <= 2; $$3++) {
            for (int $$4 = -2; $$4 <= 2; $$4++) {
               if ($$1.isEmptyBlock($$2.offset($$3, -1, $$4)) && $$1.isEmptyBlock($$2.offset($$3, -2, $$4))) {
                  return false;
               }
            }
         }

         for (int $$5 = -2; $$5 <= 0; $$5++) {
            for (int $$6 = -2; $$6 <= 2; $$6++) {
               for (int $$7 = -2; $$7 <= 2; $$7++) {
                  $$1.setBlock($$2.offset($$6, $$5, $$7), this.sandstone, 2);
               }
            }
         }

         $$1.setBlock($$2, this.water, 2);

         for (Direction $$8 : Direction.Plane.HORIZONTAL) {
            $$1.setBlock($$2.relative($$8), this.water, 2);
         }

         BlockPos $$9 = $$2.below();
         $$1.setBlock($$9, this.sand, 2);

         for (Direction $$10 : Direction.Plane.HORIZONTAL) {
            $$1.setBlock($$9.relative($$10), this.sand, 2);
         }

         for (int $$11 = -2; $$11 <= 2; $$11++) {
            for (int $$12 = -2; $$12 <= 2; $$12++) {
               if ($$11 == -2 || $$11 == 2 || $$12 == -2 || $$12 == 2) {
                  $$1.setBlock($$2.offset($$11, 1, $$12), this.sandstone, 2);
               }
            }
         }

         $$1.setBlock($$2.offset(2, 1, 0), this.sandSlab, 2);
         $$1.setBlock($$2.offset(-2, 1, 0), this.sandSlab, 2);
         $$1.setBlock($$2.offset(0, 1, 2), this.sandSlab, 2);
         $$1.setBlock($$2.offset(0, 1, -2), this.sandSlab, 2);

         for (int $$13 = -1; $$13 <= 1; $$13++) {
            for (int $$14 = -1; $$14 <= 1; $$14++) {
               if ($$13 == 0 && $$14 == 0) {
                  $$1.setBlock($$2.offset($$13, 4, $$14), this.sandstone, 2);
               } else {
                  $$1.setBlock($$2.offset($$13, 4, $$14), this.sandSlab, 2);
               }
            }
         }

         for (int $$15 = 1; $$15 <= 3; $$15++) {
            $$1.setBlock($$2.offset(-1, $$15, -1), this.sandstone, 2);
            $$1.setBlock($$2.offset(-1, $$15, 1), this.sandstone, 2);
            $$1.setBlock($$2.offset(1, $$15, -1), this.sandstone, 2);
            $$1.setBlock($$2.offset(1, $$15, 1), this.sandstone, 2);
         }

         List<BlockPos> $$17 = List.of($$2, $$2.east(), $$2.south(), $$2.west(), $$2.north());
         RandomSource $$18 = p_159571_.random();
         placeSusSand($$1, Util.getRandom($$17, $$18).below(1));
         placeSusSand($$1, Util.getRandom($$17, $$18).below(2));
         return true;
      }
   }

   private static void placeSusSand(WorldGenLevel p_278029_, BlockPos p_278082_) {
      p_278029_.setBlock(p_278082_, Blocks.SUSPICIOUS_SAND.defaultBlockState(), 3);
      p_278029_.getBlockEntity(p_278082_, BlockEntityType.BRUSHABLE_BLOCK)
         .ifPresent(p_335307_ -> p_335307_.setLootTable(BuiltInLootTables.DESERT_WELL_ARCHAEOLOGY, p_278082_.asLong()));
   }
}
