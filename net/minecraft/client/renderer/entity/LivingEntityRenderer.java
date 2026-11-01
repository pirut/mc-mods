package net.minecraft.client.renderer.entity;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class LivingEntityRenderer<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>>
   extends EntityRenderer<T, S>
   implements RenderLayerParent<S, M> {
   private static final float EYE_BED_OFFSET = 0.1F;
   protected M model;
   protected final ItemModelResolver itemModelResolver;
   protected final List<RenderLayer<S, M>> layers = Lists.newArrayList();

   public LivingEntityRenderer(EntityRendererProvider.Context p_174289_, M p_174290_, float p_174291_) {
      super(p_174289_);
      this.itemModelResolver = p_174289_.getItemModelResolver();
      this.model = p_174290_;
      this.shadowRadius = p_174291_;
   }

   public final boolean addLayer(RenderLayer<S, M> p_115327_) {
      return this.layers.add(p_115327_);
   }

   @Override
   public M getModel() {
      return this.model;
   }

   protected AABB getBoundingBoxForCulling(T p_360864_) {
      AABB aabb = super.getBoundingBoxForCulling(p_360864_);
      if (p_360864_.getItemBySlot(EquipmentSlot.HEAD).is(Items.DRAGON_HEAD)) {
         float f = 0.5F;
         return aabb.inflate(0.5, 0.5, 0.5);
      } else {
         return aabb;
      }
   }

   public void submit(S p_433493_, PoseStack p_434615_, SubmitNodeCollector p_433768_, CameraRenderState p_450931_) {
      if (!((RenderLivingEvent.Pre)NeoForge.EVENT_BUS.post(new RenderLivingEvent.Pre(p_433493_, this, p_433493_.partialTick, p_434615_, p_433768_)))
         .isCanceled()) {
         p_434615_.pushPose();
         if (p_433493_.hasPose(Pose.SLEEPING)) {
            Direction direction = p_433493_.bedOrientation;
            if (direction != null) {
               float f = p_433493_.eyeHeight - 0.1F;
               p_434615_.translate(-direction.getStepX() * f, 0.0F, -direction.getStepZ() * f);
            }
         }

         float f1 = p_433493_.scale;
         p_434615_.scale(f1, f1, f1);
         this.setupRotations(p_433493_, p_434615_, p_433493_.bodyRot, f1);
         p_434615_.scale(-1.0F, -1.0F, 1.0F);
         this.scale(p_433493_, p_434615_);
         p_434615_.translate(0.0F, -1.501F, 0.0F);
         boolean flag1 = this.isBodyVisible(p_433493_);
         boolean flag = !flag1 && !p_433493_.isInvisibleToPlayer;
         RenderType rendertype = this.getRenderType(p_433493_, flag1, flag, p_433493_.appearsGlowing());
         if (rendertype != null) {
            int i = getOverlayCoords(p_433493_, this.getWhiteOverlayProgress(p_433493_));
            int j = flag ? 654311423 : -1;
            int k = ARGB.multiply(j, this.getModelTint(p_433493_));
            p_433768_.submitModel(this.model, p_433493_, p_434615_, rendertype, p_433493_.lightCoords, i, k, null, p_433493_.outlineColor, null);
         }

         if (this.shouldRenderLayers(p_433493_) && !this.layers.isEmpty()) {
            this.model.setupAnim(p_433493_);

            for (RenderLayer<S, M> renderlayer : this.layers) {
               renderlayer.submit(p_434615_, p_433768_, p_433493_.lightCoords, p_433493_, p_433493_.yRot, p_433493_.xRot);
            }
         }

         p_434615_.popPose();
         super.submit(p_433493_, p_434615_, p_433768_, p_450931_);
         NeoForge.EVENT_BUS.post(new RenderLivingEvent.Post<>(p_433493_, this, p_433493_.partialTick, p_434615_, p_433768_));
      }
   }

   protected boolean shouldRenderLayers(S p_364697_) {
      return true;
   }

   protected int getModelTint(S p_360502_) {
      return -1;
   }

   public abstract Identifier getTextureLocation(S var1);

   @Nullable
   protected RenderType getRenderType(S p_360858_, boolean p_115323_, boolean p_115324_, boolean p_115325_) {
      Identifier identifier = this.getTextureLocation(p_360858_);
      if (p_115324_) {
         return RenderTypes.itemEntityTranslucentCull(identifier);
      } else if (p_115323_) {
         return this.model.renderType(identifier);
      } else {
         return p_115325_ ? RenderTypes.outline(identifier) : null;
      }
   }

   public static int getOverlayCoords(LivingEntityRenderState p_362833_, float p_115340_) {
      return OverlayTexture.pack(OverlayTexture.u(p_115340_), OverlayTexture.v(p_362833_.hasRedOverlay));
   }

   protected boolean isBodyVisible(S p_361327_) {
      return !p_361327_.isInvisible;
   }

   private static float sleepDirectionToRotation(Direction p_115329_) {
      switch (p_115329_) {
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

   protected boolean isShaking(S p_361234_) {
      return p_361234_.isFullyFrozen;
   }

   protected void setupRotations(S p_364714_, PoseStack p_115318_, float p_115319_, float p_115320_) {
      if (this.isShaking(p_364714_)) {
         p_115319_ += (float)(Math.cos(Mth.floor(p_364714_.ageInTicks) * 3.25F) * Math.PI * 0.4F);
      }

      if (!p_364714_.hasPose(Pose.SLEEPING)) {
         p_115318_.mulPose(Axis.YP.rotationDegrees(180.0F - p_115319_));
      }

      if (p_364714_.deathTime > 0.0F) {
         float f = (p_364714_.deathTime - 1.0F) / 20.0F * 1.6F;
         f = Mth.sqrt(f);
         if (f > 1.0F) {
            f = 1.0F;
         }

         p_115318_.mulPose(Axis.ZP.rotationDegrees(f * this.getFlipDegrees()));
      } else if (p_364714_.isAutoSpinAttack) {
         p_115318_.mulPose(Axis.XP.rotationDegrees(-90.0F - p_364714_.xRot));
         p_115318_.mulPose(Axis.YP.rotationDegrees(p_364714_.ageInTicks * -75.0F));
      } else if (p_364714_.hasPose(Pose.SLEEPING)) {
         Direction direction = p_364714_.bedOrientation;
         float f1 = direction != null ? sleepDirectionToRotation(direction) : p_115319_;
         p_115318_.mulPose(Axis.YP.rotationDegrees(f1));
         p_115318_.mulPose(Axis.ZP.rotationDegrees(this.getFlipDegrees()));
         p_115318_.mulPose(Axis.YP.rotationDegrees(270.0F));
      } else if (p_364714_.isUpsideDown) {
         p_115318_.translate(0.0F, (p_364714_.boundingBoxHeight + 0.1F) / p_115320_, 0.0F);
         p_115318_.mulPose(Axis.ZP.rotationDegrees(180.0F));
      }
   }

   protected float getFlipDegrees() {
      return 90.0F;
   }

   protected float getWhiteOverlayProgress(S p_362441_) {
      return 0.0F;
   }

   protected void scale(S p_362272_, PoseStack p_115315_) {
   }

   protected boolean shouldShowName(T p_363517_, double p_365448_) {
      if (p_363517_.isDiscrete()) {
         float f = 32.0F;
         if (!ClientHooks.isNameplateInRenderDistance(p_363517_, p_365448_)) {
            return false;
         }
      }

      Minecraft minecraft = Minecraft.getInstance();
      LocalPlayer localplayer = minecraft.player;
      boolean flag = !p_363517_.isInvisibleTo(localplayer);
      if (p_363517_ != localplayer) {
         Team team = p_363517_.getTeam();
         Team team1 = localplayer.getTeam();
         if (team != null) {
            Team.Visibility team$visibility = team.getNameTagVisibility();
            switch (team$visibility) {
               case ALWAYS:
                  return flag;
               case NEVER:
                  return false;
               case HIDE_FOR_OTHER_TEAMS:
                  return team1 == null ? flag : team.isAlliedTo(team1) && (team.canSeeFriendlyInvisibles() || flag);
               case HIDE_FOR_OWN_TEAM:
                  return team1 == null ? flag : !team.isAlliedTo(team1) && flag;
               default:
                  return true;
            }
         }
      }

      return Minecraft.renderNames() && p_363517_ != minecraft.getCameraEntity() && flag && !p_363517_.isVehicle();
   }

   public boolean isEntityUpsideDown(T p_194454_) {
      Component component = p_194454_.getCustomName();
      return component != null && isUpsideDownName(component.getString());
   }

   protected static boolean isUpsideDownName(String p_451499_) {
      return "Dinnerbone".equals(p_451499_) || "Grumm".equals(p_451499_);
   }

   protected float getShadowRadius(S p_361012_) {
      return super.getShadowRadius(p_361012_) * p_361012_.scale;
   }

   public void extractRenderState(T p_362733_, S p_360515_, float p_361157_) {
      super.extractRenderState(p_362733_, p_360515_, p_361157_);
      float f = Mth.rotLerp(p_361157_, p_362733_.yHeadRotO, p_362733_.yHeadRot);
      p_360515_.bodyRot = solveBodyRot(p_362733_, f, p_361157_);
      p_360515_.yRot = Mth.wrapDegrees(f - p_360515_.bodyRot);
      p_360515_.xRot = p_362733_.getXRot(p_361157_);
      p_360515_.isUpsideDown = this.isEntityUpsideDown(p_362733_);
      if (p_360515_.isUpsideDown) {
         p_360515_.xRot *= -1.0F;
         p_360515_.yRot *= -1.0F;
      }

      if (!p_362733_.isPassenger() && p_362733_.isAlive()) {
         p_360515_.walkAnimationPos = p_362733_.walkAnimation.position(p_361157_);
         p_360515_.walkAnimationSpeed = p_362733_.walkAnimation.speed(p_361157_);
      } else {
         p_360515_.walkAnimationPos = 0.0F;
         p_360515_.walkAnimationSpeed = 0.0F;
      }

      if (p_362733_.getVehicle() instanceof LivingEntity livingentity) {
         p_360515_.wornHeadAnimationPos = livingentity.walkAnimation.position(p_361157_);
      } else {
         p_360515_.wornHeadAnimationPos = p_360515_.walkAnimationPos;
      }

      p_360515_.scale = p_362733_.getScale();
      p_360515_.ageScale = p_362733_.getAgeScale();
      p_360515_.pose = p_362733_.getPose();
      p_360515_.bedOrientation = p_362733_.getBedOrientation();
      if (p_360515_.bedOrientation != null) {
         p_360515_.eyeHeight = p_362733_.getEyeHeight(Pose.STANDING);
      }

      p_360515_.isFullyFrozen = p_362733_.isFullyFrozen();
      p_360515_.isBaby = p_362733_.isBaby();
      p_360515_.isInWater = p_362733_.isInWater() || p_362733_.isInFluidType((fluidType, height) -> p_362733_.canSwimInFluidType(fluidType));
      p_360515_.isAutoSpinAttack = p_362733_.isAutoSpinAttack();
      p_360515_.ticksSinceKineticHitFeedback = p_362733_.getTicksSinceLastKineticHitFeedback(p_361157_);
      p_360515_.hasRedOverlay = p_362733_.hurtTime > 0 || p_362733_.deathTime > 0;
      ItemStack itemstack = p_362733_.getItemBySlot(EquipmentSlot.HEAD);
      if (itemstack.getItem() instanceof BlockItem blockitem && blockitem.getBlock() instanceof AbstractSkullBlock abstractskullblock) {
         p_360515_.wornHeadType = abstractskullblock.getType();
         p_360515_.wornHeadProfile = itemstack.get(DataComponents.PROFILE);
         p_360515_.headItem.clear();
      } else {
         p_360515_.wornHeadType = null;
         p_360515_.wornHeadProfile = null;
         if (!HumanoidArmorLayer.shouldRender(itemstack, EquipmentSlot.HEAD)) {
            this.itemModelResolver.updateForLiving(p_360515_.headItem, itemstack, ItemDisplayContext.HEAD, p_362733_);
         } else {
            p_360515_.headItem.clear();
         }
      }

      p_360515_.deathTime = p_362733_.deathTime > 0 ? p_362733_.deathTime + p_361157_ : 0.0F;
      Minecraft minecraft = Minecraft.getInstance();
      p_360515_.isInvisibleToPlayer = p_360515_.isInvisible && p_362733_.isInvisibleTo(minecraft.player);
   }

   private static float solveBodyRot(LivingEntity p_362839_, float p_361247_, float p_361564_) {
      if (p_362839_.getVehicle() instanceof LivingEntity livingentity) {
         float f2 = Mth.rotLerp(p_361564_, livingentity.yBodyRotO, livingentity.yBodyRot);
         float f = 85.0F;
         float f1 = Mth.clamp(Mth.wrapDegrees(p_361247_ - f2), -85.0F, 85.0F);
         f2 = p_361247_ - f1;
         if (Math.abs(f1) > 50.0F) {
            f2 += f1 * 0.2F;
         }

         return f2;
      } else {
         return Mth.rotLerp(p_361564_, p_362839_.yBodyRotO, p_362839_.yBodyRot);
      }
   }
}
