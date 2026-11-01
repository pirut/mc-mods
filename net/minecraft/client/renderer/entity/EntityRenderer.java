package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.renderstate.RenderStateExtensions;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class EntityRenderer<T extends Entity, S extends EntityRenderState> {
   private static final float SHADOW_POWER_FALLOFF_Y = 0.5F;
   private static final float MAX_SHADOW_RADIUS = 32.0F;
   public static final float NAMETAG_SCALE = 0.025F;
   protected final EntityRenderDispatcher entityRenderDispatcher;
   private final Font font;
   protected float shadowRadius;
   protected float shadowStrength = 1.0F;

   protected EntityRenderer(EntityRendererProvider.Context p_174008_) {
      this.entityRenderDispatcher = p_174008_.getEntityRenderDispatcher();
      this.font = p_174008_.getFont();
   }

   public final int getPackedLightCoords(T p_114506_, float p_114507_) {
      BlockPos blockpos = BlockPos.containing(p_114506_.getLightProbePosition(p_114507_));
      return LightTexture.pack(this.getBlockLightLevel(p_114506_, blockpos), this.getSkyLightLevel(p_114506_, blockpos));
   }

   protected int getSkyLightLevel(T p_114509_, BlockPos p_114510_) {
      return p_114509_.level().getBrightness(LightLayer.SKY, p_114510_);
   }

   protected int getBlockLightLevel(T p_114496_, BlockPos p_114497_) {
      return p_114496_.isOnFire() ? 15 : p_114496_.level().getBrightness(LightLayer.BLOCK, p_114497_);
   }

   public boolean shouldRender(T p_114491_, Frustum p_114492_, double p_114493_, double p_114494_, double p_114495_) {
      if (!p_114491_.shouldRender(p_114493_, p_114494_, p_114495_)) {
         return false;
      } else if (!this.affectedByCulling(p_114491_)) {
         return true;
      } else {
         AABB aabb = this.getBoundingBoxForCulling(p_114491_).inflate(0.5);
         if (aabb.hasNaN() || aabb.getSize() == 0.0) {
            aabb = new AABB(
               p_114491_.getX() - 2.0, p_114491_.getY() - 2.0, p_114491_.getZ() - 2.0, p_114491_.getX() + 2.0, p_114491_.getY() + 2.0, p_114491_.getZ() + 2.0
            );
         }

         if (p_114492_.isVisible(aabb)) {
            return true;
         } else {
            if (p_114491_ instanceof Leashable leashable) {
               Entity entity = leashable.getLeashHolder();
               if (entity != null) {
                  AABB aabb1 = this.entityRenderDispatcher.getRenderer(entity).getBoundingBoxForCulling(entity);
                  return p_114492_.isVisible(aabb1) || p_114492_.isVisible(aabb.minmax(aabb1));
               }
            }

            return false;
         }
      }
   }

   protected AABB getBoundingBoxForCulling(T p_361260_) {
      return p_361260_.getBoundingBox();
   }

   protected boolean affectedByCulling(T p_365169_) {
      return true;
   }

   public Vec3 getRenderOffset(S p_364311_) {
      return p_364311_.passengerOffset != null ? p_364311_.passengerOffset : Vec3.ZERO;
   }

   public void submit(S p_433712_, PoseStack p_434897_, SubmitNodeCollector p_433775_, CameraRenderState p_451076_) {
      if (p_433712_.leashStates != null) {
         for (EntityRenderState.LeashState entityrenderstate$leashstate : p_433712_.leashStates) {
            p_433775_.submitLeash(p_434897_, entityrenderstate$leashstate);
         }
      }

      this.submitNameTag(p_433712_, p_434897_, p_433775_, p_451076_);
   }

   protected boolean shouldShowName(T p_114504_, double p_361299_) {
      return p_114504_.shouldShowName() || p_114504_.hasCustomName() && p_114504_ == this.entityRenderDispatcher.crosshairPickEntity;
   }

   public Font getFont() {
      return this.font;
   }

   protected void submitNameTag(S p_436016_, PoseStack p_433473_, SubmitNodeCollector p_434265_, CameraRenderState p_451278_) {
      if (p_436016_.nameTag != null) {
         RenderNameTagEvent.DoRender event = new RenderNameTagEvent.DoRender(
            p_436016_, p_436016_.nameTag, this, p_433473_, p_434265_, p_451278_, p_436016_.partialTick
         );
         if (!((RenderNameTagEvent.DoRender)NeoForge.EVENT_BUS.post(event)).isCanceled()) {
            p_434265_.submitNameTag(
               p_433473_,
               p_436016_.nameTagAttachment,
               0,
               p_436016_.nameTag,
               !p_436016_.isDiscrete,
               p_436016_.lightCoords,
               p_436016_.distanceToCameraSq,
               p_451278_
            );
         }
      }
   }

   @Nullable
   protected Component getNameTag(T p_364352_) {
      return p_364352_.getDisplayName();
   }

   protected float getShadowRadius(S p_365191_) {
      return this.shadowRadius;
   }

   protected float getShadowStrength(S p_383214_) {
      return this.shadowStrength;
   }

   public abstract S createRenderState();

   public final S createRenderState(T p_361382_, float p_360885_) {
      S s = this.createRenderState();
      this.extractRenderState(p_361382_, s, p_360885_);
      this.finalizeRenderState(p_361382_, s);
      RenderStateExtensions.onUpdateEntityRenderState(this, p_361382_, s);
      return s;
   }

   public void extractRenderState(T p_362104_, S p_361028_, float p_362204_) {
      p_361028_.entityType = p_362104_.getType();
      p_361028_.x = Mth.lerp((double)p_362204_, p_362104_.xOld, p_362104_.getX());
      p_361028_.y = Mth.lerp((double)p_362204_, p_362104_.yOld, p_362104_.getY());
      p_361028_.z = Mth.lerp((double)p_362204_, p_362104_.zOld, p_362104_.getZ());
      p_361028_.isInvisible = p_362104_.isInvisible();
      p_361028_.partialTick = p_362204_;
      p_361028_.ageInTicks = p_362104_.tickCount + p_362204_;
      p_361028_.boundingBoxWidth = p_362104_.getBbWidth();
      p_361028_.boundingBoxHeight = p_362104_.getBbHeight();
      p_361028_.eyeHeight = p_362104_.getEyeHeight();
      if (p_362104_.isPassenger()
         && p_362104_.getVehicle() instanceof AbstractMinecart abstractminecart
         && abstractminecart.getBehavior() instanceof NewMinecartBehavior newminecartbehavior
         && newminecartbehavior.cartHasPosRotLerp()) {
         double d2 = Mth.lerp((double)p_362204_, abstractminecart.xOld, abstractminecart.getX());
         double d0 = Mth.lerp((double)p_362204_, abstractminecart.yOld, abstractminecart.getY());
         double d1 = Mth.lerp((double)p_362204_, abstractminecart.zOld, abstractminecart.getZ());
         p_361028_.passengerOffset = newminecartbehavior.getCartLerpPosition(p_362204_).subtract(new Vec3(d2, d0, d1));
      } else {
         p_361028_.passengerOffset = null;
      }

      if (this.entityRenderDispatcher.camera != null) {
         p_361028_.distanceToCameraSq = this.entityRenderDispatcher.distanceToSqr(p_362104_);
         RenderNameTagEvent.CanRender event = new RenderNameTagEvent.CanRender(p_362104_, p_361028_, this.getNameTag(p_362104_), this, p_362204_);
         NeoForge.EVENT_BUS.post(event);
         boolean flag1 = event.canRender().isTrue()
            || event.canRender().isDefault() && p_361028_.distanceToCameraSq < 4096.0 && this.shouldShowName(p_362104_, p_361028_.distanceToCameraSq);
         if (flag1) {
            p_361028_.nameTag = event.getContent();
            p_361028_.nameTagAttachment = p_362104_.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, p_362104_.getYRot(p_362204_));
         } else {
            p_361028_.nameTag = null;
         }
      }

      label73: {
         p_361028_.isDiscrete = p_362104_.isDiscrete();
         Level level = p_362104_.level();
         if (p_362104_ instanceof Leashable leashable) {
            Entity $$12 = leashable.getLeashHolder();
            if ($$12 instanceof Entity) {
               float f = p_362104_.getPreciseBodyRotation(p_362204_) * (float) (Math.PI / 180.0);
               Vec3 vec31 = leashable.getLeashOffset(p_362204_);
               BlockPos blockpos = BlockPos.containing(p_362104_.getEyePosition(p_362204_));
               BlockPos blockpos1 = BlockPos.containing($$12.getEyePosition(p_362204_));
               int i = this.getBlockLightLevel(p_362104_, blockpos);
               int j = this.entityRenderDispatcher.getRenderer($$12).getBlockLightLevel($$12, blockpos1);
               int k = level.getBrightness(LightLayer.SKY, blockpos);
               int l = level.getBrightness(LightLayer.SKY, blockpos1);
               boolean flag = $$12.supportQuadLeashAsHolder() && leashable.supportQuadLeash();
               int i1 = flag ? 4 : 1;
               if (p_361028_.leashStates == null || p_361028_.leashStates.size() != i1) {
                  p_361028_.leashStates = new ArrayList<>(i1);

                  for (int j1 = 0; j1 < i1; j1++) {
                     p_361028_.leashStates.add(new EntityRenderState.LeashState());
                  }
               }

               if (flag) {
                  float f1 = $$12.getPreciseBodyRotation(p_362204_) * (float) (Math.PI / 180.0);
                  Vec3 vec3 = $$12.getPosition(p_362204_);
                  Vec3[] avec3 = leashable.getQuadLeashOffsets();
                  Vec3[] avec31 = $$12.getQuadLeashHolderOffsets();
                  int k1 = 0;

                  while (true) {
                     if (k1 >= i1) {
                        break label73;
                     }

                     EntityRenderState.LeashState entityrenderstate$leashstate = p_361028_.leashStates.get(k1);
                     entityrenderstate$leashstate.offset = avec3[k1].yRot(-f);
                     entityrenderstate$leashstate.start = p_362104_.getPosition(p_362204_).add(entityrenderstate$leashstate.offset);
                     entityrenderstate$leashstate.end = vec3.add(avec31[k1].yRot(-f1));
                     entityrenderstate$leashstate.startBlockLight = i;
                     entityrenderstate$leashstate.endBlockLight = j;
                     entityrenderstate$leashstate.startSkyLight = k;
                     entityrenderstate$leashstate.endSkyLight = l;
                     entityrenderstate$leashstate.slack = false;
                     k1++;
                  }
               } else {
                  Vec3 vec32 = vec31.yRot(-f);
                  EntityRenderState.LeashState entityrenderstate$leashstate1 = p_361028_.leashStates.getFirst();
                  entityrenderstate$leashstate1.offset = vec32;
                  entityrenderstate$leashstate1.start = p_362104_.getPosition(p_362204_).add(vec32);
                  entityrenderstate$leashstate1.end = $$12.getRopeHoldPosition(p_362204_);
                  entityrenderstate$leashstate1.startBlockLight = i;
                  entityrenderstate$leashstate1.endBlockLight = j;
                  entityrenderstate$leashstate1.startSkyLight = k;
                  entityrenderstate$leashstate1.endSkyLight = l;
                  break label73;
               }
            }
         }

         p_361028_.leashStates = null;
      }

      p_361028_.displayFireAnimation = p_362104_.displayFireAnimation();
      Minecraft minecraft = Minecraft.getInstance();
      boolean flag2 = minecraft.shouldEntityAppearGlowing(p_362104_);
      p_361028_.outlineColor = flag2 ? ARGB.opaque(p_362104_.getTeamColor()) : 0;
      p_361028_.lightCoords = this.getPackedLightCoords(p_362104_, p_362204_);
   }

   protected void finalizeRenderState(T p_436699_, S p_436746_) {
      Minecraft minecraft = Minecraft.getInstance();
      Level level = p_436699_.level();
      this.extractShadow(p_436746_, minecraft, level);
   }

   private void extractShadow(S p_435327_, Minecraft p_435665_, Level p_433187_) {
      p_435327_.shadowPieces.clear();
      if (p_435665_.options.entityShadows().get() && !p_435327_.isInvisible) {
         float f = Math.min(this.getShadowRadius(p_435327_), 32.0F);
         p_435327_.shadowRadius = f;
         if (f > 0.0F) {
            double d0 = p_435327_.distanceToCameraSq;
            float f1 = (float)((1.0 - d0 / 256.0) * this.getShadowStrength(p_435327_));
            if (f1 > 0.0F) {
               int i = Mth.floor(p_435327_.x - f);
               int j = Mth.floor(p_435327_.x + f);
               int k = Mth.floor(p_435327_.z - f);
               int l = Mth.floor(p_435327_.z + f);
               float f2 = Math.min(f1 / 0.5F - 1.0F, f);
               int i1 = Mth.floor(p_435327_.y - f2);
               int j1 = Mth.floor(p_435327_.y);
               BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

               for (int k1 = k; k1 <= l; k1++) {
                  for (int l1 = i; l1 <= j; l1++) {
                     blockpos$mutableblockpos.set(l1, 0, k1);
                     ChunkAccess chunkaccess = p_433187_.getChunk(blockpos$mutableblockpos);

                     for (int i2 = i1; i2 <= j1; i2++) {
                        blockpos$mutableblockpos.setY(i2);
                        this.extractShadowPiece(p_435327_, p_433187_, f1, blockpos$mutableblockpos, chunkaccess);
                     }
                  }
               }
            }
         }
      } else {
         p_435327_.shadowRadius = 0.0F;
      }
   }

   private void extractShadowPiece(S p_435298_, Level p_434853_, float p_434960_, BlockPos.MutableBlockPos p_433774_, ChunkAccess p_434777_) {
      float f = p_434960_ - (float)(p_435298_.y - p_433774_.getY()) * 0.5F;
      BlockPos blockpos = p_433774_.below();
      BlockState blockstate = p_434777_.getBlockState(blockpos);
      if (blockstate.getRenderShape() != RenderShape.INVISIBLE) {
         int i = p_434853_.getMaxLocalRawBrightness(p_433774_);
         if (i > 3 && blockstate.isCollisionShapeFullBlock(p_434777_, blockpos)) {
            VoxelShape voxelshape = blockstate.getShape(p_434777_, blockpos);
            if (!voxelshape.isEmpty()) {
               float f1 = Mth.clamp(f * 0.5F * LightTexture.getBrightness(p_434853_.dimensionType(), i), 0.0F, 1.0F);
               float f2 = (float)(p_433774_.getX() - p_435298_.x);
               float f3 = (float)(p_433774_.getY() - p_435298_.y);
               float f4 = (float)(p_433774_.getZ() - p_435298_.z);
               p_435298_.shadowPieces.add(new EntityRenderState.ShadowPiece(f2, f3, f4, voxelshape, f1));
            }
         }
      }
   }

   @Nullable
   private static Entity getServerSideEntity(Entity p_412597_) {
      IntegratedServer integratedserver = Minecraft.getInstance().getSingleplayerServer();
      if (integratedserver != null) {
         ServerLevel serverlevel = integratedserver.getLevel(p_412597_.level().dimension());
         if (serverlevel != null) {
            return serverlevel.getEntity(p_412597_.getId());
         }
      }

      return null;
   }
}
