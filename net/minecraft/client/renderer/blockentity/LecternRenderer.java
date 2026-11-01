package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.LecternRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class LecternRenderer implements BlockEntityRenderer<LecternBlockEntity, LecternRenderState> {
   private final MaterialSet materials;
   private final BookModel bookModel;
   private final BookModel.State bookState = new BookModel.State(0.0F, 0.1F, 0.9F, 1.2F);

   public LecternRenderer(BlockEntityRendererProvider.Context p_173621_) {
      this.materials = p_173621_.materials();
      this.bookModel = new BookModel(p_173621_.bakeLayer(ModelLayers.BOOK));
   }

   public LecternRenderState createRenderState() {
      return new LecternRenderState();
   }

   public void extractRenderState(
      LecternBlockEntity p_447206_, LecternRenderState p_445480_, float p_445502_, Vec3 p_446398_, @Nullable ModelFeatureRenderer.CrumblingOverlay p_446019_
   ) {
      BlockEntityRenderer.super.extractRenderState(p_447206_, p_445480_, p_445502_, p_446398_, p_446019_);
      p_445480_.hasBook = p_447206_.getBlockState().getValue(LecternBlock.HAS_BOOK);
      p_445480_.yRot = p_447206_.getBlockState().getValue(LecternBlock.FACING).getClockWise().toYRot();
   }

   public void submit(LecternRenderState p_445746_, PoseStack p_439985_, SubmitNodeCollector p_439033_, CameraRenderState p_451358_) {
      if (p_445746_.hasBook) {
         p_439985_.pushPose();
         p_439985_.translate(0.5F, 1.0625F, 0.5F);
         p_439985_.mulPose(Axis.YP.rotationDegrees(-p_445746_.yRot));
         p_439985_.mulPose(Axis.ZP.rotationDegrees(67.5F));
         p_439985_.translate(0.0F, -0.125F, 0.0F);
         p_439033_.submitModel(
            this.bookModel,
            this.bookState,
            p_439985_,
            EnchantTableRenderer.BOOK_TEXTURE.renderType(RenderTypes::entitySolid),
            p_445746_.lightCoords,
            OverlayTexture.NO_OVERLAY,
            -1,
            this.materials.get(EnchantTableRenderer.BOOK_TEXTURE),
            0,
            p_445746_.breakProgress
         );
         p_439985_.popPose();
      }
   }

   public AABB getRenderBoundingBox(LecternBlockEntity blockEntity) {
      BlockPos pos = blockEntity.getBlockPos();
      return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 1.5, pos.getZ() + 1.0);
   }
}
