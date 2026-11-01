package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.SpawnerRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerStateData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class TrialSpawnerRenderer implements BlockEntityRenderer<TrialSpawnerBlockEntity, SpawnerRenderState> {
   private final EntityRenderDispatcher entityRenderer;

   public TrialSpawnerRenderer(BlockEntityRendererProvider.Context p_312398_) {
      this.entityRenderer = p_312398_.entityRenderer();
   }

   public SpawnerRenderState createRenderState() {
      return new SpawnerRenderState();
   }

   public void extractRenderState(
      TrialSpawnerBlockEntity p_446337_,
      SpawnerRenderState p_445637_,
      float p_446553_,
      Vec3 p_446985_,
      @Nullable ModelFeatureRenderer.CrumblingOverlay p_447263_
   ) {
      BlockEntityRenderer.super.extractRenderState(p_446337_, p_445637_, p_446553_, p_446985_, p_447263_);
      if (p_446337_.getLevel() != null) {
         TrialSpawner trialspawner = p_446337_.getTrialSpawner();
         TrialSpawnerStateData trialspawnerstatedata = trialspawner.getStateData();
         Entity entity = trialspawnerstatedata.getOrCreateDisplayEntity(trialspawner, p_446337_.getLevel(), trialspawner.getState());
         extractSpawnerData(p_445637_, p_446553_, entity, this.entityRenderer, trialspawnerstatedata.getOSpin(), trialspawnerstatedata.getSpin());
      }
   }

   static void extractSpawnerData(
      SpawnerRenderState p_447339_, float p_445795_, @Nullable Entity p_447101_, EntityRenderDispatcher p_445573_, double p_445737_, double p_445854_
   ) {
      if (p_447101_ != null) {
         p_447339_.displayEntity = p_445573_.extractEntity(p_447101_, p_445795_);
         p_447339_.displayEntity.lightCoords = p_447339_.lightCoords;
         p_447339_.spin = (float)Mth.lerp((double)p_445795_, p_445737_, p_445854_) * 10.0F;
         p_447339_.scale = 0.53125F;
         float f = Math.max(p_447101_.getBbWidth(), p_447101_.getBbHeight());
         if (f > 1.0) {
            p_447339_.scale /= f;
         }
      }
   }

   public void submit(SpawnerRenderState p_446605_, PoseStack p_438985_, SubmitNodeCollector p_440689_, CameraRenderState p_451456_) {
      if (p_446605_.displayEntity != null) {
         SpawnerRenderer.submitEntityInSpawner(p_438985_, p_440689_, p_446605_.displayEntity, this.entityRenderer, p_446605_.spin, p_446605_.scale, p_451456_);
      }
   }

   public AABB getRenderBoundingBox(TrialSpawnerBlockEntity blockEntity) {
      BlockPos pos = blockEntity.getBlockPos();
      return new AABB(pos.getX() - 1.0, pos.getY() - 1.0, pos.getZ() - 1.0, pos.getX() + 2.0, pos.getY() + 2.0, pos.getZ() + 2.0);
   }
}
