package net.minecraft.client.renderer.fog;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import net.minecraft.client.renderer.fog.environment.BlindnessFogEnvironment;
import net.minecraft.client.renderer.fog.environment.DarknessFogEnvironment;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.client.renderer.fog.environment.LavaFogEnvironment;
import net.minecraft.client.renderer.fog.environment.PowderedSnowFogEnvironment;
import net.minecraft.client.renderer.fog.environment.WaterFogEnvironment;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

@Environment(EnvType.CLIENT)
public class FogRenderer implements AutoCloseable {
   public static final int FOG_UBO_SIZE = new Std140SizeCalculator().putVec4().putFloat().putFloat().putFloat().putFloat().putFloat().putFloat().get();
   private static final List<FogEnvironment> FOG_ENVIRONMENTS = Lists.newArrayList(
      new FogEnvironment[]{
         new LavaFogEnvironment(),
         new PowderedSnowFogEnvironment(),
         new BlindnessFogEnvironment(),
         new DarknessFogEnvironment(),
         new WaterFogEnvironment(),
         new AtmosphericFogEnvironment()
      }
   );
   private static boolean fogEnabled = true;
   private final GpuBuffer emptyBuffer;
   private final MappableRingBuffer regularBuffer;

   public FogRenderer() {
      GpuDevice gpuDevice = RenderSystem.getDevice();
      this.regularBuffer = new MappableRingBuffer(() -> "Fog UBO", 130, FOG_UBO_SIZE);
      MemoryStack memoryStack = MemoryStack.stackPush();

      try {
         ByteBuffer byteBuffer = memoryStack.malloc(FOG_UBO_SIZE);
         this.updateBuffer(
            byteBuffer, 0, new Vector4f(0.0F), Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE
         );
         this.emptyBuffer = gpuDevice.createBuffer(() -> "Empty fog", 128, byteBuffer.flip());
      } catch (Throwable var6) {
         if (memoryStack != null) {
            try {
               memoryStack.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }
         }

         throw var6;
      }

      if (memoryStack != null) {
         memoryStack.close();
      }

      RenderSystem.setShaderFog(this.getBuffer(FogRenderer.FogMode.NONE));
   }

   @Override
   public void close() {
      this.emptyBuffer.close();
      this.regularBuffer.close();
   }

   public void endFrame() {
      this.regularBuffer.rotate();
   }

   public GpuBufferSlice getBuffer(FogRenderer.FogMode fogMode) {
      if (!fogEnabled) {
         return this.emptyBuffer.slice(0L, FOG_UBO_SIZE);
      } else {
         return switch (fogMode) {
            case NONE -> this.emptyBuffer.slice(0L, FOG_UBO_SIZE);
            case WORLD -> this.regularBuffer.currentBuffer().slice(0L, FOG_UBO_SIZE);
         };
      }
   }

   private Vector4f computeFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount) {
      FogType fogType = this.getFogType(camera);
      Entity entity = camera.entity();
      FogEnvironment fogEnvironment = null;
      FogEnvironment fogEnvironment2 = null;

      for (FogEnvironment fogEnvironment3 : FOG_ENVIRONMENTS) {
         if (fogEnvironment3.isApplicable(fogType, entity)) {
            if (fogEnvironment == null && fogEnvironment3.providesColor()) {
               fogEnvironment = fogEnvironment3;
            }

            if (fogEnvironment2 == null && fogEnvironment3.modifiesDarkness()) {
               fogEnvironment2 = fogEnvironment3;
            }
         }
      }

      if (fogEnvironment == null) {
         throw new IllegalStateException("No color source environment found");
      } else {
         int i = fogEnvironment.getBaseColor(level, camera, renderDistance, partialTick);
         float f = level.getLevelData().voidDarknessOnsetRange();
         float g = Mth.clamp((f + level.getMinY() - (float)camera.position().y) / f, 0.0F, 1.0F);
         if (fogEnvironment2 != null) {
            LivingEntity livingEntity = (LivingEntity)entity;
            g = fogEnvironment2.getModifiedDarkness(livingEntity, g, partialTick);
         }

         float h = ARGB.redFloat(i);
         float j = ARGB.greenFloat(i);
         float k = ARGB.blueFloat(i);
         if (g > 0.0F && fogType != FogType.LAVA && fogType != FogType.POWDER_SNOW) {
            float l = Mth.square(1.0F - g);
            h *= l;
            j *= l;
            k *= l;
         }

         if (darkenWorldAmount > 0.0F) {
            h = Mth.lerp(darkenWorldAmount, h, h * 0.7F);
            j = Mth.lerp(darkenWorldAmount, j, j * 0.6F);
            k = Mth.lerp(darkenWorldAmount, k, k * 0.6F);
         }

         float l;
         if (fogType == FogType.WATER) {
            if (entity instanceof LocalPlayer) {
               l = ((LocalPlayer)entity).getWaterVision();
            } else {
               l = 1.0F;
            }
         } else if (entity instanceof LivingEntity livingEntity2
            && livingEntity2.hasEffect(MobEffects.NIGHT_VISION)
            && !livingEntity2.hasEffect(MobEffects.DARKNESS)) {
            l = GameRenderer.getNightVisionScale(livingEntity2, partialTick);
         } else {
            l = 0.0F;
         }

         if (h != 0.0F && j != 0.0F && k != 0.0F) {
            float m = 1.0F / Math.max(h, Math.max(j, k));
            h = Mth.lerp(l, h, h * m);
            j = Mth.lerp(l, j, j * m);
            k = Mth.lerp(l, k, k * m);
         }

         return new Vector4f(h, j, k, 1.0F);
      }
   }

   public static boolean toggleFog() {
      return fogEnabled = !fogEnabled;
   }

   public Vector4f setupFog(Camera camera, int renderDistance, DeltaTracker deltaTracker, float darkenWorldAmount, ClientLevel level) {
      float f = deltaTracker.getGameTimeDeltaPartialTick(false);
      Vector4f vector4f = this.computeFogColor(camera, f, level, renderDistance, darkenWorldAmount);
      float g = renderDistance * 16;
      FogType fogType = this.getFogType(camera);
      Entity entity = camera.entity();
      FogData fogData = new FogData();

      for (FogEnvironment fogEnvironment : FOG_ENVIRONMENTS) {
         if (fogEnvironment.isApplicable(fogType, entity)) {
            fogEnvironment.setupFog(fogData, camera, level, g, deltaTracker);
            break;
         }
      }

      float h = Mth.clamp(g / 10.0F, 4.0F, 64.0F);
      fogData.renderDistanceStart = g - h;
      fogData.renderDistanceEnd = g;

      try (GpuBuffer.MappedView mappedView = RenderSystem.getDevice().createCommandEncoder().mapBuffer(this.regularBuffer.currentBuffer(), false, true)) {
         this.updateBuffer(
            mappedView.data(),
            0,
            vector4f,
            fogData.environmentalStart,
            fogData.environmentalEnd,
            fogData.renderDistanceStart,
            fogData.renderDistanceEnd,
            fogData.skyEnd,
            fogData.cloudEnd
         );
      }

      return vector4f;
   }

   private FogType getFogType(Camera camera) {
      FogType fogType = camera.getFluidInCamera();
      return fogType == FogType.NONE ? FogType.ATMOSPHERIC : fogType;
   }

   private void updateBuffer(
      ByteBuffer buffer,
      int position,
      Vector4f fogColor,
      float environmentalStart,
      float environmentalEnd,
      float renderDistanceStart,
      float renderDistanceEnd,
      float skyEnd,
      float cloudEnd
   ) {
      buffer.position(position);
      Std140Builder.intoBuffer(buffer)
         .putVec4(fogColor)
         .putFloat(environmentalStart)
         .putFloat(environmentalEnd)
         .putFloat(renderDistanceStart)
         .putFloat(renderDistanceEnd)
         .putFloat(skyEnd)
         .putFloat(cloudEnd);
   }

   @Environment(EnvType.CLIENT)
   public static enum FogMode {
      NONE,
      WORLD;
   }
}
