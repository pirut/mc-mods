package net.minecraft.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

@Environment(EnvType.CLIENT)
public class EndFlashState {
   public static final int SOUND_DELAY_IN_TICKS = 30;
   private static final int FLASH_INTERVAL_IN_TICKS = 600;
   private static final int MAX_FLASH_OFFSET_IN_TICKS = 200;
   private static final int MIN_FLASH_DURATION_IN_TICKS = 100;
   private static final int MAX_FLASH_DURATION_IN_TICKS = 380;
   private long flashSeed;
   private int offset;
   private int duration;
   private float intensity;
   private float oldIntensity;
   private float xAngle;
   private float yAngle;

   public void tick(long gametime) {
      this.calculateFlashParameters(gametime);
      this.oldIntensity = this.intensity;
      this.intensity = this.calculateIntensity(gametime);
   }

   private void calculateFlashParameters(long gametime) {
      long l = gametime / 600L;
      if (l != this.flashSeed) {
         RandomSource randomSource = RandomSource.create(l);
         randomSource.nextFloat();
         this.offset = Mth.randomBetweenInclusive(randomSource, 0, 200);
         this.duration = Mth.randomBetweenInclusive(randomSource, 100, Math.min(380, 600 - this.offset));
         this.xAngle = Mth.randomBetween(randomSource, -60.0F, 10.0F);
         this.yAngle = Mth.randomBetween(randomSource, -180.0F, 180.0F);
         this.flashSeed = l;
      }
   }

   private float calculateIntensity(long gametime) {
      long l = gametime % 600L;
      return l >= this.offset && l <= this.offset + this.duration ? Mth.sin((float)(l - this.offset) * (float) Math.PI / this.duration) : 0.0F;
   }

   public float getXAngle() {
      return this.xAngle;
   }

   public float getYAngle() {
      return this.yAngle;
   }

   public float getIntensity(float partialTick) {
      return Mth.lerp(partialTick, this.oldIntensity, this.intensity);
   }

   public boolean flashStartedThisTick() {
      return this.intensity > 0.0F && this.oldIntensity <= 0.0F;
   }
}
