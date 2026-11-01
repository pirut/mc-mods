package net.minecraft.world.item;

import com.google.common.collect.ImmutableMap.Builder;
import java.util.Map;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import net.neoforged.neoforge.registries.datamaps.builtin.Strippable;
import org.jspecify.annotations.Nullable;

public class AxeItem extends Item {
   @Deprecated
   protected static final Map<Block, Block> STRIPPABLES = new Builder()
      .put(Blocks.OAK_WOOD, Blocks.STRIPPED_OAK_WOOD)
      .put(Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG)
      .put(Blocks.DARK_OAK_WOOD, Blocks.STRIPPED_DARK_OAK_WOOD)
      .put(Blocks.DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_LOG)
      .put(Blocks.PALE_OAK_WOOD, Blocks.STRIPPED_PALE_OAK_WOOD)
      .put(Blocks.PALE_OAK_LOG, Blocks.STRIPPED_PALE_OAK_LOG)
      .put(Blocks.ACACIA_WOOD, Blocks.STRIPPED_ACACIA_WOOD)
      .put(Blocks.ACACIA_LOG, Blocks.STRIPPED_ACACIA_LOG)
      .put(Blocks.CHERRY_WOOD, Blocks.STRIPPED_CHERRY_WOOD)
      .put(Blocks.CHERRY_LOG, Blocks.STRIPPED_CHERRY_LOG)
      .put(Blocks.BIRCH_WOOD, Blocks.STRIPPED_BIRCH_WOOD)
      .put(Blocks.BIRCH_LOG, Blocks.STRIPPED_BIRCH_LOG)
      .put(Blocks.JUNGLE_WOOD, Blocks.STRIPPED_JUNGLE_WOOD)
      .put(Blocks.JUNGLE_LOG, Blocks.STRIPPED_JUNGLE_LOG)
      .put(Blocks.SPRUCE_WOOD, Blocks.STRIPPED_SPRUCE_WOOD)
      .put(Blocks.SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_LOG)
      .put(Blocks.WARPED_STEM, Blocks.STRIPPED_WARPED_STEM)
      .put(Blocks.WARPED_HYPHAE, Blocks.STRIPPED_WARPED_HYPHAE)
      .put(Blocks.CRIMSON_STEM, Blocks.STRIPPED_CRIMSON_STEM)
      .put(Blocks.CRIMSON_HYPHAE, Blocks.STRIPPED_CRIMSON_HYPHAE)
      .put(Blocks.MANGROVE_WOOD, Blocks.STRIPPED_MANGROVE_WOOD)
      .put(Blocks.MANGROVE_LOG, Blocks.STRIPPED_MANGROVE_LOG)
      .put(Blocks.BAMBOO_BLOCK, Blocks.STRIPPED_BAMBOO_BLOCK)
      .build();

   public AxeItem(ToolMaterial p_360755_, float p_364213_, float p_365432_, Item.Properties p_40524_) {
      super(p_40524_.axe(p_360755_, p_364213_, p_365432_));
   }

   @Override
   public InteractionResult useOn(UseOnContext p_40529_) {
      Level level = p_40529_.getLevel();
      BlockPos blockpos = p_40529_.getClickedPos();
      Player player = p_40529_.getPlayer();
      if (playerHasBlockingItemUseIntent(p_40529_)) {
         return InteractionResult.PASS;
      } else {
         Optional<BlockState> optional = this.evaluateNewBlockState(level, blockpos, player, level.getBlockState(blockpos), p_40529_);
         if (optional.isEmpty()) {
            return InteractionResult.PASS;
         } else {
            ItemStack itemstack = p_40529_.getItemInHand();
            if (player instanceof ServerPlayer) {
               CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger((ServerPlayer)player, blockpos, itemstack);
            }

            level.setBlock(blockpos, optional.get(), 11);
            level.gameEvent(GameEvent.BLOCK_CHANGE, blockpos, GameEvent.Context.of(player, optional.get()));
            if (player != null) {
               itemstack.hurtAndBreak(1, player, p_40529_.getHand().asEquipmentSlot());
            }

            return InteractionResult.SUCCESS;
         }
      }
   }

   private static boolean playerHasBlockingItemUseIntent(UseOnContext p_400108_) {
      Player player = p_400108_.getPlayer();
      return p_400108_.getHand().equals(InteractionHand.MAIN_HAND)
         && player.getOffhandItem().has(DataComponents.BLOCKS_ATTACKS)
         && !player.isSecondaryUseActive();
   }

   private Optional<BlockState> evaluateNewBlockState(
      Level p_308922_, BlockPos p_308899_, @Nullable Player p_309192_, BlockState p_308900_, UseOnContext p_40529_
   ) {
      Optional<BlockState> optional = Optional.ofNullable(p_308900_.getToolModifiedState(p_40529_, ItemAbilities.AXE_STRIP, false));
      if (optional.isPresent()) {
         p_308922_.playSound(p_309192_, p_308899_, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0F, 1.0F);
         return optional;
      } else {
         Optional<BlockState> optional1 = Optional.ofNullable(p_308900_.getToolModifiedState(p_40529_, ItemAbilities.AXE_SCRAPE, false));
         if (optional1.isPresent()) {
            spawnSoundAndParticle(p_308922_, p_308899_, p_309192_, p_308900_, SoundEvents.AXE_SCRAPE, 3005);
            return optional1;
         } else {
            Optional<BlockState> optional2 = Optional.ofNullable(p_308900_.getToolModifiedState(p_40529_, ItemAbilities.AXE_WAX_OFF, false));
            if (optional2.isPresent()) {
               spawnSoundAndParticle(p_308922_, p_308899_, p_309192_, p_308900_, SoundEvents.AXE_WAX_OFF, 3004);
               return optional2;
            } else {
               return Optional.empty();
            }
         }
      }
   }

   private static void spawnSoundAndParticle(
      Level p_451237_, BlockPos p_451207_, @Nullable Player p_451349_, BlockState p_451347_, SoundEvent p_450923_, int p_450956_
   ) {
      p_451237_.playSound(p_451349_, p_451207_, p_450923_, SoundSource.BLOCKS, 1.0F, 1.0F);
      p_451237_.levelEvent(p_451349_, p_450956_, p_451207_, 0);
      if (p_451347_.getBlock() instanceof ChestBlock && p_451347_.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
         BlockPos blockpos = ChestBlock.getConnectedBlockPos(p_451207_, p_451347_);
         p_451237_.gameEvent(GameEvent.BLOCK_CHANGE, blockpos, GameEvent.Context.of(p_451349_, p_451237_.getBlockState(blockpos)));
         p_451237_.levelEvent(p_451349_, p_450956_, blockpos, 0);
      }
   }

   @Nullable
   public static BlockState getAxeStrippingState(BlockState originalState) {
      Strippable strippable = originalState.getBlock().builtInRegistryHolder().getData(NeoForgeDataMaps.STRIPPABLES);
      if (strippable != null) {
         return strippable.strippedBlock().withPropertiesOf(originalState);
      } else {
         Block block = STRIPPABLES.get(originalState.getBlock());
         return block != null ? block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, originalState.getValue(RotatedPillarBlock.AXIS)) : null;
      }
   }

   private Optional<BlockState> getStripped(BlockState p_150691_) {
      Strippable strippable = p_150691_.getBlock().builtInRegistryHolder().getData(NeoForgeDataMaps.STRIPPABLES);
      return strippable != null
         ? Optional.of(strippable.strippedBlock().withPropertiesOf(p_150691_))
         : Optional.ofNullable(STRIPPABLES.get(p_150691_.getBlock()))
            .map(p_360012_ -> p_360012_.defaultBlockState().setValue(RotatedPillarBlock.AXIS, p_150691_.getValue(RotatedPillarBlock.AXIS)));
   }

   @Override
   public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
      return ItemAbilities.DEFAULT_AXE_ACTIONS.contains(itemAbility);
   }
}
