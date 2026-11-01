package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;

public class TreeFeature extends Feature<TreeConfiguration> {
   private static final int BLOCK_UPDATE_FLAGS = 19;

   public TreeFeature(Codec<TreeConfiguration> p_67201_) {
      super(p_67201_);
   }

   private static boolean isVine(LevelSimulatedReader p_67278_, BlockPos p_67279_) {
      return p_67278_.isStateAtPosition(p_67279_, p_225299_ -> p_225299_.is(Blocks.VINE));
   }

   public static boolean isAirOrLeaves(LevelSimulatedReader p_67268_, BlockPos p_67269_) {
      return p_67268_.isStateAtPosition(p_67269_, p_284924_ -> p_284924_.isAir() || p_284924_.is(BlockTags.LEAVES));
   }

   private static void setBlockKnownShape(LevelWriter p_67257_, BlockPos p_67258_, BlockState p_67259_) {
      p_67257_.setBlock(p_67258_, p_67259_, 19);
   }

   public static boolean validTreePos(LevelSimulatedReader p_67273_, BlockPos p_67274_) {
      return p_67273_.isStateAtPosition(p_67274_, p_284925_ -> p_284925_.isAir() || p_284925_.is(BlockTags.REPLACEABLE_BY_TREES));
   }

   private boolean doPlace(
      WorldGenLevel p_225258_,
      RandomSource p_225259_,
      BlockPos p_225260_,
      BiConsumer<BlockPos, BlockState> p_225261_,
      BiConsumer<BlockPos, BlockState> p_225262_,
      FoliagePlacer.FoliageSetter p_273670_,
      TreeConfiguration p_225264_
   ) {
      int $$7 = p_225264_.trunkPlacer.getTreeHeight(p_225259_);
      int $$8 = p_225264_.foliagePlacer.foliageHeight(p_225259_, $$7, p_225264_);
      int $$9 = $$7 - $$8;
      int $$10 = p_225264_.foliagePlacer.foliageRadius(p_225259_, $$9);
      BlockPos $$11 = p_225264_.rootPlacer.<BlockPos>map(p_225286_ -> p_225286_.getTrunkOrigin(p_225260_, p_225259_)).orElse(p_225260_);
      int $$12 = Math.min(p_225260_.getY(), $$11.getY());
      int $$13 = Math.max(p_225260_.getY(), $$11.getY()) + $$7 + 1;
      if ($$12 >= p_225258_.getMinBuildHeight() + 1 && $$13 <= p_225258_.getMaxBuildHeight()) {
         OptionalInt $$14 = p_225264_.minimumSize.minClippedHeight();
         int $$15 = this.getMaxFreeTreeHeight(p_225258_, $$7, $$11, p_225264_);
         if ($$15 >= $$7 || !$$14.isEmpty() && $$15 >= $$14.getAsInt()) {
            if (p_225264_.rootPlacer.isPresent() && !p_225264_.rootPlacer.get().placeRoots(p_225258_, p_225261_, p_225259_, p_225260_, $$11, p_225264_)) {
               return false;
            } else {
               List<FoliagePlacer.FoliageAttachment> $$16 = p_225264_.trunkPlacer.placeTrunk(p_225258_, p_225262_, p_225259_, $$15, $$11, p_225264_);
               $$16.forEach(p_272582_ -> p_225264_.foliagePlacer.createFoliage(p_225258_, p_273670_, p_225259_, p_225264_, $$15, p_272582_, $$8, $$10));
               return true;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private int getMaxFreeTreeHeight(LevelSimulatedReader p_67216_, int p_67217_, BlockPos p_67218_, TreeConfiguration p_67219_) {
      BlockPos.MutableBlockPos $$4 = new BlockPos.MutableBlockPos();

      for (int $$5 = 0; $$5 <= p_67217_ + 1; $$5++) {
         int $$6 = p_67219_.minimumSize.getSizeAtHeight(p_67217_, $$5);

         for (int $$7 = -$$6; $$7 <= $$6; $$7++) {
            for (int $$8 = -$$6; $$8 <= $$6; $$8++) {
               $$4.setWithOffset(p_67218_, $$7, $$5, $$8);
               if (!p_67219_.trunkPlacer.isFree(p_67216_, $$4) || !p_67219_.ignoreVines && isVine(p_67216_, $$4)) {
                  return $$5 - 2;
               }
            }
         }
      }

      return p_67217_;
   }

   @Override
   protected void setBlock(LevelWriter p_67221_, BlockPos p_67222_, BlockState p_67223_) {
      setBlockKnownShape(p_67221_, p_67222_, p_67223_);
   }

   @Override
   public final boolean place(FeaturePlaceContext<TreeConfiguration> p_160530_) {
      final WorldGenLevel $$1 = p_160530_.level();
      RandomSource $$2 = p_160530_.random();
      BlockPos $$3 = p_160530_.origin();
      TreeConfiguration $$4 = p_160530_.config();
      Set<BlockPos> $$5 = Sets.newHashSet();
      Set<BlockPos> $$6 = Sets.newHashSet();
      final Set<BlockPos> $$7 = Sets.newHashSet();
      Set<BlockPos> $$8 = Sets.newHashSet();
      BiConsumer<BlockPos, BlockState> $$9 = (p_160555_, p_160556_) -> {
         $$5.add(p_160555_.immutable());
         $$1.setBlock(p_160555_, p_160556_, 19);
      };
      BiConsumer<BlockPos, BlockState> $$10 = (p_160548_, p_160549_) -> {
         $$6.add(p_160548_.immutable());
         $$1.setBlock(p_160548_, p_160549_, 19);
      };
      FoliagePlacer.FoliageSetter $$11 = new FoliagePlacer.FoliageSetter() {
         @Override
         public void set(BlockPos p_272825_, BlockState p_273311_) {
            $$7.add(p_272825_.immutable());
            $$1.setBlock(p_272825_, p_273311_, 19);
         }

         @Override
         public boolean isSet(BlockPos p_272999_) {
            return $$7.contains(p_272999_);
         }
      };
      BiConsumer<BlockPos, BlockState> $$12 = (p_160543_, p_160544_) -> {
         $$8.add(p_160543_.immutable());
         $$1.setBlock(p_160543_, p_160544_, 19);
      };
      boolean $$13 = this.doPlace($$1, $$2, $$3, $$9, $$10, $$11, $$4);
      if ($$13 && (!$$6.isEmpty() || !$$7.isEmpty())) {
         if (!$$4.decorators.isEmpty()) {
            TreeDecorator.Context $$14 = new TreeDecorator.Context($$1, $$12, $$2, $$6, $$7, $$5);
            $$4.decorators.forEach(p_225282_ -> p_225282_.place($$14));
         }

         return BoundingBox.encapsulatingPositions(Iterables.concat($$5, $$6, $$7, $$8)).map(p_225270_ -> {
            DiscreteVoxelShape $$5x = updateLeaves($$1, p_225270_, $$6, $$8, $$5);
            StructureTemplate.updateShapeAtEdge($$1, 3, $$5x, p_225270_.minX(), p_225270_.minY(), p_225270_.minZ());
            return true;
         }).orElse(false);
      } else {
         return false;
      }
   }

   private static DiscreteVoxelShape updateLeaves(
      LevelAccessor p_225252_, BoundingBox p_225253_, Set<BlockPos> p_225254_, Set<BlockPos> p_225255_, Set<BlockPos> p_225256_
   ) {
      DiscreteVoxelShape $$5 = new BitSetDiscreteVoxelShape(p_225253_.getXSpan(), p_225253_.getYSpan(), p_225253_.getZSpan());
      int $$6 = 7;
      List<Set<BlockPos>> $$7 = Lists.newArrayList();

      for (int $$8 = 0; $$8 < 7; $$8++) {
         $$7.add(Sets.newHashSet());
      }

      for (BlockPos $$9 : Lists.newArrayList(Sets.union(p_225255_, p_225256_))) {
         if (p_225253_.isInside($$9)) {
            $$5.fill($$9.getX() - p_225253_.minX(), $$9.getY() - p_225253_.minY(), $$9.getZ() - p_225253_.minZ());
         }
      }

      BlockPos.MutableBlockPos $$10 = new BlockPos.MutableBlockPos();
      int $$11 = 0;
      $$7.get(0).addAll(p_225254_);

      while (true) {
         while ($$11 >= 7 || !$$7.get($$11).isEmpty()) {
            if ($$11 >= 7) {
               return $$5;
            }

            Iterator<BlockPos> $$12 = $$7.get($$11).iterator();
            BlockPos $$13 = $$12.next();
            $$12.remove();
            if (p_225253_.isInside($$13)) {
               if ($$11 != 0) {
                  BlockState $$14 = p_225252_.getBlockState($$13);
                  setBlockKnownShape(p_225252_, $$13, $$14.setValue(BlockStateProperties.DISTANCE, $$11));
               }

               $$5.fill($$13.getX() - p_225253_.minX(), $$13.getY() - p_225253_.minY(), $$13.getZ() - p_225253_.minZ());

               for (Direction $$15 : Direction.values()) {
                  $$10.setWithOffset($$13, $$15);
                  if (p_225253_.isInside($$10)) {
                     int $$16 = $$10.getX() - p_225253_.minX();
                     int $$17 = $$10.getY() - p_225253_.minY();
                     int $$18 = $$10.getZ() - p_225253_.minZ();
                     if (!$$5.isFull($$16, $$17, $$18)) {
                        BlockState $$19 = p_225252_.getBlockState($$10);
                        OptionalInt $$20 = LeavesBlock.getOptionalDistanceAt($$19);
                        if (!$$20.isEmpty()) {
                           int $$21 = Math.min($$20.getAsInt(), $$11 + 1);
                           if ($$21 < 7) {
                              $$7.get($$21).add($$10.immutable());
                              $$11 = Math.min($$11, $$21);
                           }
                        }
                     }
                  }
               }
            }
         }

         $$11++;
      }
   }
}
