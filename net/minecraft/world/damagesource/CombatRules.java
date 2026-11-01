package net.minecraft.world.damagesource;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public class CombatRules {
   public static final float MAX_ARMOR = 20.0F;
   public static final float ARMOR_PROTECTION_DIVIDER = 25.0F;
   public static final float BASE_ARMOR_TOUGHNESS = 2.0F;
   public static final float MIN_ARMOR_RATIO = 0.2F;
   private static final int NUM_ARMOR_ITEMS = 4;

   public static float getDamageAfterAbsorb(LivingEntity p_345376_, float p_19273_, DamageSource p_338892_, float p_19274_, float p_19275_) {
      float $$5 = 2.0F + p_19275_ / 4.0F;
      float $$6 = Mth.clamp(p_19274_ - p_19273_ / $$5, p_19274_ * 0.2F, 20.0F);
      float $$7 = $$6 / 25.0F;
      ItemStack $$8 = p_338892_.getWeaponItem();
      float $$10;
      if ($$8 != null && p_345376_.level() instanceof ServerLevel $$9) {
         $$10 = Mth.clamp(EnchantmentHelper.modifyArmorEffectiveness($$9, $$8, p_345376_, p_338892_, $$7), 0.0F, 1.0F);
      } else {
         $$10 = $$7;
      }

      float $$12 = 1.0F - $$10;
      return p_19273_ * $$12;
   }

   public static float getDamageAfterMagicAbsorb(float p_19270_, float p_19271_) {
      float $$2 = Mth.clamp(p_19271_, 0.0F, 20.0F);
      return p_19270_ * (1.0F - $$2 / 25.0F);
   }
}
