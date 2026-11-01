package net.minecraft.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class BlockRenderDispatcher implements ResourceManagerReloadListener {
   private final BlockModelShaper blockModelShaper;
   private final MaterialSet materials;
   private final ModelBlockRenderer modelRenderer;
   @Nullable
   private LiquidBlockRenderer liquidBlockRenderer;
   private final RandomSource singleThreadRandom = RandomSource.create();
   private final List<BlockModelPart> singleThreadPartList = new ArrayList<>();
   private final BlockColors blockColors;

   public BlockRenderDispatcher(BlockModelShaper blockModelShaper, MaterialSet materials, BlockColors blockColors) {
      this.blockModelShaper = blockModelShaper;
      this.materials = materials;
      this.blockColors = blockColors;
      this.modelRenderer = new ModelBlockRenderer(this.blockColors);
   }

   public BlockModelShaper getBlockModelShaper() {
      return this.blockModelShaper;
   }

   public void renderBreakingTexture(BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack poseStack, VertexConsumer consumer) {
      if (state.getRenderShape() == RenderShape.MODEL) {
         BlockStateModel blockStateModel = this.blockModelShaper.getBlockModel(state);
         this.singleThreadRandom.setSeed(state.getSeed(pos));
         this.singleThreadPartList.clear();
         blockStateModel.collectParts(this.singleThreadRandom, this.singleThreadPartList);
         this.modelRenderer.tesselateBlock(level, this.singleThreadPartList, state, pos, poseStack, consumer, true, OverlayTexture.NO_OVERLAY);
      }
   }

   public void renderBatched(
      BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack poseStack, VertexConsumer consumer, boolean checkSides, List<BlockModelPart> parts
   ) {
      try {
         this.modelRenderer.tesselateBlock(level, parts, state, pos, poseStack, consumer, checkSides, OverlayTexture.NO_OVERLAY);
      } catch (Throwable var11) {
         CrashReport crashReport = CrashReport.forThrowable(var11, "Tesselating block in world");
         CrashReportCategory crashReportCategory = crashReport.addCategory("Block being tesselated");
         CrashReportCategory.populateBlockDetails(crashReportCategory, level, pos, state);
         throw new ReportedException(crashReport);
      }
   }

   public void renderLiquid(BlockPos pos, BlockAndTintGetter level, VertexConsumer consumer, BlockState blockState, FluidState fluidState) {
      try {
         Objects.requireNonNull(this.liquidBlockRenderer).tesselate(level, pos, consumer, blockState, fluidState);
      } catch (Throwable var9) {
         CrashReport crashReport = CrashReport.forThrowable(var9, "Tesselating liquid in world");
         CrashReportCategory crashReportCategory = crashReport.addCategory("Block being tesselated");
         CrashReportCategory.populateBlockDetails(crashReportCategory, level, pos, blockState);
         throw new ReportedException(crashReport);
      }
   }

   public ModelBlockRenderer getModelRenderer() {
      return this.modelRenderer;
   }

   public BlockStateModel getBlockModel(BlockState state) {
      return this.blockModelShaper.getBlockModel(state);
   }

   public void renderSingleBlock(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
      RenderShape renderShape = state.getRenderShape();
      if (renderShape != RenderShape.INVISIBLE) {
         BlockStateModel blockStateModel = this.getBlockModel(state);
         int i = this.blockColors.getColor(state, null, null, 0);
         float f = (i >> 16 & 0xFF) / 255.0F;
         float g = (i >> 8 & 0xFF) / 255.0F;
         float h = (i & 0xFF) / 255.0F;
         ModelBlockRenderer.renderModel(
            poseStack.last(), bufferSource.getBuffer(ItemBlockRenderTypes.getRenderType(state)), blockStateModel, f, g, h, packedLight, packedOverlay
         );
      }
   }

   @Override
   public void onResourceManagerReload(ResourceManager resourceManager) {
      this.liquidBlockRenderer = new LiquidBlockRenderer(this.materials);
   }
}
