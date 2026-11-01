package net.minecraft.world.item;

import java.util.List;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

public class ShearsItem extends Item {
   public ShearsItem(Item.Properties p_43074_) {
      super(p_43074_);
   }

   public static Tool createToolProperties() {
      return new Tool(
         List.of(
            Tool.Rule.minesAndDrops(List.of(Blocks.COBWEB), 15.0F),
            Tool.Rule.overrideSpeed(BlockTags.LEAVES, 15.0F),
            Tool.Rule.overrideSpeed(BlockTags.WOOL, 5.0F),
            Tool.Rule.overrideSpeed(List.of(Blocks.VINE, Blocks.GLOW_LICHEN), 2.0F)
         ),
         1.0F,
         1
      );
   }

   @Override
   public boolean mineBlock(ItemStack p_43078_, Level p_43079_, BlockState p_43080_, BlockPos p_43081_, LivingEntity p_43082_) {
      if (!p_43079_.isClientSide && !p_43080_.is(BlockTags.FIRE)) {
         p_43078_.hurtAndBreak(1, p_43082_, EquipmentSlot.MAINHAND);
      }

      return p_43080_.is(BlockTags.LEAVES)
         || p_43080_.is(Blocks.COBWEB)
         || p_43080_.is(Blocks.SHORT_GRASS)
         || p_43080_.is(Blocks.FERN)
         || p_43080_.is(Blocks.DEAD_BUSH)
         || p_43080_.is(Blocks.HANGING_ROOTS)
         || p_43080_.is(Blocks.VINE)
         || p_43080_.is(Blocks.TRIPWIRE)
         || p_43080_.is(BlockTags.WOOL);
   }

   @Override
   public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
      if (entity instanceof IShearable target) {
         BlockPos pos = entity.blockPosition();
         boolean isClient = entity.level().isClientSide();
         if (target.isShearable(player, stack, entity.level(), pos)) {
            List<ItemStack> drops = target.onSheared(player, stack, entity.level(), pos);
            if (!isClient) {
               for (ItemStack drop : drops) {
                  target.spawnShearedDrop(entity.level(), pos, drop);
               }
            }

            entity.gameEvent(GameEvent.SHEAR, player);
            if (!isClient) {
               stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            }

            return InteractionResult.sidedSuccess(isClient);
         }
      }

      return InteractionResult.PASS;
   }

   @Override
   public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
      return ItemAbilities.DEFAULT_SHEARS_ACTIONS.contains(itemAbility);
   }

   @Override
   public InteractionResult useOn(UseOnContext p_186371_) {
      Level level = p_186371_.getLevel();
      BlockPos blockpos = p_186371_.getClickedPos();
      BlockState blockstate = level.getBlockState(blockpos);
      BlockState blockstate1 = blockstate.getToolModifiedState(p_186371_, ItemAbilities.SHEARS_TRIM, false);
      if (blockstate1 != null) {
         Player player = p_186371_.getPlayer();
         ItemStack itemstack = p_186371_.getItemInHand();
         if (player instanceof ServerPlayer) {
            CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger((ServerPlayer)player, blockpos, itemstack);
         }

         level.setBlockAndUpdate(blockpos, blockstate1);
         level.gameEvent(GameEvent.BLOCK_CHANGE, blockpos, GameEvent.Context.of(p_186371_.getPlayer(), blockstate1));
         if (player != null) {
            itemstack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(p_186371_.getHand()));
         }

         return InteractionResult.sidedSuccess(level.isClientSide);
      } else {
         return super.useOn(p_186371_);
      }
   }
}
