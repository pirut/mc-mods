package net.minecraft.client.model.animal.axolotl;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.AxolotlRenderState;
import net.minecraft.util.Mth;

@Environment(EnvType.CLIENT)
public class AxolotlModel extends EntityModel<AxolotlRenderState> {
   public static final float SWIMMING_LEG_XROT = 1.8849558F;
   public static final MeshTransformer BABY_TRANSFORMER = MeshTransformer.scaling(0.5F);
   private final ModelPart tail;
   private final ModelPart leftHindLeg;
   private final ModelPart rightHindLeg;
   private final ModelPart leftFrontLeg;
   private final ModelPart rightFrontLeg;
   private final ModelPart body;
   private final ModelPart head;
   private final ModelPart topGills;
   private final ModelPart leftGills;
   private final ModelPart rightGills;

   public AxolotlModel(ModelPart root) {
      super(root);
      this.body = root.getChild("body");
      this.head = this.body.getChild("head");
      this.rightHindLeg = this.body.getChild("right_hind_leg");
      this.leftHindLeg = this.body.getChild("left_hind_leg");
      this.rightFrontLeg = this.body.getChild("right_front_leg");
      this.leftFrontLeg = this.body.getChild("left_front_leg");
      this.tail = this.body.getChild("tail");
      this.topGills = this.head.getChild("top_gills");
      this.leftGills = this.head.getChild("left_gills");
      this.rightGills = this.head.getChild("right_gills");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshDefinition = new MeshDefinition();
      PartDefinition partDefinition = meshDefinition.getRoot();
      PartDefinition partDefinition2 = partDefinition.addOrReplaceChild(
         "body",
         CubeListBuilder.create().texOffs(0, 11).addBox(-4.0F, -2.0F, -9.0F, 8.0F, 4.0F, 10.0F).texOffs(2, 17).addBox(0.0F, -3.0F, -8.0F, 0.0F, 5.0F, 9.0F),
         PartPose.offset(0.0F, 20.0F, 5.0F)
      );
      CubeDeformation cubeDeformation = new CubeDeformation(0.001F);
      PartDefinition partDefinition3 = partDefinition2.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(0, 1).addBox(-4.0F, -3.0F, -5.0F, 8.0F, 5.0F, 5.0F, cubeDeformation), PartPose.offset(0.0F, 0.0F, -9.0F)
      );
      CubeListBuilder cubeListBuilder = CubeListBuilder.create().texOffs(3, 37).addBox(-4.0F, -3.0F, 0.0F, 8.0F, 3.0F, 0.0F, cubeDeformation);
      CubeListBuilder cubeListBuilder2 = CubeListBuilder.create().texOffs(0, 40).addBox(-3.0F, -5.0F, 0.0F, 3.0F, 7.0F, 0.0F, cubeDeformation);
      CubeListBuilder cubeListBuilder3 = CubeListBuilder.create().texOffs(11, 40).addBox(0.0F, -5.0F, 0.0F, 3.0F, 7.0F, 0.0F, cubeDeformation);
      partDefinition3.addOrReplaceChild("top_gills", cubeListBuilder, PartPose.offset(0.0F, -3.0F, -1.0F));
      partDefinition3.addOrReplaceChild("left_gills", cubeListBuilder2, PartPose.offset(-4.0F, 0.0F, -1.0F));
      partDefinition3.addOrReplaceChild("right_gills", cubeListBuilder3, PartPose.offset(4.0F, 0.0F, -1.0F));
      CubeListBuilder cubeListBuilder4 = CubeListBuilder.create().texOffs(2, 13).addBox(-1.0F, 0.0F, 0.0F, 3.0F, 5.0F, 0.0F, cubeDeformation);
      CubeListBuilder cubeListBuilder5 = CubeListBuilder.create().texOffs(2, 13).addBox(-2.0F, 0.0F, 0.0F, 3.0F, 5.0F, 0.0F, cubeDeformation);
      partDefinition2.addOrReplaceChild("right_hind_leg", cubeListBuilder5, PartPose.offset(-3.5F, 1.0F, -1.0F));
      partDefinition2.addOrReplaceChild("left_hind_leg", cubeListBuilder4, PartPose.offset(3.5F, 1.0F, -1.0F));
      partDefinition2.addOrReplaceChild("right_front_leg", cubeListBuilder5, PartPose.offset(-3.5F, 1.0F, -8.0F));
      partDefinition2.addOrReplaceChild("left_front_leg", cubeListBuilder4, PartPose.offset(3.5F, 1.0F, -8.0F));
      partDefinition2.addOrReplaceChild(
         "tail", CubeListBuilder.create().texOffs(2, 19).addBox(0.0F, -3.0F, 0.0F, 0.0F, 5.0F, 12.0F), PartPose.offset(0.0F, 0.0F, 1.0F)
      );
      return LayerDefinition.create(meshDefinition, 64, 64);
   }

   public void setupAnim(AxolotlRenderState axolotlRenderState) {
      super.setupAnim(axolotlRenderState);
      float f = axolotlRenderState.playingDeadFactor;
      float g = axolotlRenderState.inWaterFactor;
      float h = axolotlRenderState.onGroundFactor;
      float i = axolotlRenderState.movingFactor;
      float j = 1.0F - i;
      float k = 1.0F - Math.min(h, i);
      this.body.yRot = this.body.yRot + axolotlRenderState.yRot * (float) (Math.PI / 180.0);
      this.setupSwimmingAnimation(axolotlRenderState.ageInTicks, axolotlRenderState.xRot, Math.min(i, g));
      this.setupWaterHoveringAnimation(axolotlRenderState.ageInTicks, Math.min(j, g));
      this.setupGroundCrawlingAnimation(axolotlRenderState.ageInTicks, Math.min(i, h));
      this.setupLayStillOnGroundAnimation(axolotlRenderState.ageInTicks, Math.min(j, h));
      this.setupPlayDeadAnimation(f);
      this.applyMirrorLegRotations(k);
   }

   private void setupLayStillOnGroundAnimation(float ageInTicks, float netHeadYaw) {
      if (!(netHeadYaw <= 1.0E-5F)) {
         float f = ageInTicks * 0.09F;
         float g = Mth.sin(f);
         float h = Mth.cos(f);
         float i = g * g - 2.0F * g;
         float j = h * h - 3.0F * g;
         this.head.xRot += -0.09F * i * netHeadYaw;
         this.head.zRot += -0.2F * netHeadYaw;
         this.tail.yRot += (-0.1F + 0.1F * i) * netHeadYaw;
         float k = (0.6F + 0.05F * j) * netHeadYaw;
         this.topGills.xRot += k;
         this.leftGills.yRot -= k;
         this.rightGills.yRot += k;
         this.leftHindLeg.xRot += 1.1F * netHeadYaw;
         this.leftHindLeg.yRot += 1.0F * netHeadYaw;
         this.leftFrontLeg.xRot += 0.8F * netHeadYaw;
         this.leftFrontLeg.yRot += 2.3F * netHeadYaw;
         this.leftFrontLeg.zRot -= 0.5F * netHeadYaw;
      }
   }

   private void setupGroundCrawlingAnimation(float ageInTicks, float netHeadYaw) {
      if (!(netHeadYaw <= 1.0E-5F)) {
         float f = ageInTicks * 0.11F;
         float g = Mth.cos(f);
         float h = (g * g - 2.0F * g) / 5.0F;
         float i = 0.7F * g;
         float j = 0.09F * g * netHeadYaw;
         this.head.yRot += j;
         this.tail.yRot += j;
         float k = (0.6F - 0.08F * (g * g + 2.0F * Mth.sin(f))) * netHeadYaw;
         this.topGills.xRot += k;
         this.leftGills.yRot -= k;
         this.rightGills.yRot += k;
         float l = 0.9424779F * netHeadYaw;
         float m = 1.0995574F * netHeadYaw;
         this.leftHindLeg.xRot += l;
         this.leftHindLeg.yRot += (1.5F - h) * netHeadYaw;
         this.leftHindLeg.zRot += -0.1F * netHeadYaw;
         this.leftFrontLeg.xRot += m;
         this.leftFrontLeg.yRot += ((float) (Math.PI / 2) - i) * netHeadYaw;
         this.rightHindLeg.xRot += l;
         this.rightHindLeg.yRot += (-1.0F - h) * netHeadYaw;
         this.rightFrontLeg.xRot += m;
         this.rightFrontLeg.yRot += ((float) (-Math.PI / 2) - i) * netHeadYaw;
      }
   }

   private void setupWaterHoveringAnimation(float ageInTicks, float swimmingFactor) {
      if (!(swimmingFactor <= 1.0E-5F)) {
         float f = ageInTicks * 0.075F;
         float g = Mth.cos(f);
         float h = Mth.sin(f) * 0.15F;
         float i = (-0.15F + 0.075F * g) * swimmingFactor;
         this.body.xRot += i;
         this.body.y -= h * swimmingFactor;
         this.head.xRot -= i;
         this.topGills.xRot += 0.2F * g * swimmingFactor;
         float j = (-0.3F * g - 0.19F) * swimmingFactor;
         this.leftGills.yRot += j;
         this.rightGills.yRot -= j;
         this.leftHindLeg.xRot += ((float) (Math.PI * 3.0 / 4.0) - g * 0.11F) * swimmingFactor;
         this.leftHindLeg.yRot += 0.47123894F * swimmingFactor;
         this.leftHindLeg.zRot += 1.7278761F * swimmingFactor;
         this.leftFrontLeg.xRot += ((float) (Math.PI / 4) - g * 0.2F) * swimmingFactor;
         this.leftFrontLeg.yRot += 2.042035F * swimmingFactor;
         this.tail.yRot += 0.5F * g * swimmingFactor;
      }
   }

   private void setupSwimmingAnimation(float ageInTicks, float xRot, float movingFactor) {
      if (!(movingFactor <= 1.0E-5F)) {
         float f = ageInTicks * 0.33F;
         float g = Mth.sin(f);
         float h = Mth.cos(f);
         float i = 0.13F * g;
         this.body.xRot += (xRot * (float) (Math.PI / 180.0) + i) * movingFactor;
         this.head.xRot -= i * 1.8F * movingFactor;
         this.body.y -= 0.45F * h * movingFactor;
         this.topGills.xRot += (-0.5F * g - 0.8F) * movingFactor;
         float j = (0.3F * g + 0.9F) * movingFactor;
         this.leftGills.yRot += j;
         this.rightGills.yRot -= j;
         this.tail.yRot = this.tail.yRot + 0.3F * Mth.cos(f * 0.9F) * movingFactor;
         this.leftHindLeg.xRot += 1.8849558F * movingFactor;
         this.leftHindLeg.yRot += -0.4F * g * movingFactor;
         this.leftHindLeg.zRot += (float) (Math.PI / 2) * movingFactor;
         this.leftFrontLeg.xRot += 1.8849558F * movingFactor;
         this.leftFrontLeg.yRot += (-0.2F * h - 0.1F) * movingFactor;
         this.leftFrontLeg.zRot += (float) (Math.PI / 2) * movingFactor;
      }
   }

   private void setupPlayDeadAnimation(float playingDeadFactor) {
      if (!(playingDeadFactor <= 1.0E-5F)) {
         this.leftHindLeg.xRot += 1.4137167F * playingDeadFactor;
         this.leftHindLeg.yRot += 1.0995574F * playingDeadFactor;
         this.leftHindLeg.zRot += (float) (Math.PI / 4) * playingDeadFactor;
         this.leftFrontLeg.xRot += (float) (Math.PI / 4) * playingDeadFactor;
         this.leftFrontLeg.yRot += 2.042035F * playingDeadFactor;
         this.body.xRot += -0.15F * playingDeadFactor;
         this.body.zRot += 0.35F * playingDeadFactor;
      }
   }

   private void applyMirrorLegRotations(float factor) {
      if (!(factor <= 1.0E-5F)) {
         this.rightHindLeg.xRot = this.rightHindLeg.xRot + this.leftHindLeg.xRot * factor;
         ModelPart var2 = this.rightHindLeg;
         var2.yRot = var2.yRot + -this.leftHindLeg.yRot * factor;
         var2 = this.rightHindLeg;
         var2.zRot = var2.zRot + -this.leftHindLeg.zRot * factor;
         this.rightFrontLeg.xRot = this.rightFrontLeg.xRot + this.leftFrontLeg.xRot * factor;
         var2 = this.rightFrontLeg;
         var2.yRot = var2.yRot + -this.leftFrontLeg.yRot * factor;
         var2 = this.rightFrontLeg;
         var2.zRot = var2.zRot + -this.leftFrontLeg.zRot * factor;
      }
   }
}
