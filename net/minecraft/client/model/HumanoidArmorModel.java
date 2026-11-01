package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HumanoidArmorModel<T extends LivingEntity> extends HumanoidModel<T> {
   public HumanoidArmorModel(ModelPart p_270765_) {
      super(p_270765_);
   }

   public static MeshDefinition createBodyLayer(CubeDeformation p_270527_) {
      MeshDefinition $$1 = HumanoidModel.createMesh(p_270527_, 0.0F);
      PartDefinition $$2 = $$1.getRoot();
      $$2.addOrReplaceChild(
         "right_leg",
         CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, p_270527_.extend(-0.1F)),
         PartPose.offset(-1.9F, 12.0F, 0.0F)
      );
      $$2.addOrReplaceChild(
         "left_leg",
         CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, p_270527_.extend(-0.1F)),
         PartPose.offset(1.9F, 12.0F, 0.0F)
      );
      return $$1;
   }
}
