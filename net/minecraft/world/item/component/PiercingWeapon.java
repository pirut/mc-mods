package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;

public record PiercingWeapon(boolean dealsKnockback, boolean dismounts, Optional<Holder<SoundEvent>> sound, Optional<Holder<SoundEvent>> hitSound) {
   public static final Codec<PiercingWeapon> CODEC = RecordCodecBuilder.create(
      p_477902_ -> p_477902_.group(
            Codec.BOOL.optionalFieldOf("deals_knockback", true).forGetter(PiercingWeapon::dealsKnockback),
            Codec.BOOL.optionalFieldOf("dismounts", false).forGetter(PiercingWeapon::dismounts),
            SoundEvent.CODEC.optionalFieldOf("sound").forGetter(PiercingWeapon::sound),
            SoundEvent.CODEC.optionalFieldOf("hit_sound").forGetter(PiercingWeapon::hitSound)
         )
         .apply(p_477902_, PiercingWeapon::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, PiercingWeapon> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL,
      PiercingWeapon::dealsKnockback,
      ByteBufCodecs.BOOL,
      PiercingWeapon::dismounts,
      SoundEvent.STREAM_CODEC.apply(ByteBufCodecs::optional),
      PiercingWeapon::sound,
      SoundEvent.STREAM_CODEC.apply(ByteBufCodecs::optional),
      PiercingWeapon::hitSound,
      PiercingWeapon::new
   );

   public void makeSound(Entity p_455182_) {
      this.sound
         .ifPresent(
            p_454914_ -> p_455182_.level()
               .playSound(
                  p_455182_, p_455182_.getX(), p_455182_.getY(), p_455182_.getZ(), (Holder<SoundEvent>)p_454914_, p_455182_.getSoundSource(), 1.0F, 1.0F
               )
         );
   }

   public void makeHitSound(Entity p_456040_) {
      this.hitSound
         .ifPresent(
            p_454940_ -> p_456040_.level()
               .playSound(null, p_456040_.getX(), p_456040_.getY(), p_456040_.getZ(), (Holder<SoundEvent>)p_454940_, p_456040_.getSoundSource(), 1.0F, 1.0F)
         );
   }

   public static boolean canHitEntity(Entity p_455393_, Entity p_455409_) {
      if (p_455409_.isInvulnerable() || !p_455409_.isAlive()) {
         return false;
      } else if (p_455409_ instanceof Interaction) {
         return true;
      } else if (!p_455409_.canBeHitByProjectile()) {
         return false;
      } else {
         return p_455409_ instanceof Player player && p_455393_ instanceof Player player1 && !player1.canHarmPlayer(player)
            ? false
            : !p_455393_.isPassengerOfSameVehicle(p_455409_);
      }
   }

   public void attack(LivingEntity p_457703_, EquipmentSlot p_455828_) {
      float f = (float)p_457703_.getAttributeValue(Attributes.ATTACK_DAMAGE);
      AttackRange attackrange = p_457703_.entityAttackRange();
      boolean flag = false;

      for (EntityHitResult entityhitresult : (Collection)ProjectileUtil.getHitEntitiesAlong(
            p_457703_, attackrange, p_455447_ -> canHitEntity(p_457703_, p_455447_), ClipContext.Block.COLLIDER
         )
         .map(p_477900_ -> List.of(), p_477901_ -> p_477901_)) {
         flag |= p_457703_.stabAttack(p_455828_, entityhitresult.getEntity(), f, true, this.dealsKnockback, this.dismounts);
      }

      p_457703_.onAttack();
      p_457703_.lungeForwardMaybe();
      if (flag) {
         this.makeHitSound(p_457703_);
      }

      this.makeSound(p_457703_);
      p_457703_.swing(InteractionHand.MAIN_HAND, false);
   }
}
