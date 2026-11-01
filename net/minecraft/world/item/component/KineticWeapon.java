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
      instance -> instance.group(
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
         .apply(instance, KineticWeapon::new)
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

   public static Vec3 getMotion(Entity entity) {
      if (!(entity instanceof Player) && entity.isPassenger()) {
         entity = entity.getRootVehicle();
      }

      return entity.getKnownSpeed().scale(20.0);
   }

   public void makeSound(Entity entity) {
      this.sound
         .ifPresent(
            holder -> entity.level()
               .playSound(entity, entity.getX(), entity.getY(), entity.getZ(), (Holder<SoundEvent>)holder, entity.getSoundSource(), 1.0F, 1.0F)
         );
   }

   public void makeLocalHitSound(Entity entity) {
      this.hitSound.ifPresent(holder -> entity.level().playLocalSound(entity, holder.value(), entity.getSoundSource(), 1.0F, 1.0F));
   }

   public int computeDamageUseDuration() {
      return this.delayTicks + this.damageConditions.map(KineticWeapon.Condition::maxDurationTicks).orElse(0);
   }

   public void damageEntities(ItemStack stack, int remainingUseDuration, LivingEntity entity, EquipmentSlot slot) {
      int i = stack.getUseDuration(entity) - remainingUseDuration;
      if (i >= this.delayTicks) {
         i -= this.delayTicks;
         Vec3 vec3 = entity.getLookAngle();
         double d = vec3.dot(getMotion(entity));
         float f = entity instanceof Player ? 1.0F : 0.2F;
         AttackRange attackRange = entity.entityAttackRange();
         double e = entity.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
         boolean bl = false;

         for (EntityHitResult entityHitResult : (Collection)ProjectileUtil.getHitEntitiesAlong(
               entity, attackRange, entityx -> PiercingWeapon.canHitEntity(entity, entityx), ClipContext.Block.COLLIDER
            )
            .map(blockHitResult -> List.of(), collection -> collection)) {
            Entity entity2 = entityHitResult.getEntity();
            if (entity2 instanceof EnderDragonPart enderDragonPart) {
               entity2 = enderDragonPart.parentMob;
            }

            boolean bl2 = entity.wasRecentlyStabbed(entity2, this.contactCooldownTicks);
            if (!bl2) {
               entity.rememberStabbedEntity(entity2);
               double g = vec3.dot(getMotion(entity2));
               double h = Math.max(0.0, d - g);
               boolean bl3 = this.dismountConditions.isPresent() && this.dismountConditions.get().test(i, d, h, f);
               boolean bl4 = this.knockbackConditions.isPresent() && this.knockbackConditions.get().test(i, d, h, f);
               boolean bl5 = this.damageConditions.isPresent() && this.damageConditions.get().test(i, d, h, f);
               if (bl3 || bl4 || bl5) {
                  float j = (float)e + Mth.floor(h * this.damageMultiplier);
                  bl |= entity.stabAttack(slot, entity2, j, bl5, bl4, bl3);
               }
            }
         }

         if (bl) {
            entity.level().broadcastEntityEvent(entity, (byte)2);
            if (entity instanceof ServerPlayer serverPlayer) {
               CriteriaTriggers.SPEAR_MOBS_TRIGGER.trigger(serverPlayer, entity.stabbedEntities(entityx -> entityx instanceof LivingEntity));
            }
         }
      }
   }

   public record Condition(int maxDurationTicks, float minSpeed, float minRelativeSpeed) {
      public static final Codec<KineticWeapon.Condition> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               ExtraCodecs.NON_NEGATIVE_INT.fieldOf("max_duration_ticks").forGetter(KineticWeapon.Condition::maxDurationTicks),
               Codec.FLOAT.optionalFieldOf("min_speed", 0.0F).forGetter(KineticWeapon.Condition::minSpeed),
               Codec.FLOAT.optionalFieldOf("min_relative_speed", 0.0F).forGetter(KineticWeapon.Condition::minRelativeSpeed)
            )
            .apply(instance, KineticWeapon.Condition::new)
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

      public boolean test(int duration, double speed, double relativeSpeed, double speedMultiplier) {
         return duration <= this.maxDurationTicks && speed >= this.minSpeed * speedMultiplier && relativeSpeed >= this.minRelativeSpeed * speedMultiplier;
      }

      public static Optional<KineticWeapon.Condition> ofAttackerSpeed(int maxDurationTicks, float minSpeed) {
         return Optional.of(new KineticWeapon.Condition(maxDurationTicks, minSpeed, 0.0F));
      }

      public static Optional<KineticWeapon.Condition> ofRelativeSpeed(int maxDurationTicks, float minRelativeSpeed) {
         return Optional.of(new KineticWeapon.Condition(maxDurationTicks, 0.0F, minRelativeSpeed));
      }
   }
}
