package net.minecraft.core.dispenser;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

public class ProjectileDispenseBehavior extends DefaultDispenseItemBehavior {
   private final ProjectileItem projectileItem;
   private final ProjectileItem.DispenseConfig dispenseConfig;

   public ProjectileDispenseBehavior(Item p_338781_) {
      if (p_338781_ instanceof ProjectileItem $$1) {
         this.projectileItem = $$1;
         this.dispenseConfig = $$1.createDispenseConfig();
      } else {
         throw new IllegalArgumentException(p_338781_ + " not instance of " + ProjectileItem.class.getSimpleName());
      }
   }

   @Override
   public ItemStack execute(BlockSource p_338635_, ItemStack p_338423_) {
      Level $$2 = p_338635_.level();
      Direction $$3 = p_338635_.state().getValue(DispenserBlock.FACING);
      Position $$4 = this.dispenseConfig.positionFunction().getDispensePosition(p_338635_, $$3);
      Projectile $$5 = this.projectileItem.asProjectile($$2, $$4, p_338423_, $$3);
      this.projectileItem.shoot($$5, $$3.getStepX(), $$3.getStepY(), $$3.getStepZ(), this.dispenseConfig.power(), this.dispenseConfig.uncertainty());
      $$2.addFreshEntity($$5);
      p_338423_.shrink(1);
      return p_338423_;
   }

   @Override
   protected void playSound(BlockSource p_338184_) {
      p_338184_.level().levelEvent(this.dispenseConfig.overrideDispenseEvent().orElse(1002), p_338184_.pos(), 0);
   }
}
