package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public abstract class AbstractSignRenderer implements BlockEntityRenderer<SignBlockEntity, SignRenderState> {
   private static final int BLACK_TEXT_OUTLINE_COLOR = -988212;
   private static final int OUTLINE_RENDER_DISTANCE = Mth.square(16);
   private final Font font;
   private final MaterialSet materials;

   public AbstractSignRenderer(BlockEntityRendererProvider.Context context) {
      this.font = context.font();
      this.materials = context.materials();
   }

   protected abstract Model.Simple getSignModel(BlockState blockState, WoodType woodType);

   protected abstract Material getSignMaterial(WoodType woodType);

   protected abstract float getSignModelRenderScale();

   protected abstract float getSignTextRenderScale();

   protected abstract Vec3 getTextOffset();

   protected abstract void translateSign(PoseStack poseStack, float yRot, BlockState state);

   public void submit(SignRenderState signRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
      BlockState blockState = signRenderState.blockState;
      SignBlock signBlock = (SignBlock)blockState.getBlock();
      Model.Simple simple = this.getSignModel(blockState, signBlock.type());
      this.submitSignWithText(signRenderState, poseStack, blockState, signBlock, signBlock.type(), simple, signRenderState.breakProgress, submitNodeCollector);
   }

   private void submitSignWithText(
      SignRenderState renderState,
      PoseStack poseStack,
      BlockState blockState,
      SignBlock sign,
      WoodType woodType,
      Model.Simple model,
      @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
      SubmitNodeCollector nodeCollector
   ) {
      poseStack.pushPose();
      this.translateSign(poseStack, -sign.getYRotationDegrees(blockState), blockState);
      this.submitSign(poseStack, renderState.lightCoords, woodType, model, crumblingOverlay, nodeCollector);
      this.submitSignText(renderState, poseStack, nodeCollector, true);
      this.submitSignText(renderState, poseStack, nodeCollector, false);
      poseStack.popPose();
   }

   protected void submitSign(
      PoseStack poseStack,
      int packedLight,
      WoodType woodType,
      Model.Simple model,
      @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
      SubmitNodeCollector nodeCollector
   ) {
      poseStack.pushPose();
      float f = this.getSignModelRenderScale();
      poseStack.scale(f, -f, -f);
      Material material = this.getSignMaterial(woodType);
      RenderType renderType = material.renderType(model::renderType);
      nodeCollector.submitModel(
         model, Unit.INSTANCE, poseStack, renderType, packedLight, OverlayTexture.NO_OVERLAY, -1, this.materials.get(material), 0, crumblingOverlay
      );
      poseStack.popPose();
   }

   private void submitSignText(SignRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, boolean isFront) {
      SignText signText = isFront ? renderState.frontText : renderState.backText;
      if (signText != null) {
         poseStack.pushPose();
         this.translateSignText(poseStack, isFront, this.getTextOffset());
         int i = getDarkColor(signText);
         int j = 4 * renderState.textLineHeight / 2;
         FormattedCharSequence[] formattedCharSequences = signText.getRenderMessages(renderState.isTextFilteringEnabled, component -> {
            List<FormattedCharSequence> list = this.font.split(component, renderState.maxTextLineWidth);
            return list.isEmpty() ? FormattedCharSequence.EMPTY : list.get(0);
         });
         int k;
         boolean bl;
         int l;
         if (signText.hasGlowingText()) {
            k = signText.getColor().getTextColor();
            bl = k == DyeColor.BLACK.getTextColor() || renderState.drawOutline;
            l = 15728880;
         } else {
            k = i;
            bl = false;
            l = renderState.lightCoords;
         }

         for (int m = 0; m < 4; m++) {
            FormattedCharSequence formattedCharSequence = formattedCharSequences[m];
            float f = -this.font.width(formattedCharSequence) / 2;
            nodeCollector.submitText(
               poseStack, f, m * renderState.textLineHeight - j, formattedCharSequence, false, Font.DisplayMode.POLYGON_OFFSET, l, k, 0, bl ? i : 0
            );
         }

         poseStack.popPose();
      }
   }

   private void translateSignText(PoseStack poseStack, boolean isFront, Vec3 offset) {
      if (!isFront) {
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
      }

      float f = 0.015625F * this.getSignTextRenderScale();
      poseStack.translate(offset);
      poseStack.scale(f, -f, f);
   }

   private static boolean isOutlineVisible(BlockPos pos) {
      Minecraft minecraft = Minecraft.getInstance();
      LocalPlayer localPlayer = minecraft.player;
      if (localPlayer != null && minecraft.options.getCameraType().isFirstPerson() && localPlayer.isScoping()) {
         return true;
      } else {
         Entity entity = minecraft.getCameraEntity();
         return entity != null && entity.distanceToSqr(Vec3.atCenterOf(pos)) < OUTLINE_RENDER_DISTANCE;
      }
   }

   public static int getDarkColor(SignText text) {
      int i = text.getColor().getTextColor();
      return i == DyeColor.BLACK.getTextColor() && text.hasGlowingText() ? -988212 : ARGB.scaleRGB(i, 0.4F);
   }

   public SignRenderState createRenderState() {
      return new SignRenderState();
   }

   public void extractRenderState(
      SignBlockEntity signBlockEntity, SignRenderState signRenderState, float f, Vec3 vec3, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
   ) {
      BlockEntityRenderer.super.extractRenderState(signBlockEntity, signRenderState, f, vec3, crumblingOverlay);
      signRenderState.maxTextLineWidth = signBlockEntity.getMaxTextLineWidth();
      signRenderState.textLineHeight = signBlockEntity.getTextLineHeight();
      signRenderState.frontText = signBlockEntity.getFrontText();
      signRenderState.backText = signBlockEntity.getBackText();
      signRenderState.isTextFilteringEnabled = Minecraft.getInstance().isTextFilteringEnabled();
      signRenderState.drawOutline = isOutlineVisible(signBlockEntity.getBlockPos());
   }
}
