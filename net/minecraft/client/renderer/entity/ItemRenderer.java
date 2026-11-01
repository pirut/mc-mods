package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.math.MatrixUtil;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemRenderer {
   public static final Identifier ENCHANTED_GLINT_ARMOR = Identifier.withDefaultNamespace("textures/misc/enchanted_glint_armor.png");
   public static final Identifier ENCHANTED_GLINT_ITEM = Identifier.withDefaultNamespace("textures/misc/enchanted_glint_item.png");
   public static final float SPECIAL_FOIL_UI_SCALE = 0.5F;
   public static final float SPECIAL_FOIL_FIRST_PERSON_SCALE = 0.75F;
   public static final float SPECIAL_FOIL_TEXTURE_SCALE = 0.0078125F;
   public static final int NO_TINT = -1;

   public static void renderItem(
      ItemDisplayContext p_361627_,
      PoseStack p_360423_,
      MultiBufferSource p_360415_,
      int p_361265_,
      int p_364771_,
      int[] p_386517_,
      List<BakedQuad> p_404702_,
      RenderType p_459022_,
      ItemStackRenderState.FoilType p_387026_
   ) {
      VertexConsumer vertexconsumer;
      if (p_387026_ == ItemStackRenderState.FoilType.SPECIAL) {
         PoseStack.Pose posestack$pose = p_360423_.last().copy();
         if (p_361627_ == ItemDisplayContext.GUI) {
            MatrixUtil.mulComponentWise(posestack$pose.pose(), 0.5F);
         } else if (p_361627_.firstPerson()) {
            MatrixUtil.mulComponentWise(posestack$pose.pose(), 0.75F);
         }

         vertexconsumer = getSpecialFoilBuffer(p_360415_, p_459022_, posestack$pose);
      } else {
         vertexconsumer = getFoilBuffer(p_360415_, p_459022_, true, p_387026_ != ItemStackRenderState.FoilType.NONE);
      }

      renderQuadList(p_360423_, vertexconsumer, p_404702_, p_386517_, p_361265_, p_364771_);
   }

   private static VertexConsumer getSpecialFoilBuffer(MultiBufferSource p_418260_, RenderType p_459085_, PoseStack.Pose p_418129_) {
      return VertexMultiConsumer.create(
         new SheetedDecalTextureGenerator(
            p_418260_.getBuffer(useTransparentGlint(p_459085_) ? RenderTypes.glintTranslucent() : RenderTypes.glint()), p_418129_, 0.0078125F
         ),
         p_418260_.getBuffer(p_459085_)
      );
   }

   public static VertexConsumer getFoilBuffer(MultiBufferSource p_115212_, RenderType p_459194_, boolean p_115214_, boolean p_115215_) {
      if (p_115215_) {
         return useTransparentGlint(p_459194_)
            ? VertexMultiConsumer.create(p_115212_.getBuffer(RenderTypes.glintTranslucent()), p_115212_.getBuffer(p_459194_))
            : VertexMultiConsumer.create(p_115212_.getBuffer(p_115214_ ? RenderTypes.glint() : RenderTypes.entityGlint()), p_115212_.getBuffer(p_459194_));
      } else {
         return p_115212_.getBuffer(p_459194_);
      }
   }

   public static List<RenderType> getFoilRenderTypes(RenderType p_459012_, boolean p_435513_, boolean p_434766_) {
      if (p_434766_) {
         return useTransparentGlint(p_459012_)
            ? List.of(p_459012_, RenderTypes.glintTranslucent())
            : List.of(p_459012_, p_435513_ ? RenderTypes.glint() : RenderTypes.entityGlint());
      } else {
         return List.of(p_459012_);
      }
   }

   private static boolean useTransparentGlint(RenderType p_459084_) {
      return Minecraft.useShaderTransparency() && (p_459084_ == Sheets.translucentItemSheet() || p_459084_ == Sheets.translucentBlockItemSheet());
   }

   private static int getLayerColorSafe(int[] p_387884_, int p_388524_) {
      return p_388524_ >= 0 && p_388524_ < p_387884_.length ? p_387884_[p_388524_] : -1;
   }

   private static void renderQuadList(PoseStack p_115163_, VertexConsumer p_115164_, List<BakedQuad> p_115165_, int[] p_387305_, int p_115167_, int p_115168_) {
      PoseStack.Pose posestack$pose = p_115163_.last();

      for (BakedQuad bakedquad : p_115165_) {
         float f;
         float f1;
         float f2;
         float f3;
         if (bakedquad.isTinted()) {
            int i = getLayerColorSafe(p_387305_, bakedquad.tintIndex());
            f = ARGB.alpha(i) / 255.0F;
            f1 = ARGB.red(i) / 255.0F;
            f2 = ARGB.green(i) / 255.0F;
            f3 = ARGB.blue(i) / 255.0F;
         } else {
            f = 1.0F;
            f1 = 1.0F;
            f2 = 1.0F;
            f3 = 1.0F;
         }

         p_115164_.putBulkData(posestack$pose, bakedquad, f1, f2, f3, f, p_115167_, p_115168_);
      }
   }
}
