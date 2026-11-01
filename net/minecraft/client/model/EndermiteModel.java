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
public class EndermiteModel<T extends Entity> extends HierarchicalModel<T> {
   private static final int BODY_COUNT = 4;
   private static final int[][] BODY_SIZES = new int[][]{{4, 3, 2}, {6, 4, 5}, {3, 3, 1}, {1, 2, 1}};
   private static final int[][] BODY_TEXS = new int[][]{{0, 0}, {0, 5}, {0, 14}, {0, 18}};
   private final ModelPart root;
   private final ModelPart[] bodyParts;

   public EndermiteModel(ModelPart p_170545_) {
      this.root = p_170545_;
      this.bodyParts = new ModelPart[4];

      for (int $$1 = 0; $$1 < 4; $$1++) {
         this.bodyParts[$$1] = p_170545_.getChild(createSegmentName($$1));
      }
   }

   private static String createSegmentName(int p_170548_) {
      return "segment" + p_170548_;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition $$0 = new MeshDefinition();
      PartDefinition $$1 = $$0.getRoot();
      float $$2 = -3.5F;

      for (int $$3 = 0; $$3 < 4; $$3++) {
         $$1.addOrReplaceChild(
            createSegmentName($$3),
            CubeListBuilder.create()
               .texOffs(BODY_TEXS[$$3][0], BODY_TEXS[$$3][1])
               .addBox(BODY_SIZES[$$3][0] * -0.5F, 0.0F, BODY_SIZES[$$3][2] * -0.5F, BODY_SIZES[$$3][0], BODY_SIZES[$$3][1], BODY_SIZES[$$3][2]),
            PartPose.offset(0.0F, 24 - BODY_SIZES[$$3][1], $$2)
         );
         if ($$3 < 3) {
            $$2 += (BODY_SIZES[$$3][2] + BODY_SIZES[$$3 + 1][2]) * 0.5F;
         }
      }

      return LayerDefinition.create($$0, 64, 32);
   }

   @Override
   public ModelPart root() {
      return this.root;
   }

   @Override
   public void setupAnim(T p_102602_, float p_102603_, float p_102604_, float p_102605_, float p_102606_, float p_102607_) {
      for (int $$6 = 0; $$6 < this.bodyParts.length; $$6++) {
         this.bodyParts[$$6].yRot = Mth.cos(p_102605_ * 0.9F + $$6 * 0.15F * (float) Math.PI) * (float) Math.PI * 0.01F * (1 + Math.abs($$6 - 2));
         this.bodyParts[$$6].x = Mth.sin(p_102605_ * 0.9F + $$6 * 0.15F * (float) Math.PI) * (float) Math.PI * 0.1F * Math.abs($$6 - 2);
      }
   }
}
