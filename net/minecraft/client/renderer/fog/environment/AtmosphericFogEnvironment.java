package net.minecraft.client.renderer.fog.environment;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.PanoramicScreenshotParameters;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class AtmosphericFogEnvironment extends FogEnvironment {
   private static final int MIN_RAIN_FOG_SKY_LIGHT = 8;
   private static final float RAIN_FOG_START_OFFSET = -160.0F;
   private static final float RAIN_FOG_END_OFFSET = -256.0F;
   private float rainFogMultiplier;

   @Override
   public int getBaseColor(ClientLevel level, Camera camera, int renderDistance, float partialTick) {
      int i = camera.attributeProbe().getValue(EnvironmentAttributes.FOG_COLOR, partialTick);
      if (renderDistance >= 4) {
         float f = camera.attributeProbe().getValue(EnvironmentAttributes.SUN_ANGLE, partialTick) * (float) (Math.PI / 180.0);
         float g = Mth.sin(f) > 0.0F ? -1.0F : 1.0F;
         PanoramicScreenshotParameters panoramicScreenshotParameters = Minecraft.getInstance().gameRenderer.getPanoramicScreenshotParameters();
         Vector3fc vector3fc = panoramicScreenshotParameters != null ? panoramicScreenshotParameters.forwardVector() : camera.forwardVector();
         float h = vector3fc.dot(g, 0.0F, 0.0F);
         if (h > 0.0F) {
            int j = camera.attributeProbe().getValue(EnvironmentAttributes.SUNRISE_SUNSET_COLOR, partialTick);
            float k = ARGB.alphaFloat(j);
            if (k > 0.0F) {
               i = ARGB.srgbLerp(h * k, i, ARGB.opaque(j));
            }
         }
      }

      int l = camera.attributeProbe().getValue(EnvironmentAttributes.SKY_COLOR, partialTick);
      l = applyWeatherDarken(l, level.getRainLevel(partialTick), level.getThunderLevel(partialTick));
      float g = Math.min(camera.attributeProbe().getValue(EnvironmentAttributes.SKY_FOG_END_DISTANCE, partialTick) / 16.0F, (float)renderDistance);
      float m = Mth.clampedLerp(g / 32.0F, 0.25F, 1.0F);
      m = 1.0F - (float)Math.pow(m, 0.25);
      return ARGB.srgbLerp(m, i, l);
   }

   private static int applyWeatherDarken(int skyColor, float rainLevel, float thunderLevel) {
      if (rainLevel > 0.0F) {
         float f = 1.0F - rainLevel * 0.5F;
         float g = 1.0F - rainLevel * 0.4F;
         skyColor = ARGB.scaleRGB(skyColor, f, f, g);
      }

      if (thunderLevel > 0.0F) {
         skyColor = ARGB.scaleRGB(skyColor, 1.0F - thunderLevel * 0.5F);
      }

      return skyColor;
   }

   @Override
   public void setupFog(FogData fogData, Camera camera, ClientLevel level, float renderDistance, DeltaTracker deltaTracker) {
      this.updateRainFogState(camera, level, deltaTracker);
      float f = deltaTracker.getGameTimeDeltaPartialTick(false);
      fogData.environmentalStart = camera.attributeProbe().getValue(EnvironmentAttributes.FOG_START_DISTANCE, f);
      fogData.environmentalEnd = camera.attributeProbe().getValue(EnvironmentAttributes.FOG_END_DISTANCE, f);
      fogData.environmentalStart = fogData.environmentalStart + -160.0F * this.rainFogMultiplier;
      float g = Math.min(96.0F, fogData.environmentalEnd);
      fogData.environmentalEnd = Math.max(g, fogData.environmentalEnd + -256.0F * this.rainFogMultiplier);
      fogData.skyEnd = Math.min(renderDistance, camera.attributeProbe().getValue(EnvironmentAttributes.SKY_FOG_END_DISTANCE, f));
      fogData.cloudEnd = Math.min(
         (float)(Minecraft.getInstance().options.cloudRange().get() * 16), camera.attributeProbe().getValue(EnvironmentAttributes.CLOUD_FOG_END_DISTANCE, f)
      );
      if (Minecraft.getInstance().gui.getBossOverlay().shouldCreateWorldFog()) {
         fogData.environmentalStart = Math.min(fogData.environmentalStart, 10.0F);
         fogData.environmentalEnd = Math.min(fogData.environmentalEnd, 96.0F);
         fogData.skyEnd = fogData.environmentalEnd;
         fogData.cloudEnd = fogData.environmentalEnd;
      }
   }

   private void updateRainFogState(Camera camera, ClientLevel level, DeltaTracker deltaTracker) {
      BlockPos blockPos = camera.blockPosition();
      Biome biome = level.getBiome(blockPos).value();
      float f = deltaTracker.getGameTimeDeltaTicks();
      float g = deltaTracker.getGameTimeDeltaPartialTick(false);
      boolean bl = biome.hasPrecipitation();
      float h = Mth.clamp((level.getLightEngine().getLayerListener(LightLayer.SKY).getLightValue(blockPos) - 8.0F) / 7.0F, 0.0F, 1.0F);
      float i = level.getRainLevel(g) * h * (bl ? 1.0F : 0.5F);
      this.rainFogMultiplier = this.rainFogMultiplier + (i - this.rainFogMultiplier) * f * 0.2F;
   }

   @Override
   public boolean isApplicable(@Nullable FogType fogType, Entity entity) {
      return fogType == FogType.ATMOSPHERIC;
   }
}
