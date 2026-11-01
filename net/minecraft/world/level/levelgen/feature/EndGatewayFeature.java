package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.levelgen.feature.configurations.EndGatewayConfiguration;

public class EndGatewayFeature extends Feature<EndGatewayConfiguration> {
   public EndGatewayFeature(Codec<EndGatewayConfiguration> p_65682_) {
      super(p_65682_);
   }

   @Override
   public boolean place(FeaturePlaceContext<EndGatewayConfiguration> p_159715_) {
      BlockPos $$1 = p_159715_.origin();
      WorldGenLevel $$2 = p_159715_.level();
      EndGatewayConfiguration $$3 = p_159715_.config();

      for (BlockPos $$4 : BlockPos.betweenClosed($$1.offset(-1, -2, -1), $$1.offset(1, 2, 1))) {
         boolean $$5 = $$4.getX() == $$1.getX();
         boolean $$6 = $$4.getY() == $$1.getY();
         boolean $$7 = $$4.getZ() == $$1.getZ();
         boolean $$8 = Math.abs($$4.getY() - $$1.getY()) == 2;
         if ($$5 && $$6 && $$7) {
            BlockPos $$9 = $$4.immutable();
            this.setBlock($$2, $$9, Blocks.END_GATEWAY.defaultBlockState());
            $$3.getExit().ifPresent(p_352890_ -> {
               if ($$2.getBlockEntity($$9) instanceof TheEndGatewayBlockEntity $$5x) {
                  $$5x.setExitPosition(p_352890_, $$3.isExitExact());
               }
            });
         } else if ($$6) {
            this.setBlock($$2, $$4, Blocks.AIR.defaultBlockState());
         } else if ($$8 && $$5 && $$7) {
            this.setBlock($$2, $$4, Blocks.BEDROCK.defaultBlockState());
         } else if (($$5 || $$7) && !$$8) {
            this.setBlock($$2, $$4, Blocks.BEDROCK.defaultBlockState());
         } else {
            this.setBlock($$2, $$4, Blocks.AIR.defaultBlockState());
         }
      }

      return true;
   }
}
