package net.minecraft.core.dispenser;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.block.DispenserBlock;
import org.slf4j.Logger;

public class ShulkerBoxDispenseBehavior extends OptionalDispenseItemBehavior {
   private static final Logger LOGGER = LogUtils.getLogger();

   @Override
   protected ItemStack execute(BlockSource p_302426_, ItemStack p_123588_) {
      this.setSuccess(false);
      Item $$2 = p_123588_.getItem();
      if ($$2 instanceof BlockItem) {
         Direction $$3 = p_302426_.state().getValue(DispenserBlock.FACING);
         BlockPos $$4 = p_302426_.pos().relative($$3);
         Direction $$5 = p_302426_.level().isEmptyBlock($$4.below()) ? $$3 : Direction.UP;

         try {
            this.setSuccess(((BlockItem)$$2).place(new DirectionalPlaceContext(p_302426_.level(), $$4, $$3, p_123588_, $$5)).consumesAction());
         } catch (Exception var8) {
            LOGGER.error("Error trying to place shulker box at {}", $$4, var8);
         }
      }

      return p_123588_;
   }
}
