package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MinecartModel<T extends Entity> extends HierarchicalModel<T> {
   private final ModelPart root;

   public MinecartModel(ModelPart p_170737_) {
      this.root = p_170737_;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition $$0 = new MeshDefinition();
      PartDefinition $$1 = $$0.getRoot();
      int $$2 = 20;
      int $$3 = 8;
      int $$4 = 16;
      int $$5 = 4;
      $$1.addOrReplaceChild(
         "bottom",
         CubeListBuilder.create().texOffs(0, 10).addBox(-10.0F, -8.0F, -1.0F, 20.0F, 16.0F, 2.0F),
         PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
      );
      $$1.addOrReplaceChild(
         "front",
         CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -9.0F, -1.0F, 16.0F, 8.0F, 2.0F),
         PartPose.offsetAndRotation(-9.0F, 4.0F, 0.0F, 0.0F, (float) (Math.PI * 3.0 / 2.0), 0.0F)
      );
      $$1.addOrReplaceChild(
         "back",
         CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -9.0F, -1.0F, 16.0F, 8.0F, 2.0F),
         PartPose.offsetAndRotation(9.0F, 4.0F, 0.0F, 0.0F, (float) (Math.PI / 2), 0.0F)
      );
      $$1.addOrReplaceChild(
         "left",
         CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -9.0F, -1.0F, 16.0F, 8.0F, 2.0F),
         PartPose.offsetAndRotation(0.0F, 4.0F, -7.0F, 0.0F, (float) Math.PI, 0.0F)
      );
      $$1.addOrReplaceChild("right", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -9.0F, -1.0F, 16.0F, 8.0F, 2.0F), PartPose.offset(0.0F, 4.0F, 7.0F));
      return LayerDefinition.create($$0, 64, 32);
   }

   @Override
   public void setupAnim(T p_103100_, float p_103101_, float p_103102_, float p_103103_, float p_103104_, float p_103105_) {
   }

   @Override
   public ModelPart root() {
      return this.root;
   }
}
