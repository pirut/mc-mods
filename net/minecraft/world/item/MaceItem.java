package net.minecraft.world.item;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class MaceItem extends Item {
   private static final int DEFAULT_ATTACK_DAMAGE = 5;
   private static final float DEFAULT_ATTACK_SPEED = -3.4F;
   public static final float SMASH_ATTACK_FALL_THRESHOLD = 1.5F;
   private static final float SMASH_ATTACK_HEAVY_THRESHOLD = 5.0F;
   public static final float SMASH_ATTACK_KNOCKBACK_RADIUS = 3.5F;
   private static final float SMASH_ATTACK_KNOCKBACK_POWER = 0.7F;

   public MaceItem(Item.Properties p_333796_) {
      super(p_333796_);
   }

   public static ItemAttributeModifiers createAttributes() {
      return ItemAttributeModifiers.builder()
         .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 5.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
         .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -3.4F, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
         .build();
   }

   public static Tool createToolProperties() {
      return new Tool(List.of(), 1.0F, 2);
   }

   @Override
   public boolean canAttackBlock(BlockState p_333875_, Level p_333847_, BlockPos p_334073_, Player p_334042_) {
      return !p_334042_.isCreative();
   }

   @Override
   public int getEnchantmentValue() {
      return 15;
   }

   @Override
   public boolean hurtEnemy(ItemStack p_334046_, LivingEntity p_333712_, LivingEntity p_333812_) {
      if (p_333812_ instanceof ServerPlayer $$3 && canSmashAttack($$3)) {
         ServerLevel $$4 = (ServerLevel)p_333812_.level();
         if ($$3.isIgnoringFallDamageFromCurrentImpulse() && $$3.currentImpulseImpactPos != null) {
            if ($$3.currentImpulseImpactPos.y > $$3.position().y) {
               $$3.currentImpulseImpactPos = $$3.position();
            }
         } else {
            $$3.currentImpulseImpactPos = $$3.position();
         }

         $$3.setIgnoreFallDamageFromCurrentImpulse(true);
         $$3.setDeltaMovement($$3.getDeltaMovement().with(Direction.Axis.Y, 0.01F));
         $$3.connection.send(new ClientboundSetEntityMotionPacket($$3));
         if (p_333712_.onGround()) {
            $$3.setSpawnExtraParticlesOnFall(true);
            SoundEvent $$5 = $$3.fallDistance > 5.0F ? SoundEvents.MACE_SMASH_GROUND_HEAVY : SoundEvents.MACE_SMASH_GROUND;
            $$4.playSound(null, $$3.getX(), $$3.getY(), $$3.getZ(), $$5, $$3.getSoundSource(), 1.0F, 1.0F);
         } else {
            $$4.playSound(null, $$3.getX(), $$3.getY(), $$3.getZ(), SoundEvents.MACE_SMASH_AIR, $$3.getSoundSource(), 1.0F, 1.0F);
         }

         knockback($$4, $$3, p_333712_);
      }

      return true;
   }

   @Override
   public void postHurtEnemy(ItemStack p_345716_, LivingEntity p_345817_, LivingEntity p_346003_) {
      p_345716_.hurtAndBreak(1, p_346003_, EquipmentSlot.MAINHAND);
      if (canSmashAttack(p_346003_)) {
         p_346003_.resetFallDistance();
      }
   }

   @Override
   public boolean isValidRepairItem(ItemStack p_334031_, ItemStack p_334058_) {
      return p_334058_.is(Items.BREEZE_ROD);
   }

   @Override
   public float getAttackDamageBonus(Entity p_344900_, float p_335575_, DamageSource p_344972_) {
      if (p_344972_.getDirectEntity() instanceof LivingEntity $$3) {
         if (!canSmashAttack($$3)) {
            return 0.0F;
         } else {
            float $$5 = 3.0F;
            float $$6 = 8.0F;
            float $$7 = $$3.fallDistance;
            float $$8;
            if ($$7 <= 3.0F) {
               $$8 = 4.0F * $$7;
            } else if ($$7 <= 8.0F) {
               $$8 = 12.0F + 2.0F * ($$7 - 3.0F);
            } else {
               $$8 = 22.0F + $$7 - 8.0F;
            }

            return $$3.level() instanceof ServerLevel $$11
               ? $$8 + EnchantmentHelper.modifyFallBasedDamage($$11, $$3.getWeaponItem(), p_344900_, p_344972_, 0.0F) * $$7
               : $$8;
         }
      } else {
         return 0.0F;
      }
   }

   private static void knockback(Level p_335716_, Player p_335955_, Entity p_335810_) {
      p_335716_.levelEvent(2013, p_335810_.getOnPos(), 750);
      p_335716_.getEntitiesOfClass(LivingEntity.class, p_335810_.getBoundingBox().inflate(3.5), knockbackPredicate(p_335955_, p_335810_))
         .forEach(p_347296_ -> {
            Vec3 $$3 = p_347296_.position().subtract(p_335810_.position());
            double $$4 = getKnockbackPower(p_335955_, p_347296_, $$3);
            Vec3 $$5 = $$3.normalize().scale($$4);
            if ($$4 > 0.0) {
               p_347296_.push($$5.x, 0.7F, $$5.z);
               if (p_347296_ instanceof ServerPlayer $$6) {
                  $$6.connection.send(new ClientboundSetEntityMotionPacket($$6));
               }
            }
         });
   }

   private static Predicate<LivingEntity> knockbackPredicate(Player p_338613_, Entity p_338698_) {
      return p_344407_ -> {
         boolean $$3 = !p_344407_.isSpectator();
         boolean $$4 = p_344407_ != p_338613_ && p_344407_ != p_338698_;
         boolean $$5 = !p_338613_.isAlliedTo(p_344407_);
         boolean $$7 = !(p_344407_ instanceof TamableAnimal $$6 && $$6.isTame() && p_338613_.getUUID().equals($$6.getOwnerUUID()));
         boolean $$10 = !(p_344407_ instanceof ArmorStand $$8 && $$8.isMarker());
         boolean $$11 = p_338698_.distanceToSqr(p_344407_) <= Math.pow(3.5, 2.0);
         return $$3 && $$4 && $$5 && $$7 && $$10 && $$11;
      };
   }

   private static double getKnockbackPower(Player p_338265_, LivingEntity p_338630_, Vec3 p_338866_) {
      return (3.5 - p_338866_.length()) * 0.7F * (p_338265_.fallDistance > 5.0F ? 2 : 1) * (1.0 - p_338630_.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
   }

   public static boolean canSmashAttack(LivingEntity p_344836_) {
      return p_344836_.fallDistance > 1.5F && !p_344836_.isFallFlying();
   }
}
