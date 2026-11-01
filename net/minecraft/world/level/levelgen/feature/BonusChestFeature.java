package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import java.util.stream.IntStream;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class BonusChestFeature extends Feature<NoneFeatureConfiguration> {
   public BonusChestFeature(Codec<NoneFeatureConfiguration> p_65299_) {
      super(p_65299_);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> p_159477_) {
      RandomSource $$1 = p_159477_.random();
      WorldGenLevel $$2 = p_159477_.level();
      ChunkPos $$3 = new ChunkPos(p_159477_.origin());
      IntArrayList $$4 = Util.toShuffledList(IntStream.rangeClosed($$3.getMinBlockX(), $$3.getMaxBlockX()), $$1);
      IntArrayList $$5 = Util.toShuffledList(IntStream.rangeClosed($$3.getMinBlockZ(), $$3.getMaxBlockZ()), $$1);
      BlockPos.MutableBlockPos $$6 = new BlockPos.MutableBlockPos();
      IntListIterator var8 = $$4.iterator();

      while (var8.hasNext()) {
         Integer $$7 = (Integer)var8.next();
         IntListIterator var10 = $$5.iterator();

         while (var10.hasNext()) {
            Integer $$8 = (Integer)var10.next();
            $$6.set($$7, 0, $$8);
            BlockPos $$9 = $$2.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, $$6);
            if ($$2.isEmptyBlock($$9) || $$2.getBlockState($$9).getCollisionShape($$2, $$9).isEmpty()) {
               $$2.setBlock($$9, Blocks.CHEST.defaultBlockState(), 2);
               RandomizableContainer.setBlockEntityLootTable($$2, $$1, $$9, BuiltInLootTables.SPAWN_BONUS_CHEST);
               BlockState $$10 = Blocks.TORCH.defaultBlockState();

               for (Direction $$11 : Direction.Plane.HORIZONTAL) {
                  BlockPos $$12 = $$9.relative($$11);
                  if ($$10.canSurvive($$2, $$12)) {
                     $$2.setBlock($$12, $$10, 2);
                  }
               }

               return true;
            }
         }
      }

      return false;
   }
}
