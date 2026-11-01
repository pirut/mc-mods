package net.minecraft.client.renderer.entity;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.Team;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public abstract class LivingEntityRenderer<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>>
   extends EntityRenderer<T, S>
   implements RenderLayerParent<S, M> {
   private static final float EYE_BED_OFFSET = 0.1F;
   protected M model;
   protected final ItemModelResolver itemModelResolver;
   protected final List<RenderLayer<S, M>> layers = Lists.newArrayList();

   public LivingEntityRenderer(EntityRendererProvider.Context context, M model, float shadowRadius) {
      super(context);
      this.itemModelResolver = context.getItemModelResolver();
      this.model = model;
      this.shadowRadius = shadowRadius;
   }

   protected final boolean addLayer(RenderLayer<S, M> layer) {
      return this.layers.add(layer);
   }

   @Override
   public M getModel() {
      return this.model;
   }

   protected AABB getBoundingBoxForCulling(T livingEntity) {
      AABB aABB = super.getBoundingBoxForCulling(livingEntity);
      if (livingEntity.getItemBySlot(EquipmentSlot.HEAD).is(Items.DRAGON_HEAD)) {
         float f = 0.5F;
         return aABB.inflate(0.5, 0.5, 0.5);
      } else {
         return aABB;
      }
   }

   public void submit(S livingEntityRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
      poseStack.pushPose();
      if (livingEntityRenderState.hasPose(Pose.SLEEPING)) {
         Direction direction = livingEntityRenderState.bedOrientation;
         if (direction != null) {
            float f = livingEntityRenderState.eyeHeight - 0.1F;
            poseStack.translate(-direction.getStepX() * f, 0.0F, -direction.getStepZ() * f);
         }
      }

      float g = livingEntityRenderState.scale;
      poseStack.scale(g, g, g);
      this.setupRotations(livingEntityRenderState, poseStack, livingEntityRenderState.bodyRot, g);
      poseStack.scale(-1.0F, -1.0F, 1.0F);
      this.scale(livingEntityRenderState, poseStack);
      poseStack.translate(0.0F, -1.501F, 0.0F);
      boolean bl = this.isBodyVisible(livingEntityRenderState);
      boolean bl2 = !bl && !livingEntityRenderState.isInvisibleToPlayer;
      RenderType renderType = this.getRenderType(livingEntityRenderState, bl, bl2, livingEntityRenderState.appearsGlowing());
      if (renderType != null) {
         int i = getOverlayCoords(livingEntityRenderState, this.getWhiteOverlayProgress(livingEntityRenderState));
         int j = bl2 ? 654311423 : -1;
         int k = ARGB.multiply(j, this.getModelTint(livingEntityRenderState));
         submitNodeCollector.submitModel(
            this.model,
            livingEntityRenderState,
            poseStack,
            renderType,
            livingEntityRenderState.lightCoords,
            i,
            k,
            null,
            livingEntityRenderState.outlineColor,
            null
         );
      }

      if (this.shouldRenderLayers(livingEntityRenderState) && !this.layers.isEmpty()) {
         this.model.setupAnim(livingEntityRenderState);

         for (RenderLayer<S, M> renderLayer : this.layers) {
            renderLayer.submit(
               poseStack,
               submitNodeCollector,
               livingEntityRenderState.lightCoords,
               livingEntityRenderState,
               livingEntityRenderState.yRot,
               livingEntityRenderState.xRot
            );
         }
      }

      poseStack.popPose();
      super.submit(livingEntityRenderState, poseStack, submitNodeCollector, cameraRenderState);
   }

   protected boolean shouldRenderLayers(S renderState) {
      return true;
   }

   protected int getModelTint(S renderState) {
      return -1;
   }

   public abstract Identifier getTextureLocation(S renderState);

   @Nullable
   protected RenderType getRenderType(S renderState, boolean visible, boolean translucent, boolean glowing) {
      Identifier identifier = this.getTextureLocation(renderState);
      if (translucent) {
         return RenderTypes.itemEntityTranslucentCull(identifier);
      } else if (visible) {
         return this.model.renderType(identifier);
      } else {
         return glowing ? RenderTypes.outline(identifier) : null;
      }
   }

   public static int getOverlayCoords(LivingEntityRenderState renderState, float overlay) {
      return OverlayTexture.pack(OverlayTexture.u(overlay), OverlayTexture.v(renderState.hasRedOverlay));
   }

   protected boolean isBodyVisible(S renderState) {
      return !renderState.isInvisible;
   }

   private static float sleepDirectionToRotation(Direction facing) {
      switch (facing) {
         case SOUTH:
            return 90.0F;
         case WEST:
            return 0.0F;
         case NORTH:
            return 270.0F;
         case EAST:
            return 180.0F;
         default:
            return 0.0F;
      }
   }

   protected boolean isShaking(S renderState) {
      return renderState.isFullyFrozen;
   }

   protected void setupRotations(S renderState, PoseStack poseStack, float bodyRot, float scale) {
      if (this.isShaking(renderState)) {
         bodyRot += (float)(Math.cos(Mth.floor(renderState.ageInTicks) * 3.25F) * Math.PI * 0.4F);
      }

      if (!renderState.hasPose(Pose.SLEEPING)) {
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyRot));
      }

      if (renderState.deathTime > 0.0F) {
         float f = (renderState.deathTime - 1.0F) / 20.0F * 1.6F;
         f = Mth.sqrt(f);
         if (f > 1.0F) {
            f = 1.0F;
         }

         poseStack.mulPose(Axis.ZP.rotationDegrees(f * this.getFlipDegrees()));
      } else if (renderState.isAutoSpinAttack) {
         poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F - renderState.xRot));
         poseStack.mulPose(Axis.YP.rotationDegrees(renderState.ageInTicks * -75.0F));
      } else if (renderState.hasPose(Pose.SLEEPING)) {
         Direction direction = renderState.bedOrientation;
         float g = direction != null ? sleepDirectionToRotation(direction) : bodyRot;
         poseStack.mulPose(Axis.YP.rotationDegrees(g));
         poseStack.mulPose(Axis.ZP.rotationDegrees(this.getFlipDegrees()));
         poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
      } else if (renderState.isUpsideDown) {
         poseStack.translate(0.0F, (renderState.boundingBoxHeight + 0.1F) / scale, 0.0F);
         poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
      }
   }

   protected float getFlipDegrees() {
      return 90.0F;
   }

   protected float getWhiteOverlayProgress(S renderState) {
      return 0.0F;
   }

   protected void scale(S renderState, PoseStack poseStack) {
   }

   protected boolean shouldShowName(T livingEntity, double d) {
      if (livingEntity.isDiscrete()) {
         float f = 32.0F;
         if (d >= 1024.0) {
            return false;
         }
      }

      Minecraft minecraft = Minecraft.getInstance();
      LocalPlayer localPlayer = minecraft.player;
      boolean bl = !livingEntity.isInvisibleTo(localPlayer);
      if (livingEntity != localPlayer) {
         Team team = livingEntity.getTeam();
         Team team2 = localPlayer.getTeam();
         if (team != null) {
            Team.Visibility visibility = team.getNameTagVisibility();
            switch (visibility) {
               case ALWAYS:
                  return bl;
               case NEVER:
                  return false;
               case HIDE_FOR_OTHER_TEAMS:
                  return team2 == null ? bl : team.isAlliedTo(team2) && (team.canSeeFriendlyInvisibles() || bl);
               case HIDE_FOR_OWN_TEAM:
                  return team2 == null ? bl : !team.isAlliedTo(team2) && bl;
               default:
                  return true;
            }
         }
      }

      return Minecraft.renderNames() && livingEntity != minecraft.getCameraEntity() && bl && !livingEntity.isVehicle();
   }

   public boolean isEntityUpsideDown(T entity) {
      Component component = entity.getCustomName();
      return component != null && isUpsideDownName(component.getString());
   }

   protected static boolean isUpsideDownName(String name) {
      return "Dinnerbone".equals(name) || "Grumm".equals(name);
   }

   protected float getShadowRadius(S livingEntityRenderState) {
      return super.getShadowRadius(livingEntityRenderState) * livingEntityRenderState.scale;
   }

   public void extractRenderState(T livingEntity, S livingEntityRenderState, float f) {
      super.extractRenderState(livingEntity, livingEntityRenderState, f);
      float g = Mth.rotLerp(f, livingEntity.yHeadRotO, livingEntity.yHeadRot);
      livingEntityRenderState.bodyRot = solveBodyRot(livingEntity, g, f);
      livingEntityRenderState.yRot = Mth.wrapDegrees(g - livingEntityRenderState.bodyRot);
      livingEntityRenderState.xRot = livingEntity.getXRot(f);
      livingEntityRenderState.isUpsideDown = this.isEntityUpsideDown(livingEntity);
      if (livingEntityRenderState.isUpsideDown) {
         livingEntityRenderState.xRot *= -1.0F;
         livingEntityRenderState.yRot *= -1.0F;
      }

      if (!livingEntity.isPassenger() && livingEntity.isAlive()) {
         livingEntityRenderState.walkAnimationPos = livingEntity.walkAnimation.position(f);
         livingEntityRenderState.walkAnimationSpeed = livingEntity.walkAnimation.speed(f);
      } else {
         livingEntityRenderState.walkAnimationPos = 0.0F;
         livingEntityRenderState.walkAnimationSpeed = 0.0F;
      }

      if (livingEntity.getVehicle() instanceof LivingEntity livingEntity2) {
         livingEntityRenderState.wornHeadAnimationPos = livingEntity2.walkAnimation.position(f);
      } else {
         livingEntityRenderState.wornHeadAnimationPos = livingEntityRenderState.walkAnimationPos;
      }

      livingEntityRenderState.scale = livingEntity.getScale();
      livingEntityRenderState.ageScale = livingEntity.getAgeScale();
      livingEntityRenderState.pose = livingEntity.getPose();
      livingEntityRenderState.bedOrientation = livingEntity.getBedOrientation();
      if (livingEntityRenderState.bedOrientation != null) {
         livingEntityRenderState.eyeHeight = livingEntity.getEyeHeight(Pose.STANDING);
      }

      livingEntityRenderState.isFullyFrozen = livingEntity.isFullyFrozen();
      livingEntityRenderState.isBaby = livingEntity.isBaby();
      livingEntityRenderState.isInWater = livingEntity.isInWater();
      livingEntityRenderState.isAutoSpinAttack = livingEntity.isAutoSpinAttack();
      livingEntityRenderState.ticksSinceKineticHitFeedback = livingEntity.getTicksSinceLastKineticHitFeedback(f);
      livingEntityRenderState.hasRedOverlay = livingEntity.hurtTime > 0 || livingEntity.deathTime > 0;
      ItemStack itemStack = livingEntity.getItemBySlot(EquipmentSlot.HEAD);
      if (itemStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof AbstractSkullBlock abstractSkullBlock) {
         livingEntityRenderState.wornHeadType = abstractSkullBlock.getType();
         livingEntityRenderState.wornHeadProfile = itemStack.get(DataComponents.PROFILE);
         livingEntityRenderState.headItem.clear();
      } else {
         livingEntityRenderState.wornHeadType = null;
         livingEntityRenderState.wornHeadProfile = null;
         if (!HumanoidArmorLayer.shouldRender(itemStack, EquipmentSlot.HEAD)) {
            this.itemModelResolver.updateForLiving(livingEntityRenderState.headItem, itemStack, ItemDisplayContext.HEAD, livingEntity);
         } else {
            livingEntityRenderState.headItem.clear();
         }
      }

      livingEntityRenderState.deathTime = livingEntity.deathTime > 0 ? livingEntity.deathTime + f : 0.0F;
      Minecraft minecraft = Minecraft.getInstance();
      livingEntityRenderState.isInvisibleToPlayer = livingEntityRenderState.isInvisible && livingEntity.isInvisibleTo(minecraft.player);
   }

   private static float solveBodyRot(LivingEntity entity, float yHeadRot, float partialTick) {
      if (entity.getVehicle() instanceof LivingEntity livingEntity) {
         float f = Mth.rotLerp(partialTick, livingEntity.yBodyRotO, livingEntity.yBodyRot);
         float g = 85.0F;
         float h = Mth.clamp(Mth.wrapDegrees(yHeadRot - f), -85.0F, 85.0F);
         f = yHeadRot - h;
         if (Math.abs(h) > 50.0F) {
            f += h * 0.2F;
         }

         return f;
      } else {
         return Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
      }
   }
}
