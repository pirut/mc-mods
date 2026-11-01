package net.minecraft.world.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public class CrossbowItem extends ProjectileWeaponItem {
   private static final float MAX_CHARGE_DURATION = 1.25F;
   public static final int DEFAULT_RANGE = 8;
   private boolean startSoundPlayed = false;
   private boolean midLoadSoundPlayed = false;
   private static final float START_SOUND_PERCENT = 0.2F;
   private static final float MID_SOUND_PERCENT = 0.5F;
   private static final float ARROW_POWER = 3.15F;
   private static final float FIREWORK_POWER = 1.6F;
   public static final float MOB_ARROW_POWER = 1.6F;
   private static final CrossbowItem.ChargingSounds DEFAULT_SOUNDS = new CrossbowItem.ChargingSounds(
      Optional.of(SoundEvents.CROSSBOW_LOADING_START), Optional.of(SoundEvents.CROSSBOW_LOADING_MIDDLE), Optional.of(SoundEvents.CROSSBOW_LOADING_END)
   );

   public CrossbowItem(Item.Properties properties) {
      super(properties);
   }

   @Override
   public Predicate<ItemStack> getSupportedHeldProjectiles() {
      return ARROW_OR_FIREWORK;
   }

   @Override
   public Predicate<ItemStack> getAllSupportedProjectiles() {
      return ARROW_ONLY;
   }

   @Override
   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      ChargedProjectiles chargedProjectiles = itemStack.get(DataComponents.CHARGED_PROJECTILES);
      if (chargedProjectiles != null && !chargedProjectiles.isEmpty()) {
         this.performShooting(level, player, hand, itemStack, getShootingPower(chargedProjectiles), 1.0F, null);
         return InteractionResult.CONSUME;
      } else if (!player.getProjectile(itemStack).isEmpty()) {
         this.startSoundPlayed = false;
         this.midLoadSoundPlayed = false;
         player.startUsingItem(hand);
         return InteractionResult.CONSUME;
      } else {
         return InteractionResult.FAIL;
      }
   }

   private static float getShootingPower(ChargedProjectiles projectile) {
      return projectile.contains(Items.FIREWORK_ROCKET) ? 1.6F : 3.15F;
   }

   @Override
   public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
      int i = this.getUseDuration(stack, entity) - timeLeft;
      return getPowerForTime(i, stack, entity) >= 1.0F && isCharged(stack);
   }

   private static boolean tryLoadProjectiles(LivingEntity shooter, ItemStack crossbowStack) {
      List<ItemStack> list = draw(crossbowStack, shooter.getProjectile(crossbowStack), shooter);
      if (!list.isEmpty()) {
         crossbowStack.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(list));
         return true;
      } else {
         return false;
      }
   }

   public static boolean isCharged(ItemStack crossbowStack) {
      ChargedProjectiles chargedProjectiles = crossbowStack.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
      return !chargedProjectiles.isEmpty();
   }

   @Override
   protected void shootProjectile(
      LivingEntity shooter, Projectile projectile, int index, float velocity, float inaccuracy, float angle, @Nullable LivingEntity target
   ) {
      Vector3f vector3f;
      if (target != null) {
         double d = target.getX() - shooter.getX();
         double e = target.getZ() - shooter.getZ();
         double f = Math.sqrt(d * d + e * e);
         double g = target.getY(0.3333333333333333) - projectile.getY() + f * 0.2F;
         vector3f = getProjectileShotVector(shooter, new Vec3(d, g, e), angle);
      } else {
         Vec3 vec3 = shooter.getUpVector(1.0F);
         Quaternionf quaternionf = new Quaternionf().setAngleAxis(angle * (float) (Math.PI / 180.0), vec3.x, vec3.y, vec3.z);
         Vec3 vec32 = shooter.getViewVector(1.0F);
         vector3f = vec32.toVector3f().rotate(quaternionf);
      }

      projectile.shoot(vector3f.x(), vector3f.y(), vector3f.z(), velocity, inaccuracy);
      float h = getShotPitch(shooter.getRandom(), index);
      shooter.level().playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), SoundEvents.CROSSBOW_SHOOT, shooter.getSoundSource(), 1.0F, h);
   }

   private static Vector3f getProjectileShotVector(LivingEntity shooter, Vec3 distance, float angle) {
      Vector3f vector3f = distance.toVector3f().normalize();
      Vector3f vector3f2 = new Vector3f(vector3f).cross(new Vector3f(0.0F, 1.0F, 0.0F));
      if (vector3f2.lengthSquared() <= 1.0E-7) {
         Vec3 vec3 = shooter.getUpVector(1.0F);
         vector3f2 = new Vector3f(vector3f).cross(vec3.toVector3f());
      }

      Vector3f vector3f3 = new Vector3f(vector3f).rotateAxis((float) (Math.PI / 2), vector3f2.x, vector3f2.y, vector3f2.z);
      return new Vector3f(vector3f).rotateAxis(angle * (float) (Math.PI / 180.0), vector3f3.x, vector3f3.y, vector3f3.z);
   }

   @Override
   protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammo, boolean isCrit) {
      if (ammo.is(Items.FIREWORK_ROCKET)) {
         return new FireworkRocketEntity(level, ammo, shooter, shooter.getX(), shooter.getEyeY() - 0.15F, shooter.getZ(), true);
      } else {
         Projectile projectile = super.createProjectile(level, shooter, weapon, ammo, isCrit);
         if (projectile instanceof AbstractArrow abstractArrow) {
            abstractArrow.setSoundEvent(SoundEvents.CROSSBOW_HIT);
         }

         return projectile;
      }
   }

   @Override
   protected int getDurabilityUse(ItemStack stack) {
      return stack.is(Items.FIREWORK_ROCKET) ? 3 : 1;
   }

   public void performShooting(
      Level level, LivingEntity shooter, InteractionHand hand, ItemStack weapon, float velocity, float inaccuracy, @Nullable LivingEntity target
   ) {
      if (level instanceof ServerLevel serverLevel) {
         ChargedProjectiles chargedProjectiles = weapon.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
         if (chargedProjectiles != null && !chargedProjectiles.isEmpty()) {
            this.shoot(serverLevel, shooter, hand, weapon, chargedProjectiles.getItems(), velocity, inaccuracy, shooter instanceof Player, target);
            if (shooter instanceof ServerPlayer serverPlayer) {
               CriteriaTriggers.SHOT_CROSSBOW.trigger(serverPlayer, weapon);
               serverPlayer.awardStat(Stats.ITEM_USED.get(weapon.getItem()));
            }
         }
      }
   }

   private static float getShotPitch(RandomSource random, int index) {
      return index == 0 ? 1.0F : getRandomShotPitch((index & 1) == 1, random);
   }

   private static float getRandomShotPitch(boolean isHighPitched, RandomSource random) {
      float f = isHighPitched ? 0.63F : 0.43F;
      return 1.0F / (random.nextFloat() * 0.5F + 1.8F) + f;
   }

   @Override
   public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
      if (!level.isClientSide()) {
         CrossbowItem.ChargingSounds chargingSounds = this.getChargingSounds(stack);
         float f = (float)(stack.getUseDuration(livingEntity) - remainingUseDuration) / getChargeDuration(stack, livingEntity);
         if (f < 0.2F) {
            this.startSoundPlayed = false;
            this.midLoadSoundPlayed = false;
         }

         if (f >= 0.2F && !this.startSoundPlayed) {
            this.startSoundPlayed = true;
            chargingSounds.start()
               .ifPresent(
                  holder -> level.playSound(
                     null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), holder.value(), SoundSource.PLAYERS, 0.5F, 1.0F
                  )
               );
         }

         if (f >= 0.5F && !this.midLoadSoundPlayed) {
            this.midLoadSoundPlayed = true;
            chargingSounds.mid()
               .ifPresent(
                  holder -> level.playSound(
                     null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), holder.value(), SoundSource.PLAYERS, 0.5F, 1.0F
                  )
               );
         }

         if (f >= 1.0F && !isCharged(stack) && tryLoadProjectiles(livingEntity, stack)) {
            chargingSounds.end()
               .ifPresent(
                  holder -> level.playSound(
                     null,
                     livingEntity.getX(),
                     livingEntity.getY(),
                     livingEntity.getZ(),
                     holder.value(),
                     livingEntity.getSoundSource(),
                     1.0F,
                     1.0F / (level.getRandom().nextFloat() * 0.5F + 1.0F) + 0.2F
                  )
               );
         }
      }
   }

   @Override
   public int getUseDuration(ItemStack stack, LivingEntity entity) {
      return 72000;
   }

   public static int getChargeDuration(ItemStack stack, LivingEntity shooter) {
      float f = EnchantmentHelper.modifyCrossbowChargingTime(stack, shooter, 1.25F);
      return Mth.floor(f * 20.0F);
   }

   @Override
   public ItemUseAnimation getUseAnimation(ItemStack stack) {
      return ItemUseAnimation.CROSSBOW;
   }

   CrossbowItem.ChargingSounds getChargingSounds(ItemStack stack) {
      return EnchantmentHelper.pickHighestLevel(stack, EnchantmentEffectComponents.CROSSBOW_CHARGING_SOUNDS).orElse(DEFAULT_SOUNDS);
   }

   private static float getPowerForTime(int timeLeft, ItemStack stack, LivingEntity shooter) {
      float f = (float)timeLeft / getChargeDuration(stack, shooter);
      if (f > 1.0F) {
         f = 1.0F;
      }

      return f;
   }

   @Override
   public boolean useOnRelease(ItemStack stack) {
      return stack.is(this);
   }

   @Override
   public int getDefaultProjectileRange() {
      return 8;
   }

   public static enum ChargeType implements StringRepresentable {
      NONE("none"),
      ARROW("arrow"),
      ROCKET("rocket");

      public static final Codec<CrossbowItem.ChargeType> CODEC = StringRepresentable.fromEnum(CrossbowItem.ChargeType::values);
      private final String name;

      private ChargeType(final String name) {
         this.name = name;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }

   public record ChargingSounds(Optional<Holder<SoundEvent>> start, Optional<Holder<SoundEvent>> mid, Optional<Holder<SoundEvent>> end) {
      public static final Codec<CrossbowItem.ChargingSounds> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               SoundEvent.CODEC.optionalFieldOf("start").forGetter(CrossbowItem.ChargingSounds::start),
               SoundEvent.CODEC.optionalFieldOf("mid").forGetter(CrossbowItem.ChargingSounds::mid),
               SoundEvent.CODEC.optionalFieldOf("end").forGetter(CrossbowItem.ChargingSounds::end)
            )
            .apply(instance, CrossbowItem.ChargingSounds::new)
      );
   }
}
