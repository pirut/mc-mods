package net.minecraft.client.renderer.blockentity;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.util.Unit;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SignRenderer extends AbstractSignRenderer {
   public static final float RENDER_SCALE = 0.6666667F;
   private static final Vec3 TEXT_OFFSET = new Vec3(0.0, 0.33333334F, 0.046666667F);
   private final Map<WoodType, SignRenderer.Models> signModels;

   public SignRenderer(BlockEntityRendererProvider.Context p_173636_) {
      super(p_173636_);
      this.signModels = WoodType.values()
         .collect(
            ImmutableMap.toImmutableMap(
               p_173645_ -> p_173645_,
               p_432289_ -> new SignRenderer.Models(
                  createSignModel(p_173636_.entityModelSet(), p_432289_, true), createSignModel(p_173636_.entityModelSet(), p_432289_, false)
               )
            )
         );
   }

   @Override
   protected Model.Simple getSignModel(BlockState p_389425_, WoodType p_389581_) {
      SignRenderer.Models signrenderer$models = this.signModels.get(p_389581_);
      return p_389425_.getBlock() instanceof StandingSignBlock ? signrenderer$models.standing() : signrenderer$models.wall();
   }

   @Override
   protected Material getSignMaterial(WoodType p_251961_) {
      return Sheets.getSignMaterial(p_251961_);
   }

   @Override
   protected float getSignModelRenderScale() {
      return 0.6666667F;
   }

   @Override
   protected float getSignTextRenderScale() {
      return 0.6666667F;
   }

   private static void translateBase(PoseStack p_389606_, float p_389676_) {
      p_389606_.translate(0.5F, 0.5F, 0.5F);
      p_389606_.mulPose(Axis.YP.rotationDegrees(p_389676_));
   }

   @Override
   protected void translateSign(PoseStack p_278074_, float p_277875_, BlockState p_277559_) {
      translateBase(p_278074_, p_277875_);
      if (!(p_277559_.getBlock() instanceof StandingSignBlock)) {
         p_278074_.translate(0.0F, -0.3125F, -0.4375F);
      }
   }

   @Override
   protected Vec3 getTextOffset() {
      return TEXT_OFFSET;
   }

   public static void submitSpecial(
      MaterialSet p_439989_, PoseStack p_440179_, SubmitNodeCollector p_440546_, int p_439649_, int p_440581_, Model.Simple p_440051_, Material p_438922_
   ) {
      p_440179_.pushPose();
      applyInHandTransforms(p_440179_);
      p_440546_.submitModel(
         p_440051_, Unit.INSTANCE, p_440179_, p_438922_.renderType(p_440051_::renderType), p_439649_, p_440581_, -1, p_439989_.get(p_438922_), 0, null
      );
      p_440179_.popPose();
   }

   public static void applyInHandTransforms(PoseStack p_428280_) {
      translateBase(p_428280_, 0.0F);
      p_428280_.scale(0.6666667F, -0.6666667F, -0.6666667F);
   }

   public static Model.Simple createSignModel(EntityModelSet p_173647_, WoodType p_173648_, boolean p_363591_) {
      ModelLayerLocation modellayerlocation = p_363591_ ? ModelLayers.createStandingSignModelName(p_173648_) : ModelLayers.createWallSignModelName(p_173648_);
      return new Model.Simple(p_173647_.bakeLayer(modellayerlocation), RenderTypes::entityCutoutNoCull);
   }

   public static LayerDefinition createSignLayer(boolean p_365434_) {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild("sign", CubeListBuilder.create().texOffs(0, 0).addBox(-12.0F, -14.0F, -1.0F, 24.0F, 12.0F, 2.0F), PartPose.ZERO);
      if (p_365434_) {
         partdefinition.addOrReplaceChild("stick", CubeListBuilder.create().texOffs(0, 14).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 14.0F, 2.0F), PartPose.ZERO);
      }

      return LayerDefinition.create(meshdefinition, 64, 32);
   }

   @OnlyIn(Dist.CLIENT)
   record Models(Model.Simple standing, Model.Simple wall) {
   }
}
