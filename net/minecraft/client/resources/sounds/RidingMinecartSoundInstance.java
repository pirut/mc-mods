package net.minecraft.client.resources.sounds;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RidingMinecartSoundInstance extends RidingEntitySoundInstance {
   private final Player player;
   private final AbstractMinecart minecart;
   private final boolean underwaterSound;

   public RidingMinecartSoundInstance(
      Player p_174940_, AbstractMinecart p_481943_, boolean p_174942_, SoundEvent p_457856_, float p_457544_, float p_458274_, float p_457636_
   ) {
      super(p_174940_, p_481943_, p_174942_, p_457856_, SoundSource.NEUTRAL, p_457544_, p_458274_, p_457636_);
      this.player = p_174940_;
      this.minecart = p_481943_;
      this.underwaterSound = p_174942_;
   }

   @Override
   protected boolean shouldNotPlayUnderwaterSound() {
      return this.underwaterSound != this.player.isUnderWater();
   }

   @Override
   protected float getEntitySpeed() {
      return (float)this.minecart.getDeltaMovement().horizontalDistance();
   }

   @Override
   protected boolean shoudlPlaySound() {
      return this.minecart.isOnRails() || !(this.minecart.getBehavior() instanceof NewMinecartBehavior);
   }
}
