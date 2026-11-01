package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public class EndCrystalRenderer extends EntityRenderer<EndCrystal> {
   private static final ResourceLocation END_CRYSTAL_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/end_crystal/end_crystal.png");
   private static final RenderType RENDER_TYPE = RenderType.entityCutoutNoCull(END_CRYSTAL_LOCATION);
   private static final float SIN_45 = (float)Math.sin(Math.PI / 4);
   private static final String GLASS = "glass";
   private static final String BASE = "base";
   private final ModelPart cube;
   private final ModelPart glass;
   private final ModelPart base;

   public EndCrystalRenderer(EntityRendererProvider.Context p_173970_) {
      super(p_173970_);
      this.shadowRadius = 0.5F;
      ModelPart $$1 = p_173970_.bakeLayer(ModelLayers.END_CRYSTAL);
      this.glass = $$1.getChild("glass");
      this.cube = $$1.getChild("cube");
      this.base = $$1.getChild("base");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition $$0 = new MeshDefinition();
      PartDefinition $$1 = $$0.getRoot();
      $$1.addOrReplaceChild("glass", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
      $$1.addOrReplaceChild("cube", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
      $$1.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 16).addBox(-6.0F, 0.0F, -6.0F, 12.0F, 4.0F, 12.0F), PartPose.ZERO);
      return LayerDefinition.create($$0, 64, 32);
   }

   public void render(EndCrystal p_114162_, float p_114163_, float p_114164_, PoseStack p_114165_, MultiBufferSource p_114166_, int p_114167_) {
      p_114165_.pushPose();
      float $$6 = getY(p_114162_, p_114164_);
      float $$7 = (p_114162_.time + p_114164_) * 3.0F;
      VertexConsumer $$8 = p_114166_.getBuffer(RENDER_TYPE);
      p_114165_.pushPose();
      p_114165_.scale(2.0F, 2.0F, 2.0F);
      p_114165_.translate(0.0F, -0.5F, 0.0F);
      int $$9 = OverlayTexture.NO_OVERLAY;
      if (p_114162_.showsBottom()) {
         this.base.render(p_114165_, $$8, p_114167_, $$9);
      }

      p_114165_.mulPose(Axis.YP.rotationDegrees($$7));
      p_114165_.translate(0.0F, 1.5F + $$6 / 2.0F, 0.0F);
      p_114165_.mulPose(new Quaternionf().setAngleAxis((float) (Math.PI / 3), SIN_45, 0.0F, SIN_45));
      this.glass.render(p_114165_, $$8, p_114167_, $$9);
      float $$10 = 0.875F;
      p_114165_.scale(0.875F, 0.875F, 0.875F);
      p_114165_.mulPose(new Quaternionf().setAngleAxis((float) (Math.PI / 3), SIN_45, 0.0F, SIN_45));
      p_114165_.mulPose(Axis.YP.rotationDegrees($$7));
      this.glass.render(p_114165_, $$8, p_114167_, $$9);
      p_114165_.scale(0.875F, 0.875F, 0.875F);
      p_114165_.mulPose(new Quaternionf().setAngleAxis((float) (Math.PI / 3), SIN_45, 0.0F, SIN_45));
      p_114165_.mulPose(Axis.YP.rotationDegrees($$7));
      this.cube.render(p_114165_, $$8, p_114167_, $$9);
      p_114165_.popPose();
      p_114165_.popPose();
      BlockPos $$11 = p_114162_.getBeamTarget();
      if ($$11 != null) {
         float $$12 = $$11.getX() + 0.5F;
         float $$13 = $$11.getY() + 0.5F;
         float $$14 = $$11.getZ() + 0.5F;
         float $$15 = (float)($$12 - p_114162_.getX());
         float $$16 = (float)($$13 - p_114162_.getY());
         float $$17 = (float)($$14 - p_114162_.getZ());
         p_114165_.translate($$15, $$16, $$17);
         EnderDragonRenderer.renderCrystalBeams(-$$15, -$$16 + $$6, -$$17, p_114164_, p_114162_.time, p_114165_, p_114166_, p_114167_);
      }

      super.render(p_114162_, p_114163_, p_114164_, p_114165_, p_114166_, p_114167_);
   }

   public static float getY(EndCrystal p_114159_, float p_114160_) {
      float $$2 = p_114159_.time + p_114160_;
      float $$3 = Mth.sin($$2 * 0.2F) / 2.0F + 0.5F;
      $$3 = ($$3 * $$3 + $$3) * 0.4F;
      return $$3 - 1.4F;
   }

   public ResourceLocation getTextureLocation(EndCrystal p_114157_) {
      return END_CRYSTAL_LOCATION;
   }

   public boolean shouldRender(EndCrystal p_114169_, Frustum p_114170_, double p_114171_, double p_114172_, double p_114173_) {
      return super.shouldRender(p_114169_, p_114170_, p_114171_, p_114172_, p_114173_) || p_114169_.getBeamTarget() != null;
   }
}
