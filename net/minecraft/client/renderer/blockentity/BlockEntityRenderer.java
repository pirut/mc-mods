package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;

@OnlyIn(Dist.CLIENT)
public interface BlockEntityRenderer<T extends BlockEntity> extends IBlockEntityRendererExtension<T> {
   void render(T var1, float var2, PoseStack var3, MultiBufferSource var4, int var5, int var6);

   default boolean shouldRenderOffScreen(T p_112306_) {
      return false;
   }

   default int getViewDistance() {
      return 64;
   }

   default boolean shouldRender(T p_173568_, Vec3 p_173569_) {
      return Vec3.atCenterOf(p_173568_.getBlockPos()).closerThan(p_173569_, this.getViewDistance());
   }
}
