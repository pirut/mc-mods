package net.minecraft.client.renderer.fog.environment;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

@Environment(EnvType.CLIENT)
public class DarknessFogEnvironment extends MobEffectFogEnvironment {
   @Override
   public Holder<MobEffect> getMobEffect() {
      return MobEffects.DARKNESS;
   }

   @Override
   public void setupFog(FogData fogData, Camera camera, ClientLevel level, float renderDistance, DeltaTracker deltaTracker) {
      if (camera.entity() instanceof LivingEntity livingEntity) {
         MobEffectInstance mobEffectInstance = livingEntity.getEffect(this.getMobEffect());
         if (mobEffectInstance != null) {
            float f = Mth.lerp(mobEffectInstance.getBlendFactor(livingEntity, deltaTracker.getGameTimeDeltaPartialTick(false)), renderDistance, 15.0F);
            fogData.environmentalStart = f * 0.75F;
            fogData.environmentalEnd = f;
            fogData.skyEnd = f;
            fogData.cloudEnd = f;
         }
      }
   }

   @Override
   public float getModifiedDarkness(LivingEntity entity, float darkness, float partialTick) {
      MobEffectInstance mobEffectInstance = entity.getEffect(this.getMobEffect());
      return mobEffectInstance != null ? Math.max(mobEffectInstance.getBlendFactor(entity, partialTick), darkness) : darkness;
   }
}
