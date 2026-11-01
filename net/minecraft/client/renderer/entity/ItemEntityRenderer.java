package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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

@Environment(EnvType.CLIENT)
public class ItemEntityRenderer extends EntityRenderer<ItemEntity, ItemEntityRenderState> {
   private static final float ITEM_MIN_HOVER_HEIGHT = 0.0625F;
   private static final float ITEM_BUNDLE_OFFSET_SCALE = 0.15F;
   private static final float FLAT_ITEM_DEPTH_THRESHOLD = 0.0625F;
   private final ItemModelResolver itemModelResolver;
   private final RandomSource random = RandomSource.create();

   public ItemEntityRenderer(EntityRendererProvider.Context context) {
      super(context);
      this.itemModelResolver = context.getItemModelResolver();
      this.shadowRadius = 0.15F;
      this.shadowStrength = 0.75F;
   }

   public ItemEntityRenderState createRenderState() {
      return new ItemEntityRenderState();
   }

   public void extractRenderState(ItemEntity itemEntity, ItemEntityRenderState itemEntityRenderState, float f) {
      super.extractRenderState(itemEntity, itemEntityRenderState, f);
      itemEntityRenderState.bobOffset = itemEntity.bobOffs;
      itemEntityRenderState.extractItemGroupRenderState(itemEntity, itemEntity.getItem(), this.itemModelResolver);
   }

   public void submit(
      ItemEntityRenderState itemEntityRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState
   ) {
      if (!itemEntityRenderState.item.isEmpty()) {
         poseStack.pushPose();
         AABB aABB = itemEntityRenderState.item.getModelBoundingBox();
         float f = -((float)aABB.minY) + 0.0625F;
         float g = Mth.sin(itemEntityRenderState.ageInTicks / 10.0F + itemEntityRenderState.bobOffset) * 0.1F + 0.1F;
         poseStack.translate(0.0F, g + f, 0.0F);
         float h = ItemEntity.getSpin(itemEntityRenderState.ageInTicks, itemEntityRenderState.bobOffset);
         poseStack.mulPose(Axis.YP.rotation(h));
         submitMultipleFromCount(poseStack, submitNodeCollector, itemEntityRenderState.lightCoords, itemEntityRenderState, this.random, aABB);
         poseStack.popPose();
         super.submit(itemEntityRenderState, poseStack, submitNodeCollector, cameraRenderState);
      }
   }

   public static void submitMultipleFromCount(
      PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, ItemClusterRenderState renderState, RandomSource random
   ) {
      submitMultipleFromCount(poseStack, nodeCollector, packedLight, renderState, random, renderState.item.getModelBoundingBox());
   }

   public static void submitMultipleFromCount(
      PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, ItemClusterRenderState renderState, RandomSource random, AABB boundingBox
   ) {
      int i = renderState.count;
      if (i != 0) {
         random.setSeed(renderState.seed);
         ItemStackRenderState itemStackRenderState = renderState.item;
         float f = (float)boundingBox.getZsize();
         if (f > 0.0625F) {
            itemStackRenderState.submit(poseStack, nodeCollector, packedLight, OverlayTexture.NO_OVERLAY, renderState.outlineColor);

            for (int j = 1; j < i; j++) {
               poseStack.pushPose();
               float g = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
               float h = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
               float k = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
               poseStack.translate(g, h, k);
               itemStackRenderState.submit(poseStack, nodeCollector, packedLight, OverlayTexture.NO_OVERLAY, renderState.outlineColor);
               poseStack.popPose();
            }
         } else {
            float l = f * 1.5F;
            poseStack.translate(0.0F, 0.0F, -(l * (i - 1) / 2.0F));
            itemStackRenderState.submit(poseStack, nodeCollector, packedLight, OverlayTexture.NO_OVERLAY, renderState.outlineColor);
            poseStack.translate(0.0F, 0.0F, l);

            for (int m = 1; m < i; m++) {
               poseStack.pushPose();
               float h = (random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
               float k = (random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
               poseStack.translate(h, k, 0.0F);
               itemStackRenderState.submit(poseStack, nodeCollector, packedLight, OverlayTexture.NO_OVERLAY, renderState.outlineColor);
               poseStack.popPose();
               poseStack.translate(0.0F, 0.0F, l);
            }
         }
      }
   }

   public static void renderMultipleFromCount(
      PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, ItemClusterRenderState renderState, RandomSource random
   ) {
      AABB aABB = renderState.item.getModelBoundingBox();
      int i = renderState.count;
      if (i != 0) {
         random.setSeed(renderState.seed);
         ItemStackRenderState itemStackRenderState = renderState.item;
         float f = (float)aABB.getZsize();
         if (f > 0.0625F) {
            itemStackRenderState.submit(poseStack, nodeCollector, packedLight, OverlayTexture.NO_OVERLAY, renderState.outlineColor);

            for (int j = 1; j < i; j++) {
               poseStack.pushPose();
               float g = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
               float h = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
               float k = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
               poseStack.translate(g, h, k);
               itemStackRenderState.submit(poseStack, nodeCollector, packedLight, OverlayTexture.NO_OVERLAY, renderState.outlineColor);
               poseStack.popPose();
            }
         } else {
            float l = f * 1.5F;
            poseStack.translate(0.0F, 0.0F, -(l * (i - 1) / 2.0F));
            itemStackRenderState.submit(poseStack, nodeCollector, packedLight, OverlayTexture.NO_OVERLAY, renderState.outlineColor);
            poseStack.translate(0.0F, 0.0F, l);

            for (int m = 1; m < i; m++) {
               poseStack.pushPose();
               float h = (random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
               float k = (random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
               poseStack.translate(h, k, 0.0F);
               itemStackRenderState.submit(poseStack, nodeCollector, packedLight, OverlayTexture.NO_OVERLAY, renderState.outlineColor);
               poseStack.popPose();
               poseStack.translate(0.0F, 0.0F, l);
            }
         }
      }
   }
}
