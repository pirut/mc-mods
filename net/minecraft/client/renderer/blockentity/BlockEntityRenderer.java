package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface BlockEntityRenderer<T extends BlockEntity, S extends BlockEntityRenderState> extends IBlockEntityRendererExtension<T> {
   S createRenderState();

   default void extractRenderState(T p_445916_, S p_447093_, float p_446851_, Vec3 p_445788_, @Nullable ModelFeatureRenderer.CrumblingOverlay p_446944_) {
      BlockEntityRenderState.extractBase(p_445916_, p_447093_, p_446944_);
   }

   void submit(S var1, PoseStack var2, SubmitNodeCollector var3, CameraRenderState var4);

   default boolean shouldRenderOffScreen() {
      return false;
   }

   default int getViewDistance() {
      return 64;
   }

   default boolean shouldRender(T p_173568_, Vec3 p_173569_) {
      return Vec3.atCenterOf(p_173568_.getBlockPos()).closerThan(p_173569_, this.getViewDistance());
   }
}
