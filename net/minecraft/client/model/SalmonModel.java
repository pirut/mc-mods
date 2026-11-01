package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SalmonModel<T extends Entity> extends HierarchicalModel<T> {
   private static final String BODY_FRONT = "body_front";
   private static final String BODY_BACK = "body_back";
   private final ModelPart root;
   private final ModelPart bodyBack;

   public SalmonModel(ModelPart p_170896_) {
      this.root = p_170896_;
      this.bodyBack = p_170896_.getChild("body_back");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition $$0 = new MeshDefinition();
      PartDefinition $$1 = $$0.getRoot();
      int $$2 = 20;
      PartDefinition $$3 = $$1.addOrReplaceChild(
         "body_front", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -2.5F, 0.0F, 3.0F, 5.0F, 8.0F), PartPose.offset(0.0F, 20.0F, 0.0F)
      );
      PartDefinition $$4 = $$1.addOrReplaceChild(
         "body_back", CubeListBuilder.create().texOffs(0, 13).addBox(-1.5F, -2.5F, 0.0F, 3.0F, 5.0F, 8.0F), PartPose.offset(0.0F, 20.0F, 8.0F)
      );
      $$1.addOrReplaceChild("head", CubeListBuilder.create().texOffs(22, 0).addBox(-1.0F, -2.0F, -3.0F, 2.0F, 4.0F, 3.0F), PartPose.offset(0.0F, 20.0F, 0.0F));
      $$4.addOrReplaceChild("back_fin", CubeListBuilder.create().texOffs(20, 10).addBox(0.0F, -2.5F, 0.0F, 0.0F, 5.0F, 6.0F), PartPose.offset(0.0F, 0.0F, 8.0F));
      $$3.addOrReplaceChild(
         "top_front_fin", CubeListBuilder.create().texOffs(2, 1).addBox(0.0F, 0.0F, 0.0F, 0.0F, 2.0F, 3.0F), PartPose.offset(0.0F, -4.5F, 5.0F)
      );
      $$4.addOrReplaceChild(
         "top_back_fin", CubeListBuilder.create().texOffs(0, 2).addBox(0.0F, 0.0F, 0.0F, 0.0F, 2.0F, 4.0F), PartPose.offset(0.0F, -4.5F, -1.0F)
      );
      $$1.addOrReplaceChild(
         "right_fin",
         CubeListBuilder.create().texOffs(-4, 0).addBox(-2.0F, 0.0F, 0.0F, 2.0F, 0.0F, 2.0F),
         PartPose.offsetAndRotation(-1.5F, 21.5F, 0.0F, 0.0F, 0.0F, (float) (-Math.PI / 4))
      );
      $$1.addOrReplaceChild(
         "left_fin",
         CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 2.0F, 0.0F, 2.0F),
         PartPose.offsetAndRotation(1.5F, 21.5F, 0.0F, 0.0F, 0.0F, (float) (Math.PI / 4))
      );
      return LayerDefinition.create($$0, 32, 32);
   }

   @Override
   public ModelPart root() {
      return this.root;
   }

   @Override
   public void setupAnim(T p_103640_, float p_103641_, float p_103642_, float p_103643_, float p_103644_, float p_103645_) {
      float $$6 = 1.0F;
      float $$7 = 1.0F;
      if (!p_103640_.isInWater()) {
         $$6 = 1.3F;
         $$7 = 1.7F;
      }

      this.bodyBack.yRot = -$$6 * 0.25F * Mth.sin($$7 * 0.6F * p_103643_);
   }
}
