package net.minecraft.client.renderer.state;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockBreakingRenderState extends MovingBlockRenderState {
   public int progress;

   public BlockBreakingRenderState(ClientLevel p_451020_, BlockPos p_451559_, int p_451196_) {
      this.level = p_451020_;
      this.blockPos = p_451559_;
      this.blockState = p_451020_.getBlockState(p_451559_);
      this.progress = p_451196_;
      this.biome = p_451020_.getBiome(p_451559_);
   }
}
