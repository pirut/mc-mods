package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.dragon.EnderDragonModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class EnderDragonRenderer extends EntityRenderer<EnderDragon, EnderDragonRenderState> {
   public static final Identifier CRYSTAL_BEAM_LOCATION = Identifier.withDefaultNamespace("textures/entity/end_crystal/end_crystal_beam.png");
   private static final Identifier DRAGON_EXPLODING_LOCATION = Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon_exploding.png");
   private static final Identifier DRAGON_LOCATION = Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon.png");
   private static final Identifier DRAGON_EYES_LOCATION = Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon_eyes.png");
   private static final RenderType RENDER_TYPE = RenderTypes.entityCutoutNoCull(DRAGON_LOCATION);
   private static final RenderType DECAL = RenderTypes.entityDecal(DRAGON_LOCATION);
   private static final RenderType EYES = RenderTypes.eyes(DRAGON_EYES_LOCATION);
   private static final RenderType BEAM = RenderTypes.entitySmoothCutout(CRYSTAL_BEAM_LOCATION);
   private static final float HALF_SQRT_3 = (float)(Math.sqrt(3.0) / 2.0);
   private final EnderDragonModel model;

   public EnderDragonRenderer(EntityRendererProvider.Context context) {
      super(context);
      this.shadowRadius = 0.5F;
      this.model = new EnderDragonModel(context.bakeLayer(ModelLayers.ENDER_DRAGON));
   }

   public void submit(
      EnderDragonRenderState enderDragonRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState
   ) {
      poseStack.pushPose();
      float f = enderDragonRenderState.getHistoricalPos(7).yRot();
      float g = (float)(enderDragonRenderState.getHistoricalPos(5).y() - enderDragonRenderState.getHistoricalPos(10).y());
      poseStack.mulPose(Axis.YP.rotationDegrees(-f));
      poseStack.mulPose(Axis.XP.rotationDegrees(g * 10.0F));
      poseStack.translate(0.0F, 0.0F, 1.0F);
      poseStack.scale(-1.0F, -1.0F, 1.0F);
      poseStack.translate(0.0F, -1.501F, 0.0F);
      int i = OverlayTexture.pack(0.0F, enderDragonRenderState.hasRedOverlay);
      if (enderDragonRenderState.deathTime > 0.0F) {
         int j = ARGB.white(enderDragonRenderState.deathTime / 200.0F);
         submitNodeCollector.order(0)
            .submitModel(
               this.model,
               enderDragonRenderState,
               poseStack,
               RenderTypes.dragonExplosionAlpha(DRAGON_EXPLODING_LOCATION),
               enderDragonRenderState.lightCoords,
               OverlayTexture.NO_OVERLAY,
               j,
               null,
               enderDragonRenderState.outlineColor,
               null
            );
         submitNodeCollector.order(1)
            .submitModel(
               this.model, enderDragonRenderState, poseStack, DECAL, enderDragonRenderState.lightCoords, i, -1, null, enderDragonRenderState.outlineColor, null
            );
      } else {
         submitNodeCollector.order(0)
            .submitModel(
               this.model,
               enderDragonRenderState,
               poseStack,
               RENDER_TYPE,
               enderDragonRenderState.lightCoords,
               i,
               -1,
               null,
               enderDragonRenderState.outlineColor,
               null
            );
      }

      submitNodeCollector.submitModel(
         this.model,
         enderDragonRenderState,
         poseStack,
         EYES,
         enderDragonRenderState.lightCoords,
         OverlayTexture.NO_OVERLAY,
         enderDragonRenderState.outlineColor,
         null
      );
      if (enderDragonRenderState.deathTime > 0.0F) {
         float h = enderDragonRenderState.deathTime / 200.0F;
         poseStack.pushPose();
         poseStack.translate(0.0F, -1.0F, -2.0F);
         submitRays(poseStack, h, submitNodeCollector, RenderTypes.dragonRays());
         submitRays(poseStack, h, submitNodeCollector, RenderTypes.dragonRaysDepth());
         poseStack.popPose();
      }

      poseStack.popPose();
      if (enderDragonRenderState.beamOffset != null) {
         submitCrystalBeams(
            (float)enderDragonRenderState.beamOffset.x,
            (float)enderDragonRenderState.beamOffset.y,
            (float)enderDragonRenderState.beamOffset.z,
            enderDragonRenderState.ageInTicks,
            poseStack,
            submitNodeCollector,
            enderDragonRenderState.lightCoords
         );
      }

      super.submit(enderDragonRenderState, poseStack, submitNodeCollector, cameraRenderState);
   }

   private static void submitRays(PoseStack poseStack, float deathProgress, SubmitNodeCollector nodeCollector, RenderType renderType) {
      nodeCollector.submitCustomGeometry(
         poseStack,
         renderType,
         (pose, vertexConsumer) -> {
            float g = Math.min(deathProgress > 0.8F ? (deathProgress - 0.8F) / 0.2F : 0.0F, 1.0F);
            int i = ARGB.colorFromFloat(1.0F - g, 1.0F, 1.0F, 1.0F);
            int j = 16711935;
            RandomSource randomSource = RandomSource.create(432L);
            Vector3f vector3f = new Vector3f();
            Vector3f vector3f2 = new Vector3f();
            Vector3f vector3f3 = new Vector3f();
            Vector3f vector3f4 = new Vector3f();
            Quaternionf quaternionf = new Quaternionf();
            int k = Mth.floor((deathProgress + deathProgress * deathProgress) / 2.0F * 60.0F);

            for (int l = 0; l < k; l++) {
               quaternionf.rotationXYZ(
                     randomSource.nextFloat() * (float) (Math.PI * 2),
                     randomSource.nextFloat() * (float) (Math.PI * 2),
                     randomSource.nextFloat() * (float) (Math.PI * 2)
                  )
                  .rotateXYZ(
                     randomSource.nextFloat() * (float) (Math.PI * 2),
                     randomSource.nextFloat() * (float) (Math.PI * 2),
                     randomSource.nextFloat() * (float) (Math.PI * 2) + deathProgress * (float) (Math.PI / 2)
                  );
               pose.rotate(quaternionf);
               float h = randomSource.nextFloat() * 20.0F + 5.0F + g * 10.0F;
               float m = randomSource.nextFloat() * 2.0F + 1.0F + g * 2.0F;
               vector3f2.set(-HALF_SQRT_3 * m, h, -0.5F * m);
               vector3f3.set(HALF_SQRT_3 * m, h, -0.5F * m);
               vector3f4.set(0.0F, h, m);
               vertexConsumer.addVertex(pose, vector3f).setColor(i);
               vertexConsumer.addVertex(pose, vector3f2).setColor(16711935);
               vertexConsumer.addVertex(pose, vector3f3).setColor(16711935);
               vertexConsumer.addVertex(pose, vector3f).setColor(i);
               vertexConsumer.addVertex(pose, vector3f3).setColor(16711935);
               vertexConsumer.addVertex(pose, vector3f4).setColor(16711935);
               vertexConsumer.addVertex(pose, vector3f).setColor(i);
               vertexConsumer.addVertex(pose, vector3f4).setColor(16711935);
               vertexConsumer.addVertex(pose, vector3f2).setColor(16711935);
            }
         }
      );
   }

   public static void submitCrystalBeams(
      float offsetX, float offsetY, float offsetZ, float ageInTicks, PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight
   ) {
      float f = Mth.sqrt(offsetX * offsetX + offsetZ * offsetZ);
      float g = Mth.sqrt(offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ);
      poseStack.pushPose();
      poseStack.translate(0.0F, 2.0F, 0.0F);
      poseStack.mulPose(Axis.YP.rotation((float)(-Math.atan2(offsetZ, offsetX)) - (float) (Math.PI / 2)));
      poseStack.mulPose(Axis.XP.rotation((float)(-Math.atan2(f, offsetY)) - (float) (Math.PI / 2)));
      float h = 0.0F - ageInTicks * 0.01F;
      float i = g / 32.0F - ageInTicks * 0.01F;
      nodeCollector.submitCustomGeometry(
         poseStack,
         BEAM,
         (pose, vertexConsumer) -> {
            int j = 8;
            float k = 0.0F;
            float l = 0.75F;
            float m = 0.0F;

            for (int n = 1; n <= 8; n++) {
               float o = Mth.sin(n * (float) (Math.PI * 2) / 8.0F) * 0.75F;
               float p = Mth.cos(n * (float) (Math.PI * 2) / 8.0F) * 0.75F;
               float q = n / 8.0F;
               vertexConsumer.addVertex(pose, k * 0.2F, l * 0.2F, 0.0F)
                  .setColor(-16777216)
                  .setUv(m, h)
                  .setOverlay(OverlayTexture.NO_OVERLAY)
                  .setLight(packedLight)
                  .setNormal(pose, 0.0F, -1.0F, 0.0F);
               vertexConsumer.addVertex(pose, k, l, g)
                  .setColor(-1)
                  .setUv(m, i)
                  .setOverlay(OverlayTexture.NO_OVERLAY)
                  .setLight(packedLight)
                  .setNormal(pose, 0.0F, -1.0F, 0.0F);
               vertexConsumer.addVertex(pose, o, p, g)
                  .setColor(-1)
                  .setUv(q, i)
                  .setOverlay(OverlayTexture.NO_OVERLAY)
                  .setLight(packedLight)
                  .setNormal(pose, 0.0F, -1.0F, 0.0F);
               vertexConsumer.addVertex(pose, o * 0.2F, p * 0.2F, 0.0F)
                  .setColor(-16777216)
                  .setUv(q, h)
                  .setOverlay(OverlayTexture.NO_OVERLAY)
                  .setLight(packedLight)
                  .setNormal(pose, 0.0F, -1.0F, 0.0F);
               k = o;
               l = p;
               m = q;
            }
         }
      );
      poseStack.popPose();
   }

   public EnderDragonRenderState createRenderState() {
      return new EnderDragonRenderState();
   }

   public void extractRenderState(EnderDragon enderDragon, EnderDragonRenderState enderDragonRenderState, float f) {
      super.extractRenderState(enderDragon, enderDragonRenderState, f);
      enderDragonRenderState.flapTime = Mth.lerp(f, enderDragon.oFlapTime, enderDragon.flapTime);
      enderDragonRenderState.deathTime = enderDragon.dragonDeathTime > 0 ? enderDragon.dragonDeathTime + f : 0.0F;
      enderDragonRenderState.hasRedOverlay = enderDragon.hurtTime > 0;
      EndCrystal endCrystal = enderDragon.nearestCrystal;
      if (endCrystal != null) {
         Vec3 vec3 = endCrystal.getPosition(f).add(0.0, EndCrystalRenderer.getY(endCrystal.time + f), 0.0);
         enderDragonRenderState.beamOffset = vec3.subtract(enderDragon.getPosition(f));
      } else {
         enderDragonRenderState.beamOffset = null;
      }

      DragonPhaseInstance dragonPhaseInstance = enderDragon.getPhaseManager().getCurrentPhase();
      enderDragonRenderState.isLandingOrTakingOff = dragonPhaseInstance == EnderDragonPhase.LANDING || dragonPhaseInstance == EnderDragonPhase.TAKEOFF;
      enderDragonRenderState.isSitting = dragonPhaseInstance.isSitting();
      BlockPos blockPos = enderDragon.level()
         .getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EndPodiumFeature.getLocation(enderDragon.getFightOrigin()));
      enderDragonRenderState.distanceToEgg = blockPos.distToCenterSqr(enderDragon.position());
      enderDragonRenderState.partialTicks = enderDragon.isDeadOrDying() ? 0.0F : f;
      enderDragonRenderState.flightHistory.copyFrom(enderDragon.flightHistory);
   }

   protected boolean affectedByCulling(EnderDragon enderDragon) {
      return false;
   }
}
