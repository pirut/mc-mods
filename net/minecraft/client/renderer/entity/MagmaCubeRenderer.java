package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.LavaSlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.MagmaCube;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MagmaCubeRenderer extends MobRenderer<MagmaCube, LavaSlimeModel<MagmaCube>> {
   private static final ResourceLocation MAGMACUBE_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/slime/magmacube.png");

   public MagmaCubeRenderer(EntityRendererProvider.Context p_174298_) {
      super(p_174298_, new LavaSlimeModel<>(p_174298_.bakeLayer(ModelLayers.MAGMA_CUBE)), 0.25F);
   }

   protected int getBlockLightLevel(MagmaCube p_115399_, BlockPos p_115400_) {
      return 15;
   }

   public ResourceLocation getTextureLocation(MagmaCube p_115393_) {
      return MAGMACUBE_LOCATION;
   }

   public void render(MagmaCube p_265315_, float p_265620_, float p_265669_, PoseStack p_265647_, MultiBufferSource p_265147_, int p_265465_) {
      this.shadowRadius = 0.25F * p_265315_.getSize();
      super.render(p_265315_, p_265620_, p_265669_, p_265647_, p_265147_, p_265465_);
   }

   protected void scale(MagmaCube p_115395_, PoseStack p_115396_, float p_115397_) {
      int $$3 = p_115395_.getSize();
      float $$4 = Mth.lerp(p_115397_, p_115395_.oSquish, p_115395_.squish) / ($$3 * 0.5F + 1.0F);
      float $$5 = 1.0F / ($$4 + 1.0F);
      p_115396_.scale($$5 * $$3, 1.0F / $$5 * $$3, $$5 * $$3);
   }
}
