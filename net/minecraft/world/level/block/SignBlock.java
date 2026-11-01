package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Arrays;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignApplicator;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class SignBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private static final VoxelShape SHAPE = Block.column(8.0, 0.0, 16.0);
   private final WoodType type;

   protected SignBlock(WoodType type, BlockBehaviour.Properties properties) {
      super(properties);
      this.type = type;
   }

   @Override
   protected abstract MapCodec<? extends SignBlock> codec();

   @Override
   protected BlockState updateShape(
      BlockState state,
      LevelReader level,
      ScheduledTickAccess scheduledTickAccess,
      BlockPos pos,
      Direction direction,
      BlockPos neighborPos,
      BlockState neighborState,
      RandomSource random
   ) {
      if (state.getValue(WATERLOGGED)) {
         scheduledTickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
      }

      return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   @Override
   public boolean isPossibleToRespawnInThis(BlockState state) {
      return true;
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new SignBlockEntity(pos, state);
   }

   @Override
   protected InteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (level.getBlockEntity(pos) instanceof SignBlockEntity signBlockEntity) {
         SignApplicator signApplicator2 = stack.getItem() instanceof SignApplicator signApplicator ? signApplicator : null;
         boolean bl = signApplicator2 != null && player.mayBuild();
         if (level instanceof ServerLevel serverLevel) {
            if (bl && !signBlockEntity.isWaxed() && !this.otherPlayerIsEditingSign(player, signBlockEntity)) {
               boolean bl2 = signBlockEntity.isFacingFrontText(player);
               if (signApplicator2.canApplyToSign(signBlockEntity.getText(bl2), player)
                  && signApplicator2.tryApplyToSign(serverLevel, signBlockEntity, bl2, player)) {
                  signBlockEntity.executeClickCommandsIfPresent(serverLevel, player, pos, bl2);
                  player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                  serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, signBlockEntity.getBlockPos(), GameEvent.Context.of(player, signBlockEntity.getBlockState()));
                  stack.consume(1, player);
                  return InteractionResult.SUCCESS;
               } else {
                  return InteractionResult.TRY_WITH_EMPTY_HAND;
               }
            } else {
               return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
         } else {
            return !bl && !signBlockEntity.isWaxed() ? InteractionResult.CONSUME : InteractionResult.SUCCESS;
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (level.getBlockEntity(pos) instanceof SignBlockEntity signBlockEntity) {
         if (level instanceof ServerLevel serverLevel) {
            boolean bl = signBlockEntity.isFacingFrontText(player);
            boolean bl2 = signBlockEntity.executeClickCommandsIfPresent(serverLevel, player, pos, bl);
            if (signBlockEntity.isWaxed()) {
               serverLevel.playSound(null, signBlockEntity.getBlockPos(), signBlockEntity.getSignInteractionFailedSoundEvent(), SoundSource.BLOCKS);
               return InteractionResult.SUCCESS_SERVER;
            } else if (bl2) {
               return InteractionResult.SUCCESS_SERVER;
            } else if (!this.otherPlayerIsEditingSign(player, signBlockEntity) && player.mayBuild() && this.hasEditableText(player, signBlockEntity, bl)) {
               this.openTextEdit(player, signBlockEntity, bl);
               return InteractionResult.SUCCESS_SERVER;
            } else {
               return InteractionResult.PASS;
            }
         } else {
            Util.pauseInIde(new IllegalStateException("Expected to only call this on server"));
            return InteractionResult.CONSUME;
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   private boolean hasEditableText(Player player, SignBlockEntity signEntity, boolean isFrontText) {
      SignText signText = signEntity.getText(isFrontText);
      return Arrays.stream(signText.getMessages(player.isTextFilteringEnabled()))
         .allMatch(component -> component.equals(CommonComponents.EMPTY) || component.getContents() instanceof PlainTextContents);
   }

   public abstract float getYRotationDegrees(BlockState state);

   public Vec3 getSignHitboxCenterPosition(BlockState state) {
      return new Vec3(0.5, 0.5, 0.5);
   }

   @Override
   protected FluidState getFluidState(BlockState state) {
      return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
   }

   public WoodType type() {
      return this.type;
   }

   public static WoodType getWoodType(Block block) {
      WoodType woodType;
      if (block instanceof SignBlock) {
         woodType = ((SignBlock)block).type();
      } else {
         woodType = WoodType.OAK;
      }

      return woodType;
   }

   public void openTextEdit(Player player, SignBlockEntity signEntity, boolean isFrontText) {
      signEntity.setAllowedPlayerEditor(player.getUUID());
      player.openTextEdit(signEntity, isFrontText);
   }

   private boolean otherPlayerIsEditingSign(Player player, SignBlockEntity signEntity) {
      UUID uUID = signEntity.getPlayerWhoMayEdit();
      return uUID != null && !uUID.equals(player.getUUID());
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
      return createTickerHelper(blockEntityType, BlockEntityType.SIGN, SignBlockEntity::tick);
   }
}
