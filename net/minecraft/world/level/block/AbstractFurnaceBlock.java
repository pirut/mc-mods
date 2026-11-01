package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public abstract class AbstractFurnaceBlock extends BaseEntityBlock {
   public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
   public static final BooleanProperty LIT = BlockStateProperties.LIT;

   public AbstractFurnaceBlock(BlockBehaviour.Properties p_48687_) {
      super(p_48687_);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
   }

   @Override
   protected abstract MapCodec<? extends AbstractFurnaceBlock> codec();

   @Override
   protected InteractionResult useWithoutItem(BlockState p_48706_, Level p_48707_, BlockPos p_48708_, Player p_48709_, BlockHitResult p_48711_) {
      if (!p_48707_.isClientSide()) {
         this.openContainer(p_48707_, p_48708_, p_48709_);
      }

      return InteractionResult.SUCCESS;
   }

   protected abstract void openContainer(Level var1, BlockPos var2, Player var3);

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_48689_) {
      return this.defaultBlockState().setValue(FACING, p_48689_.getHorizontalDirection().getOpposite());
   }

   @Override
   protected void affectNeighborsAfterRemoval(BlockState p_393619_, ServerLevel p_394633_, BlockPos p_393784_, boolean p_393627_) {
      Containers.updateNeighboursAfterDestroy(p_393619_, p_394633_, p_393784_);
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState p_48700_) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(BlockState p_48702_, Level p_48703_, BlockPos p_48704_, Direction p_436054_) {
      return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(p_48703_.getBlockEntity(p_48704_));
   }

   @Override
   protected BlockState rotate(BlockState p_48722_, Rotation p_48723_) {
      return p_48722_.setValue(FACING, p_48723_.rotate(p_48722_.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState p_48719_, Mirror p_48720_) {
      return p_48719_.rotate(p_48720_.getRotation(p_48719_.getValue(FACING)));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_48725_) {
      p_48725_.add(FACING, LIT);
   }

   @Nullable
   protected static <T extends BlockEntity> BlockEntityTicker<T> createFurnaceTicker(
      Level p_151988_, BlockEntityType<T> p_151989_, BlockEntityType<? extends AbstractFurnaceBlockEntity> p_151990_
   ) {
      return p_151988_ instanceof ServerLevel serverlevel
         ? createTickerHelper(
            p_151989_,
            p_151990_,
            (p_380330_, p_379922_, p_379493_, p_380329_) -> AbstractFurnaceBlockEntity.serverTick(serverlevel, p_379922_, p_379493_, p_380329_)
         )
         : null;
   }
}
