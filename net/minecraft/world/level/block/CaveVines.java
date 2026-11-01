package net.minecraft.world.level.block;

import java.util.function.ToIntFunction;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface CaveVines {
   VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);
   BooleanProperty BERRIES = BlockStateProperties.BERRIES;

   static InteractionResult use(@Nullable Entity p_270738_, BlockState p_270772_, Level p_270721_, BlockPos p_270587_) {
      if (p_270772_.getValue(BERRIES)) {
         Block.popResource(p_270721_, p_270587_, new ItemStack(Items.GLOW_BERRIES, 1));
         float $$4 = Mth.randomBetween(p_270721_.random, 0.8F, 1.2F);
         p_270721_.playSound(null, p_270587_, SoundEvents.CAVE_VINES_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, $$4);
         BlockState $$5 = p_270772_.setValue(BERRIES, false);
         p_270721_.setBlock(p_270587_, $$5, 2);
         p_270721_.gameEvent(GameEvent.BLOCK_CHANGE, p_270587_, GameEvent.Context.of(p_270738_, $$5));
         return InteractionResult.sidedSuccess(p_270721_.isClientSide);
      } else {
         return InteractionResult.PASS;
      }
   }

   static boolean hasGlowBerries(BlockState p_152952_) {
      return p_152952_.hasProperty(BERRIES) && p_152952_.getValue(BERRIES);
   }

   static ToIntFunction<BlockState> emission(int p_181218_) {
      return p_181216_ -> p_181216_.getValue(BlockStateProperties.BERRIES) ? p_181218_ : 0;
   }
}
