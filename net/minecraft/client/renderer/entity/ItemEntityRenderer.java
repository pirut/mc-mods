package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

@OnlyIn(Dist.CLIENT)
public class ItemEntityRenderer extends EntityRenderer<ItemEntity, ItemEntityRenderState> {
   private static final float ITEM_MIN_HOVER_HEIGHT = 0.0625F;
   private static final float ITEM_BUNDLE_OFFSET_SCALE = 0.15F;
   private static final float FLAT_ITEM_DEPTH_THRESHOLD = 0.0625F;
   private final ItemModelResolver itemModelResolver;
   private final RandomSource random = RandomSource.create();

   public ItemEntityRenderer(EntityRendererProvider.Context p_174198_) {
      super(p_174198_);
      this.itemModelResolver = p_174198_.getItemModelResolver();
      this.shadowRadius = 0.15F;
      this.shadowStrength = 0.75F;
   }

   public ItemEntityRenderState createRenderState() {
      return new ItemEntityRenderState();
   }

   public void extractRenderState(ItemEntity p_362393_, ItemEntityRenderState p_361441_, float p_360849_) {
      super.extractRenderState(p_362393_, p_361441_, p_360849_);
      p_361441_.bobOffset = p_362393_.bobOffs;
      p_361441_.shouldBob = IClientItemExtensions.of(p_362393_.getItem()).shouldBobAsEntity(p_362393_.getItem());
      p_361441_.shouldSpread = IClientItemExtensions.of(p_362393_.getItem()).shouldSpreadAsEntity(p_362393_.getItem());
      p_361441_.extractItemGroupRenderState(p_362393_, p_362393_.getItem(), this.itemModelResolver);
   }

   public void submit(ItemEntityRenderState p_435798_, PoseStack p_435012_, SubmitNodeCollector p_435566_, CameraRenderState p_451462_) {
      if (!p_435798_.item.isEmpty()) {
         p_435012_.pushPose();
         AABB aabb = p_435798_.item.getModelBoundingBox();
         float f = -((float)aabb.minY) + 0.0625F;
         float f1 = p_435798_.shouldBob ? Mth.sin(p_435798_.ageInTicks / 10.0F + p_435798_.bobOffset) * 0.1F + 0.1F : 0.0F;
         p_435012_.translate(0.0F, f1 + f, 0.0F);
         float f2 = ItemEntity.getSpin(p_435798_.ageInTicks, p_435798_.bobOffset);
         p_435012_.mulPose(Axis.YP.rotation(f2));
         submitMultipleFromCount(p_435012_, p_435566_, p_435798_.lightCoords, p_435798_, this.random, aabb);
         p_435012_.popPose();
         super.submit(p_435798_, p_435012_, p_435566_, p_451462_);
      }
   }

   public static void submitMultipleFromCount(
      PoseStack p_435192_, SubmitNodeCollector p_435555_, int p_435661_, ItemClusterRenderState p_435706_, RandomSource p_435000_
   ) {
      submitMultipleFromCount(p_435192_, p_435555_, p_435661_, p_435706_, p_435000_, p_435706_.item.getModelBoundingBox());
   }

   public static void submitMultipleFromCount(
      PoseStack p_404890_, SubmitNodeCollector p_433429_, int p_404762_, ItemClusterRenderState p_405697_, RandomSource p_404894_, AABB p_405524_
   ) {
      int i = p_405697_.count;
      if (i != 0) {
         p_404894_.setSeed(p_405697_.seed);
         ItemStackRenderState itemstackrenderstate = p_405697_.item;
         float f = (float)p_405524_.getZsize();
         if (f > 0.0625F) {
            itemstackrenderstate.submit(p_404890_, p_433429_, p_404762_, OverlayTexture.NO_OVERLAY, p_405697_.outlineColor);

            for (int j = 1; j < i; j++) {
               p_404890_.pushPose();
               float f1 = (p_404894_.nextFloat() * 2.0F - 1.0F) * 0.15F;
               float f2 = (p_404894_.nextFloat() * 2.0F - 1.0F) * 0.15F;
               float f3 = (p_404894_.nextFloat() * 2.0F - 1.0F) * 0.15F;
               if (p_405697_.shouldSpread) {
                  p_404890_.translate(f1, f2, f3);
               }

               itemstackrenderstate.submit(p_404890_, p_433429_, p_404762_, OverlayTexture.NO_OVERLAY, p_405697_.outlineColor);
               p_404890_.popPose();
            }
         } else {
            float f4 = f * 1.5F;
            p_404890_.translate(0.0F, 0.0F, -(f4 * (i - 1) / 2.0F));
            itemstackrenderstate.submit(p_404890_, p_433429_, p_404762_, OverlayTexture.NO_OVERLAY, p_405697_.outlineColor);
            p_404890_.translate(0.0F, 0.0F, f4);

            for (int k = 1; k < i; k++) {
               p_404890_.pushPose();
               float f5 = (p_404894_.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
               float f6 = (p_404894_.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
               if (p_405697_.shouldSpread) {
                  p_404890_.translate(f5, f6, 0.0F);
               }

               itemstackrenderstate.submit(p_404890_, p_433429_, p_404762_, OverlayTexture.NO_OVERLAY, p_405697_.outlineColor);
               p_404890_.popPose();
               p_404890_.translate(0.0F, 0.0F, f4);
            }
         }
      }
   }

   public static void renderMultipleFromCount(
      PoseStack p_323763_, SubmitNodeCollector p_439514_, int p_323603_, ItemClusterRenderState p_388704_, RandomSource p_324507_
   ) {
      AABB aabb = p_388704_.item.getModelBoundingBox();
      int i = p_388704_.count;
      if (i != 0) {
         p_324507_.setSeed(p_388704_.seed);
         ItemStackRenderState itemstackrenderstate = p_388704_.item;
         float f = (float)aabb.getZsize();
         if (f > 0.0625F) {
            itemstackrenderstate.submit(p_323763_, p_439514_, p_323603_, OverlayTexture.NO_OVERLAY, p_388704_.outlineColor);

            for (int j = 1; j < i; j++) {
               p_323763_.pushPose();
               float f1 = (p_324507_.nextFloat() * 2.0F - 1.0F) * 0.15F;
               float f2 = (p_324507_.nextFloat() * 2.0F - 1.0F) * 0.15F;
               float f3 = (p_324507_.nextFloat() * 2.0F - 1.0F) * 0.15F;
               p_323763_.translate(f1, f2, f3);
               itemstackrenderstate.submit(p_323763_, p_439514_, p_323603_, OverlayTexture.NO_OVERLAY, p_388704_.outlineColor);
               p_323763_.popPose();
            }
         } else {
            float f4 = f * 1.5F;
            p_323763_.translate(0.0F, 0.0F, -(f4 * (i - 1) / 2.0F));
            itemstackrenderstate.submit(p_323763_, p_439514_, p_323603_, OverlayTexture.NO_OVERLAY, p_388704_.outlineColor);
            p_323763_.translate(0.0F, 0.0F, f4);

            for (int k = 1; k < i; k++) {
               p_323763_.pushPose();
               float f5 = (p_324507_.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
               float f6 = (p_324507_.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
               p_323763_.translate(f5, f6, 0.0F);
               itemstackrenderstate.submit(p_323763_, p_439514_, p_323603_, OverlayTexture.NO_OVERLAY, p_388704_.outlineColor);
               p_323763_.popPose();
               p_323763_.translate(0.0F, 0.0F, f4);
            }
         }
      }
   }
}
