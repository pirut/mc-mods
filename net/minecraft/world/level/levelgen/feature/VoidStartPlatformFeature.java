package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class VoidStartPlatformFeature extends Feature<NoneFeatureConfiguration> {
   private static final BlockPos PLATFORM_OFFSET = new BlockPos(8, 3, 8);
   private static final ChunkPos PLATFORM_ORIGIN_CHUNK = new ChunkPos(PLATFORM_OFFSET);
   private static final int PLATFORM_RADIUS = 16;
   private static final int PLATFORM_RADIUS_CHUNKS = 1;

   public VoidStartPlatformFeature(Codec<NoneFeatureConfiguration> p_67354_) {
      super(p_67354_);
   }

   private static int checkerboardDistance(int p_67356_, int p_67357_, int p_67358_, int p_67359_) {
      return Math.max(Math.abs(p_67356_ - p_67358_), Math.abs(p_67357_ - p_67359_));
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> p_160633_) {
      WorldGenLevel $$1 = p_160633_.level();
      ChunkPos $$2 = new ChunkPos(p_160633_.origin());
      if (checkerboardDistance($$2.x, $$2.z, PLATFORM_ORIGIN_CHUNK.x, PLATFORM_ORIGIN_CHUNK.z) > 1) {
         return true;
      } else {
         BlockPos $$3 = PLATFORM_OFFSET.atY(p_160633_.origin().getY() + PLATFORM_OFFSET.getY());
         BlockPos.MutableBlockPos $$4 = new BlockPos.MutableBlockPos();

         for (int $$5 = $$2.getMinBlockZ(); $$5 <= $$2.getMaxBlockZ(); $$5++) {
            for (int $$6 = $$2.getMinBlockX(); $$6 <= $$2.getMaxBlockX(); $$6++) {
               if (checkerboardDistance($$3.getX(), $$3.getZ(), $$6, $$5) <= 16) {
                  $$4.set($$6, $$3.getY(), $$5);
                  if ($$4.equals($$3)) {
                     $$1.setBlock($$4, Blocks.COBBLESTONE.defaultBlockState(), 2);
                  } else {
                     $$1.setBlock($$4, Blocks.STONE.defaultBlockState(), 2);
                  }
               }
            }
         }

         return true;
      }
   }
}
