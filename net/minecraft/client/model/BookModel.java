package net.minecraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BookModel extends Model {
   private static final String LEFT_PAGES = "left_pages";
   private static final String RIGHT_PAGES = "right_pages";
   private static final String FLIP_PAGE_1 = "flip_page1";
   private static final String FLIP_PAGE_2 = "flip_page2";
   private final ModelPart root;
   private final ModelPart leftLid;
   private final ModelPart rightLid;
   private final ModelPart leftPages;
   private final ModelPart rightPages;
   private final ModelPart flipPage1;
   private final ModelPart flipPage2;

   public BookModel(ModelPart p_170475_) {
      super(RenderType::entitySolid);
      this.root = p_170475_;
      this.leftLid = p_170475_.getChild("left_lid");
      this.rightLid = p_170475_.getChild("right_lid");
      this.leftPages = p_170475_.getChild("left_pages");
      this.rightPages = p_170475_.getChild("right_pages");
      this.flipPage1 = p_170475_.getChild("flip_page1");
      this.flipPage2 = p_170475_.getChild("flip_page2");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition $$0 = new MeshDefinition();
      PartDefinition $$1 = $$0.getRoot();
      $$1.addOrReplaceChild(
         "left_lid", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -5.0F, -0.005F, 6.0F, 10.0F, 0.005F), PartPose.offset(0.0F, 0.0F, -1.0F)
      );
      $$1.addOrReplaceChild(
         "right_lid", CubeListBuilder.create().texOffs(16, 0).addBox(0.0F, -5.0F, -0.005F, 6.0F, 10.0F, 0.005F), PartPose.offset(0.0F, 0.0F, 1.0F)
      );
      $$1.addOrReplaceChild(
         "seam", CubeListBuilder.create().texOffs(12, 0).addBox(-1.0F, -5.0F, 0.0F, 2.0F, 10.0F, 0.005F), PartPose.rotation(0.0F, (float) (Math.PI / 2), 0.0F)
      );
      $$1.addOrReplaceChild("left_pages", CubeListBuilder.create().texOffs(0, 10).addBox(0.0F, -4.0F, -0.99F, 5.0F, 8.0F, 1.0F), PartPose.ZERO);
      $$1.addOrReplaceChild("right_pages", CubeListBuilder.create().texOffs(12, 10).addBox(0.0F, -4.0F, -0.01F, 5.0F, 8.0F, 1.0F), PartPose.ZERO);
      CubeListBuilder $$2 = CubeListBuilder.create().texOffs(24, 10).addBox(0.0F, -4.0F, 0.0F, 5.0F, 8.0F, 0.005F);
      $$1.addOrReplaceChild("flip_page1", $$2, PartPose.ZERO);
      $$1.addOrReplaceChild("flip_page2", $$2, PartPose.ZERO);
      return LayerDefinition.create($$0, 64, 32);
   }

   @Override
   public void renderToBuffer(PoseStack p_102298_, VertexConsumer p_102299_, int p_102300_, int p_102301_, int p_350575_) {
      this.render(p_102298_, p_102299_, p_102300_, p_102301_, p_350575_);
   }

   public void render(PoseStack p_102317_, VertexConsumer p_102318_, int p_102319_, int p_102320_, int p_350753_) {
      this.root.render(p_102317_, p_102318_, p_102319_, p_102320_, p_350753_);
   }

   public void setupAnim(float p_102293_, float p_102294_, float p_102295_, float p_102296_) {
      float $$4 = (Mth.sin(p_102293_ * 0.02F) * 0.1F + 1.25F) * p_102296_;
      this.leftLid.yRot = (float) Math.PI + $$4;
      this.rightLid.yRot = -$$4;
      this.leftPages.yRot = $$4;
      this.rightPages.yRot = -$$4;
      this.flipPage1.yRot = $$4 - $$4 * 2.0F * p_102294_;
      this.flipPage2.yRot = $$4 - $$4 * 2.0F * p_102295_;
      this.leftPages.x = Mth.sin($$4);
      this.rightPages.x = Mth.sin($$4);
      this.flipPage1.x = Mth.sin($$4);
      this.flipPage2.x = Mth.sin($$4);
   }
}
