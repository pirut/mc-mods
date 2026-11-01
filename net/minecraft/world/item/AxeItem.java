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
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jspecify.annotations.Nullable;

public class AxeItem extends Item {
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

   public AxeItem(ToolMaterial material, float attackDamage, float attackSpeed, Item.Properties properties) {
      super(properties.axe(material, attackDamage, attackSpeed));
   }

   @Override
   public InteractionResult useOn(UseOnContext context) {
      Level level = context.getLevel();
      BlockPos blockPos = context.getClickedPos();
      Player player = context.getPlayer();
      if (playerHasBlockingItemUseIntent(context)) {
         return InteractionResult.PASS;
      } else {
         Optional<BlockState> optional = this.evaluateNewBlockState(level, blockPos, player, level.getBlockState(blockPos));
         if (optional.isEmpty()) {
            return InteractionResult.PASS;
         } else {
            ItemStack itemStack = context.getItemInHand();
            if (player instanceof ServerPlayer) {
               CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger((ServerPlayer)player, blockPos, itemStack);
            }

            level.setBlock(blockPos, optional.get(), 11);
            level.gameEvent(GameEvent.BLOCK_CHANGE, blockPos, GameEvent.Context.of(player, optional.get()));
            if (player != null) {
               itemStack.hurtAndBreak(1, player, context.getHand().asEquipmentSlot());
            }

            return InteractionResult.SUCCESS;
         }
      }
   }

   private static boolean playerHasBlockingItemUseIntent(UseOnContext context) {
      Player player = context.getPlayer();
      return context.getHand().equals(InteractionHand.MAIN_HAND)
         && player.getOffhandItem().has(DataComponents.BLOCKS_ATTACKS)
         && !player.isSecondaryUseActive();
   }

   private Optional<BlockState> evaluateNewBlockState(Level level, BlockPos pos, @Nullable Player player, BlockState state) {
      Optional<BlockState> optional = this.getStripped(state);
      if (optional.isPresent()) {
         level.playSound(player, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0F, 1.0F);
         return optional;
      } else {
         Optional<BlockState> optional2 = WeatheringCopper.getPrevious(state);
         if (optional2.isPresent()) {
            spawnSoundAndParticle(level, pos, player, state, SoundEvents.AXE_SCRAPE, 3005);
            return optional2;
         } else {
            Optional<BlockState> optional3 = Optional.ofNullable((Block)HoneycombItem.WAX_OFF_BY_BLOCK.get().get(state.getBlock()))
               .map(block -> ((Block)block).withPropertiesOf(state));
            if (optional3.isPresent()) {
               spawnSoundAndParticle(level, pos, player, state, SoundEvents.AXE_WAX_OFF, 3004);
               return optional3;
            } else {
               return Optional.empty();
            }
         }
      }
   }

   private static void spawnSoundAndParticle(Level level, BlockPos pos, @Nullable Player player, BlockState state, SoundEvent sount, int event) {
      level.playSound(player, pos, sount, SoundSource.BLOCKS, 1.0F, 1.0F);
      level.levelEvent(player, event, pos, 0);
      if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
         BlockPos blockPos = ChestBlock.getConnectedBlockPos(pos, state);
         level.gameEvent(GameEvent.BLOCK_CHANGE, blockPos, GameEvent.Context.of(player, level.getBlockState(blockPos)));
         level.levelEvent(player, event, blockPos, 0);
      }
   }

   private Optional<BlockState> getStripped(BlockState unstrippedState) {
      return Optional.ofNullable(STRIPPABLES.get(unstrippedState.getBlock()))
         .map(block -> block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, unstrippedState.getValue(RotatedPillarBlock.AXIS)));
   }
}
