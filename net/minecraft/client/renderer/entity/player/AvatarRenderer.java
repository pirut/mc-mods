package net.minecraft.client.renderer.entity.player;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.entity.ClientAvatarState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.ArrowLayer;
import net.minecraft.client.renderer.entity.layers.BeeStingerLayer;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.Deadmau5EarsLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ParrotOnShoulderLayer;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.SpinAttackEffectLayer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.NeoForge;

@OnlyIn(Dist.CLIENT)
public class AvatarRenderer<AvatarlikeEntity extends Avatar & ClientAvatarEntity>
   extends LivingEntityRenderer<AvatarlikeEntity, AvatarRenderState, PlayerModel> {
   public AvatarRenderer(EntityRendererProvider.Context p_445612_, boolean p_445726_) {
      super(p_445612_, new PlayerModel(p_445612_.bakeLayer(p_445726_ ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), p_445726_), 0.5F);
      this.addLayer(
         new HumanoidArmorLayer<>(
            this,
            ArmorModelSet.bake(
               p_445726_ ? ModelLayers.PLAYER_SLIM_ARMOR : ModelLayers.PLAYER_ARMOR,
               p_445612_.getModelSet(),
               p_477740_ -> new PlayerModel(p_477740_, p_445726_)
            ),
            p_445612_.getEquipmentRenderer()
         )
      );
      this.addLayer(new PlayerItemInHandLayer<>(this));
      this.addLayer(new ArrowLayer<>(this, p_445612_));
      this.addLayer(new Deadmau5EarsLayer(this, p_445612_.getModelSet()));
      this.addLayer(new CapeLayer(this, p_445612_.getModelSet(), p_445612_.getEquipmentAssets()));
      this.addLayer(new CustomHeadLayer<>(this, p_445612_.getModelSet(), p_445612_.getPlayerSkinRenderCache()));
      this.addLayer(new WingsLayer<>(this, p_445612_.getModelSet(), p_445612_.getEquipmentRenderer()));
      this.addLayer(new ParrotOnShoulderLayer(this, p_445612_.getModelSet()));
      this.addLayer(new SpinAttackEffectLayer(this, p_445612_.getModelSet()));
      this.addLayer(new BeeStingerLayer<>(this, p_445612_));
   }

   protected boolean shouldRenderLayers(AvatarRenderState p_447057_) {
      return !p_447057_.isSpectator;
   }

   public Vec3 getRenderOffset(AvatarRenderState p_446468_) {
      Vec3 vec3 = super.getRenderOffset(p_446468_);
      return p_446468_.isCrouching ? vec3.add(0.0, p_446468_.scale * -2.0F / 16.0, 0.0) : vec3;
   }

   private static HumanoidModel.ArmPose getArmPose(Avatar p_445620_, HumanoidArm p_446498_) {
      ItemStack itemstack = p_445620_.getItemInHand(InteractionHand.MAIN_HAND);
      ItemStack itemstack1 = p_445620_.getItemInHand(InteractionHand.OFF_HAND);
      HumanoidModel.ArmPose humanoidmodel$armpose = getArmPose(p_445620_, itemstack, InteractionHand.MAIN_HAND);
      HumanoidModel.ArmPose humanoidmodel$armpose1 = getArmPose(p_445620_, itemstack1, InteractionHand.OFF_HAND);
      if (humanoidmodel$armpose.isTwoHanded()) {
         humanoidmodel$armpose1 = itemstack1.isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
      }

      return p_445620_.getMainArm() == p_446498_ ? humanoidmodel$armpose : humanoidmodel$armpose1;
   }

   private static HumanoidModel.ArmPose getArmPose(Avatar p_446397_, ItemStack p_446508_, InteractionHand p_445956_) {
      IClientItemExtensions extensions = IClientItemExtensions.of(p_446508_);
      HumanoidModel.ArmPose armPose = extensions.getArmPose(p_446397_, p_445956_, p_446508_);
      if (armPose != null) {
         return armPose;
      } else if (p_446508_.isEmpty()) {
         return HumanoidModel.ArmPose.EMPTY;
      } else if (!p_446397_.swinging && p_446508_.is(Items.CROSSBOW) && CrossbowItem.isCharged(p_446508_)) {
         return HumanoidModel.ArmPose.CROSSBOW_HOLD;
      } else {
         if (p_446397_.getUsedItemHand() == p_445956_ && p_446397_.getUseItemRemainingTicks() > 0) {
            ItemUseAnimation itemuseanimation = p_446508_.getUseAnimation();
            if (itemuseanimation == ItemUseAnimation.BLOCK) {
               return HumanoidModel.ArmPose.BLOCK;
            }

            if (itemuseanimation == ItemUseAnimation.BOW) {
               return HumanoidModel.ArmPose.BOW_AND_ARROW;
            }

            if (itemuseanimation == ItemUseAnimation.TRIDENT) {
               return HumanoidModel.ArmPose.THROW_TRIDENT;
            }

            if (itemuseanimation == ItemUseAnimation.CROSSBOW) {
               return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
            }

            if (itemuseanimation == ItemUseAnimation.SPYGLASS) {
               return HumanoidModel.ArmPose.SPYGLASS;
            }

            if (itemuseanimation == ItemUseAnimation.TOOT_HORN) {
               return HumanoidModel.ArmPose.TOOT_HORN;
            }

            if (itemuseanimation == ItemUseAnimation.BRUSH) {
               return HumanoidModel.ArmPose.BRUSH;
            }

            if (itemuseanimation == ItemUseAnimation.SPEAR) {
               return HumanoidModel.ArmPose.SPEAR;
            }
         }

         SwingAnimation swinganimation = p_446508_.get(DataComponents.SWING_ANIMATION);
         if (swinganimation != null && swinganimation.type() == SwingAnimationType.STAB && p_446397_.swinging) {
            return HumanoidModel.ArmPose.SPEAR;
         } else {
            return p_446508_.is(ItemTags.SPEARS) ? HumanoidModel.ArmPose.SPEAR : HumanoidModel.ArmPose.ITEM;
         }
      }
   }

   public Identifier getTextureLocation(AvatarRenderState p_469499_) {
      return p_469499_.skin.body().texturePath();
   }

   protected void scale(AvatarRenderState p_447098_, PoseStack p_445727_) {
      float f = 0.9375F;
      p_445727_.scale(0.9375F, 0.9375F, 0.9375F);
   }

   public void submit(AvatarRenderState p_433493_, PoseStack p_434615_, SubmitNodeCollector p_433768_, CameraRenderState p_450931_) {
      if (!((RenderPlayerEvent.Pre)NeoForge.EVENT_BUS.post(new RenderPlayerEvent.Pre(p_433493_, this, p_433493_.partialTick, p_434615_, p_433768_)))
         .isCanceled()) {
         super.submit(p_433493_, p_434615_, p_433768_, p_450931_);
         NeoForge.EVENT_BUS.post(new RenderPlayerEvent.Post<>(p_433493_, this, p_433493_.partialTick, p_434615_, p_433768_));
      }
   }

   protected void submitNameTag(AvatarRenderState p_447013_, PoseStack p_446358_, SubmitNodeCollector p_446248_, CameraRenderState p_451056_) {
      p_446358_.pushPose();
      int i = p_447013_.showExtraEars ? -10 : 0;
      if (p_447013_.scoreText != null) {
         p_446248_.submitNameTag(
            p_446358_,
            p_447013_.nameTagAttachment,
            i,
            p_447013_.scoreText,
            !p_447013_.isDiscrete,
            p_447013_.lightCoords,
            p_447013_.distanceToCameraSq,
            p_451056_
         );
         p_446358_.translate(0.0F, 0.25875F, 0.0F);
      }

      if (p_447013_.nameTag != null) {
         RenderNameTagEvent.DoRender event = new RenderNameTagEvent.DoRender(
            p_447013_, p_447013_.nameTag, this, p_446358_, p_446248_, p_451056_, p_447013_.partialTick
         );
         if (!((RenderNameTagEvent.DoRender)NeoForge.EVENT_BUS.post(event)).isCanceled()) {
            p_446248_.submitNameTag(
               p_446358_,
               p_447013_.nameTagAttachment,
               i,
               p_447013_.nameTag,
               !p_447013_.isDiscrete,
               p_447013_.lightCoords,
               p_447013_.distanceToCameraSq,
               p_451056_
            );
         }
      }

      p_446358_.popPose();
   }

   public AvatarRenderState createRenderState() {
      return new AvatarRenderState();
   }

   public void extractRenderState(AvatarlikeEntity p_445469_, AvatarRenderState p_446472_, float p_445702_) {
      super.extractRenderState(p_445469_, p_446472_, p_445702_);
      HumanoidMobRenderer.extractHumanoidRenderState(p_445469_, p_446472_, p_445702_, this.itemModelResolver);
      p_446472_.leftArmPose = getArmPose(p_445469_, HumanoidArm.LEFT);
      p_446472_.rightArmPose = getArmPose(p_445469_, HumanoidArm.RIGHT);
      p_446472_.skin = p_445469_.getSkin();
      p_446472_.arrowCount = p_445469_.getArrowCount();
      p_446472_.stingerCount = p_445469_.getStingerCount();
      p_446472_.isSpectator = p_445469_.isSpectator();
      p_446472_.showHat = p_445469_.isModelPartShown(PlayerModelPart.HAT);
      p_446472_.showJacket = p_445469_.isModelPartShown(PlayerModelPart.JACKET);
      p_446472_.showLeftPants = p_445469_.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG);
      p_446472_.showRightPants = p_445469_.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG);
      p_446472_.showLeftSleeve = p_445469_.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
      p_446472_.showRightSleeve = p_445469_.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
      p_446472_.showCape = p_445469_.isModelPartShown(PlayerModelPart.CAPE);
      this.extractFlightData(p_445469_, p_446472_, p_445702_);
      this.extractCapeState(p_445469_, p_446472_, p_445702_);
      if (p_446472_.distanceToCameraSq < 100.0) {
         p_446472_.scoreText = p_445469_.belowNameDisplay();
      } else {
         p_446472_.scoreText = null;
      }

      p_446472_.parrotOnLeftShoulder = p_445469_.getParrotVariantOnShoulder(true);
      p_446472_.parrotOnRightShoulder = p_445469_.getParrotVariantOnShoulder(false);
      p_446472_.id = p_445469_.getId();
      p_446472_.showExtraEars = p_445469_.showExtraEars();
      p_446472_.heldOnHead.clear();
      if (p_446472_.isUsingItem) {
         ItemStack itemstack = p_445469_.getItemInHand(p_446472_.useItemHand);
         if (itemstack.canPerformAction(ItemAbilities.SPYGLASS_SCOPE)) {
            this.itemModelResolver.updateForLiving(p_446472_.heldOnHead, itemstack, ItemDisplayContext.HEAD, p_445469_);
         }
      }
   }

   protected boolean shouldShowName(AvatarlikeEntity p_451069_, double p_451150_) {
      return super.shouldShowName(p_451069_, p_451150_)
         && (p_451069_.shouldShowName() || p_451069_.hasCustomName() && p_451069_ == this.entityRenderDispatcher.crosshairPickEntity);
   }

   private void extractFlightData(AvatarlikeEntity p_445624_, AvatarRenderState p_446060_, float p_445590_) {
      p_446060_.fallFlyingTimeInTicks = p_445624_.getFallFlyingTicks() + p_445590_;
      Vec3 vec3 = p_445624_.getViewVector(p_445590_);
      Vec3 vec31 = p_445624_.avatarState().deltaMovementOnPreviousTick().lerp(p_445624_.getDeltaMovement(), p_445590_);
      if (vec31.horizontalDistanceSqr() > 1.0E-5F && vec3.horizontalDistanceSqr() > 1.0E-5F) {
         p_446060_.shouldApplyFlyingYRot = true;
         double d0 = vec31.horizontal().normalize().dot(vec3.horizontal().normalize());
         double d1 = vec31.x * vec3.z - vec31.z * vec3.x;
         p_446060_.flyingYRot = (float)(Math.signum(d1) * Math.acos(Math.min(1.0, Math.abs(d0))));
      } else {
         p_446060_.shouldApplyFlyingYRot = false;
         p_446060_.flyingYRot = 0.0F;
      }
   }

   private void extractCapeState(AvatarlikeEntity p_446502_, AvatarRenderState p_446374_, float p_445869_) {
      ClientAvatarState clientavatarstate = p_446502_.avatarState();
      double d0 = clientavatarstate.getInterpolatedCloakX(p_445869_) - Mth.lerp((double)p_445869_, p_446502_.xo, p_446502_.getX());
      double d1 = clientavatarstate.getInterpolatedCloakY(p_445869_) - Mth.lerp((double)p_445869_, p_446502_.yo, p_446502_.getY());
      double d2 = clientavatarstate.getInterpolatedCloakZ(p_445869_) - Mth.lerp((double)p_445869_, p_446502_.zo, p_446502_.getZ());
      float f = Mth.rotLerp(p_445869_, p_446502_.yBodyRotO, p_446502_.yBodyRot);
      double d3 = Mth.sin(f * (float) (Math.PI / 180.0));
      double d4 = -Mth.cos(f * (float) (Math.PI / 180.0));
      p_446374_.capeFlap = (float)d1 * 10.0F;
      p_446374_.capeFlap = Mth.clamp(p_446374_.capeFlap, -6.0F, 32.0F);
      p_446374_.capeLean = (float)(d0 * d3 + d2 * d4) * 100.0F;
      p_446374_.capeLean = p_446374_.capeLean * (1.0F - p_446374_.fallFlyingScale());
      p_446374_.capeLean = Mth.clamp(p_446374_.capeLean, 0.0F, 150.0F);
      p_446374_.capeLean2 = (float)(d0 * d4 - d2 * d3) * 100.0F;
      p_446374_.capeLean2 = Mth.clamp(p_446374_.capeLean2, -20.0F, 20.0F);
      float f1 = clientavatarstate.getInterpolatedBob(p_445869_);
      float f2 = clientavatarstate.getInterpolatedWalkDistance(p_445869_);
      p_446374_.capeFlap = p_446374_.capeFlap + Mth.sin(f2 * 6.0F) * 32.0F * f1;
   }

   @Deprecated
   public void renderRightHand(PoseStack p_446962_, SubmitNodeCollector p_445938_, int p_445470_, Identifier p_468798_, boolean p_446672_) {
      this.renderRightHand(p_446962_, p_445938_, p_445470_, p_468798_, p_446672_, Minecraft.getInstance().player);
   }

   public void renderRightHand(
      PoseStack p_446962_, SubmitNodeCollector p_445938_, int p_445470_, Identifier p_445487_, boolean p_446672_, AbstractClientPlayer player
   ) {
      if (!ClientHooks.renderSpecificFirstPersonArm(p_446962_, p_445938_, p_445470_, player, HumanoidArm.RIGHT)) {
         this.renderHand(p_446962_, p_445938_, p_445470_, p_445487_, this.model.rightArm, p_446672_);
      }
   }

   @Deprecated
   public void renderLeftHand(PoseStack p_445618_, SubmitNodeCollector p_447076_, int p_446116_, Identifier p_467487_, boolean p_446755_) {
      this.renderLeftHand(p_445618_, p_447076_, p_446116_, p_467487_, p_446755_, Minecraft.getInstance().player);
   }

   public void renderLeftHand(
      PoseStack p_445618_, SubmitNodeCollector p_447076_, int p_446116_, Identifier p_446675_, boolean p_446755_, AbstractClientPlayer player
   ) {
      if (!ClientHooks.renderSpecificFirstPersonArm(p_445618_, p_447076_, p_446116_, player, HumanoidArm.LEFT)) {
         this.renderHand(p_445618_, p_447076_, p_446116_, p_446675_, this.model.leftArm, p_446755_);
      }
   }

   private void renderHand(PoseStack p_447067_, SubmitNodeCollector p_446294_, int p_447015_, Identifier p_467332_, ModelPart p_447044_, boolean p_447011_) {
      PlayerModel playermodel = this.getModel();
      p_447044_.resetPose();
      p_447044_.visible = true;
      playermodel.leftSleeve.visible = p_447011_;
      playermodel.rightSleeve.visible = p_447011_;
      playermodel.leftArm.zRot = -0.1F;
      playermodel.rightArm.zRot = 0.1F;
      p_446294_.submitModelPart(p_447044_, p_447067_, RenderTypes.entityTranslucent(p_467332_), p_447015_, OverlayTexture.NO_OVERLAY, null);
   }

   protected void setupRotations(AvatarRenderState p_446425_, PoseStack p_446166_, float p_445813_, float p_446015_) {
      float f = p_446425_.swimAmount;
      float f1 = p_446425_.xRot;
      if (p_446425_.isFallFlying) {
         super.setupRotations(p_446425_, p_446166_, p_445813_, p_446015_);
         float f2 = p_446425_.fallFlyingScale();
         if (!p_446425_.isAutoSpinAttack) {
            p_446166_.mulPose(Axis.XP.rotationDegrees(f2 * (-90.0F - f1)));
         }

         if (p_446425_.shouldApplyFlyingYRot) {
            p_446166_.mulPose(Axis.YP.rotation(p_446425_.flyingYRot));
         }
      } else if (f > 0.0F) {
         super.setupRotations(p_446425_, p_446166_, p_445813_, p_446015_);
         float f4 = p_446425_.isInWater ? -90.0F - f1 : -90.0F;
         float f3 = Mth.lerp(f, 0.0F, f4);
         p_446166_.mulPose(Axis.XP.rotationDegrees(f3));
         if (p_446425_.isVisuallySwimming) {
            p_446166_.translate(0.0F, -1.0F, 0.3F);
         }
      } else {
         super.setupRotations(p_446425_, p_446166_, p_445813_, p_446015_);
      }
   }

   public boolean isEntityUpsideDown(AvatarlikeEntity p_451232_) {
      if (p_451232_.isModelPartShown(PlayerModelPart.CAPE)) {
         return p_451232_ instanceof Player player ? isPlayerUpsideDown(player) : super.isEntityUpsideDown(p_451232_);
      } else {
         return false;
      }
   }

   public static boolean isPlayerUpsideDown(Player p_451222_) {
      return isUpsideDownName(p_451222_.getGameProfile().name());
   }
}
