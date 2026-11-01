package net.minecraft.client.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.Boat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RaftModel extends ListModel<Boat> {
   private static final String LEFT_PADDLE = "left_paddle";
   private static final String RIGHT_PADDLE = "right_paddle";
   private static final String BOTTOM = "bottom";
   private final ModelPart leftPaddle;
   private final ModelPart rightPaddle;
   private final ImmutableList<ModelPart> parts;

   public RaftModel(ModelPart p_251383_) {
      this.leftPaddle = p_251383_.getChild("left_paddle");
      this.rightPaddle = p_251383_.getChild("right_paddle");
      this.parts = this.createPartsBuilder(p_251383_).build();
   }

   protected Builder<ModelPart> createPartsBuilder(ModelPart p_250773_) {
      Builder<ModelPart> $$1 = new Builder();
      $$1.add(new ModelPart[]{p_250773_.getChild("bottom"), this.leftPaddle, this.rightPaddle});
      return $$1;
   }

   public static void createChildren(PartDefinition p_250262_) {
      p_250262_.addOrReplaceChild(
         "bottom",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-14.0F, -11.0F, -4.0F, 28.0F, 20.0F, 4.0F)
            .texOffs(0, 0)
            .addBox(-14.0F, -9.0F, -8.0F, 28.0F, 16.0F, 4.0F),
         PartPose.offsetAndRotation(0.0F, -2.1F, 1.0F, 1.5708F, 0.0F, 0.0F)
      );
      int $$1 = 20;
      int $$2 = 7;
      int $$3 = 6;
      float $$4 = -5.0F;
      p_250262_.addOrReplaceChild(
         "left_paddle",
         CubeListBuilder.create().texOffs(0, 24).addBox(-1.0F, 0.0F, -5.0F, 2.0F, 2.0F, 18.0F).addBox(-1.001F, -3.0F, 8.0F, 1.0F, 6.0F, 7.0F),
         PartPose.offsetAndRotation(3.0F, -4.0F, 9.0F, 0.0F, 0.0F, (float) (Math.PI / 16))
      );
      p_250262_.addOrReplaceChild(
         "right_paddle",
         CubeListBuilder.create().texOffs(40, 24).addBox(-1.0F, 0.0F, -5.0F, 2.0F, 2.0F, 18.0F).addBox(0.001F, -3.0F, 8.0F, 1.0F, 6.0F, 7.0F),
         PartPose.offsetAndRotation(3.0F, -4.0F, -9.0F, 0.0F, (float) Math.PI, (float) (Math.PI / 16))
      );
   }

   public static LayerDefinition createBodyModel() {
      MeshDefinition $$0 = new MeshDefinition();
      PartDefinition $$1 = $$0.getRoot();
      createChildren($$1);
      return LayerDefinition.create($$0, 128, 64);
   }

   public void setupAnim(Boat p_249733_, float p_249202_, float p_252219_, float p_249366_, float p_249759_, float p_250286_) {
      animatePaddle(p_249733_, 0, this.leftPaddle, p_249202_);
      animatePaddle(p_249733_, 1, this.rightPaddle, p_249202_);
   }

   public ImmutableList<ModelPart> parts() {
      return this.parts;
   }

   private static void animatePaddle(Boat p_250792_, int p_249947_, ModelPart p_248943_, float p_251990_) {
      float $$4 = p_250792_.getRowingTime(p_249947_, p_251990_);
      p_248943_.xRot = Mth.clampedLerp((float) (-Math.PI / 3), (float) (-Math.PI / 12), (Mth.sin(-$$4) + 1.0F) / 2.0F);
      p_248943_.yRot = Mth.clampedLerp((float) (-Math.PI / 4), (float) (Math.PI / 4), (Mth.sin(-$$4 + 1.0F) + 1.0F) / 2.0F);
      if (p_249947_ == 1) {
         p_248943_.yRot = (float) Math.PI - p_248943_.yRot;
      }
   }
}
