package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpinAttackEffectLayer<T extends LivingEntity> extends RenderLayer<T, PlayerModel<T>> {
   public static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/trident_riptide.png");
   public static final String BOX = "box";
   private final ModelPart box;

   public SpinAttackEffectLayer(RenderLayerParent<T, PlayerModel<T>> p_174540_, EntityModelSet p_174541_) {
      super(p_174540_);
      ModelPart $$2 = p_174541_.bakeLayer(ModelLayers.PLAYER_SPIN_ATTACK);
      this.box = $$2.getChild("box");
   }

   public static LayerDefinition createLayer() {
      MeshDefinition $$0 = new MeshDefinition();
      PartDefinition $$1 = $$0.getRoot();
      $$1.addOrReplaceChild("box", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -16.0F, -8.0F, 16.0F, 32.0F, 16.0F), PartPose.ZERO);
      return LayerDefinition.create($$0, 64, 64);
   }

   public void render(
      PoseStack p_117526_,
      MultiBufferSource p_117527_,
      int p_117528_,
      T p_117529_,
      float p_117530_,
      float p_117531_,
      float p_117532_,
      float p_117533_,
      float p_117534_,
      float p_117535_
   ) {
      if (p_117529_.isAutoSpinAttack()) {
         VertexConsumer $$10 = p_117527_.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

         for (int $$11 = 0; $$11 < 3; $$11++) {
            p_117526_.pushPose();
            float $$12 = p_117533_ * -(45 + $$11 * 5);
            p_117526_.mulPose(Axis.YP.rotationDegrees($$12));
            float $$13 = 0.75F * $$11;
            p_117526_.scale($$13, $$13, $$13);
            p_117526_.translate(0.0F, -0.2F + 0.6F * $$11, 0.0F);
            this.box.render(p_117526_, $$10, p_117528_, OverlayTexture.NO_OVERLAY);
            p_117526_.popPose();
         }
      }
   }
}
