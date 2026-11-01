package net.minecraft.world.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public abstract class ProjectileWeaponItem extends Item {
   public static final Predicate<ItemStack> ARROW_ONLY = itemStack -> itemStack.is(ItemTags.ARROWS);
   public static final Predicate<ItemStack> ARROW_OR_FIREWORK = ARROW_ONLY.or(itemStack -> itemStack.is(Items.FIREWORK_ROCKET));

   public ProjectileWeaponItem(Item.Properties properties) {
      super(properties);
   }

   public Predicate<ItemStack> getSupportedHeldProjectiles() {
      return this.getAllSupportedProjectiles();
   }

   public abstract Predicate<ItemStack> getAllSupportedProjectiles();

   public static ItemStack getHeldProjectile(LivingEntity shooter, Predicate<ItemStack> isAmmo) {
      if (isAmmo.test(shooter.getItemInHand(InteractionHand.OFF_HAND))) {
         return shooter.getItemInHand(InteractionHand.OFF_HAND);
      } else {
         return isAmmo.test(shooter.getItemInHand(InteractionHand.MAIN_HAND)) ? shooter.getItemInHand(InteractionHand.MAIN_HAND) : ItemStack.EMPTY;
      }
   }

   public abstract int getDefaultProjectileRange();

   protected void shoot(
      ServerLevel level,
      LivingEntity shooter,
      InteractionHand hand,
      ItemStack weapon,
      List<ItemStack> projectileItems,
      float velocity,
      float inaccuracy,
      boolean isCrit,
      @Nullable LivingEntity target
   ) {
      float f = EnchantmentHelper.processProjectileSpread(level, weapon, shooter, 0.0F);
      float g = projectileItems.size() == 1 ? 0.0F : 2.0F * f / (projectileItems.size() - 1);
      float h = (projectileItems.size() - 1) % 2 * g / 2.0F;
      float i = 1.0F;

      for (int j = 0; j < projectileItems.size(); j++) {
         ItemStack itemStack = projectileItems.get(j);
         if (!itemStack.isEmpty()) {
            float k = h + i * ((j + 1) / 2) * g;
            i = -i;
            int l = j;
            Projectile.spawnProjectile(
               this.createProjectile(level, shooter, weapon, itemStack, isCrit),
               level,
               itemStack,
               projectile -> this.shootProjectile(shooter, projectile, l, velocity, inaccuracy, k, target)
            );
            weapon.hurtAndBreak(this.getDurabilityUse(itemStack), shooter, hand.asEquipmentSlot());
            if (weapon.isEmpty()) {
               break;
            }
         }
      }
   }

   protected int getDurabilityUse(ItemStack stack) {
      return 1;
   }

   protected abstract void shootProjectile(
      LivingEntity shooter, Projectile projectile, int index, float velocity, float inaccuracy, float angle, @Nullable LivingEntity target
   );

   protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammo, boolean isCrit) {
      ArrowItem arrowItem2 = ammo.getItem() instanceof ArrowItem arrowItem ? arrowItem : (ArrowItem)Items.ARROW;
      AbstractArrow abstractArrow = arrowItem2.createArrow(level, ammo, shooter, weapon);
      if (isCrit) {
         abstractArrow.setCritArrow(true);
      }

      return abstractArrow;
   }

   protected static List<ItemStack> draw(ItemStack weapon, ItemStack ammo, LivingEntity shooter) {
      if (ammo.isEmpty()) {
         return List.of();
      } else {
         int i = shooter.level() instanceof ServerLevel serverLevel ? EnchantmentHelper.processProjectileCount(serverLevel, weapon, shooter, 1) : 1;
         List<ItemStack> list = new ArrayList<>(i);
         ItemStack itemStack = ammo.copy();

         for (int j = 0; j < i; j++) {
            ItemStack itemStack2 = useAmmo(weapon, j == 0 ? ammo : itemStack, shooter, j > 0);
            if (!itemStack2.isEmpty()) {
               list.add(itemStack2);
            }
         }

         return list;
      }
   }

   protected static ItemStack useAmmo(ItemStack weapon, ItemStack ammo, LivingEntity shooter, boolean intangible) {
      int i = !intangible && !shooter.hasInfiniteMaterials() && shooter.level() instanceof ServerLevel serverLevel
         ? EnchantmentHelper.processAmmoUse(serverLevel, weapon, ammo, 1)
         : 0;
      if (i > ammo.getCount()) {
         return ItemStack.EMPTY;
      } else if (i == 0) {
         ItemStack itemStack = ammo.copyWithCount(1);
         itemStack.set(DataComponents.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
         return itemStack;
      } else {
         ItemStack itemStack = ammo.split(i);
         if (ammo.isEmpty() && shooter instanceof Player player) {
            player.getInventory().removeItem(ammo);
         }

         return itemStack;
      }
   }
}
