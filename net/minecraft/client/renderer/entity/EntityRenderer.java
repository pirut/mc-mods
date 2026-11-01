package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public abstract class EntityRenderer<T extends Entity, S extends EntityRenderState> {
   private static final float SHADOW_POWER_FALLOFF_Y = 0.5F;
   private static final float MAX_SHADOW_RADIUS = 32.0F;
   public static final float NAMETAG_SCALE = 0.025F;
   protected final EntityRenderDispatcher entityRenderDispatcher;
   private final Font font;
   protected float shadowRadius;
   protected float shadowStrength = 1.0F;

   protected EntityRenderer(EntityRendererProvider.Context context) {
      this.entityRenderDispatcher = context.getEntityRenderDispatcher();
      this.font = context.getFont();
   }

   public final int getPackedLightCoords(T entity, float partialTicks) {
      BlockPos blockPos = BlockPos.containing(entity.getLightProbePosition(partialTicks));
      return LightTexture.pack(this.getBlockLightLevel(entity, blockPos), this.getSkyLightLevel(entity, blockPos));
   }

   protected int getSkyLightLevel(T entity, BlockPos pos) {
      return entity.level().getBrightness(LightLayer.SKY, pos);
   }

   protected int getBlockLightLevel(T entity, BlockPos pos) {
      return entity.isOnFire() ? 15 : entity.level().getBrightness(LightLayer.BLOCK, pos);
   }

   public boolean shouldRender(T livingEntity, Frustum camera, double camX, double camY, double camZ) {
      if (!livingEntity.shouldRender(camX, camY, camZ)) {
         return false;
      } else if (!this.affectedByCulling(livingEntity)) {
         return true;
      } else {
         AABB aABB = this.getBoundingBoxForCulling(livingEntity).inflate(0.5);
         if (aABB.hasNaN() || aABB.getSize() == 0.0) {
            aABB = new AABB(
               livingEntity.getX() - 2.0,
               livingEntity.getY() - 2.0,
               livingEntity.getZ() - 2.0,
               livingEntity.getX() + 2.0,
               livingEntity.getY() + 2.0,
               livingEntity.getZ() + 2.0
            );
         }

         if (camera.isVisible(aABB)) {
            return true;
         } else {
            if (livingEntity instanceof Leashable leashable) {
               Entity entity = leashable.getLeashHolder();
               if (entity != null) {
                  AABB aABB2 = this.entityRenderDispatcher.getRenderer(entity).getBoundingBoxForCulling(entity);
                  return camera.isVisible(aABB2) || camera.isVisible(aABB.minmax(aABB2));
               }
            }

            return false;
         }
      }
   }

   protected AABB getBoundingBoxForCulling(T minecraft) {
      return minecraft.getBoundingBox();
   }

   protected boolean affectedByCulling(T display) {
      return true;
   }

   public Vec3 getRenderOffset(S renderState) {
      return renderState.passengerOffset != null ? renderState.passengerOffset : Vec3.ZERO;
   }

   public void submit(S renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
      if (renderState.leashStates != null) {
         for (EntityRenderState.LeashState leashState : renderState.leashStates) {
            nodeCollector.submitLeash(poseStack, leashState);
         }
      }

      this.submitNameTag(renderState, poseStack, nodeCollector, cameraRenderState);
   }

   protected boolean shouldShowName(T entity, double distanceToCameraSq) {
      return entity.shouldShowName() || entity.hasCustomName() && entity == this.entityRenderDispatcher.crosshairPickEntity;
   }

   public Font getFont() {
      return this.font;
   }

   protected void submitNameTag(S renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
      if (renderState.nameTag != null) {
         nodeCollector.submitNameTag(
            poseStack,
            renderState.nameTagAttachment,
            0,
            renderState.nameTag,
            !renderState.isDiscrete,
            renderState.lightCoords,
            renderState.distanceToCameraSq,
            cameraRenderState
         );
      }
   }

   @Nullable
   protected Component getNameTag(T entity) {
      return entity.getDisplayName();
   }

   protected float getShadowRadius(S renderState) {
      return this.shadowRadius;
   }

   protected float getShadowStrength(S renderState) {
      return this.shadowStrength;
   }

   public abstract S createRenderState();

   public final S createRenderState(T entity, float partialTick) {
      S entityRenderState = this.createRenderState();
      this.extractRenderState(entity, entityRenderState, partialTick);
      this.finalizeRenderState(entity, entityRenderState);
      return entityRenderState;
   }

   public void extractRenderState(T entity, S reusedState, float partialTick) {
      reusedState.entityType = entity.getType();
      reusedState.x = Mth.lerp((double)partialTick, entity.xOld, entity.getX());
      reusedState.y = Mth.lerp((double)partialTick, entity.yOld, entity.getY());
      reusedState.z = Mth.lerp((double)partialTick, entity.zOld, entity.getZ());
      reusedState.isInvisible = entity.isInvisible();
      reusedState.ageInTicks = entity.tickCount + partialTick;
      reusedState.boundingBoxWidth = entity.getBbWidth();
      reusedState.boundingBoxHeight = entity.getBbHeight();
      reusedState.eyeHeight = entity.getEyeHeight();
      if (entity.isPassenger()
         && entity.getVehicle() instanceof AbstractMinecart abstractMinecart
         && abstractMinecart.getBehavior() instanceof NewMinecartBehavior newMinecartBehavior
         && newMinecartBehavior.cartHasPosRotLerp()) {
         double d = Mth.lerp((double)partialTick, abstractMinecart.xOld, abstractMinecart.getX());
         double e = Mth.lerp((double)partialTick, abstractMinecart.yOld, abstractMinecart.getY());
         double f = Mth.lerp((double)partialTick, abstractMinecart.zOld, abstractMinecart.getZ());
         reusedState.passengerOffset = newMinecartBehavior.getCartLerpPosition(partialTick).subtract(new Vec3(d, e, f));
      } else {
         reusedState.passengerOffset = null;
      }

      if (this.entityRenderDispatcher.camera != null) {
         reusedState.distanceToCameraSq = this.entityRenderDispatcher.distanceToSqr(entity);
         boolean bl = reusedState.distanceToCameraSq < 4096.0 && this.shouldShowName(entity, reusedState.distanceToCameraSq);
         if (bl) {
            reusedState.nameTag = this.getNameTag(entity);
            reusedState.nameTagAttachment = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getYRot(partialTick));
         } else {
            reusedState.nameTag = null;
         }
      }

      label72: {
         reusedState.isDiscrete = entity.isDiscrete();
         Level level = entity.level();
         if (entity instanceof Leashable leashable) {
            Entity g = leashable.getLeashHolder();
            if (g instanceof Entity) {
               float gx = entity.getPreciseBodyRotation(partialTick) * (float) (Math.PI / 180.0);
               Vec3 vec3 = leashable.getLeashOffset(partialTick);
               BlockPos blockPos = BlockPos.containing(entity.getEyePosition(partialTick));
               BlockPos blockPos2 = BlockPos.containing(g.getEyePosition(partialTick));
               int i = this.getBlockLightLevel(entity, blockPos);
               int j = this.entityRenderDispatcher.getRenderer(g).getBlockLightLevel(g, blockPos2);
               int k = level.getBrightness(LightLayer.SKY, blockPos);
               int l = level.getBrightness(LightLayer.SKY, blockPos2);
               boolean bl2 = g.supportQuadLeashAsHolder() && leashable.supportQuadLeash();
               int m = bl2 ? 4 : 1;
               if (reusedState.leashStates == null || reusedState.leashStates.size() != m) {
                  reusedState.leashStates = new ArrayList<>(m);

                  for (int n = 0; n < m; n++) {
                     reusedState.leashStates.add(new EntityRenderState.LeashState());
                  }
               }

               if (bl2) {
                  float h = g.getPreciseBodyRotation(partialTick) * (float) (Math.PI / 180.0);
                  Vec3 vec32 = g.getPosition(partialTick);
                  Vec3[] vec3s = leashable.getQuadLeashOffsets();
                  Vec3[] vec3s2 = g.getQuadLeashHolderOffsets();
                  int o = 0;

                  while (true) {
                     if (o >= m) {
                        break label72;
                     }

                     EntityRenderState.LeashState leashState = reusedState.leashStates.get(o);
                     leashState.offset = vec3s[o].yRot(-gx);
                     leashState.start = entity.getPosition(partialTick).add(leashState.offset);
                     leashState.end = vec32.add(vec3s2[o].yRot(-h));
                     leashState.startBlockLight = i;
                     leashState.endBlockLight = j;
                     leashState.startSkyLight = k;
                     leashState.endSkyLight = l;
                     leashState.slack = false;
                     o++;
                  }
               } else {
                  Vec3 vec33 = vec3.yRot(-gx);
                  EntityRenderState.LeashState leashState2 = reusedState.leashStates.getFirst();
                  leashState2.offset = vec33;
                  leashState2.start = entity.getPosition(partialTick).add(vec33);
                  leashState2.end = g.getRopeHoldPosition(partialTick);
                  leashState2.startBlockLight = i;
                  leashState2.endBlockLight = j;
                  leashState2.startSkyLight = k;
                  leashState2.endSkyLight = l;
                  break label72;
               }
            }
         }

         reusedState.leashStates = null;
      }

      reusedState.displayFireAnimation = entity.displayFireAnimation();
      Minecraft minecraft = Minecraft.getInstance();
      boolean bl3 = minecraft.shouldEntityAppearGlowing(entity);
      reusedState.outlineColor = bl3 ? ARGB.opaque(entity.getTeamColor()) : 0;
      reusedState.lightCoords = this.getPackedLightCoords(entity, partialTick);
   }

   protected void finalizeRenderState(T entity, S renderState) {
      Minecraft minecraft = Minecraft.getInstance();
      Level level = entity.level();
      this.extractShadow(renderState, minecraft, level);
   }

   private void extractShadow(S renderState, Minecraft minecraft, Level level) {
      renderState.shadowPieces.clear();
      if (minecraft.options.entityShadows().get() && !renderState.isInvisible) {
         float f = Math.min(this.getShadowRadius(renderState), 32.0F);
         renderState.shadowRadius = f;
         if (f > 0.0F) {
            double d = renderState.distanceToCameraSq;
            float g = (float)((1.0 - d / 256.0) * this.getShadowStrength(renderState));
            if (g > 0.0F) {
               int i = Mth.floor(renderState.x - f);
               int j = Mth.floor(renderState.x + f);
               int k = Mth.floor(renderState.z - f);
               int l = Mth.floor(renderState.z + f);
               float h = Math.min(g / 0.5F - 1.0F, f);
               int m = Mth.floor(renderState.y - h);
               int n = Mth.floor(renderState.y);
               BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

               for (int o = k; o <= l; o++) {
                  for (int p = i; p <= j; p++) {
                     mutableBlockPos.set(p, 0, o);
                     ChunkAccess chunkAccess = level.getChunk(mutableBlockPos);

                     for (int q = m; q <= n; q++) {
                        mutableBlockPos.setY(q);
                        this.extractShadowPiece(renderState, level, g, mutableBlockPos, chunkAccess);
                     }
                  }
               }
            }
         }
      } else {
         renderState.shadowRadius = 0.0F;
      }
   }

   private void extractShadowPiece(S renderState, Level level, float shadowStrength, BlockPos.MutableBlockPos pos, ChunkAccess chunk) {
      float f = shadowStrength - (float)(renderState.y - pos.getY()) * 0.5F;
      BlockPos blockPos = pos.below();
      BlockState blockState = chunk.getBlockState(blockPos);
      if (blockState.getRenderShape() != RenderShape.INVISIBLE) {
         int i = level.getMaxLocalRawBrightness(pos);
         if (i > 3) {
            if (blockState.isCollisionShapeFullBlock(chunk, blockPos)) {
               VoxelShape voxelShape = blockState.getShape(chunk, blockPos);
               if (!voxelShape.isEmpty()) {
                  float g = Mth.clamp(f * 0.5F * LightTexture.getBrightness(level.dimensionType(), i), 0.0F, 1.0F);
                  float h = (float)(pos.getX() - renderState.x);
                  float j = (float)(pos.getY() - renderState.y);
                  float k = (float)(pos.getZ() - renderState.z);
                  renderState.shadowPieces.add(new EntityRenderState.ShadowPiece(h, j, k, voxelShape, g));
               }
            }
         }
      }
   }

   @Nullable
   private static Entity getServerSideEntity(Entity entity) {
      IntegratedServer integratedServer = Minecraft.getInstance().getSingleplayerServer();
      if (integratedServer != null) {
         ServerLevel serverLevel = integratedServer.getLevel(entity.level().dimension());
         if (serverLevel != null) {
            return serverLevel.getEntity(entity.getId());
         }
      }

      return null;
   }
}
