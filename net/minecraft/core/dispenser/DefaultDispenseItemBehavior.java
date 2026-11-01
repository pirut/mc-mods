package net.minecraft.core.dispenser;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

public class DefaultDispenseItemBehavior implements DispenseItemBehavior {
   private static final int DEFAULT_ACCURACY = 6;

   @Override
   public final ItemStack dispense(BlockSource p_302432_, ItemStack p_123392_) {
      ItemStack $$2 = this.execute(p_302432_, p_123392_);
      this.playSound(p_302432_);
      this.playAnimation(p_302432_, p_302432_.state().getValue(DispenserBlock.FACING));
      return $$2;
   }

   protected ItemStack execute(BlockSource p_302420_, ItemStack p_123386_) {
      Direction $$2 = p_302420_.state().getValue(DispenserBlock.FACING);
      Position $$3 = DispenserBlock.getDispensePosition(p_302420_);
      ItemStack $$4 = p_123386_.split(1);
      spawnItem(p_302420_.level(), $$4, 6, $$2, $$3);
      return p_123386_;
   }

   public static void spawnItem(Level p_123379_, ItemStack p_123380_, int p_123381_, Direction p_123382_, Position p_123383_) {
      double $$5 = p_123383_.x();
      double $$6 = p_123383_.y();
      double $$7 = p_123383_.z();
      if (p_123382_.getAxis() == Direction.Axis.Y) {
         $$6 -= 0.125;
      } else {
         $$6 -= 0.15625;
      }

      ItemEntity $$8 = new ItemEntity(p_123379_, $$5, $$6, $$7, p_123380_);
      double $$9 = p_123379_.random.nextDouble() * 0.1 + 0.2;
      $$8.setDeltaMovement(
         p_123379_.random.triangle(p_123382_.getStepX() * $$9, 0.0172275 * p_123381_),
         p_123379_.random.triangle(0.2, 0.0172275 * p_123381_),
         p_123379_.random.triangle(p_123382_.getStepZ() * $$9, 0.0172275 * p_123381_)
      );
      p_123379_.addFreshEntity($$8);
   }

   protected void playSound(BlockSource p_302471_) {
      playDefaultSound(p_302471_);
   }

   protected void playAnimation(BlockSource p_302462_, Direction p_123389_) {
      playDefaultAnimation(p_302462_, p_123389_);
   }

   private static void playDefaultSound(BlockSource p_347476_) {
      p_347476_.level().levelEvent(1000, p_347476_.pos(), 0);
   }

   private static void playDefaultAnimation(BlockSource p_347531_, Direction p_347570_) {
      p_347531_.level().levelEvent(2000, p_347531_.pos(), p_347570_.get3DDataValue());
   }

   protected ItemStack consumeWithRemainder(BlockSource p_347658_, ItemStack p_347682_, ItemStack p_347670_) {
      p_347682_.shrink(1);
      if (p_347682_.isEmpty()) {
         return p_347670_;
      } else {
         this.addToInventoryOrDispense(p_347658_, p_347670_);
         return p_347682_;
      }
   }

   private void addToInventoryOrDispense(BlockSource p_347634_, ItemStack p_347604_) {
      ItemStack $$2 = p_347634_.blockEntity().insertItem(p_347604_);
      if (!$$2.isEmpty()) {
         Direction $$3 = p_347634_.state().getValue(DispenserBlock.FACING);
         spawnItem(p_347634_.level(), $$2, 6, $$3, DispenserBlock.getDispensePosition(p_347634_));
         playDefaultSound(p_347634_);
         playDefaultAnimation(p_347634_, $$3);
      }
   }
}
