package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.PaintingTextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PaintingRenderer extends EntityRenderer<Painting> {
   public PaintingRenderer(EntityRendererProvider.Context p_174332_) {
      super(p_174332_);
   }

   public void render(Painting p_115552_, float p_115553_, float p_115554_, PoseStack p_115555_, MultiBufferSource p_115556_, int p_115557_) {
      p_115555_.pushPose();
      p_115555_.mulPose(Axis.YP.rotationDegrees(180.0F - p_115553_));
      PaintingVariant $$6 = p_115552_.getVariant().value();
      VertexConsumer $$7 = p_115556_.getBuffer(RenderType.entitySolid(this.getTextureLocation(p_115552_)));
      PaintingTextureManager $$8 = Minecraft.getInstance().getPaintingTextures();
      this.renderPainting(p_115555_, $$7, p_115552_, $$6.width(), $$6.height(), $$8.get($$6), $$8.getBackSprite());
      p_115555_.popPose();
      super.render(p_115552_, p_115553_, p_115554_, p_115555_, p_115556_, p_115557_);
   }

   public ResourceLocation getTextureLocation(Painting p_115550_) {
      return Minecraft.getInstance().getPaintingTextures().getBackSprite().atlasLocation();
   }

   private void renderPainting(
      PoseStack p_115559_,
      VertexConsumer p_115560_,
      Painting p_115561_,
      int p_115562_,
      int p_115563_,
      TextureAtlasSprite p_115564_,
      TextureAtlasSprite p_115565_
   ) {
      PoseStack.Pose $$7 = p_115559_.last();
      float $$8 = -p_115562_ / 2.0F;
      float $$9 = -p_115563_ / 2.0F;
      float $$10 = 0.03125F;
      float $$11 = p_115565_.getU0();
      float $$12 = p_115565_.getU1();
      float $$13 = p_115565_.getV0();
      float $$14 = p_115565_.getV1();
      float $$15 = p_115565_.getU0();
      float $$16 = p_115565_.getU1();
      float $$17 = p_115565_.getV0();
      float $$18 = p_115565_.getV(0.0625F);
      float $$19 = p_115565_.getU0();
      float $$20 = p_115565_.getU(0.0625F);
      float $$21 = p_115565_.getV0();
      float $$22 = p_115565_.getV1();
      double $$23 = 1.0 / p_115562_;
      double $$24 = 1.0 / p_115563_;

      for (int $$25 = 0; $$25 < p_115562_; $$25++) {
         for (int $$26 = 0; $$26 < p_115563_; $$26++) {
            float $$27 = $$8 + ($$25 + 1);
            float $$28 = $$8 + $$25;
            float $$29 = $$9 + ($$26 + 1);
            float $$30 = $$9 + $$26;
            int $$31 = p_115561_.getBlockX();
            int $$32 = Mth.floor(p_115561_.getY() + ($$29 + $$30) / 2.0F);
            int $$33 = p_115561_.getBlockZ();
            Direction $$34 = p_115561_.getDirection();
            if ($$34 == Direction.NORTH) {
               $$31 = Mth.floor(p_115561_.getX() + ($$27 + $$28) / 2.0F);
            }

            if ($$34 == Direction.WEST) {
               $$33 = Mth.floor(p_115561_.getZ() - ($$27 + $$28) / 2.0F);
            }

            if ($$34 == Direction.SOUTH) {
               $$31 = Mth.floor(p_115561_.getX() - ($$27 + $$28) / 2.0F);
            }

            if ($$34 == Direction.EAST) {
               $$33 = Mth.floor(p_115561_.getZ() + ($$27 + $$28) / 2.0F);
            }

            int $$35 = LevelRenderer.getLightColor(p_115561_.level(), new BlockPos($$31, $$32, $$33));
            float $$36 = p_115564_.getU((float)($$23 * (p_115562_ - $$25)));
            float $$37 = p_115564_.getU((float)($$23 * (p_115562_ - ($$25 + 1))));
            float $$38 = p_115564_.getV((float)($$24 * (p_115563_ - $$26)));
            float $$39 = p_115564_.getV((float)($$24 * (p_115563_ - ($$26 + 1))));
            this.vertex($$7, p_115560_, $$27, $$30, $$37, $$38, -0.03125F, 0, 0, -1, $$35);
            this.vertex($$7, p_115560_, $$28, $$30, $$36, $$38, -0.03125F, 0, 0, -1, $$35);
            this.vertex($$7, p_115560_, $$28, $$29, $$36, $$39, -0.03125F, 0, 0, -1, $$35);
            this.vertex($$7, p_115560_, $$27, $$29, $$37, $$39, -0.03125F, 0, 0, -1, $$35);
            this.vertex($$7, p_115560_, $$27, $$29, $$12, $$13, 0.03125F, 0, 0, 1, $$35);
            this.vertex($$7, p_115560_, $$28, $$29, $$11, $$13, 0.03125F, 0, 0, 1, $$35);
            this.vertex($$7, p_115560_, $$28, $$30, $$11, $$14, 0.03125F, 0, 0, 1, $$35);
            this.vertex($$7, p_115560_, $$27, $$30, $$12, $$14, 0.03125F, 0, 0, 1, $$35);
            this.vertex($$7, p_115560_, $$27, $$29, $$15, $$17, -0.03125F, 0, 1, 0, $$35);
            this.vertex($$7, p_115560_, $$28, $$29, $$16, $$17, -0.03125F, 0, 1, 0, $$35);
            this.vertex($$7, p_115560_, $$28, $$29, $$16, $$18, 0.03125F, 0, 1, 0, $$35);
            this.vertex($$7, p_115560_, $$27, $$29, $$15, $$18, 0.03125F, 0, 1, 0, $$35);
            this.vertex($$7, p_115560_, $$27, $$30, $$15, $$17, 0.03125F, 0, -1, 0, $$35);
            this.vertex($$7, p_115560_, $$28, $$30, $$16, $$17, 0.03125F, 0, -1, 0, $$35);
            this.vertex($$7, p_115560_, $$28, $$30, $$16, $$18, -0.03125F, 0, -1, 0, $$35);
            this.vertex($$7, p_115560_, $$27, $$30, $$15, $$18, -0.03125F, 0, -1, 0, $$35);
            this.vertex($$7, p_115560_, $$27, $$29, $$20, $$21, 0.03125F, -1, 0, 0, $$35);
            this.vertex($$7, p_115560_, $$27, $$30, $$20, $$22, 0.03125F, -1, 0, 0, $$35);
            this.vertex($$7, p_115560_, $$27, $$30, $$19, $$22, -0.03125F, -1, 0, 0, $$35);
            this.vertex($$7, p_115560_, $$27, $$29, $$19, $$21, -0.03125F, -1, 0, 0, $$35);
            this.vertex($$7, p_115560_, $$28, $$29, $$20, $$21, -0.03125F, 1, 0, 0, $$35);
            this.vertex($$7, p_115560_, $$28, $$30, $$20, $$22, -0.03125F, 1, 0, 0, $$35);
            this.vertex($$7, p_115560_, $$28, $$30, $$19, $$22, 0.03125F, 1, 0, 0, $$35);
            this.vertex($$7, p_115560_, $$28, $$29, $$19, $$21, 0.03125F, 1, 0, 0, $$35);
         }
      }
   }

   private void vertex(
      PoseStack.Pose p_323991_,
      VertexConsumer p_254114_,
      float p_254164_,
      float p_254459_,
      float p_254183_,
      float p_253615_,
      float p_254448_,
      int p_253660_,
      int p_254342_,
      int p_253757_,
      int p_254101_
   ) {
      p_254114_.addVertex(p_323991_, p_254164_, p_254459_, p_254448_)
         .setColor(-1)
         .setUv(p_254183_, p_253615_)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(p_254101_)
         .setNormal(p_323991_, p_253660_, p_254342_, p_253757_);
   }
}
