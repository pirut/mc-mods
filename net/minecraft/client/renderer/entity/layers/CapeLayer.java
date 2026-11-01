package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CapeLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   public CapeLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> p_116602_) {
      super(p_116602_);
   }

   public void render(
      PoseStack p_116615_,
      MultiBufferSource p_116616_,
      int p_116617_,
      AbstractClientPlayer p_116618_,
      float p_116619_,
      float p_116620_,
      float p_116621_,
      float p_116622_,
      float p_116623_,
      float p_116624_
   ) {
      if (!p_116618_.isInvisible() && p_116618_.isModelPartShown(PlayerModelPart.CAPE)) {
         PlayerSkin $$10 = p_116618_.getSkin();
         if ($$10.capeTexture() != null) {
            ItemStack $$11 = p_116618_.getItemBySlot(EquipmentSlot.CHEST);
            if (!$$11.is(Items.ELYTRA)) {
               p_116615_.pushPose();
               p_116615_.translate(0.0F, 0.0F, 0.125F);
               double $$12 = Mth.lerp((double)p_116621_, p_116618_.xCloakO, p_116618_.xCloak) - Mth.lerp((double)p_116621_, p_116618_.xo, p_116618_.getX());
               double $$13 = Mth.lerp((double)p_116621_, p_116618_.yCloakO, p_116618_.yCloak) - Mth.lerp((double)p_116621_, p_116618_.yo, p_116618_.getY());
               double $$14 = Mth.lerp((double)p_116621_, p_116618_.zCloakO, p_116618_.zCloak) - Mth.lerp((double)p_116621_, p_116618_.zo, p_116618_.getZ());
               float $$15 = Mth.rotLerp(p_116621_, p_116618_.yBodyRotO, p_116618_.yBodyRot);
               double $$16 = Mth.sin($$15 * (float) (Math.PI / 180.0));
               double $$17 = -Mth.cos($$15 * (float) (Math.PI / 180.0));
               float $$18 = (float)$$13 * 10.0F;
               $$18 = Mth.clamp($$18, -6.0F, 32.0F);
               float $$19 = (float)($$12 * $$16 + $$14 * $$17) * 100.0F;
               $$19 = Mth.clamp($$19, 0.0F, 150.0F);
               float $$20 = (float)($$12 * $$17 - $$14 * $$16) * 100.0F;
               $$20 = Mth.clamp($$20, -20.0F, 20.0F);
               if ($$19 < 0.0F) {
                  $$19 = 0.0F;
               }

               float $$21 = Mth.lerp(p_116621_, p_116618_.oBob, p_116618_.bob);
               $$18 += Mth.sin(Mth.lerp(p_116621_, p_116618_.walkDistO, p_116618_.walkDist) * 6.0F) * 32.0F * $$21;
               if (p_116618_.isCrouching()) {
                  $$18 += 25.0F;
               }

               p_116615_.mulPose(Axis.XP.rotationDegrees(6.0F + $$19 / 2.0F + $$18));
               p_116615_.mulPose(Axis.ZP.rotationDegrees($$20 / 2.0F));
               p_116615_.mulPose(Axis.YP.rotationDegrees(180.0F - $$20 / 2.0F));
               VertexConsumer $$22 = p_116616_.getBuffer(RenderType.entitySolid($$10.capeTexture()));
               this.getParentModel().renderCloak(p_116615_, $$22, p_116617_, OverlayTexture.NO_OVERLAY);
               p_116615_.popPose();
            }
         }
      }
   }
}
