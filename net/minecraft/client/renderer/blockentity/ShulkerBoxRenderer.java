package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Consumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.ShulkerBoxRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ShulkerBoxRenderer implements BlockEntityRenderer<ShulkerBoxBlockEntity, ShulkerBoxRenderState> {
   private final MaterialSet materials;
   private final ShulkerBoxRenderer.ShulkerBoxModel model;

   public ShulkerBoxRenderer(BlockEntityRendererProvider.Context p_173626_) {
      this(p_173626_.entityModelSet(), p_173626_.materials());
   }

   public ShulkerBoxRenderer(SpecialModelRenderer.BakingContext p_435273_) {
      this(p_435273_.entityModelSet(), p_435273_.materials());
   }

   public ShulkerBoxRenderer(EntityModelSet p_435039_, MaterialSet p_434694_) {
      this.materials = p_434694_;
      this.model = new ShulkerBoxRenderer.ShulkerBoxModel(p_435039_.bakeLayer(ModelLayers.SHULKER_BOX));
   }

   public ShulkerBoxRenderState createRenderState() {
      return new ShulkerBoxRenderState();
   }

   public void extractRenderState(
      ShulkerBoxBlockEntity p_445428_,
      ShulkerBoxRenderState p_446695_,
      float p_446436_,
      Vec3 p_446068_,
      @Nullable ModelFeatureRenderer.CrumblingOverlay p_446363_
   ) {
      BlockEntityRenderer.super.extractRenderState(p_445428_, p_446695_, p_446436_, p_446068_, p_446363_);
      p_446695_.direction = p_445428_.getBlockState().getValueOrElse(ShulkerBoxBlock.FACING, Direction.UP);
      p_446695_.color = p_445428_.getColor();
      p_446695_.progress = p_445428_.getProgress(p_446436_);
   }

   public void submit(ShulkerBoxRenderState p_446892_, PoseStack p_439668_, SubmitNodeCollector p_440606_, CameraRenderState p_451427_) {
      DyeColor dyecolor = p_446892_.color;
      Material material;
      if (dyecolor == null) {
         material = Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION;
      } else {
         material = Sheets.getShulkerBoxMaterial(dyecolor);
      }

      this.submit(
         p_439668_, p_440606_, p_446892_.lightCoords, OverlayTexture.NO_OVERLAY, p_446892_.direction, p_446892_.progress, p_446892_.breakProgress, material, 0
      );
   }

   public void submit(
      PoseStack p_440659_,
      SubmitNodeCollector p_440531_,
      int p_440374_,
      int p_440011_,
      Direction p_439059_,
      float p_439448_,
      @Nullable ModelFeatureRenderer.CrumblingOverlay p_440691_,
      Material p_439769_,
      int p_451697_
   ) {
      p_440659_.pushPose();
      this.prepareModel(p_440659_, p_439059_, p_439448_);
      p_440531_.submitModel(
         this.model,
         p_439448_,
         p_440659_,
         p_439769_.renderType(this.model::renderType),
         p_440374_,
         p_440011_,
         -1,
         this.materials.get(p_439769_),
         p_451697_,
         p_440691_
      );
      p_440659_.popPose();
   }

   private void prepareModel(PoseStack p_428342_, Direction p_428479_, float p_428538_) {
      p_428342_.translate(0.5F, 0.5F, 0.5F);
      float f = 0.9995F;
      p_428342_.scale(0.9995F, 0.9995F, 0.9995F);
      p_428342_.mulPose(p_428479_.getRotation());
      p_428342_.scale(1.0F, -1.0F, -1.0F);
      p_428342_.translate(0.0F, -1.0F, 0.0F);
      this.model.setupAnim(p_428538_);
   }

   public void getExtents(Direction p_428199_, float p_428569_, Consumer<Vector3fc> p_470652_) {
      PoseStack posestack = new PoseStack();
      this.prepareModel(posestack, p_428199_, p_428569_);
      this.model.root().getExtentsForGui(posestack, p_470652_);
   }

   public AABB getRenderBoundingBox(ShulkerBoxBlockEntity blockEntity) {
      BlockPos pos = blockEntity.getBlockPos();
      return new AABB(pos.getX() - 0.5, pos.getY() - 0.5, pos.getZ() - 0.5, pos.getX() + 1.5, pos.getY() + 1.5, pos.getZ() + 1.5);
   }

   @OnlyIn(Dist.CLIENT)
   static class ShulkerBoxModel extends Model<Float> {
      private final ModelPart lid;

      public ShulkerBoxModel(ModelPart p_361240_) {
         super(p_361240_, RenderTypes::entityCutoutNoCull);
         this.lid = p_361240_.getChild("lid");
      }

      public void setupAnim(Float p_434331_) {
         super.setupAnim(p_434331_);
         this.lid.setPos(0.0F, 24.0F - p_434331_ * 0.5F * 16.0F, 0.0F);
         this.lid.yRot = 270.0F * p_434331_ * (float) (Math.PI / 180.0);
      }
   }
}
