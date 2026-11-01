package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.object.cart.MinecartModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public abstract class AbstractMinecartRenderer<T extends AbstractMinecart, S extends MinecartRenderState> extends EntityRenderer<T, S> {
   private static final Identifier MINECART_LOCATION = Identifier.withDefaultNamespace("textures/entity/minecart.png");
   private static final float DISPLAY_BLOCK_SCALE = 0.75F;
   protected final MinecartModel model;

   public AbstractMinecartRenderer(EntityRendererProvider.Context context, ModelLayerLocation modelLayer) {
      super(context);
      this.shadowRadius = 0.7F;
      this.model = new MinecartModel(context.bakeLayer(modelLayer));
   }

   public void submit(S minecartRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
      super.submit(minecartRenderState, poseStack, submitNodeCollector, cameraRenderState);
      poseStack.pushPose();
      long l = minecartRenderState.offsetSeed;
      float f = (((float)(l >> 16 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
      float g = (((float)(l >> 20 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
      float h = (((float)(l >> 24 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
      poseStack.translate(f, g, h);
      if (minecartRenderState.isNewRender) {
         newRender(minecartRenderState, poseStack);
      } else {
         oldRender(minecartRenderState, poseStack);
      }

      float i = minecartRenderState.hurtTime;
      if (i > 0.0F) {
         poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(i) * i * minecartRenderState.damageTime / 10.0F * minecartRenderState.hurtDir));
      }

      BlockState blockState = minecartRenderState.displayBlockState;
      if (blockState.getRenderShape() != RenderShape.INVISIBLE) {
         poseStack.pushPose();
         poseStack.scale(0.75F, 0.75F, 0.75F);
         poseStack.translate(-0.5F, (minecartRenderState.displayOffset - 8) / 16.0F, 0.5F);
         poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
         this.submitMinecartContents(minecartRenderState, blockState, poseStack, submitNodeCollector, minecartRenderState.lightCoords);
         poseStack.popPose();
      }

      poseStack.scale(-1.0F, -1.0F, 1.0F);
      submitNodeCollector.submitModel(
         this.model,
         minecartRenderState,
         poseStack,
         this.model.renderType(MINECART_LOCATION),
         minecartRenderState.lightCoords,
         OverlayTexture.NO_OVERLAY,
         minecartRenderState.outlineColor,
         null
      );
      poseStack.popPose();
   }

   private static <S extends MinecartRenderState> void newRender(S renderState, PoseStack poseStack) {
      poseStack.mulPose(Axis.YP.rotationDegrees(renderState.yRot));
      poseStack.mulPose(Axis.ZP.rotationDegrees(-renderState.xRot));
      poseStack.translate(0.0F, 0.375F, 0.0F);
   }

   private static <S extends MinecartRenderState> void oldRender(S renderState, PoseStack poseStack) {
      double d = renderState.x;
      double e = renderState.y;
      double f = renderState.z;
      float g = renderState.xRot;
      float h = renderState.yRot;
      if (renderState.posOnRail != null && renderState.frontPos != null && renderState.backPos != null) {
         Vec3 vec3 = renderState.frontPos;
         Vec3 vec32 = renderState.backPos;
         poseStack.translate(renderState.posOnRail.x - d, (vec3.y + vec32.y) / 2.0 - e, renderState.posOnRail.z - f);
         Vec3 vec33 = vec32.add(-vec3.x, -vec3.y, -vec3.z);
         if (vec33.length() != 0.0) {
            vec33 = vec33.normalize();
            h = (float)(Math.atan2(vec33.z, vec33.x) * 180.0 / Math.PI);
            g = (float)(Math.atan(vec33.y) * 73.0);
         }
      }

      poseStack.translate(0.0F, 0.375F, 0.0F);
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - h));
      poseStack.mulPose(Axis.ZP.rotationDegrees(-g));
   }

   public void extractRenderState(T abstractMinecart, S minecartRenderState, float f) {
      super.extractRenderState(abstractMinecart, minecartRenderState, f);
      if (abstractMinecart.getBehavior() instanceof NewMinecartBehavior newMinecartBehavior) {
         newExtractState(abstractMinecart, newMinecartBehavior, minecartRenderState, f);
         minecartRenderState.isNewRender = true;
      } else if (abstractMinecart.getBehavior() instanceof OldMinecartBehavior oldMinecartBehavior) {
         oldExtractState(abstractMinecart, oldMinecartBehavior, minecartRenderState, f);
         minecartRenderState.isNewRender = false;
      }

      long l = abstractMinecart.getId() * 493286711L;
      minecartRenderState.offsetSeed = l * l * 4392167121L + l * 98761L;
      minecartRenderState.hurtTime = abstractMinecart.getHurtTime() - f;
      minecartRenderState.hurtDir = abstractMinecart.getHurtDir();
      minecartRenderState.damageTime = Math.max(abstractMinecart.getDamage() - f, 0.0F);
      minecartRenderState.displayOffset = abstractMinecart.getDisplayOffset();
      minecartRenderState.displayBlockState = abstractMinecart.getDisplayBlockState();
   }

   private static <T extends AbstractMinecart, S extends MinecartRenderState> void newExtractState(
      T minecart, NewMinecartBehavior behavior, S reusedState, float partialTick
   ) {
      if (behavior.cartHasPosRotLerp()) {
         reusedState.renderPos = behavior.getCartLerpPosition(partialTick);
         reusedState.xRot = behavior.getCartLerpXRot(partialTick);
         reusedState.yRot = behavior.getCartLerpYRot(partialTick);
      } else {
         reusedState.renderPos = null;
         reusedState.xRot = minecart.getXRot();
         reusedState.yRot = minecart.getYRot();
      }
   }

   private static <T extends AbstractMinecart, S extends MinecartRenderState> void oldExtractState(
      T minecart, OldMinecartBehavior behavior, S reusedState, float partialTick
   ) {
      float f = 0.3F;
      reusedState.xRot = minecart.getXRot(partialTick);
      reusedState.yRot = minecart.getYRot(partialTick);
      double d = reusedState.x;
      double e = reusedState.y;
      double g = reusedState.z;
      Vec3 vec3 = behavior.getPos(d, e, g);
      if (vec3 != null) {
         reusedState.posOnRail = vec3;
         Vec3 vec32 = behavior.getPosOffs(d, e, g, 0.3F);
         Vec3 vec33 = behavior.getPosOffs(d, e, g, -0.3F);
         reusedState.frontPos = Objects.requireNonNullElse(vec32, vec3);
         reusedState.backPos = Objects.requireNonNullElse(vec33, vec3);
      } else {
         reusedState.posOnRail = null;
         reusedState.frontPos = null;
         reusedState.backPos = null;
      }
   }

   protected void submitMinecartContents(S renderState, BlockState blockState, PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight) {
      nodeCollector.submitBlock(poseStack, blockState, packedLight, OverlayTexture.NO_OVERLAY, renderState.outlineColor);
   }

   protected AABB getBoundingBoxForCulling(T abstractMinecart) {
      AABB aABB = super.getBoundingBoxForCulling(abstractMinecart);
      return !abstractMinecart.getDisplayBlockState().isAir() ? aABB.expandTowards(0.0, abstractMinecart.getDisplayOffset() * 0.75F / 16.0F, 0.0) : aABB;
   }

   public Vec3 getRenderOffset(S minecartRenderState) {
      Vec3 vec3 = super.getRenderOffset(minecartRenderState);
      return minecartRenderState.isNewRender && minecartRenderState.renderPos != null
         ? vec3.add(
            minecartRenderState.renderPos.x - minecartRenderState.x,
            minecartRenderState.renderPos.y - minecartRenderState.y,
            minecartRenderState.renderPos.z - minecartRenderState.z
         )
         : vec3;
   }
}
