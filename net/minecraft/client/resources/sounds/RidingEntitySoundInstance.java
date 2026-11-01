package net.minecraft.client.resources.sounds;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RidingEntitySoundInstance extends AbstractTickableSoundInstance {
   private final Player player;
   private final Entity entity;
   private final boolean underwaterSound;
   private final float volumeMin;
   private final float volumeMax;
   private final float volumeAmplifier;

   public RidingEntitySoundInstance(
      Player p_458271_, Entity p_457701_, boolean p_457663_, SoundEvent p_457903_, SoundSource p_457963_, float p_458140_, float p_458022_, float p_457542_
   ) {
      super(p_457903_, p_457963_, SoundInstance.createUnseededRandom());
      this.player = p_458271_;
      this.entity = p_457701_;
      this.underwaterSound = p_457663_;
      this.volumeMin = p_458140_;
      this.volumeMax = p_458022_;
      this.volumeAmplifier = p_457542_;
      this.attenuation = SoundInstance.Attenuation.NONE;
      this.looping = true;
      this.delay = 0;
      this.volume = p_458140_;
   }

   @Override
   public boolean canPlaySound() {
      return !this.entity.isSilent();
   }

   @Override
   public boolean canStartSilent() {
      return true;
   }

   protected boolean shouldNotPlayUnderwaterSound() {
      return this.underwaterSound != this.entity.isUnderWater();
   }

   protected float getEntitySpeed() {
      return (float)this.entity.getDeltaMovement().length();
   }

   protected boolean shoudlPlaySound() {
      return true;
   }

   @Override
   public void tick() {
      if (this.entity.isRemoved() || !this.player.isPassenger() || this.player.getVehicle() != this.entity) {
         this.stop();
      } else if (this.shouldNotPlayUnderwaterSound()) {
         this.volume = this.volumeMin;
      } else {
         float f = this.getEntitySpeed();
         if (f >= 0.01F && this.shoudlPlaySound()) {
            this.volume = this.volumeAmplifier * Mth.clampedLerp(f, this.volumeMin, this.volumeMax);
         } else {
            this.volume = this.volumeMin;
         }
      }
   }
}
