package net.minecraft.world.item;

import java.util.List;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.Block;
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
      HolderGetter<Block> holdergetter = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);
      return new Tool(
         List.of(
            Tool.Rule.minesAndDrops(HolderSet.direct(Blocks.COBWEB.builtInRegistryHolder()), 15.0F),
            Tool.Rule.overrideSpeed(holdergetter.getOrThrow(BlockTags.LEAVES), 15.0F),
            Tool.Rule.overrideSpeed(holdergetter.getOrThrow(BlockTags.WOOL), 5.0F),
            Tool.Rule.overrideSpeed(HolderSet.direct(Blocks.VINE.builtInRegistryHolder(), Blocks.GLOW_LICHEN.builtInRegistryHolder()), 2.0F)
         ),
         1.0F,
         1,
         true
      );
   }

   @Override
   public boolean mineBlock(ItemStack p_411066_, Level p_411074_, BlockState p_411062_, BlockPos p_411048_, LivingEntity p_411065_) {
      Tool tool = p_411066_.get(DataComponents.TOOL);
      if (tool == null) {
         return false;
      } else {
         if (!p_411074_.isClientSide() && !p_411062_.is(BlockTags.FIRE) && tool.damagePerBlock() > 0) {
            p_411066_.hurtAndBreak(tool.damagePerBlock(), p_411065_, EquipmentSlot.MAINHAND);
         }

         return true;
      }
   }

   @Override
   public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
      if (entity instanceof IShearable target) {
         BlockPos pos = entity.blockPosition();
         boolean isClient = entity.level().isClientSide();
         if (target.isShearable(player, stack, entity.level(), pos)) {
            List<ItemStack> drops = target.onSheared(player, stack, entity.level(), pos);
            if (entity.level() instanceof ServerLevel serverLevel) {
               for (ItemStack drop : drops) {
                  target.spawnShearedDrop(serverLevel, pos, drop);
               }
            }

            entity.gameEvent(GameEvent.SHEAR, player);
            if (!isClient) {
               stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            }

            return InteractionResult.SUCCESS;
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
            itemstack.hurtAndBreak(1, player, p_186371_.getHand().asEquipmentSlot());
         }

         return InteractionResult.SUCCESS;
      } else {
         return super.useOn(p_186371_);
      }
   }
}
