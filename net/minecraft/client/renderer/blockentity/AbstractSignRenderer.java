package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractSignRenderer implements BlockEntityRenderer<SignBlockEntity, SignRenderState> {
   private static final int BLACK_TEXT_OUTLINE_COLOR = -988212;
   private static final int OUTLINE_RENDER_DISTANCE = Mth.square(16);
   private final Font font;
   private final MaterialSet materials;

   public AbstractSignRenderer(BlockEntityRendererProvider.Context p_389447_) {
      this.font = p_389447_.font();
      this.materials = p_389447_.materials();
   }

   protected abstract Model.Simple getSignModel(BlockState var1, WoodType var2);

   protected abstract Material getSignMaterial(WoodType var1);

   protected abstract float getSignModelRenderScale();

   protected abstract float getSignTextRenderScale();

   protected abstract Vec3 getTextOffset();

   protected abstract void translateSign(PoseStack var1, float var2, BlockState var3);

   public void submit(SignRenderState p_446471_, PoseStack p_440400_, SubmitNodeCollector p_439180_, CameraRenderState p_450989_) {
      BlockState blockstate = p_446471_.blockState;
      SignBlock signblock = (SignBlock)blockstate.getBlock();
      Model.Simple model$simple = this.getSignModel(blockstate, signblock.type());
      this.submitSignWithText(p_446471_, p_440400_, blockstate, signblock, signblock.type(), model$simple, p_446471_.breakProgress, p_439180_);
   }

   private void submitSignWithText(
      SignRenderState p_445829_,
      PoseStack p_440527_,
      BlockState p_439522_,
      SignBlock p_440419_,
      WoodType p_439397_,
      Model.Simple p_439044_,
      @Nullable ModelFeatureRenderer.CrumblingOverlay p_440491_,
      SubmitNodeCollector p_440535_
   ) {
      p_440527_.pushPose();
      this.translateSign(p_440527_, -p_440419_.getYRotationDegrees(p_439522_), p_439522_);
      this.submitSign(p_440527_, p_445829_.lightCoords, p_439397_, p_439044_, p_440491_, p_440535_);
      this.submitSignText(p_445829_, p_440527_, p_440535_, true);
      this.submitSignText(p_445829_, p_440527_, p_440535_, false);
      p_440527_.popPose();
   }

   protected void submitSign(
      PoseStack p_440481_,
      int p_440082_,
      WoodType p_439943_,
      Model.Simple p_440270_,
      @Nullable ModelFeatureRenderer.CrumblingOverlay p_439834_,
      SubmitNodeCollector p_440131_
   ) {
      p_440481_.pushPose();
      float f = this.getSignModelRenderScale();
      p_440481_.scale(f, -f, -f);
      Material material = this.getSignMaterial(p_439943_);
      RenderType rendertype = material.renderType(p_440270_::renderType);
      p_440131_.submitModel(
         p_440270_, Unit.INSTANCE, p_440481_, rendertype, p_440082_, OverlayTexture.NO_OVERLAY, -1, this.materials.get(material), 0, p_439834_
      );
      p_440481_.popPose();
   }

   private void submitSignText(SignRenderState p_445553_, PoseStack p_439568_, SubmitNodeCollector p_438891_, boolean p_439429_) {
      SignText signtext = p_439429_ ? p_445553_.frontText : p_445553_.backText;
      if (signtext != null) {
         p_439568_.pushPose();
         this.translateSignText(p_439568_, p_439429_, this.getTextOffset());
         int i = getDarkColor(signtext);
         int j = 4 * p_445553_.textLineHeight / 2;
         FormattedCharSequence[] aformattedcharsequence = signtext.getRenderMessages(p_445553_.isTextFilteringEnabled, p_445219_ -> {
            List<FormattedCharSequence> list = this.font.split(p_445219_, p_445553_.maxTextLineWidth);
            return list.isEmpty() ? FormattedCharSequence.EMPTY : list.get(0);
         });
         int k;
         boolean flag;
         int l;
         if (!signtext.hasGlowingText()) {
            k = i;
            flag = false;
            l = p_445553_.lightCoords;
         } else {
            k = signtext.getColor().getTextColor();
            flag = k == DyeColor.BLACK.getTextColor() || p_445553_.drawOutline;
            l = 15728880;
         }

         for (int i1 = 0; i1 < 4; i1++) {
            FormattedCharSequence formattedcharsequence = aformattedcharsequence[i1];
            float f = -this.font.width(formattedcharsequence) / 2;
            p_438891_.submitText(
               p_439568_, f, i1 * p_445553_.textLineHeight - j, formattedcharsequence, false, Font.DisplayMode.POLYGON_OFFSET, l, k, 0, flag ? i : 0
            );
         }

         p_439568_.popPose();
      }
   }

   private void translateSignText(PoseStack p_389535_, boolean p_389461_, Vec3 p_389589_) {
      if (!p_389461_) {
         p_389535_.mulPose(Axis.YP.rotationDegrees(180.0F));
      }

      float f = 0.015625F * this.getSignTextRenderScale();
      p_389535_.translate(p_389589_);
      p_389535_.scale(f, -f, f);
   }

   private static boolean isOutlineVisible(BlockPos p_389438_) {
      Minecraft minecraft = Minecraft.getInstance();
      LocalPlayer localplayer = minecraft.player;
      if (localplayer != null && minecraft.options.getCameraType().isFirstPerson() && localplayer.isScoping()) {
         return true;
      } else {
         Entity entity = minecraft.getCameraEntity();
         return entity != null && entity.distanceToSqr(Vec3.atCenterOf(p_389438_)) < OUTLINE_RENDER_DISTANCE;
      }
   }

   public static int getDarkColor(SignText p_389470_) {
      int i = p_389470_.getColor().getTextColor();
      return i == DyeColor.BLACK.getTextColor() && p_389470_.hasGlowingText() ? -988212 : ARGB.scaleRGB(i, 0.4F);
   }

   public SignRenderState createRenderState() {
      return new SignRenderState();
   }

   public void extractRenderState(
      SignBlockEntity p_446457_, SignRenderState p_446340_, float p_445525_, Vec3 p_445758_, @Nullable ModelFeatureRenderer.CrumblingOverlay p_446708_
   ) {
      BlockEntityRenderer.super.extractRenderState(p_446457_, p_446340_, p_445525_, p_445758_, p_446708_);
      p_446340_.maxTextLineWidth = p_446457_.getMaxTextLineWidth();
      p_446340_.textLineHeight = p_446457_.getTextLineHeight();
      p_446340_.frontText = p_446457_.getFrontText();
      p_446340_.backText = p_446457_.getBackText();
      p_446340_.isTextFilteringEnabled = Minecraft.getInstance().isTextFilteringEnabled();
      p_446340_.drawOutline = isOutlineVisible(p_446457_.getBlockPos());
   }

   public AABB getRenderBoundingBox(SignBlockEntity blockEntity) {
      if (blockEntity.getBlockState().getBlock() instanceof StandingSignBlock) {
         BlockPos pos = blockEntity.getBlockPos();
         return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 1.125, pos.getZ() + 1.0);
      } else {
         return BlockEntityRenderer.super.getRenderBoundingBox(blockEntity);
      }
   }
}
