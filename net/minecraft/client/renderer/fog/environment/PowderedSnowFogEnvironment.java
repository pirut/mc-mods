package net.minecraft.client.renderer.fog.environment;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class PowderedSnowFogEnvironment extends FogEnvironment {
   private static final int COLOR = -6308916;

   @Override
   public int getBaseColor(ClientLevel level, Camera camera, int renderDistance, float partialTick) {
      return -6308916;
   }

   @Override
   public void setupFog(FogData fogData, Camera camera, ClientLevel level, float renderDistance, DeltaTracker deltaTracker) {
      if (camera.entity().isSpectator()) {
         fogData.environmentalStart = -8.0F;
         fogData.environmentalEnd = renderDistance * 0.5F;
      } else {
         fogData.environmentalStart = 0.0F;
         fogData.environmentalEnd = 2.0F;
      }

      fogData.skyEnd = fogData.environmentalEnd;
      fogData.cloudEnd = fogData.environmentalEnd;
   }

   @Override
   public boolean isApplicable(@Nullable FogType fogType, Entity entity) {
      return fogType == FogType.POWDER_SNOW;
   }
}
