package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public record KineticWeapon(
   int contactCooldownTicks,
   int delayTicks,
   Optional<KineticWeapon.Condition> dismountConditions,
   Optional<KineticWeapon.Condition> knockbackConditions,
   Optional<KineticWeapon.Condition> damageConditions,
   float forwardMovement,
   float damageMultiplier,
   Optional<Holder<SoundEvent>> sound,
   Optional<Holder<SoundEvent>> hitSound
) {
   public static final int HIT_FEEDBACK_TICKS = 10;
   public static final Codec<KineticWeapon> CODEC = RecordCodecBuilder.create(
      p_477896_ -> p_477896_.group(
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("contact_cooldown_ticks", 10).forGetter(KineticWeapon::contactCooldownTicks),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("delay_ticks", 0).forGetter(KineticWeapon::delayTicks),
            KineticWeapon.Condition.CODEC.optionalFieldOf("dismount_conditions").forGetter(KineticWeapon::dismountConditions),
            KineticWeapon.Condition.CODEC.optionalFieldOf("knockback_conditions").forGetter(KineticWeapon::knockbackConditions),
            KineticWeapon.Condition.CODEC.optionalFieldOf("damage_conditions").forGetter(KineticWeapon::damageConditions),
            Codec.FLOAT.optionalFieldOf("forward_movement", 0.0F).forGetter(KineticWeapon::forwardMovement),
            Codec.FLOAT.optionalFieldOf("damage_multiplier", 1.0F).forGetter(KineticWeapon::damageMultiplier),
            SoundEvent.CODEC.optionalFieldOf("sound").forGetter(KineticWeapon::sound),
            SoundEvent.CODEC.optionalFieldOf("hit_sound").forGetter(KineticWeapon::hitSound)
         )
         .apply(p_477896_, KineticWeapon::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, KineticWeapon> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      KineticWeapon::contactCooldownTicks,
      ByteBufCodecs.VAR_INT,
      KineticWeapon::delayTicks,
      KineticWeapon.Condition.STREAM_CODEC.apply(ByteBufCodecs::optional),
      KineticWeapon::dismountConditions,
      KineticWeapon.Condition.STREAM_CODEC.apply(ByteBufCodecs::optional),
      KineticWeapon::knockbackConditions,
      KineticWeapon.Condition.STREAM_CODEC.apply(ByteBufCodecs::optional),
      KineticWeapon::damageConditions,
      ByteBufCodecs.FLOAT,
      KineticWeapon::forwardMovement,
      ByteBufCodecs.FLOAT,
      KineticWeapon::damageMultiplier,
      SoundEvent.STREAM_CODEC.apply(ByteBufCodecs::optional),
      KineticWeapon::sound,
      SoundEvent.STREAM_CODEC.apply(ByteBufCodecs::optional),
      KineticWeapon::hitSound,
      KineticWeapon::new
   );

   public static Vec3 getMotion(Entity p_455187_) {
      if (!(p_455187_ instanceof Player) && p_455187_.isPassenger()) {
         p_455187_ = p_455187_.getRootVehicle();
      }

      return p_455187_.getKnownSpeed().scale(20.0);
   }

   public void makeSound(Entity p_454922_) {
      this.sound
         .ifPresent(
            p_455686_ -> p_454922_.level()
               .playSound(
                  p_454922_, p_454922_.getX(), p_454922_.getY(), p_454922_.getZ(), (Holder<SoundEvent>)p_455686_, p_454922_.getSoundSource(), 1.0F, 1.0F
               )
         );
   }

   public void makeLocalHitSound(Entity p_454818_) {
      this.hitSound.ifPresent(p_482276_ -> p_454818_.level().playLocalSound(p_454818_, p_482276_.value(), p_454818_.getSoundSource(), 1.0F, 1.0F));
   }

   public int computeDamageUseDuration() {
      return this.delayTicks + this.damageConditions.map(KineticWeapon.Condition::maxDurationTicks).orElse(0);
   }

   public void damageEntities(ItemStack p_455067_, int p_455721_, LivingEntity p_456105_, EquipmentSlot p_456030_) {
      int i = p_455067_.getUseDuration(p_456105_) - p_455721_;
      if (i >= this.delayTicks) {
         i -= this.delayTicks;
         Vec3 vec3 = p_456105_.getLookAngle();
         double d0 = vec3.dot(getMotion(p_456105_));
         float f = p_456105_ instanceof Player ? 1.0F : 0.2F;
         AttackRange attackrange = p_456105_.entityAttackRange();
         double d1 = p_456105_.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
         boolean flag = false;

         for (EntityHitResult entityhitresult : (Collection)ProjectileUtil.getHitEntitiesAlong(
               p_456105_, attackrange, p_455632_ -> PiercingWeapon.canHitEntity(p_456105_, p_455632_), ClipContext.Block.COLLIDER
            )
            .map(p_477897_ -> List.of(), p_477899_ -> p_477899_)) {
            Entity entity = entityhitresult.getEntity();
            if (entity instanceof EnderDragonPart enderdragonpart) {
               entity = enderdragonpart.parentMob;
            }

            boolean flag4 = p_456105_.wasRecentlyStabbed(entity, this.contactCooldownTicks);
            if (!flag4) {
               p_456105_.rememberStabbedEntity(entity);
               double d2 = vec3.dot(getMotion(entity));
               double d3 = Math.max(0.0, d0 - d2);
               boolean flag1 = this.dismountConditions.isPresent() && this.dismountConditions.get().test(i, d0, d3, f);
               boolean flag2 = this.knockbackConditions.isPresent() && this.knockbackConditions.get().test(i, d0, d3, f);
               boolean flag3 = this.damageConditions.isPresent() && this.damageConditions.get().test(i, d0, d3, f);
               if (flag1 || flag2 || flag3) {
                  float f1 = (float)d1 + Mth.floor(d3 * this.damageMultiplier);
                  flag |= p_456105_.stabAttack(p_456030_, entity, f1, flag3, flag2, flag1);
               }
            }
         }

         if (flag) {
            p_456105_.level().broadcastEntityEvent(p_456105_, (byte)2);
            if (p_456105_ instanceof ServerPlayer serverplayer) {
               CriteriaTriggers.SPEAR_MOBS_TRIGGER.trigger(serverplayer, p_456105_.stabbedEntities(p_477898_ -> p_477898_ instanceof LivingEntity));
            }
         }
      }
   }

   public record Condition(int maxDurationTicks, float minSpeed, float minRelativeSpeed) {
      public static final Codec<KineticWeapon.Condition> CODEC = RecordCodecBuilder.create(
         p_456286_ -> p_456286_.group(
               ExtraCodecs.NON_NEGATIVE_INT.fieldOf("max_duration_ticks").forGetter(KineticWeapon.Condition::maxDurationTicks),
               Codec.FLOAT.optionalFieldOf("min_speed", 0.0F).forGetter(KineticWeapon.Condition::minSpeed),
               Codec.FLOAT.optionalFieldOf("min_relative_speed", 0.0F).forGetter(KineticWeapon.Condition::minRelativeSpeed)
            )
            .apply(p_456286_, KineticWeapon.Condition::new)
      );
      public static final StreamCodec<ByteBuf, KineticWeapon.Condition> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.VAR_INT,
         KineticWeapon.Condition::maxDurationTicks,
         ByteBufCodecs.FLOAT,
         KineticWeapon.Condition::minSpeed,
         ByteBufCodecs.FLOAT,
         KineticWeapon.Condition::minRelativeSpeed,
         KineticWeapon.Condition::new
      );

      public boolean test(int p_455121_, double p_455101_, double p_455338_, double p_455414_) {
         return p_455121_ <= this.maxDurationTicks && p_455101_ >= this.minSpeed * p_455414_ && p_455338_ >= this.minRelativeSpeed * p_455414_;
      }

      public static Optional<KineticWeapon.Condition> ofAttackerSpeed(int p_455206_, float p_456010_) {
         return Optional.of(new KineticWeapon.Condition(p_455206_, p_456010_, 0.0F));
      }

      public static Optional<KineticWeapon.Condition> ofRelativeSpeed(int p_455277_, float p_456214_) {
         return Optional.of(new KineticWeapon.Condition(p_455277_, 0.0F, p_456214_));
      }
   }
}
