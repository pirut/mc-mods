package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.SpawnerRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class SpawnerRenderer implements BlockEntityRenderer<SpawnerBlockEntity, SpawnerRenderState> {
   private final EntityRenderDispatcher entityRenderer;

   public SpawnerRenderer(BlockEntityRendererProvider.Context p_173673_) {
      this.entityRenderer = p_173673_.entityRenderer();
   }

   public SpawnerRenderState createRenderState() {
      return new SpawnerRenderState();
   }

   public void extractRenderState(
      SpawnerBlockEntity p_446072_, SpawnerRenderState p_447179_, float p_446518_, Vec3 p_446190_, @Nullable ModelFeatureRenderer.CrumblingOverlay p_445699_
   ) {
      BlockEntityRenderer.super.extractRenderState(p_446072_, p_447179_, p_446518_, p_446190_, p_445699_);
      if (p_446072_.getLevel() != null) {
         BaseSpawner basespawner = p_446072_.getSpawner();
         Entity entity = basespawner.getOrCreateDisplayEntity(p_446072_.getLevel(), p_446072_.getBlockPos());
         TrialSpawnerRenderer.extractSpawnerData(p_447179_, p_446518_, entity, this.entityRenderer, basespawner.getOSpin(), basespawner.getSpin());
      }
   }

   public void submit(SpawnerRenderState p_446520_, PoseStack p_440479_, SubmitNodeCollector p_439725_, CameraRenderState p_451046_) {
      if (p_446520_.displayEntity != null) {
         submitEntityInSpawner(p_440479_, p_439725_, p_446520_.displayEntity, this.entityRenderer, p_446520_.spin, p_446520_.scale, p_451046_);
      }
   }

   public static void submitEntityInSpawner(
      PoseStack p_439357_,
      SubmitNodeCollector p_439460_,
      EntityRenderState p_446169_,
      EntityRenderDispatcher p_438990_,
      float p_439644_,
      float p_447153_,
      CameraRenderState p_451126_
   ) {
      p_439357_.pushPose();
      p_439357_.translate(0.5F, 0.4F, 0.5F);
      p_439357_.mulPose(Axis.YP.rotationDegrees(p_439644_));
      p_439357_.translate(0.0F, -0.2F, 0.0F);
      p_439357_.mulPose(Axis.XP.rotationDegrees(-30.0F));
      p_439357_.scale(p_447153_, p_447153_, p_447153_);
      p_438990_.submit(p_446169_, p_451126_, 0.0, 0.0, 0.0, p_439357_, p_439460_);
      p_439357_.popPose();
   }

   public AABB getRenderBoundingBox(SpawnerBlockEntity blockEntity) {
      BlockPos pos = blockEntity.getBlockPos();
      return new AABB(pos.getX() - 1.0, pos.getY() - 1.0, pos.getZ() - 1.0, pos.getX() + 2.0, pos.getY() + 2.0, pos.getZ() + 2.0);
   }
}
