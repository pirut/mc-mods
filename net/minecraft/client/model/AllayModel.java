package net.minecraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.animal.allay.Allay;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AllayModel extends HierarchicalModel<Allay> implements ArmedModel {
   private final ModelPart root;
   private final ModelPart head;
   private final ModelPart body;
   private final ModelPart right_arm;
   private final ModelPart left_arm;
   private final ModelPart right_wing;
   private final ModelPart left_wing;
   private static final float FLYING_ANIMATION_X_ROT = (float) (Math.PI / 4);
   private static final float MAX_HAND_HOLDING_ITEM_X_ROT_RAD = -1.134464F;
   private static final float MIN_HAND_HOLDING_ITEM_X_ROT_RAD = (float) (-Math.PI / 3);

   public AllayModel(ModelPart p_233312_) {
      super(RenderType::entityTranslucent);
      this.root = p_233312_.getChild("root");
      this.head = this.root.getChild("head");
      this.body = this.root.getChild("body");
      this.right_arm = this.body.getChild("right_arm");
      this.left_arm = this.body.getChild("left_arm");
      this.right_wing = this.body.getChild("right_wing");
      this.left_wing = this.body.getChild("left_wing");
   }

   @Override
   public ModelPart root() {
      return this.root;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition $$0 = new MeshDefinition();
      PartDefinition $$1 = $$0.getRoot();
      PartDefinition $$2 = $$1.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 23.5F, 0.0F));
      $$2.addOrReplaceChild(
         "head",
         CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -5.0F, -2.5F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -3.99F, 0.0F)
      );
      PartDefinition $$3 = $$2.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .texOffs(0, 10)
            .addBox(-1.5F, 0.0F, -1.0F, 3.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(0, 16)
            .addBox(-1.5F, 0.0F, -1.0F, 3.0F, 5.0F, 2.0F, new CubeDeformation(-0.2F)),
         PartPose.offset(0.0F, -4.0F, 0.0F)
      );
      $$3.addOrReplaceChild(
         "right_arm",
         CubeListBuilder.create().texOffs(23, 0).addBox(-0.75F, -0.5F, -1.0F, 1.0F, 4.0F, 2.0F, new CubeDeformation(-0.01F)),
         PartPose.offset(-1.75F, 0.5F, 0.0F)
      );
      $$3.addOrReplaceChild(
         "left_arm",
         CubeListBuilder.create().texOffs(23, 6).addBox(-0.25F, -0.5F, -1.0F, 1.0F, 4.0F, 2.0F, new CubeDeformation(-0.01F)),
         PartPose.offset(1.75F, 0.5F, 0.0F)
      );
      $$3.addOrReplaceChild(
         "right_wing",
         CubeListBuilder.create().texOffs(16, 14).addBox(0.0F, 1.0F, 0.0F, 0.0F, 5.0F, 8.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-0.5F, 0.0F, 0.6F)
      );
      $$3.addOrReplaceChild(
         "left_wing",
         CubeListBuilder.create().texOffs(16, 14).addBox(0.0F, 1.0F, 0.0F, 0.0F, 5.0F, 8.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.5F, 0.0F, 0.6F)
      );
      return LayerDefinition.create($$0, 32, 32);
   }

   public void setupAnim(Allay p_233325_, float p_233326_, float p_233327_, float p_233328_, float p_233329_, float p_233330_) {
      this.root().getAllParts().forEach(ModelPart::resetPose);
      float $$6 = p_233328_ * 20.0F * (float) (Math.PI / 180.0) + p_233326_;
      float $$7 = Mth.cos($$6) * (float) Math.PI * 0.15F + p_233327_;
      float $$8 = p_233328_ - p_233325_.tickCount;
      float $$9 = p_233328_ * 9.0F * (float) (Math.PI / 180.0);
      float $$10 = Math.min(p_233327_ / 0.3F, 1.0F);
      float $$11 = 1.0F - $$10;
      float $$12 = p_233325_.getHoldingItemAnimationProgress($$8);
      if (p_233325_.isDancing()) {
         float $$13 = p_233328_ * 8.0F * (float) (Math.PI / 180.0) + p_233327_;
         float $$14 = Mth.cos($$13) * 16.0F * (float) (Math.PI / 180.0);
         float $$15 = p_233325_.getSpinningProgress($$8);
         float $$16 = Mth.cos($$13) * 14.0F * (float) (Math.PI / 180.0);
         float $$17 = Mth.cos($$13) * 30.0F * (float) (Math.PI / 180.0);
         this.root.yRot = p_233325_.isSpinning() ? (float) (Math.PI * 4) * $$15 : this.root.yRot;
         this.root.zRot = $$14 * (1.0F - $$15);
         this.head.yRot = $$17 * (1.0F - $$15);
         this.head.zRot = $$16 * (1.0F - $$15);
      } else {
         this.head.xRot = p_233330_ * (float) (Math.PI / 180.0);
         this.head.yRot = p_233329_ * (float) (Math.PI / 180.0);
      }

      this.right_wing.xRot = 0.43633232F * (1.0F - $$10);
      this.right_wing.yRot = (float) (-Math.PI / 4) + $$7;
      this.left_wing.xRot = 0.43633232F * (1.0F - $$10);
      this.left_wing.yRot = (float) (Math.PI / 4) - $$7;
      this.body.xRot = $$10 * (float) (Math.PI / 4);
      float $$18 = $$12 * Mth.lerp($$10, (float) (-Math.PI / 3), -1.134464F);
      this.root.y = this.root.y + (float)Math.cos($$9) * 0.25F * $$11;
      this.right_arm.xRot = $$18;
      this.left_arm.xRot = $$18;
      float $$19 = $$11 * (1.0F - $$12);
      float $$20 = 0.43633232F - Mth.cos($$9 + (float) (Math.PI * 3.0 / 2.0)) * (float) Math.PI * 0.075F * $$19;
      this.left_arm.zRot = -$$20;
      this.right_arm.zRot = $$20;
      this.right_arm.yRot = 0.27925268F * $$12;
      this.left_arm.yRot = -0.27925268F * $$12;
   }

   @Override
   public void translateToHand(HumanoidArm p_233322_, PoseStack p_233323_) {
      float $$2 = 1.0F;
      float $$3 = 3.0F;
      this.root.translateAndRotate(p_233323_);
      this.body.translateAndRotate(p_233323_);
      p_233323_.translate(0.0F, 0.0625F, 0.1875F);
      p_233323_.mulPose(Axis.XP.rotation(this.right_arm.xRot));
      p_233323_.scale(0.7F, 0.7F, 0.7F);
      p_233323_.translate(0.0625F, 0.0F, 0.0F);
   }
}
