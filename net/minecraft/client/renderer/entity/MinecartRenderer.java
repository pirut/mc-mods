package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.MinecartModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MinecartRenderer<T extends AbstractMinecart> extends EntityRenderer<T> {
   private static final ResourceLocation MINECART_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/minecart.png");
   protected final EntityModel<T> model;
   private final BlockRenderDispatcher blockRenderer;

   public MinecartRenderer(EntityRendererProvider.Context p_174300_, ModelLayerLocation p_174301_) {
      super(p_174300_);
      this.shadowRadius = 0.7F;
      this.model = new MinecartModel<>(p_174300_.bakeLayer(p_174301_));
      this.blockRenderer = p_174300_.getBlockRenderDispatcher();
   }

   public void render(T p_115418_, float p_115419_, float p_115420_, PoseStack p_115421_, MultiBufferSource p_115422_, int p_115423_) {
      super.render(p_115418_, p_115419_, p_115420_, p_115421_, p_115422_, p_115423_);
      p_115421_.pushPose();
      long $$6 = p_115418_.getId() * 493286711L;
      $$6 = $$6 * $$6 * 4392167121L + $$6 * 98761L;
      float $$7 = (((float)($$6 >> 16 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
      float $$8 = (((float)($$6 >> 20 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
      float $$9 = (((float)($$6 >> 24 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
      p_115421_.translate($$7, $$8, $$9);
      double $$10 = Mth.lerp((double)p_115420_, p_115418_.xOld, p_115418_.getX());
      double $$11 = Mth.lerp((double)p_115420_, p_115418_.yOld, p_115418_.getY());
      double $$12 = Mth.lerp((double)p_115420_, p_115418_.zOld, p_115418_.getZ());
      double $$13 = 0.3F;
      Vec3 $$14 = p_115418_.getPos($$10, $$11, $$12);
      float $$15 = Mth.lerp(p_115420_, p_115418_.xRotO, p_115418_.getXRot());
      if ($$14 != null) {
         Vec3 $$16 = p_115418_.getPosOffs($$10, $$11, $$12, 0.3F);
         Vec3 $$17 = p_115418_.getPosOffs($$10, $$11, $$12, -0.3F);
         if ($$16 == null) {
            $$16 = $$14;
         }

         if ($$17 == null) {
            $$17 = $$14;
         }

         p_115421_.translate($$14.x - $$10, ($$16.y + $$17.y) / 2.0 - $$11, $$14.z - $$12);
         Vec3 $$18 = $$17.add(-$$16.x, -$$16.y, -$$16.z);
         if ($$18.length() != 0.0) {
            $$18 = $$18.normalize();
            p_115419_ = (float)(Math.atan2($$18.z, $$18.x) * 180.0 / Math.PI);
            $$15 = (float)(Math.atan($$18.y) * 73.0);
         }
      }

      p_115421_.translate(0.0F, 0.375F, 0.0F);
      p_115421_.mulPose(Axis.YP.rotationDegrees(180.0F - p_115419_));
      p_115421_.mulPose(Axis.ZP.rotationDegrees(-$$15));
      float $$19 = p_115418_.getHurtTime() - p_115420_;
      float $$20 = p_115418_.getDamage() - p_115420_;
      if ($$20 < 0.0F) {
         $$20 = 0.0F;
      }

      if ($$19 > 0.0F) {
         p_115421_.mulPose(Axis.XP.rotationDegrees(Mth.sin($$19) * $$19 * $$20 / 10.0F * p_115418_.getHurtDir()));
      }

      int $$21 = p_115418_.getDisplayOffset();
      BlockState $$22 = p_115418_.getDisplayBlockState();
      if ($$22.getRenderShape() != RenderShape.INVISIBLE) {
         p_115421_.pushPose();
         float $$23 = 0.75F;
         p_115421_.scale(0.75F, 0.75F, 0.75F);
         p_115421_.translate(-0.5F, ($$21 - 8) / 16.0F, 0.5F);
         p_115421_.mulPose(Axis.YP.rotationDegrees(90.0F));
         this.renderMinecartContents(p_115418_, p_115420_, $$22, p_115421_, p_115422_, p_115423_);
         p_115421_.popPose();
      }

      p_115421_.scale(-1.0F, -1.0F, 1.0F);
      this.model.setupAnim(p_115418_, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      VertexConsumer $$24 = p_115422_.getBuffer(this.model.renderType(this.getTextureLocation(p_115418_)));
      this.model.renderToBuffer(p_115421_, $$24, p_115423_, OverlayTexture.NO_OVERLAY);
      p_115421_.popPose();
   }

   public ResourceLocation getTextureLocation(T p_115416_) {
      return MINECART_LOCATION;
   }

   protected void renderMinecartContents(T p_115424_, float p_115425_, BlockState p_115426_, PoseStack p_115427_, MultiBufferSource p_115428_, int p_115429_) {
      this.blockRenderer.renderSingleBlock(p_115426_, p_115427_, p_115428_, p_115429_, OverlayTexture.NO_OVERLAY);
   }
}
