package net.minecraft.client.renderer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.effects.SpearAnimations;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

@Environment(EnvType.CLIENT)
public class ItemInHandRenderer {
   private static final RenderType MAP_BACKGROUND = RenderTypes.text(Identifier.withDefaultNamespace("textures/map/map_background.png"));
   private static final RenderType MAP_BACKGROUND_CHECKERBOARD = RenderTypes.text(
      Identifier.withDefaultNamespace("textures/map/map_background_checkerboard.png")
   );
   private static final float ITEM_SWING_X_POS_SCALE = -0.4F;
   private static final float ITEM_SWING_Y_POS_SCALE = 0.2F;
   private static final float ITEM_SWING_Z_POS_SCALE = -0.2F;
   private static final float ITEM_HEIGHT_SCALE = -0.6F;
   private static final float ITEM_POS_X = 0.56F;
   private static final float ITEM_POS_Y = -0.52F;
   private static final float ITEM_POS_Z = -0.72F;
   private static final float ITEM_PRESWING_ROT_Y = 45.0F;
   private static final float ITEM_SWING_X_ROT_AMOUNT = -80.0F;
   private static final float ITEM_SWING_Y_ROT_AMOUNT = -20.0F;
   private static final float ITEM_SWING_Z_ROT_AMOUNT = -20.0F;
   private static final float EAT_JIGGLE_X_ROT_AMOUNT = 10.0F;
   private static final float EAT_JIGGLE_Y_ROT_AMOUNT = 90.0F;
   private static final float EAT_JIGGLE_Z_ROT_AMOUNT = 30.0F;
   private static final float EAT_JIGGLE_X_POS_SCALE = 0.6F;
   private static final float EAT_JIGGLE_Y_POS_SCALE = -0.5F;
   private static final float EAT_JIGGLE_Z_POS_SCALE = 0.0F;
   private static final double EAT_JIGGLE_EXPONENT = 27.0;
   private static final float EAT_EXTRA_JIGGLE_CUTOFF = 0.8F;
   private static final float EAT_EXTRA_JIGGLE_SCALE = 0.1F;
   private static final float ARM_SWING_X_POS_SCALE = -0.3F;
   private static final float ARM_SWING_Y_POS_SCALE = 0.4F;
   private static final float ARM_SWING_Z_POS_SCALE = -0.4F;
   private static final float ARM_SWING_Y_ROT_AMOUNT = 70.0F;
   private static final float ARM_SWING_Z_ROT_AMOUNT = -20.0F;
   private static final float ARM_HEIGHT_SCALE = -0.6F;
   private static final float ARM_POS_SCALE = 0.8F;
   private static final float ARM_POS_X = 0.8F;
   private static final float ARM_POS_Y = -0.75F;
   private static final float ARM_POS_Z = -0.9F;
   private static final float ARM_PRESWING_ROT_Y = 45.0F;
   private static final float ARM_PREROTATION_X_OFFSET = -1.0F;
   private static final float ARM_PREROTATION_Y_OFFSET = 3.6F;
   private static final float ARM_PREROTATION_Z_OFFSET = 3.5F;
   private static final float ARM_POSTROTATION_X_OFFSET = 5.6F;
   private static final int ARM_ROT_X = 200;
   private static final int ARM_ROT_Y = -135;
   private static final int ARM_ROT_Z = 120;
   private static final float MAP_SWING_X_POS_SCALE = -0.4F;
   private static final float MAP_SWING_Z_POS_SCALE = -0.2F;
   private static final float MAP_HANDS_POS_X = 0.0F;
   private static final float MAP_HANDS_POS_Y = 0.04F;
   private static final float MAP_HANDS_POS_Z = -0.72F;
   private static final float MAP_HANDS_HEIGHT_SCALE = -1.2F;
   private static final float MAP_HANDS_TILT_SCALE = -0.5F;
   private static final float MAP_PLAYER_PITCH_SCALE = 45.0F;
   private static final float MAP_HANDS_Z_ROT_AMOUNT = -85.0F;
   private static final float MAPHAND_X_ROT_AMOUNT = 45.0F;
   private static final float MAPHAND_Y_ROT_AMOUNT = 92.0F;
   private static final float MAPHAND_Z_ROT_AMOUNT = -41.0F;
   private static final float MAP_HAND_X_POS = 0.3F;
   private static final float MAP_HAND_Y_POS = -1.1F;
   private static final float MAP_HAND_Z_POS = 0.45F;
   private static final float MAP_SWING_X_ROT_AMOUNT = 20.0F;
   private static final float MAP_PRE_ROT_SCALE = 0.38F;
   private static final float MAP_GLOBAL_X_POS = -0.5F;
   private static final float MAP_GLOBAL_Y_POS = -0.5F;
   private static final float MAP_GLOBAL_Z_POS = 0.0F;
   private static final float MAP_FINAL_SCALE = 0.0078125F;
   private static final int MAP_BORDER = 7;
   private static final int MAP_HEIGHT = 128;
   private static final int MAP_WIDTH = 128;
   private static final float BOW_CHARGE_X_POS_SCALE = 0.0F;
   private static final float BOW_CHARGE_Y_POS_SCALE = 0.0F;
   private static final float BOW_CHARGE_Z_POS_SCALE = 0.04F;
   private static final float BOW_CHARGE_SHAKE_X_SCALE = 0.0F;
   private static final float BOW_CHARGE_SHAKE_Y_SCALE = 0.004F;
   private static final float BOW_CHARGE_SHAKE_Z_SCALE = 0.0F;
   private static final float BOW_CHARGE_Z_SCALE = 0.2F;
   private static final float BOW_MIN_SHAKE_CHARGE = 0.1F;
   private final Minecraft minecraft;
   private final MapRenderState mapRenderState = new MapRenderState();
   private ItemStack mainHandItem = ItemStack.EMPTY;
   private ItemStack offHandItem = ItemStack.EMPTY;
   private float mainHandHeight;
   private float oMainHandHeight;
   private float offHandHeight;
   private float oOffHandHeight;
   private final EntityRenderDispatcher entityRenderDispatcher;
   private final ItemModelResolver itemModelResolver;

   public ItemInHandRenderer(Minecraft minecraft, EntityRenderDispatcher entityRenderDispatcher, ItemModelResolver itemModelResolver) {
      this.minecraft = minecraft;
      this.entityRenderDispatcher = entityRenderDispatcher;
      this.itemModelResolver = itemModelResolver;
   }

   public void renderItem(
      LivingEntity entity, ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight
   ) {
      if (!stack.isEmpty()) {
         ItemStackRenderState itemStackRenderState = new ItemStackRenderState();
         this.itemModelResolver
            .updateForTopItem(itemStackRenderState, stack, displayContext, entity.level(), entity, entity.getId() + displayContext.ordinal());
         itemStackRenderState.submit(poseStack, nodeCollector, packedLight, OverlayTexture.NO_OVERLAY, 0);
      }
   }

   private float calculateMapTilt(float pitch) {
      float f = 1.0F - pitch / 45.0F + 0.1F;
      f = Mth.clamp(f, 0.0F, 1.0F);
      return -Mth.cos(f * (float) Math.PI) * 0.5F + 0.5F;
   }

   private void renderMapHand(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, HumanoidArm arm) {
      AvatarRenderer<AbstractClientPlayer> avatarRenderer = this.entityRenderDispatcher.getPlayerRenderer(this.minecraft.player);
      poseStack.pushPose();
      float f = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
      poseStack.mulPose(Axis.YP.rotationDegrees(92.0F));
      poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
      poseStack.mulPose(Axis.ZP.rotationDegrees(f * -41.0F));
      poseStack.translate(f * 0.3F, -1.1F, 0.45F);
      Identifier identifier = this.minecraft.player.getSkin().body().texturePath();
      if (arm == HumanoidArm.RIGHT) {
         avatarRenderer.renderRightHand(poseStack, nodeCollector, packedLight, identifier, this.minecraft.player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE));
      } else {
         avatarRenderer.renderLeftHand(poseStack, nodeCollector, packedLight, identifier, this.minecraft.player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE));
      }

      poseStack.popPose();
   }

   private void renderOneHandedMap(
      PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, float equippedProgress, HumanoidArm arm, float swingProgress, ItemStack stack
   ) {
      float f = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
      poseStack.translate(f * 0.125F, -0.125F, 0.0F);
      if (!this.minecraft.player.isInvisible()) {
         poseStack.pushPose();
         poseStack.mulPose(Axis.ZP.rotationDegrees(f * 10.0F));
         this.renderPlayerArm(poseStack, nodeCollector, packedLight, equippedProgress, swingProgress, arm);
         poseStack.popPose();
      }

      poseStack.pushPose();
      poseStack.translate(f * 0.51F, -0.08F + equippedProgress * -1.2F, -0.75F);
      float g = Mth.sqrt(swingProgress);
      float h = Mth.sin(g * (float) Math.PI);
      float i = -0.5F * h;
      float j = 0.4F * Mth.sin(g * (float) (Math.PI * 2));
      float k = -0.3F * Mth.sin(swingProgress * (float) Math.PI);
      poseStack.translate(f * i, j - 0.3F * h, k);
      poseStack.mulPose(Axis.XP.rotationDegrees(h * -45.0F));
      poseStack.mulPose(Axis.YP.rotationDegrees(f * h * -30.0F));
      this.renderMap(poseStack, nodeCollector, packedLight, stack);
      poseStack.popPose();
   }

   private void renderTwoHandedMap(
      PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, float pitch, float equippedProgress, float swingProgress
   ) {
      float f = Mth.sqrt(swingProgress);
      float g = -0.2F * Mth.sin(swingProgress * (float) Math.PI);
      float h = -0.4F * Mth.sin(f * (float) Math.PI);
      poseStack.translate(0.0F, -g / 2.0F, h);
      float i = this.calculateMapTilt(pitch);
      poseStack.translate(0.0F, 0.04F + equippedProgress * -1.2F + i * -0.5F, -0.72F);
      poseStack.mulPose(Axis.XP.rotationDegrees(i * -85.0F));
      if (!this.minecraft.player.isInvisible()) {
         poseStack.pushPose();
         poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
         this.renderMapHand(poseStack, nodeCollector, packedLight, HumanoidArm.RIGHT);
         this.renderMapHand(poseStack, nodeCollector, packedLight, HumanoidArm.LEFT);
         poseStack.popPose();
      }

      float j = Mth.sin(f * (float) Math.PI);
      poseStack.mulPose(Axis.XP.rotationDegrees(j * 20.0F));
      poseStack.scale(2.0F, 2.0F, 2.0F);
      this.renderMap(poseStack, nodeCollector, packedLight, this.mainHandItem);
   }

   private void renderMap(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, ItemStack stack) {
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
      poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
      poseStack.scale(0.38F, 0.38F, 0.38F);
      poseStack.translate(-0.5F, -0.5F, 0.0F);
      poseStack.scale(0.0078125F, 0.0078125F, 0.0078125F);
      MapId mapId = stack.get(DataComponents.MAP_ID);
      MapItemSavedData mapItemSavedData = MapItem.getSavedData(mapId, this.minecraft.level);
      RenderType renderType = mapItemSavedData == null ? MAP_BACKGROUND : MAP_BACKGROUND_CHECKERBOARD;
      nodeCollector.submitCustomGeometry(poseStack, renderType, (pose, vertexConsumer) -> {
         vertexConsumer.addVertex(pose, -7.0F, 135.0F, 0.0F).setColor(-1).setUv(0.0F, 1.0F).setLight(packedLight);
         vertexConsumer.addVertex(pose, 135.0F, 135.0F, 0.0F).setColor(-1).setUv(1.0F, 1.0F).setLight(packedLight);
         vertexConsumer.addVertex(pose, 135.0F, -7.0F, 0.0F).setColor(-1).setUv(1.0F, 0.0F).setLight(packedLight);
         vertexConsumer.addVertex(pose, -7.0F, -7.0F, 0.0F).setColor(-1).setUv(0.0F, 0.0F).setLight(packedLight);
      });
      if (mapItemSavedData != null) {
         MapRenderer mapRenderer = this.minecraft.getMapRenderer();
         mapRenderer.extractRenderState(mapId, mapItemSavedData, this.mapRenderState);
         mapRenderer.render(this.mapRenderState, poseStack, nodeCollector, false, packedLight);
      }
   }

   private void renderPlayerArm(
      PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, float equippedProgress, float swingProgress, HumanoidArm arm
   ) {
      boolean bl = arm != HumanoidArm.LEFT;
      float f = bl ? 1.0F : -1.0F;
      float g = Mth.sqrt(swingProgress);
      float h = -0.3F * Mth.sin(g * (float) Math.PI);
      float i = 0.4F * Mth.sin(g * (float) (Math.PI * 2));
      float j = -0.4F * Mth.sin(swingProgress * (float) Math.PI);
      poseStack.translate(f * (h + 0.64000005F), i + -0.6F + equippedProgress * -0.6F, j + -0.71999997F);
      poseStack.mulPose(Axis.YP.rotationDegrees(f * 45.0F));
      float k = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
      float l = Mth.sin(g * (float) Math.PI);
      poseStack.mulPose(Axis.YP.rotationDegrees(f * l * 70.0F));
      poseStack.mulPose(Axis.ZP.rotationDegrees(f * k * -20.0F));
      AbstractClientPlayer abstractClientPlayer = this.minecraft.player;
      poseStack.translate(f * -1.0F, 3.6F, 3.5F);
      poseStack.mulPose(Axis.ZP.rotationDegrees(f * 120.0F));
      poseStack.mulPose(Axis.XP.rotationDegrees(200.0F));
      poseStack.mulPose(Axis.YP.rotationDegrees(f * -135.0F));
      poseStack.translate(f * 5.6F, 0.0F, 0.0F);
      AvatarRenderer<AbstractClientPlayer> avatarRenderer = this.entityRenderDispatcher.getPlayerRenderer(abstractClientPlayer);
      Identifier identifier = abstractClientPlayer.getSkin().body().texturePath();
      if (bl) {
         avatarRenderer.renderRightHand(poseStack, nodeCollector, packedLight, identifier, abstractClientPlayer.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE));
      } else {
         avatarRenderer.renderLeftHand(poseStack, nodeCollector, packedLight, identifier, abstractClientPlayer.isModelPartShown(PlayerModelPart.LEFT_SLEEVE));
      }
   }

   private void applyEatTransform(PoseStack poseStack, float partialTick, HumanoidArm arm, ItemStack stack, Player player) {
      float f = player.getUseItemRemainingTicks() - partialTick + 1.0F;
      float g = f / stack.getUseDuration(player);
      if (g < 0.8F) {
         float h = Mth.abs(Mth.cos(f / 4.0F * (float) Math.PI) * 0.1F);
         poseStack.translate(0.0F, h, 0.0F);
      }

      float h = 1.0F - (float)Math.pow(g, 27.0);
      int i = arm == HumanoidArm.RIGHT ? 1 : -1;
      poseStack.translate(h * 0.6F * i, h * -0.5F, h * 0.0F);
      poseStack.mulPose(Axis.YP.rotationDegrees(i * h * 90.0F));
      poseStack.mulPose(Axis.XP.rotationDegrees(h * 10.0F));
      poseStack.mulPose(Axis.ZP.rotationDegrees(i * h * 30.0F));
   }

   private void applyBrushTransform(PoseStack poseStack, float partialTick, HumanoidArm arm, Player player) {
      float f = player.getUseItemRemainingTicks() % 10;
      float g = f - partialTick + 1.0F;
      float h = 1.0F - g / 10.0F;
      float i = -90.0F;
      float j = 60.0F;
      float k = 150.0F;
      float l = -15.0F;
      int m = 2;
      float n = -15.0F + 75.0F * Mth.cos(h * 2.0F * (float) Math.PI);
      if (arm != HumanoidArm.RIGHT) {
         poseStack.translate(0.1, 0.83, 0.35);
         poseStack.mulPose(Axis.XP.rotationDegrees(-80.0F));
         poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
         poseStack.mulPose(Axis.XP.rotationDegrees(n));
         poseStack.translate(-0.3, 0.22, 0.35);
      } else {
         poseStack.translate(-0.25, 0.22, 0.35);
         poseStack.mulPose(Axis.XP.rotationDegrees(-80.0F));
         poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
         poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
         poseStack.mulPose(Axis.XP.rotationDegrees(n));
      }
   }

   private void applyItemArmAttackTransform(PoseStack poseStack, HumanoidArm hand, float swingProgress) {
      int i = hand == HumanoidArm.RIGHT ? 1 : -1;
      float f = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
      poseStack.mulPose(Axis.YP.rotationDegrees(i * (45.0F + f * -20.0F)));
      float g = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
      poseStack.mulPose(Axis.ZP.rotationDegrees(i * g * -20.0F));
      poseStack.mulPose(Axis.XP.rotationDegrees(g * -80.0F));
      poseStack.mulPose(Axis.YP.rotationDegrees(i * -45.0F));
   }

   private void applyItemArmTransform(PoseStack poseStack, HumanoidArm hand, float equippedProg) {
      int i = hand == HumanoidArm.RIGHT ? 1 : -1;
      poseStack.translate(i * 0.56F, -0.52F + equippedProg * -0.6F, -0.72F);
   }

   public void renderHandsWithItems(float partialTick, PoseStack poseStack, SubmitNodeCollector nodeCollector, LocalPlayer player, int packedLight) {
      float f = player.getAttackAnim(partialTick);
      InteractionHand interactionHand = (InteractionHand)MoreObjects.firstNonNull(player.swingingArm, InteractionHand.MAIN_HAND);
      float g = player.getXRot(partialTick);
      ItemInHandRenderer.HandRenderSelection handRenderSelection = evaluateWhichHandsToRender(player);
      float h = Mth.lerp(partialTick, player.xBobO, player.xBob);
      float i = Mth.lerp(partialTick, player.yBobO, player.yBob);
      poseStack.mulPose(Axis.XP.rotationDegrees((player.getViewXRot(partialTick) - h) * 0.1F));
      poseStack.mulPose(Axis.YP.rotationDegrees((player.getViewYRot(partialTick) - i) * 0.1F));
      if (handRenderSelection.renderMainHand) {
         float j = interactionHand == InteractionHand.MAIN_HAND ? f : 0.0F;
         float k = this.itemModelResolver.swapAnimationScale(this.mainHandItem) * (1.0F - Mth.lerp(partialTick, this.oMainHandHeight, this.mainHandHeight));
         this.renderArmWithItem(player, partialTick, g, InteractionHand.MAIN_HAND, j, this.mainHandItem, k, poseStack, nodeCollector, packedLight);
      }

      if (handRenderSelection.renderOffHand) {
         float j = interactionHand == InteractionHand.OFF_HAND ? f : 0.0F;
         float k = this.itemModelResolver.swapAnimationScale(this.offHandItem) * (1.0F - Mth.lerp(partialTick, this.oOffHandHeight, this.offHandHeight));
         this.renderArmWithItem(player, partialTick, g, InteractionHand.OFF_HAND, j, this.offHandItem, k, poseStack, nodeCollector, packedLight);
      }

      this.minecraft.gameRenderer.getFeatureRenderDispatcher().renderAllFeatures();
      this.minecraft.renderBuffers().bufferSource().endBatch();
   }

   @VisibleForTesting
   static ItemInHandRenderer.HandRenderSelection evaluateWhichHandsToRender(LocalPlayer player) {
      ItemStack itemStack = player.getMainHandItem();
      ItemStack itemStack2 = player.getOffhandItem();
      boolean bl = itemStack.is(Items.BOW) || itemStack2.is(Items.BOW);
      boolean bl2 = itemStack.is(Items.CROSSBOW) || itemStack2.is(Items.CROSSBOW);
      if (!bl && !bl2) {
         return ItemInHandRenderer.HandRenderSelection.RENDER_BOTH_HANDS;
      } else if (player.isUsingItem()) {
         return selectionUsingItemWhileHoldingBowLike(player);
      } else {
         return isChargedCrossbow(itemStack)
            ? ItemInHandRenderer.HandRenderSelection.RENDER_MAIN_HAND_ONLY
            : ItemInHandRenderer.HandRenderSelection.RENDER_BOTH_HANDS;
      }
   }

   private static ItemInHandRenderer.HandRenderSelection selectionUsingItemWhileHoldingBowLike(LocalPlayer player) {
      ItemStack itemStack = player.getUseItem();
      InteractionHand interactionHand = player.getUsedItemHand();
      if (!itemStack.is(Items.BOW) && !itemStack.is(Items.CROSSBOW)) {
         return interactionHand == InteractionHand.MAIN_HAND && isChargedCrossbow(player.getOffhandItem())
            ? ItemInHandRenderer.HandRenderSelection.RENDER_MAIN_HAND_ONLY
            : ItemInHandRenderer.HandRenderSelection.RENDER_BOTH_HANDS;
      } else {
         return ItemInHandRenderer.HandRenderSelection.onlyForHand(interactionHand);
      }
   }

   private static boolean isChargedCrossbow(ItemStack stack) {
      return stack.is(Items.CROSSBOW) && CrossbowItem.isCharged(stack);
   }

   private void renderArmWithItem(
      AbstractClientPlayer player,
      float partialTick,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack item,
      float equippedProgress,
      PoseStack poseStack,
      SubmitNodeCollector nodeCollector,
      int packedLight
   ) {
      if (!player.isScoping()) {
         boolean bl = hand == InteractionHand.MAIN_HAND;
         HumanoidArm humanoidArm = bl ? player.getMainArm() : player.getMainArm().getOpposite();
         poseStack.pushPose();
         if (item.isEmpty()) {
            if (bl && !player.isInvisible()) {
               this.renderPlayerArm(poseStack, nodeCollector, packedLight, equippedProgress, swingProgress, humanoidArm);
            }
         } else if (item.has(DataComponents.MAP_ID)) {
            if (bl && this.offHandItem.isEmpty()) {
               this.renderTwoHandedMap(poseStack, nodeCollector, packedLight, pitch, equippedProgress, swingProgress);
            } else {
               this.renderOneHandedMap(poseStack, nodeCollector, packedLight, equippedProgress, humanoidArm, swingProgress, item);
            }
         } else if (item.is(Items.CROSSBOW)) {
            this.applyItemArmTransform(poseStack, humanoidArm, equippedProgress);
            boolean bl2 = CrossbowItem.isCharged(item);
            boolean bl3 = humanoidArm == HumanoidArm.RIGHT;
            int i = bl3 ? 1 : -1;
            if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand && !bl2) {
               poseStack.translate(i * -0.4785682F, -0.094387F, 0.05731531F);
               poseStack.mulPose(Axis.XP.rotationDegrees(-11.935F));
               poseStack.mulPose(Axis.YP.rotationDegrees(i * 65.3F));
               poseStack.mulPose(Axis.ZP.rotationDegrees(i * -9.785F));
               float f = item.getUseDuration(player) - (player.getUseItemRemainingTicks() - partialTick + 1.0F);
               float g = f / CrossbowItem.getChargeDuration(item, player);
               if (g > 1.0F) {
                  g = 1.0F;
               }

               if (g > 0.1F) {
                  float h = Mth.sin((f - 0.1F) * 1.3F);
                  float j = g - 0.1F;
                  float k = h * j;
                  poseStack.translate(k * 0.0F, k * 0.004F, k * 0.0F);
               }

               poseStack.translate(g * 0.0F, g * 0.0F, g * 0.04F);
               poseStack.scale(1.0F, 1.0F, 1.0F + g * 0.2F);
               poseStack.mulPose(Axis.YN.rotationDegrees(i * 45.0F));
            } else {
               this.swingArm(swingProgress, poseStack, i, humanoidArm);
               if (bl2 && swingProgress < 0.001F && bl) {
                  poseStack.translate(i * -0.641864F, 0.0F, 0.0F);
                  poseStack.mulPose(Axis.YP.rotationDegrees(i * 10.0F));
               }
            }

            this.renderItem(
               player,
               item,
               bl3 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
               poseStack,
               nodeCollector,
               packedLight
            );
         } else {
            boolean bl2 = humanoidArm == HumanoidArm.RIGHT;
            int l = bl2 ? 1 : -1;
            if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand) {
               ItemUseAnimation itemUseAnimation = item.getUseAnimation();
               if (!itemUseAnimation.hasCustomArmTransform()) {
                  this.applyItemArmTransform(poseStack, humanoidArm, equippedProgress);
               }

               switch (itemUseAnimation) {
                  case NONE:
                  default:
                     break;
                  case EAT:
                  case DRINK:
                     this.applyEatTransform(poseStack, partialTick, humanoidArm, item, player);
                     this.applyItemArmTransform(poseStack, humanoidArm, equippedProgress);
                     break;
                  case BLOCK:
                     if (!(item.getItem() instanceof ShieldItem)) {
                        poseStack.translate(l * -0.14142136F, 0.08F, 0.14142136F);
                        poseStack.mulPose(Axis.XP.rotationDegrees(-102.25F));
                        poseStack.mulPose(Axis.YP.rotationDegrees(l * 13.365F));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(l * 78.05F));
                     }
                     break;
                  case BOW:
                     poseStack.translate(l * -0.2785682F, 0.18344387F, 0.15731531F);
                     poseStack.mulPose(Axis.XP.rotationDegrees(-13.935F));
                     poseStack.mulPose(Axis.YP.rotationDegrees(l * 35.3F));
                     poseStack.mulPose(Axis.ZP.rotationDegrees(l * -9.785F));
                     float fxxx = item.getUseDuration(player) - (player.getUseItemRemainingTicks() - partialTick + 1.0F);
                     float gxx = fxxx / 20.0F;
                     gxx = (gxx * gxx + gxx * 2.0F) / 3.0F;
                     if (gxx > 1.0F) {
                        gxx = 1.0F;
                     }

                     if (gxx > 0.1F) {
                        float h = Mth.sin((fxxx - 0.1F) * 1.3F);
                        float j = gxx - 0.1F;
                        float k = h * j;
                        poseStack.translate(k * 0.0F, k * 0.004F, k * 0.0F);
                     }

                     poseStack.translate(gxx * 0.0F, gxx * 0.0F, gxx * 0.04F);
                     poseStack.scale(1.0F, 1.0F, 1.0F + gxx * 0.2F);
                     poseStack.mulPose(Axis.YN.rotationDegrees(l * 45.0F));
                     break;
                  case TRIDENT:
                     poseStack.translate(l * -0.5F, 0.7F, 0.1F);
                     poseStack.mulPose(Axis.XP.rotationDegrees(-55.0F));
                     poseStack.mulPose(Axis.YP.rotationDegrees(l * 35.3F));
                     poseStack.mulPose(Axis.ZP.rotationDegrees(l * -9.785F));
                     float fxx = item.getUseDuration(player) - (player.getUseItemRemainingTicks() - partialTick + 1.0F);
                     float gx = fxx / 10.0F;
                     if (gx > 1.0F) {
                        gx = 1.0F;
                     }

                     if (gx > 0.1F) {
                        float h = Mth.sin((fxx - 0.1F) * 1.3F);
                        float j = gx - 0.1F;
                        float k = h * j;
                        poseStack.translate(k * 0.0F, k * 0.004F, k * 0.0F);
                     }

                     poseStack.translate(0.0F, 0.0F, gx * 0.2F);
                     poseStack.scale(1.0F, 1.0F, 1.0F + gx * 0.2F);
                     poseStack.mulPose(Axis.YN.rotationDegrees(l * 45.0F));
                     break;
                  case BRUSH:
                     this.applyBrushTransform(poseStack, partialTick, humanoidArm, player);
                     break;
                  case BUNDLE:
                     this.swingArm(swingProgress, poseStack, l, humanoidArm);
                     break;
                  case SPEAR:
                     poseStack.translate(l * 0.56F, -0.52F, -0.72F);
                     float fx = item.getUseDuration(player) - (player.getUseItemRemainingTicks() - partialTick + 1.0F);
                     SpearAnimations.firstPersonUse(player.getTicksSinceLastKineticHitFeedback(partialTick), poseStack, fx, humanoidArm, item);
               }
            } else if (player.isAutoSpinAttack()) {
               this.applyItemArmTransform(poseStack, humanoidArm, equippedProgress);
               poseStack.translate(l * -0.4F, 0.8F, 0.3F);
               poseStack.mulPose(Axis.YP.rotationDegrees(l * 65.0F));
               poseStack.mulPose(Axis.ZP.rotationDegrees(l * -85.0F));
            } else {
               this.applyItemArmTransform(poseStack, humanoidArm, equippedProgress);
               switch (item.getSwingAnimation().type()) {
                  case NONE:
                  default:
                     break;
                  case WHACK:
                     this.swingArm(swingProgress, poseStack, l, humanoidArm);
                     break;
                  case STAB:
                     SpearAnimations.firstPersonAttack(swingProgress, poseStack, l, humanoidArm);
               }
            }

            this.renderItem(
               player,
               item,
               bl2 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
               poseStack,
               nodeCollector,
               packedLight
            );
         }

         poseStack.popPose();
      }
   }

   private void swingArm(float swingProgress, PoseStack poseStack, int direction, HumanoidArm arm) {
      float f = -0.4F * Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
      float g = 0.2F * Mth.sin(Mth.sqrt(swingProgress) * (float) (Math.PI * 2));
      float h = -0.2F * Mth.sin(swingProgress * (float) Math.PI);
      poseStack.translate(direction * f, g, h);
      this.applyItemArmAttackTransform(poseStack, arm, swingProgress);
   }

   private boolean shouldInstantlyReplaceVisibleItem(ItemStack oldItem, ItemStack newItem) {
      return ItemStack.matchesIgnoringComponents(oldItem, newItem, DataComponentType::ignoreSwapAnimation)
         ? true
         : !this.itemModelResolver.shouldPlaySwapAnimation(newItem);
   }

   public void tick() {
      this.oMainHandHeight = this.mainHandHeight;
      this.oOffHandHeight = this.offHandHeight;
      LocalPlayer localPlayer = this.minecraft.player;
      ItemStack itemStack = localPlayer.getMainHandItem();
      ItemStack itemStack2 = localPlayer.getOffhandItem();
      if (this.shouldInstantlyReplaceVisibleItem(this.mainHandItem, itemStack)) {
         this.mainHandItem = itemStack;
      }

      if (this.shouldInstantlyReplaceVisibleItem(this.offHandItem, itemStack2)) {
         this.offHandItem = itemStack2;
      }

      if (localPlayer.isHandsBusy()) {
         this.mainHandHeight = Mth.clamp(this.mainHandHeight - 0.4F, 0.0F, 1.0F);
         this.offHandHeight = Mth.clamp(this.offHandHeight - 0.4F, 0.0F, 1.0F);
      } else {
         float f = localPlayer.getItemSwapScale(1.0F);
         float g = this.mainHandItem != itemStack ? 0.0F : f * f * f;
         float h = this.offHandItem != itemStack2 ? 0.0F : 1.0F;
         this.mainHandHeight = this.mainHandHeight + Mth.clamp(g - this.mainHandHeight, -0.4F, 0.4F);
         this.offHandHeight = this.offHandHeight + Mth.clamp(h - this.offHandHeight, -0.4F, 0.4F);
      }

      if (this.mainHandHeight < 0.1F) {
         this.mainHandItem = itemStack;
      }

      if (this.offHandHeight < 0.1F) {
         this.offHandItem = itemStack2;
      }
   }

   public void itemUsed(InteractionHand hand) {
      if (hand == InteractionHand.MAIN_HAND) {
         this.mainHandHeight = 0.0F;
      } else {
         this.offHandHeight = 0.0F;
      }
   }

   @Environment(EnvType.CLIENT)
   @VisibleForTesting
   static enum HandRenderSelection {
      RENDER_BOTH_HANDS(true, true),
      RENDER_MAIN_HAND_ONLY(true, false),
      RENDER_OFF_HAND_ONLY(false, true);

      final boolean renderMainHand;
      final boolean renderOffHand;

      private HandRenderSelection(final boolean renderMainHand, final boolean renderOffHand) {
         this.renderMainHand = renderMainHand;
         this.renderOffHand = renderOffHand;
      }

      public static ItemInHandRenderer.HandRenderSelection onlyForHand(InteractionHand hand) {
         return hand == InteractionHand.MAIN_HAND ? RENDER_MAIN_HAND_ONLY : RENDER_OFF_HAND_ONLY;
      }
   }
}
