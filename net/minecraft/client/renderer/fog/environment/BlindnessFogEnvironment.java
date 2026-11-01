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
public class BlindnessFogEnvironment extends MobEffectFogEnvironment {
   @Override
   public Holder<MobEffect> getMobEffect() {
      return MobEffects.BLINDNESS;
   }

   @Override
   public void setupFog(FogData fogData, Camera camera, ClientLevel level, float renderDistance, DeltaTracker deltaTracker) {
      if (camera.entity() instanceof LivingEntity livingEntity) {
         MobEffectInstance mobEffectInstance = livingEntity.getEffect(this.getMobEffect());
         if (mobEffectInstance != null) {
            float f = mobEffectInstance.isInfiniteDuration() ? 5.0F : Mth.lerp(Math.min(1.0F, mobEffectInstance.getDuration() / 20.0F), renderDistance, 5.0F);
            fogData.environmentalStart = f * 0.25F;
            fogData.environmentalEnd = f;
            fogData.skyEnd = f * 0.8F;
            fogData.cloudEnd = f * 0.8F;
         }
      }
   }

   @Override
   public float getModifiedDarkness(LivingEntity entity, float darkness, float partialTick) {
      MobEffectInstance mobEffectInstance = entity.getEffect(this.getMobEffect());
      if (mobEffectInstance != null) {
         if (mobEffectInstance.endsWithin(19)) {
            darkness = Math.max(mobEffectInstance.getDuration() / 20.0F, darkness);
         } else {
            darkness = 1.0F;
         }
      }

      return darkness;
   }
}
