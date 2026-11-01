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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ClientHooks;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

@OnlyIn(Dist.CLIENT)
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
      GpuDevice gpudevice = RenderSystem.getDevice();
      this.regularBuffer = new MappableRingBuffer(() -> "Fog UBO", 130, FOG_UBO_SIZE);
      MemoryStack memorystack = MemoryStack.stackPush();

      try {
         ByteBuffer bytebuffer = memorystack.malloc(FOG_UBO_SIZE);
         this.updateBuffer(
            bytebuffer, 0, new Vector4f(0.0F), Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE
         );
         this.emptyBuffer = gpudevice.createBuffer(() -> "Empty fog", 128, bytebuffer.flip());
      } catch (Throwable var6) {
         if (memorystack != null) {
            try {
               memorystack.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }
         }

         throw var6;
      }

      if (memorystack != null) {
         memorystack.close();
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

   public GpuBufferSlice getBuffer(FogRenderer.FogMode p_423591_) {
      if (!fogEnabled) {
         return this.emptyBuffer.slice(0L, FOG_UBO_SIZE);
      } else {
         return switch (p_423591_) {
            case NONE -> this.emptyBuffer.slice(0L, FOG_UBO_SIZE);
            case WORLD -> this.regularBuffer.currentBuffer().slice(0L, FOG_UBO_SIZE);
         };
      }
   }

   private Vector4f computeFogColor(Camera p_423439_, float p_423466_, ClientLevel p_423475_, int p_423484_, float p_423652_) {
      FogType fogtype = this.getFogType(p_423439_);
      Entity entity = p_423439_.entity();
      FogEnvironment fogenvironment = null;
      FogEnvironment fogenvironment1 = null;

      for (FogEnvironment fogenvironment2 : FOG_ENVIRONMENTS) {
         if (fogenvironment2.isApplicable(fogtype, entity)) {
            if (fogenvironment == null && fogenvironment2.providesColor()) {
               fogenvironment = fogenvironment2;
            }

            if (fogenvironment1 == null && fogenvironment2.modifiesDarkness()) {
               fogenvironment1 = fogenvironment2;
            }
         }
      }

      if (fogenvironment == null) {
         throw new IllegalStateException("No color source environment found");
      } else {
         int i = fogenvironment.getBaseColor(p_423475_, p_423439_, p_423484_, p_423466_);
         float f4 = p_423475_.getLevelData().voidDarknessOnsetRange();
         float f = Mth.clamp((f4 + p_423475_.getMinY() - (float)p_423439_.position().y) / f4, 0.0F, 1.0F);
         if (fogenvironment1 != null) {
            LivingEntity livingentity = (LivingEntity)entity;
            f = fogenvironment1.getModifiedDarkness(livingentity, f, p_423466_);
         }

         float f5 = ARGB.redFloat(i);
         float f1 = ARGB.greenFloat(i);
         float f2 = ARGB.blueFloat(i);
         if (f > 0.0F && fogtype != FogType.LAVA && fogtype != FogType.POWDER_SNOW) {
            float f3 = Mth.square(1.0F - f);
            f5 *= f3;
            f1 *= f3;
            f2 *= f3;
         }

         if (p_423652_ > 0.0F) {
            f5 = Mth.lerp(p_423652_, f5, f5 * 0.7F);
            f1 = Mth.lerp(p_423652_, f1, f1 * 0.6F);
            f2 = Mth.lerp(p_423652_, f2, f2 * 0.6F);
         }

         float f6;
         if (fogtype == FogType.WATER) {
            if (entity instanceof LocalPlayer) {
               f6 = ((LocalPlayer)entity).getWaterVision();
            } else {
               f6 = 1.0F;
            }
         } else if (entity instanceof LivingEntity livingentity1
            && livingentity1.hasEffect(MobEffects.NIGHT_VISION)
            && !livingentity1.hasEffect(MobEffects.DARKNESS)) {
            f6 = GameRenderer.getNightVisionScale(livingentity1, p_423466_);
         } else {
            f6 = 0.0F;
         }

         if (f5 != 0.0F && f1 != 0.0F && f2 != 0.0F) {
            float f7 = 1.0F / Math.max(f5, Math.max(f1, f2));
            f5 = Mth.lerp(f6, f5, f5 * f7);
            f1 = Mth.lerp(f6, f1, f1 * f7);
            f2 = Mth.lerp(f6, f2, f2 * f7);
         }

         return ClientHooks.getFogColor(p_423439_, p_423466_, p_423475_, p_423484_, p_423652_, f5, f1, f2);
      }
   }

   public static boolean toggleFog() {
      return fogEnabled = !fogEnabled;
   }

   public Vector4f setupFog(Camera p_423468_, int p_423600_, DeltaTracker p_423597_, float p_423506_, ClientLevel p_423641_) {
      float f = p_423597_.getGameTimeDeltaPartialTick(false);
      Vector4f vector4f = this.computeFogColor(p_423468_, f, p_423641_, p_423600_, p_423506_);
      float f1 = p_423600_ * 16;
      FogType fogtype = this.getFogType(p_423468_);
      Entity entity = p_423468_.entity();
      FogData fogdata = new FogData();
      FogEnvironment chosenEnvironment = null;

      for (FogEnvironment fogenvironment : FOG_ENVIRONMENTS) {
         if (fogenvironment.isApplicable(fogtype, entity)) {
            fogenvironment.setupFog(fogdata, p_423468_, p_423641_, f1, p_423597_);
            chosenEnvironment = fogenvironment;
            break;
         }
      }

      float f2 = Mth.clamp(f1 / 10.0F, 4.0F, 64.0F);
      fogdata.renderDistanceStart = f1 - f2;
      fogdata.renderDistanceEnd = f1;
      ClientHooks.onSetupFog(chosenEnvironment, fogtype, p_423468_, f, p_423600_, fogdata);

      try (GpuBuffer.MappedView gpubuffer$mappedview = RenderSystem.getDevice()
            .createCommandEncoder()
            .mapBuffer(this.regularBuffer.currentBuffer(), false, true)) {
         this.updateBuffer(
            gpubuffer$mappedview.data(),
            0,
            vector4f,
            fogdata.environmentalStart,
            fogdata.environmentalEnd,
            fogdata.renderDistanceStart,
            fogdata.renderDistanceEnd,
            fogdata.skyEnd,
            fogdata.cloudEnd
         );
      }

      return vector4f;
   }

   private FogType getFogType(Camera p_423451_) {
      FogType fogtype = p_423451_.getFluidInCamera();
      return fogtype == FogType.NONE ? FogType.ATMOSPHERIC : fogtype;
   }

   private void updateBuffer(
      ByteBuffer p_423489_,
      int p_423628_,
      Vector4f p_423543_,
      float p_423485_,
      float p_423650_,
      float p_423492_,
      float p_423500_,
      float p_423575_,
      float p_423452_
   ) {
      p_423489_.position(p_423628_);
      Std140Builder.intoBuffer(p_423489_)
         .putVec4(p_423543_)
         .putFloat(p_423485_)
         .putFloat(p_423650_)
         .putFloat(p_423492_)
         .putFloat(p_423500_)
         .putFloat(p_423575_)
         .putFloat(p_423452_);
   }

   @OnlyIn(Dist.CLIENT)
   public static enum FogMode {
      NONE,
      WORLD;
   }
}
