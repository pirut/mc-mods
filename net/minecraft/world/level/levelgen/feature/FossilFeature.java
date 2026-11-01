package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.apache.commons.lang3.mutable.MutableInt;

public class FossilFeature extends Feature<FossilFeatureConfiguration> {
   public FossilFeature(Codec<FossilFeatureConfiguration> p_65851_) {
      super(p_65851_);
   }

   @Override
   public boolean place(FeaturePlaceContext<FossilFeatureConfiguration> p_159789_) {
      RandomSource $$1 = p_159789_.random();
      WorldGenLevel $$2 = p_159789_.level();
      BlockPos $$3 = p_159789_.origin();
      Rotation $$4 = Rotation.getRandom($$1);
      FossilFeatureConfiguration $$5 = p_159789_.config();
      int $$6 = $$1.nextInt($$5.fossilStructures.size());
      StructureTemplateManager $$7 = $$2.getLevel().getServer().getStructureManager();
      StructureTemplate $$8 = $$7.getOrCreate($$5.fossilStructures.get($$6));
      StructureTemplate $$9 = $$7.getOrCreate($$5.overlayStructures.get($$6));
      ChunkPos $$10 = new ChunkPos($$3);
      BoundingBox $$11 = new BoundingBox(
         $$10.getMinBlockX() - 16,
         $$2.getMinBuildHeight(),
         $$10.getMinBlockZ() - 16,
         $$10.getMaxBlockX() + 16,
         $$2.getMaxBuildHeight(),
         $$10.getMaxBlockZ() + 16
      );
      StructurePlaceSettings $$12 = new StructurePlaceSettings().setRotation($$4).setBoundingBox($$11).setRandom($$1);
      Vec3i $$13 = $$8.getSize($$4);
      BlockPos $$14 = $$3.offset(-$$13.getX() / 2, 0, -$$13.getZ() / 2);
      int $$15 = $$3.getY();

      for (int $$16 = 0; $$16 < $$13.getX(); $$16++) {
         for (int $$17 = 0; $$17 < $$13.getZ(); $$17++) {
            $$15 = Math.min($$15, $$2.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, $$14.getX() + $$16, $$14.getZ() + $$17));
         }
      }

      int $$18 = Math.max($$15 - 15 - $$1.nextInt(10), $$2.getMinBuildHeight() + 10);
      BlockPos $$19 = $$8.getZeroPositionWithTransform($$14.atY($$18), Mirror.NONE, $$4);
      if (countEmptyCorners($$2, $$8.getBoundingBox($$12, $$19)) > $$5.maxEmptyCornersAllowed) {
         return false;
      } else {
         $$12.clearProcessors();
         $$5.fossilProcessors.value().list().forEach($$12::addProcessor);
         $$8.placeInWorld($$2, $$19, $$19, $$12, $$1, 4);
         $$12.clearProcessors();
         $$5.overlayProcessors.value().list().forEach($$12::addProcessor);
         $$9.placeInWorld($$2, $$19, $$19, $$12, $$1, 4);
         return true;
      }
   }

   private static int countEmptyCorners(WorldGenLevel p_159782_, BoundingBox p_159783_) {
      MutableInt $$2 = new MutableInt(0);
      p_159783_.forAllCorners(p_284921_ -> {
         BlockState $$3 = p_159782_.getBlockState(p_284921_);
         if ($$3.isAir() || $$3.is(Blocks.LAVA) || $$3.is(Blocks.WATER)) {
            $$2.add(1);
         }
      });
      return $$2.getValue();
   }
}
