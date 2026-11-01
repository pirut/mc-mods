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
   protected ItemStack execute(BlockSource blockSource, ItemStack item) {
      this.setSuccess(false);
      Item item2 = item.getItem();
      if (item2 instanceof BlockItem) {
         Direction direction = blockSource.state().getValue(DispenserBlock.FACING);
         BlockPos blockPos = blockSource.pos().relative(direction);
         Direction direction2 = blockSource.level().isEmptyBlock(blockPos.below()) ? direction : Direction.UP;

         try {
            this.setSuccess(((BlockItem)item2).place(new DirectionalPlaceContext(blockSource.level(), blockPos, direction, item, direction2)).consumesAction());
         } catch (Exception var8) {
            LOGGER.error("Error trying to place shulker box at {}", blockPos, var8);
         }
      }

      return item;
   }
}
