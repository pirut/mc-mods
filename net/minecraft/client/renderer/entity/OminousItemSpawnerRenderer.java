package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.OminousItemSpawner;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class OminousItemSpawnerRenderer extends EntityRenderer<OminousItemSpawner> {
   private static final float ROTATION_SPEED = 40.0F;
   private static final int TICKS_SCALING = 50;
   private final ItemRenderer itemRenderer;

   protected OminousItemSpawnerRenderer(EntityRendererProvider.Context p_338603_) {
      super(p_338603_);
      this.itemRenderer = p_338603_.getItemRenderer();
   }

   public ResourceLocation getTextureLocation(OminousItemSpawner p_338515_) {
      return TextureAtlas.LOCATION_BLOCKS;
   }

   public void render(OminousItemSpawner p_338815_, float p_338631_, float p_338539_, PoseStack p_338440_, MultiBufferSource p_338413_, int p_338541_) {
      ItemStack $$6 = p_338815_.getItem();
      if (!$$6.isEmpty()) {
         p_338440_.pushPose();
         if (p_338815_.tickCount <= 50) {
            float $$7 = Math.min(p_338815_.tickCount + p_338539_, 50.0F) / 50.0F;
            p_338440_.scale($$7, $$7, $$7);
         }

         Level $$8 = p_338815_.level();
         float $$9 = Mth.wrapDegrees((float)($$8.getGameTime() - 1L)) * 40.0F;
         float $$10 = Mth.wrapDegrees((float)$$8.getGameTime()) * 40.0F;
         p_338440_.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(p_338539_, $$9, $$10)));
         ItemEntityRenderer.renderMultipleFromCount(this.itemRenderer, p_338440_, p_338413_, 15728880, $$6, $$8.random, $$8);
         p_338440_.popPose();
      }
   }
}
