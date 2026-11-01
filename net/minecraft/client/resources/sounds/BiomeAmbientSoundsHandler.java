package net.minecraft.client.resources.sounds;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.AmbientAdditionsSettings;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BiomeAmbientSoundsHandler implements AmbientSoundHandler {
   private static final int LOOP_SOUND_CROSS_FADE_TIME = 40;
   private static final float SKY_MOOD_RECOVERY_RATE = 0.001F;
   private final LocalPlayer player;
   private final SoundManager soundManager;
   private final BiomeManager biomeManager;
   private final RandomSource random;
   private final Object2ObjectArrayMap<Biome, BiomeAmbientSoundsHandler.LoopSoundInstance> loopSounds = new Object2ObjectArrayMap();
   private Optional<AmbientMoodSettings> moodSettings = Optional.empty();
   private Optional<AmbientAdditionsSettings> additionsSettings = Optional.empty();
   private float moodiness;
   @Nullable
   private Biome previousBiome;

   public BiomeAmbientSoundsHandler(LocalPlayer p_119639_, SoundManager p_119640_, BiomeManager p_119641_) {
      this.random = p_119639_.level().getRandom();
      this.player = p_119639_;
      this.soundManager = p_119640_;
      this.biomeManager = p_119641_;
   }

   public float getMoodiness() {
      return this.moodiness;
   }

   @Override
   public void tick() {
      this.loopSounds.values().removeIf(AbstractTickableSoundInstance::isStopped);
      Biome $$0 = this.biomeManager.getNoiseBiomeAtPosition(this.player.getX(), this.player.getY(), this.player.getZ()).value();
      if ($$0 != this.previousBiome) {
         this.previousBiome = $$0;
         this.moodSettings = $$0.getAmbientMood();
         this.additionsSettings = $$0.getAmbientAdditions();
         this.loopSounds.values().forEach(BiomeAmbientSoundsHandler.LoopSoundInstance::fadeOut);
         $$0.getAmbientLoop().ifPresent(p_263342_ -> this.loopSounds.compute($$0, (p_174924_, p_174925_) -> {
            if (p_174925_ == null) {
               p_174925_ = new BiomeAmbientSoundsHandler.LoopSoundInstance((SoundEvent)p_263342_.value());
               this.soundManager.play(p_174925_);
            }

            p_174925_.fadeIn();
            return p_174925_;
         }));
      }

      this.additionsSettings.ifPresent(p_119648_ -> {
         if (this.random.nextDouble() < p_119648_.getTickChance()) {
            this.soundManager.play(SimpleSoundInstance.forAmbientAddition(p_119648_.getSoundEvent().value()));
         }
      });
      this.moodSettings
         .ifPresent(
            p_274718_ -> {
               Level $$1 = this.player.level();
               int $$2 = p_274718_.getBlockSearchExtent() * 2 + 1;
               BlockPos $$3 = BlockPos.containing(
                  this.player.getX() + this.random.nextInt($$2) - p_274718_.getBlockSearchExtent(),
                  this.player.getEyeY() + this.random.nextInt($$2) - p_274718_.getBlockSearchExtent(),
                  this.player.getZ() + this.random.nextInt($$2) - p_274718_.getBlockSearchExtent()
               );
               int $$4 = $$1.getBrightness(LightLayer.SKY, $$3);
               if ($$4 > 0) {
                  this.moodiness = this.moodiness - (float)$$4 / $$1.getMaxLightLevel() * 0.001F;
               } else {
                  this.moodiness = this.moodiness - (float)($$1.getBrightness(LightLayer.BLOCK, $$3) - 1) / p_274718_.getTickDelay();
               }

               if (this.moodiness >= 1.0F) {
                  double $$5 = $$3.getX() + 0.5;
                  double $$6 = $$3.getY() + 0.5;
                  double $$7 = $$3.getZ() + 0.5;
                  double $$8 = $$5 - this.player.getX();
                  double $$9 = $$6 - this.player.getEyeY();
                  double $$10 = $$7 - this.player.getZ();
                  double $$11 = Math.sqrt($$8 * $$8 + $$9 * $$9 + $$10 * $$10);
                  double $$12 = $$11 + p_274718_.getSoundPositionOffset();
                  SimpleSoundInstance $$13 = SimpleSoundInstance.forAmbientMood(
                     p_274718_.getSoundEvent().value(),
                     this.random,
                     this.player.getX() + $$8 / $$11 * $$12,
                     this.player.getEyeY() + $$9 / $$11 * $$12,
                     this.player.getZ() + $$10 / $$11 * $$12
                  );
                  this.soundManager.play($$13);
                  this.moodiness = 0.0F;
               } else {
                  this.moodiness = Math.max(this.moodiness, 0.0F);
               }
            }
         );
   }

   @OnlyIn(Dist.CLIENT)
   public static class LoopSoundInstance extends AbstractTickableSoundInstance {
      private int fadeDirection;
      private int fade;

      public LoopSoundInstance(SoundEvent p_119658_) {
         super(p_119658_, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
         this.looping = true;
         this.delay = 0;
         this.volume = 1.0F;
         this.relative = true;
      }

      @Override
      public void tick() {
         if (this.fade < 0) {
            this.stop();
         }

         this.fade = this.fade + this.fadeDirection;
         this.volume = Mth.clamp(this.fade / 40.0F, 0.0F, 1.0F);
      }

      public void fadeOut() {
         this.fade = Math.min(this.fade, 40);
         this.fadeDirection = -1;
      }

      public void fadeIn() {
         this.fade = Math.max(0, this.fade);
         this.fadeDirection = 1;
      }
   }
}
