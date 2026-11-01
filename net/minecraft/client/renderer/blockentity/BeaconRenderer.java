package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BeaconRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BeaconBeamOwner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class BeaconRenderer<T extends BlockEntity & BeaconBeamOwner> implements BlockEntityRenderer<T, BeaconRenderState> {
   public static final Identifier BEAM_LOCATION = Identifier.withDefaultNamespace("textures/entity/beacon_beam.png");
   public static final int MAX_RENDER_Y = 2048;
   private static final float BEAM_SCALE_THRESHOLD = 96.0F;
   public static final float SOLID_BEAM_RADIUS = 0.2F;
   public static final float BEAM_GLOW_RADIUS = 0.25F;

   public BeaconRenderState createRenderState() {
      return new BeaconRenderState();
   }

   public void extractRenderState(
      T blockEntity, BeaconRenderState beaconRenderState, float f, Vec3 vec3, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
   ) {
      BlockEntityRenderer.super.extractRenderState(blockEntity, beaconRenderState, f, vec3, crumblingOverlay);
      extract(blockEntity, beaconRenderState, f, vec3);
   }

   public static <T extends BlockEntity & BeaconBeamOwner> void extract(T sign, BeaconRenderState renderState, float partialTick, Vec3 cameraPosition) {
      renderState.animationTime = sign.getLevel() != null ? Math.floorMod(sign.getLevel().getGameTime(), 40) + partialTick : 0.0F;
      renderState.sections = sign.getBeamSections().stream().map(section -> new BeaconRenderState.Section(section.getColor(), section.getHeight())).toList();
      float f = (float)cameraPosition.subtract(renderState.blockPos.getCenter()).horizontalDistance();
      LocalPlayer localPlayer = Minecraft.getInstance().player;
      renderState.beamRadiusScale = localPlayer != null && localPlayer.isScoping() ? 1.0F : Math.max(1.0F, f / 96.0F);
   }

   public void submit(BeaconRenderState beaconRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
      int i = 0;

      for (int j = 0; j < beaconRenderState.sections.size(); j++) {
         BeaconRenderState.Section section = beaconRenderState.sections.get(j);
         submitBeaconBeam(
            poseStack,
            submitNodeCollector,
            beaconRenderState.beamRadiusScale,
            beaconRenderState.animationTime,
            i,
            j == beaconRenderState.sections.size() - 1 ? 2048 : section.height(),
            section.color()
         );
         i += section.height();
      }
   }

   private static void submitBeaconBeam(
      PoseStack poseStack, SubmitNodeCollector nodeCollector, float radiusScale, float animationTime, int yOffset, int height, int color
   ) {
      submitBeaconBeam(poseStack, nodeCollector, BEAM_LOCATION, 1.0F, animationTime, yOffset, height, color, 0.2F * radiusScale, 0.25F * radiusScale);
   }

   public static void submitBeaconBeam(
      PoseStack poseStack,
      SubmitNodeCollector nodeCollector,
      Identifier location,
      float partialTick,
      float animationTime,
      int yOffset,
      int height,
      int color,
      float beamRadius,
      float glowRadius
   ) {
      int i = yOffset + height;
      poseStack.pushPose();
      poseStack.translate(0.5, 0.0, 0.5);
      float f = height < 0 ? animationTime : -animationTime;
      float g = Mth.frac(f * 0.2F - Mth.floor(f * 0.1F));
      poseStack.pushPose();
      poseStack.mulPose(Axis.YP.rotationDegrees(animationTime * 2.25F - 45.0F));
      float h = 0.0F;
      float l = 0.0F;
      float m = -beamRadius;
      float n = 0.0F;
      float o = 0.0F;
      float p = -beamRadius;
      float q = 0.0F;
      float r = 1.0F;
      float s = -1.0F + g;
      float t = height * partialTick * (0.5F / beamRadius) + s;
      nodeCollector.submitCustomGeometry(
         poseStack,
         RenderTypes.beaconBeam(location, false),
         (pose, vertexConsumer) -> renderPart(pose, vertexConsumer, color, yOffset, i, 0.0F, beamRadius, beamRadius, 0.0F, m, 0.0F, 0.0F, p, 0.0F, 1.0F, t, s)
      );
      poseStack.popPose();
      h = -glowRadius;
      float j = -glowRadius;
      l = -glowRadius;
      m = -glowRadius;
      q = 0.0F;
      r = 1.0F;
      s = -1.0F + g;
      t = height * partialTick + s;
      nodeCollector.submitCustomGeometry(
         poseStack,
         RenderTypes.beaconBeam(location, true),
         (pose, vertexConsumer) -> renderPart(
            pose, vertexConsumer, ARGB.color(32, color), yOffset, i, h, j, glowRadius, l, m, glowRadius, glowRadius, glowRadius, 0.0F, 1.0F, t, s
         )
      );
      poseStack.popPose();
   }

   private static void renderPart(
      PoseStack.Pose pose,
      VertexConsumer consumer,
      int color,
      int minY,
      int maxY,
      float x1,
      float z1,
      float x2,
      float z2,
      float x3,
      float z3,
      float x4,
      float z4,
      float minU,
      float maxU,
      float minV,
      float maxV
   ) {
      renderQuad(pose, consumer, color, minY, maxY, x1, z1, x2, z2, minU, maxU, minV, maxV);
      renderQuad(pose, consumer, color, minY, maxY, x4, z4, x3, z3, minU, maxU, minV, maxV);
      renderQuad(pose, consumer, color, minY, maxY, x2, z2, x4, z4, minU, maxU, minV, maxV);
      renderQuad(pose, consumer, color, minY, maxY, x3, z3, x1, z1, minU, maxU, minV, maxV);
   }

   private static void renderQuad(
      PoseStack.Pose pose,
      VertexConsumer consumer,
      int color,
      int minY,
      int maxY,
      float minX,
      float minZ,
      float maxX,
      float maxZ,
      float minU,
      float maxU,
      float minV,
      float maxV
   ) {
      addVertex(pose, consumer, color, maxY, minX, minZ, maxU, minV);
      addVertex(pose, consumer, color, minY, minX, minZ, maxU, maxV);
      addVertex(pose, consumer, color, minY, maxX, maxZ, minU, maxV);
      addVertex(pose, consumer, color, maxY, maxX, maxZ, minU, minV);
   }

   private static void addVertex(PoseStack.Pose pose, VertexConsumer consumer, int color, int y, float x, float z, float u, float v) {
      consumer.addVertex(pose, x, (float)y, z)
         .setColor(color)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(pose, 0.0F, 1.0F, 0.0F);
   }

   @Override
   public boolean shouldRenderOffScreen() {
      return true;
   }

   @Override
   public int getViewDistance() {
      return Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;
   }

   @Override
   public boolean shouldRender(T blockEntity, Vec3 cameraPos) {
      return Vec3.atCenterOf(blockEntity.getBlockPos()).multiply(1.0, 0.0, 1.0).closerThan(cameraPos.multiply(1.0, 0.0, 1.0), this.getViewDistance());
   }
}
