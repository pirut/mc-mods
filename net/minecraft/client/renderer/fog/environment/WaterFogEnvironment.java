package net.minecraft.client.renderer.fog.environment;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class WaterFogEnvironment extends FogEnvironment {
   @Override
   public void setupFog(FogData fogData, Camera camera, ClientLevel level, float renderDistance, DeltaTracker deltaTracker) {
      float f = deltaTracker.getGameTimeDeltaPartialTick(false);
      fogData.environmentalStart = camera.attributeProbe().getValue(EnvironmentAttributes.WATER_FOG_START_DISTANCE, f);
      fogData.environmentalEnd = camera.attributeProbe().getValue(EnvironmentAttributes.WATER_FOG_END_DISTANCE, f);
      if (camera.entity() instanceof LocalPlayer localPlayer) {
         fogData.environmentalEnd = fogData.environmentalEnd * Math.max(0.25F, localPlayer.getWaterVision());
      }

      fogData.skyEnd = fogData.environmentalEnd;
      fogData.cloudEnd = fogData.environmentalEnd;
   }

   @Override
   public boolean isApplicable(@Nullable FogType fogType, Entity entity) {
      return fogType == FogType.WATER;
   }

   @Override
   public int getBaseColor(ClientLevel level, Camera camera, int renderDistance, float partialTick) {
      return camera.attributeProbe().getValue(EnvironmentAttributes.WATER_FOG_COLOR, partialTick);
   }
}
