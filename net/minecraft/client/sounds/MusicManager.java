package net.minecraft.client.sounds;

import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ClientHooks;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class MusicManager {
   private static final int STARTING_DELAY = 100;
   private final RandomSource random = RandomSource.create();
   private final Minecraft minecraft;
   @Nullable
   private SoundInstance currentMusic;
   private MusicManager.MusicFrequency gameMusicFrequency;
   private float currentGain = 1.0F;
   private int nextSongDelay = 100;
   private boolean toastShown = false;

   public MusicManager(Minecraft p_120182_) {
      this.minecraft = p_120182_;
      this.gameMusicFrequency = p_120182_.options.musicFrequency().get();
   }

   public void tick() {
      float f = this.minecraft.getMusicVolume();
      if (this.currentMusic != null && this.currentGain != f) {
         boolean flag = this.fadePlaying(f);
         if (!flag) {
            return;
         }
      }

      Music music = ClientHooks.selectMusic(this.minecraft.getSituationalMusic(), this.currentMusic);
      if (music == null) {
         if (this.currentMusic != null) {
            this.stopPlaying();
         }

         this.nextSongDelay = 0;
      } else {
         if (music == null) {
            this.nextSongDelay = Math.max(this.nextSongDelay, 100);
         } else {
            if (this.currentMusic != null) {
               if (canReplace(music, this.currentMusic)) {
                  this.minecraft.getSoundManager().stop(this.currentMusic);
                  this.nextSongDelay = Mth.nextInt(this.random, 0, music.minDelay() / 2);
               }

               if (!this.minecraft.getSoundManager().isActive(this.currentMusic)) {
                  this.currentMusic = null;
                  this.nextSongDelay = Math.min(this.nextSongDelay, this.gameMusicFrequency.getNextSongDelay(music, this.random));
               }
            }

            this.nextSongDelay = Math.min(this.nextSongDelay, this.gameMusicFrequency.getNextSongDelay(music, this.random));
            if (this.currentMusic == null && this.nextSongDelay-- <= 0) {
               this.startPlaying(music);
            }
         }
      }
   }

   private static boolean canReplace(Music p_457714_, SoundInstance p_457541_) {
      return p_457714_.replaceCurrentMusic() && !p_457714_.sound().value().location().equals(p_457541_.getIdentifier());
   }

   public void startPlaying(Music p_457881_) {
      SoundEvent soundevent = p_457881_.sound().value();
      this.currentMusic = SimpleSoundInstance.forMusic(soundevent);
      switch (this.minecraft.getSoundManager().play(this.currentMusic)) {
         case STARTED:
            this.minecraft.getToastManager().showNowPlayingToast();
            this.toastShown = true;
            break;
         case STARTED_SILENTLY:
            this.toastShown = false;
      }

      this.nextSongDelay = Integer.MAX_VALUE;
   }

   public void showNowPlayingToastIfNeeded() {
      if (!this.toastShown) {
         this.minecraft.getToastManager().showNowPlayingToast();
         this.toastShown = true;
      }
   }

   public void stopPlaying(Music p_278295_) {
      if (this.isPlayingMusic(p_278295_)) {
         this.stopPlaying();
      }
   }

   public void stopPlaying() {
      if (this.currentMusic != null) {
         this.minecraft.getSoundManager().stop(this.currentMusic);
         this.currentMusic = null;
         this.minecraft.getToastManager().hideNowPlayingToast();
      }

      this.nextSongDelay += 100;
   }

   private boolean fadePlaying(float p_383118_) {
      if (this.currentMusic == null) {
         return false;
      } else if (this.currentGain == p_383118_) {
         return true;
      } else {
         if (this.currentGain < p_383118_) {
            this.currentGain = this.currentGain + Mth.clamp(this.currentGain, 5.0E-4F, 0.005F);
            if (this.currentGain > p_383118_) {
               this.currentGain = p_383118_;
            }
         } else {
            this.currentGain = 0.03F * p_383118_ + 0.97F * this.currentGain;
            if (Math.abs(this.currentGain - p_383118_) < 1.0E-4F || this.currentGain < p_383118_) {
               this.currentGain = p_383118_;
            }
         }

         this.currentGain = Mth.clamp(this.currentGain, 0.0F, 1.0F);
         if (this.currentGain <= 1.0E-4F) {
            this.stopPlaying();
            return false;
         } else {
            this.minecraft.getSoundManager().updateCategoryVolume(SoundSource.MUSIC, this.currentGain);
            return true;
         }
      }
   }

   public boolean isPlayingMusic(Music p_120188_) {
      return this.currentMusic == null ? false : p_120188_.sound().value().location().equals(this.currentMusic.getIdentifier());
   }

   @Nullable
   public String getCurrentMusicTranslationKey() {
      if (this.currentMusic != null) {
         Sound sound = this.currentMusic.getSound();
         if (sound != null) {
            return sound.getLocation().toShortLanguageKey();
         }
      }

      return null;
   }

   public void setMinutesBetweenSongs(MusicManager.MusicFrequency p_426207_) {
      this.gameMusicFrequency = p_426207_;
      this.nextSongDelay = this.gameMusicFrequency.getNextSongDelay(this.minecraft.getSituationalMusic(), this.random);
   }

   @OnlyIn(Dist.CLIENT)
   public static enum MusicFrequency implements StringRepresentable {
      DEFAULT("DEFAULT", "options.music_frequency.default", 20),
      FREQUENT("FREQUENT", "options.music_frequency.frequent", 10),
      CONSTANT("CONSTANT", "options.music_frequency.constant", 0);

      public static final Codec<MusicManager.MusicFrequency> CODEC = StringRepresentable.fromEnum(MusicManager.MusicFrequency::values);
      private final String name;
      private final int maxFrequency;
      private final Component caption;

      private MusicFrequency(String p_469440_, String p_469836_, int p_426299_) {
         this.name = p_469440_;
         this.maxFrequency = p_426299_ * 1200;
         this.caption = Component.translatable(p_469836_);
      }

      int getNextSongDelay(@Nullable Music p_425869_, RandomSource p_426254_) {
         if (p_425869_ == null) {
            return this.maxFrequency;
         } else if (this == CONSTANT) {
            return 100;
         } else {
            int i = Math.min(p_425869_.minDelay(), this.maxFrequency);
            int j = Math.min(p_425869_.maxDelay(), this.maxFrequency);
            return Mth.nextInt(p_426254_, i, j);
         }
      }

      public Component caption() {
         return this.caption;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }
}
