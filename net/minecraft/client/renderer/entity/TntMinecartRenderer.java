package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TntMinecartRenderer extends MinecartRenderer<MinecartTNT> {
   private final BlockRenderDispatcher blockRenderer;

   public TntMinecartRenderer(EntityRendererProvider.Context p_174424_) {
      super(p_174424_, ModelLayers.TNT_MINECART);
      this.blockRenderer = p_174424_.getBlockRenderDispatcher();
   }

   protected void renderMinecartContents(
      MinecartTNT p_116151_, float p_116152_, BlockState p_116153_, PoseStack p_116154_, MultiBufferSource p_116155_, int p_116156_
   ) {
      int $$6 = p_116151_.getFuse();
      if ($$6 > -1 && $$6 - p_116152_ + 1.0F < 10.0F) {
         float $$7 = 1.0F - ($$6 - p_116152_ + 1.0F) / 10.0F;
         $$7 = Mth.clamp($$7, 0.0F, 1.0F);
         $$7 *= $$7;
         $$7 *= $$7;
         float $$8 = 1.0F + $$7 * 0.3F;
         p_116154_.scale($$8, $$8, $$8);
      }

      renderWhiteSolidBlock(this.blockRenderer, p_116153_, p_116154_, p_116155_, p_116156_, $$6 > -1 && $$6 / 5 % 2 == 0);
   }

   public static void renderWhiteSolidBlock(
      BlockRenderDispatcher p_234662_, BlockState p_234663_, PoseStack p_234664_, MultiBufferSource p_234665_, int p_234666_, boolean p_234667_
   ) {
      int $$6;
      if (p_234667_) {
         $$6 = OverlayTexture.pack(OverlayTexture.u(1.0F), 10);
      } else {
         $$6 = OverlayTexture.NO_OVERLAY;
      }

      p_234662_.renderSingleBlock(p_234663_, p_234664_, p_234665_, p_234666_, $$6);
   }
}
