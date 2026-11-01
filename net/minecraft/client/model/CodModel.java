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
public class CodModel<T extends Entity> extends HierarchicalModel<T> {
   private final ModelPart root;
   private final ModelPart tailFin;

   public CodModel(ModelPart p_170494_) {
      this.root = p_170494_;
      this.tailFin = p_170494_.getChild("tail_fin");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition $$0 = new MeshDefinition();
      PartDefinition $$1 = $$0.getRoot();
      int $$2 = 22;
      $$1.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -2.0F, 0.0F, 2.0F, 4.0F, 7.0F), PartPose.offset(0.0F, 22.0F, 0.0F));
      $$1.addOrReplaceChild("head", CubeListBuilder.create().texOffs(11, 0).addBox(-1.0F, -2.0F, -3.0F, 2.0F, 4.0F, 3.0F), PartPose.offset(0.0F, 22.0F, 0.0F));
      $$1.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 3.0F, 1.0F), PartPose.offset(0.0F, 22.0F, -3.0F));
      $$1.addOrReplaceChild(
         "right_fin",
         CubeListBuilder.create().texOffs(22, 1).addBox(-2.0F, 0.0F, -1.0F, 2.0F, 0.0F, 2.0F),
         PartPose.offsetAndRotation(-1.0F, 23.0F, 0.0F, 0.0F, 0.0F, (float) (-Math.PI / 4))
      );
      $$1.addOrReplaceChild(
         "left_fin",
         CubeListBuilder.create().texOffs(22, 4).addBox(0.0F, 0.0F, -1.0F, 2.0F, 0.0F, 2.0F),
         PartPose.offsetAndRotation(1.0F, 23.0F, 0.0F, 0.0F, 0.0F, (float) (Math.PI / 4))
      );
      $$1.addOrReplaceChild("tail_fin", CubeListBuilder.create().texOffs(22, 3).addBox(0.0F, -2.0F, 0.0F, 0.0F, 4.0F, 4.0F), PartPose.offset(0.0F, 22.0F, 7.0F));
      $$1.addOrReplaceChild(
         "top_fin", CubeListBuilder.create().texOffs(20, -6).addBox(0.0F, -1.0F, -1.0F, 0.0F, 1.0F, 6.0F), PartPose.offset(0.0F, 20.0F, 0.0F)
      );
      return LayerDefinition.create($$0, 32, 32);
   }

   @Override
   public ModelPart root() {
      return this.root;
   }

   @Override
   public void setupAnim(T p_102409_, float p_102410_, float p_102411_, float p_102412_, float p_102413_, float p_102414_) {
      float $$6 = 1.0F;
      if (!p_102409_.isInWater()) {
         $$6 = 1.5F;
      }

      this.tailFin.yRot = -$$6 * 0.45F * Mth.sin(0.6F * p_102412_);
   }
}
