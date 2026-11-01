package net.minecraft.client.renderer.blockentity.state;

import net.minecraft.CrashReportCategory;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BlockEntityRenderState {
   public BlockPos blockPos = BlockPos.ZERO;
   public BlockState blockState = Blocks.AIR.defaultBlockState();
   public BlockEntityType<?> blockEntityType = BlockEntityType.TEST_BLOCK;
   public int lightCoords;
   @Nullable
   public ModelFeatureRenderer.CrumblingOverlay breakProgress;

   public static void extractBase(BlockEntity p_446710_, BlockEntityRenderState p_446267_, @Nullable ModelFeatureRenderer.CrumblingOverlay p_446124_) {
      p_446267_.blockPos = p_446710_.getBlockPos();
      p_446267_.blockState = p_446710_.getBlockState();
      p_446267_.blockEntityType = p_446710_.getType();
      p_446267_.lightCoords = p_446710_.getLevel() != null ? LevelRenderer.getLightColor(p_446710_.getLevel(), p_446710_.getBlockPos()) : 15728880;
      p_446267_.breakProgress = p_446124_;
   }

   public void fillCrashReportCategory(CrashReportCategory p_445769_) {
      p_445769_.setDetail("BlockEntityRenderState", this.getClass().getCanonicalName());
      p_445769_.setDetail("Position", this.blockPos);
      p_445769_.setDetail("Block state", this.blockState::toString);
   }
}
