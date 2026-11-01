package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.FishingHookRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class FishingHookRenderer extends EntityRenderer<FishingHook, FishingHookRenderState> {
   private static final Identifier TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/fishing_hook.png");
   private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(TEXTURE_LOCATION);
   private static final double VIEW_BOBBING_SCALE = 960.0;

   public FishingHookRenderer(EntityRendererProvider.Context context) {
      super(context);
   }

   public boolean shouldRender(FishingHook fishingHook, Frustum frustum, double d, double e, double f) {
      return super.shouldRender(fishingHook, frustum, d, e, f) && fishingHook.getPlayerOwner() != null;
   }

   public void submit(
      FishingHookRenderState fishingHookRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState
   ) {
      poseStack.pushPose();
      poseStack.pushPose();
      poseStack.scale(0.5F, 0.5F, 0.5F);
      poseStack.mulPose(cameraRenderState.orientation);
      submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, vertexConsumer) -> {
         vertex(vertexConsumer, pose, fishingHookRenderState.lightCoords, 0.0F, 0, 0, 1);
         vertex(vertexConsumer, pose, fishingHookRenderState.lightCoords, 1.0F, 0, 1, 1);
         vertex(vertexConsumer, pose, fishingHookRenderState.lightCoords, 1.0F, 1, 1, 0);
         vertex(vertexConsumer, pose, fishingHookRenderState.lightCoords, 0.0F, 1, 0, 0);
      });
      poseStack.popPose();
      float f = (float)fishingHookRenderState.lineOriginOffset.x;
      float g = (float)fishingHookRenderState.lineOriginOffset.y;
      float h = (float)fishingHookRenderState.lineOriginOffset.z;
      float i = Minecraft.getInstance().getWindow().getAppropriateLineWidth();
      submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, vertexConsumer) -> {
         int j = 16;

         for (int k = 0; k < 16; k++) {
            float l = fraction(k, 16);
            float m = fraction(k + 1, 16);
            stringVertex(f, g, h, vertexConsumer, pose, l, m, i);
            stringVertex(f, g, h, vertexConsumer, pose, m, l, i);
         }
      });
      poseStack.popPose();
      super.submit(fishingHookRenderState, poseStack, submitNodeCollector, cameraRenderState);
   }

   public static HumanoidArm getHoldingArm(Player player) {
      return player.getMainHandItem().getItem() instanceof FishingRodItem ? player.getMainArm() : player.getMainArm().getOpposite();
   }

   private Vec3 getPlayerHandPos(Player player, float handAngle, float partialTick) {
      int i = getHoldingArm(player) == HumanoidArm.RIGHT ? 1 : -1;
      if (this.entityRenderDispatcher.options.getCameraType().isFirstPerson() && player == Minecraft.getInstance().player) {
         double l = 960.0 / this.entityRenderDispatcher.options.fov().get().intValue();
         Vec3 vec3 = this.entityRenderDispatcher
            .camera
            .getNearPlane()
            .getPointOnPlane(i * 0.525F, -0.1F)
            .scale(l)
            .yRot(handAngle * 0.5F)
            .xRot(-handAngle * 0.7F);
         return player.getEyePosition(partialTick).add(vec3);
      } else {
         float f = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot) * (float) (Math.PI / 180.0);
         double d = Mth.sin(f);
         double e = Mth.cos(f);
         float g = player.getScale();
         double h = i * 0.35 * g;
         double j = 0.8 * g;
         float k = player.isCrouching() ? -0.1875F : 0.0F;
         return player.getEyePosition(partialTick).add(-e * h - d * j, k - 0.45 * g, -d * h + e * j);
      }
   }

   private static float fraction(int numerator, int denominator) {
      return (float)numerator / denominator;
   }

   private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int packedLight, float x, int y, int u, int v) {
      consumer.addVertex(pose, x - 0.5F, y - 0.5F, 0.0F)
         .setColor(-1)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(packedLight)
         .setNormal(pose, 0.0F, 1.0F, 0.0F);
   }

   private static void stringVertex(
      float x, float y, float z, VertexConsumer consumer, PoseStack.Pose pose, float stringFraction, float nextStringFraction, float index
   ) {
      float f = x * stringFraction;
      float g = y * (stringFraction * stringFraction + stringFraction) * 0.5F + 0.25F;
      float h = z * stringFraction;
      float i = x * nextStringFraction - f;
      float j = y * (nextStringFraction * nextStringFraction + nextStringFraction) * 0.5F + 0.25F - g;
      float k = z * nextStringFraction - h;
      float l = Mth.sqrt(i * i + j * j + k * k);
      i /= l;
      j /= l;
      k /= l;
      consumer.addVertex(pose, f, g, h).setColor(-16777216).setNormal(pose, i, j, k).setLineWidth(index);
   }

   public FishingHookRenderState createRenderState() {
      return new FishingHookRenderState();
   }

   public void extractRenderState(FishingHook fishingHook, FishingHookRenderState fishingHookRenderState, float f) {
      super.extractRenderState(fishingHook, fishingHookRenderState, f);
      Player player = fishingHook.getPlayerOwner();
      if (player == null) {
         fishingHookRenderState.lineOriginOffset = Vec3.ZERO;
      } else {
         float g = player.getAttackAnim(f);
         float h = Mth.sin(Mth.sqrt(g) * (float) Math.PI);
         Vec3 vec3 = this.getPlayerHandPos(player, h, f);
         Vec3 vec32 = fishingHook.getPosition(f).add(0.0, 0.25, 0.0);
         fishingHookRenderState.lineOriginOffset = vec3.subtract(vec32);
      }
   }

   protected boolean affectedByCulling(FishingHook fishingHook) {
      return false;
   }
}
