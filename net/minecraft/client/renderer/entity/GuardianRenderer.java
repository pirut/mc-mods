package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.guardian.GuardianModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.GuardianRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class GuardianRenderer extends MobRenderer<Guardian, GuardianRenderState, GuardianModel> {
   private static final Identifier GUARDIAN_LOCATION = Identifier.withDefaultNamespace("textures/entity/guardian.png");
   private static final Identifier GUARDIAN_BEAM_LOCATION = Identifier.withDefaultNamespace("textures/entity/guardian_beam.png");
   private static final RenderType BEAM_RENDER_TYPE = RenderTypes.entityCutoutNoCull(GUARDIAN_BEAM_LOCATION);

   public GuardianRenderer(EntityRendererProvider.Context context) {
      this(context, 0.5F, ModelLayers.GUARDIAN);
   }

   protected GuardianRenderer(EntityRendererProvider.Context context, float shadowRadius, ModelLayerLocation layer) {
      super(context, new GuardianModel(context.bakeLayer(layer)), shadowRadius);
   }

   public boolean shouldRender(Guardian livingEntity, Frustum camera, double camX, double camY, double camZ) {
      if (super.shouldRender(livingEntity, camera, camX, camY, camZ)) {
         return true;
      } else {
         if (livingEntity.hasActiveAttackTarget()) {
            LivingEntity livingEntity2 = livingEntity.getActiveAttackTarget();
            if (livingEntity2 != null) {
               Vec3 vec3 = this.getPosition(livingEntity2, livingEntity2.getBbHeight() * 0.5, 1.0F);
               Vec3 vec32 = this.getPosition(livingEntity, livingEntity.getEyeHeight(), 1.0F);
               return camera.isVisible(new AABB(vec32.x, vec32.y, vec32.z, vec3.x, vec3.y, vec3.z));
            }
         }

         return false;
      }
   }

   private Vec3 getPosition(LivingEntity livingEntity, double yOffset, float partialTick) {
      double d = Mth.lerp((double)partialTick, livingEntity.xOld, livingEntity.getX());
      double e = Mth.lerp((double)partialTick, livingEntity.yOld, livingEntity.getY()) + yOffset;
      double f = Mth.lerp((double)partialTick, livingEntity.zOld, livingEntity.getZ());
      return new Vec3(d, e, f);
   }

   public void submit(
      GuardianRenderState guardianRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState
   ) {
      super.submit(guardianRenderState, poseStack, submitNodeCollector, cameraRenderState);
      Vec3 vec3 = guardianRenderState.attackTargetPosition;
      if (vec3 != null) {
         float f = guardianRenderState.attackTime * 0.5F % 1.0F;
         poseStack.pushPose();
         poseStack.translate(0.0F, guardianRenderState.eyeHeight, 0.0F);
         renderBeam(
            poseStack, submitNodeCollector, vec3.subtract(guardianRenderState.eyePosition), guardianRenderState.attackTime, guardianRenderState.attackScale, f
         );
         poseStack.popPose();
      }
   }

   private static void renderBeam(PoseStack poseStack, SubmitNodeCollector nodeCollector, Vec3 beamVector, float attackTime, float scale, float animationTime) {
      float f = (float)(beamVector.length() + 1.0);
      beamVector = beamVector.normalize();
      float g = (float)Math.acos(beamVector.y);
      float h = (float) (Math.PI / 2) - (float)Math.atan2(beamVector.z, beamVector.x);
      poseStack.mulPose(Axis.YP.rotationDegrees(h * (180.0F / (float)Math.PI)));
      poseStack.mulPose(Axis.XP.rotationDegrees(g * (180.0F / (float)Math.PI)));
      float i = attackTime * 0.05F * -1.5F;
      float j = scale * scale;
      int k = 64 + (int)(j * 191.0F);
      int l = 32 + (int)(j * 191.0F);
      int m = 128 - (int)(j * 64.0F);
      float n = 0.2F;
      float o = 0.282F;
      float p = Mth.cos(i + (float) (Math.PI * 3.0 / 4.0)) * 0.282F;
      float q = Mth.sin(i + (float) (Math.PI * 3.0 / 4.0)) * 0.282F;
      float r = Mth.cos(i + (float) (Math.PI / 4)) * 0.282F;
      float s = Mth.sin(i + (float) (Math.PI / 4)) * 0.282F;
      float t = Mth.cos(i + ((float) Math.PI * 5.0F / 4.0F)) * 0.282F;
      float u = Mth.sin(i + ((float) Math.PI * 5.0F / 4.0F)) * 0.282F;
      float v = Mth.cos(i + ((float) Math.PI * 7.0F / 4.0F)) * 0.282F;
      float w = Mth.sin(i + ((float) Math.PI * 7.0F / 4.0F)) * 0.282F;
      float x = Mth.cos(i + (float) Math.PI) * 0.2F;
      float y = Mth.sin(i + (float) Math.PI) * 0.2F;
      float z = Mth.cos(i + 0.0F) * 0.2F;
      float aa = Mth.sin(i + 0.0F) * 0.2F;
      float ab = Mth.cos(i + (float) (Math.PI / 2)) * 0.2F;
      float ac = Mth.sin(i + (float) (Math.PI / 2)) * 0.2F;
      float ad = Mth.cos(i + (float) (Math.PI * 3.0 / 2.0)) * 0.2F;
      float ae = Mth.sin(i + (float) (Math.PI * 3.0 / 2.0)) * 0.2F;
      float ag = 0.0F;
      float ah = 0.4999F;
      float ai = -1.0F + animationTime;
      float aj = ai + f * 2.5F;
      nodeCollector.submitCustomGeometry(poseStack, BEAM_RENDER_TYPE, (pose, vertexConsumer) -> {
         vertex(vertexConsumer, pose, x, f, y, k, l, m, 0.4999F, aj);
         vertex(vertexConsumer, pose, x, 0.0F, y, k, l, m, 0.4999F, ai);
         vertex(vertexConsumer, pose, z, 0.0F, aa, k, l, m, 0.0F, ai);
         vertex(vertexConsumer, pose, z, f, aa, k, l, m, 0.0F, aj);
         vertex(vertexConsumer, pose, ab, f, ac, k, l, m, 0.4999F, aj);
         vertex(vertexConsumer, pose, ab, 0.0F, ac, k, l, m, 0.4999F, ai);
         vertex(vertexConsumer, pose, ad, 0.0F, ae, k, l, m, 0.0F, ai);
         vertex(vertexConsumer, pose, ad, f, ae, k, l, m, 0.0F, aj);
         float acx = Mth.floor(attackTime) % 2 == 0 ? 0.5F : 0.0F;
         vertex(vertexConsumer, pose, p, f, q, k, l, m, 0.5F, acx + 0.5F);
         vertex(vertexConsumer, pose, r, f, s, k, l, m, 1.0F, acx + 0.5F);
         vertex(vertexConsumer, pose, v, f, w, k, l, m, 1.0F, acx);
         vertex(vertexConsumer, pose, t, f, u, k, l, m, 0.5F, acx);
      });
   }

   private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, int red, int green, int blue, float u, float v) {
      consumer.addVertex(pose, x, y, z)
         .setColor(red, green, blue, 255)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(pose, 0.0F, 1.0F, 0.0F);
   }

   public Identifier getTextureLocation(GuardianRenderState guardianRenderState) {
      return GUARDIAN_LOCATION;
   }

   public GuardianRenderState createRenderState() {
      return new GuardianRenderState();
   }

   public void extractRenderState(Guardian guardian, GuardianRenderState guardianRenderState, float f) {
      super.extractRenderState(guardian, guardianRenderState, f);
      guardianRenderState.spikesAnimation = guardian.getSpikesAnimation(f);
      guardianRenderState.tailAnimation = guardian.getTailAnimation(f);
      guardianRenderState.eyePosition = guardian.getEyePosition(f);
      Entity entity = getEntityToLookAt(guardian);
      if (entity != null) {
         guardianRenderState.lookDirection = guardian.getViewVector(f);
         guardianRenderState.lookAtPosition = entity.getEyePosition(f);
      } else {
         guardianRenderState.lookDirection = null;
         guardianRenderState.lookAtPosition = null;
      }

      LivingEntity livingEntity = guardian.getActiveAttackTarget();
      if (livingEntity != null) {
         guardianRenderState.attackScale = guardian.getAttackAnimationScale(f);
         guardianRenderState.attackTime = guardian.getClientSideAttackTime() + f;
         guardianRenderState.attackTargetPosition = this.getPosition(livingEntity, livingEntity.getBbHeight() * 0.5, f);
      } else {
         guardianRenderState.attackTargetPosition = null;
      }
   }

   @Nullable
   private static Entity getEntityToLookAt(Guardian guardian) {
      Entity entity = Minecraft.getInstance().getCameraEntity();
      return (Entity)(guardian.hasActiveAttackTarget() ? guardian.getActiveAttackTarget() : entity);
   }
}
