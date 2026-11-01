package net.minecraft.world.level.block;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.slf4j.Logger;

public class CommandBlock extends BaseEntityBlock implements GameMasterBlock {
   public static final MapCodec<CommandBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_308813_ -> p_308813_.group(Codec.BOOL.fieldOf("automatic").forGetter(p_304800_ -> p_304800_.automatic), propertiesCodec())
         .apply(p_308813_, CommandBlock::new)
   );
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final DirectionProperty FACING = DirectionalBlock.FACING;
   public static final BooleanProperty CONDITIONAL = BlockStateProperties.CONDITIONAL;
   private final boolean automatic;

   @Override
   public MapCodec<CommandBlock> codec() {
      return CODEC;
   }

   public CommandBlock(boolean p_153081_, BlockBehaviour.Properties p_153080_) {
      super(p_153080_);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CONDITIONAL, false));
      this.automatic = p_153081_;
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos p_153083_, BlockState p_153084_) {
      CommandBlockEntity $$2 = new CommandBlockEntity(p_153083_, p_153084_);
      $$2.setAutomatic(this.automatic);
      return $$2;
   }

   @Override
   protected void neighborChanged(BlockState p_51838_, Level p_51839_, BlockPos p_51840_, Block p_51841_, BlockPos p_51842_, boolean p_51843_) {
      if (!p_51839_.isClientSide) {
         if (p_51839_.getBlockEntity(p_51840_) instanceof CommandBlockEntity $$7) {
            boolean $$8 = p_51839_.hasNeighborSignal(p_51840_);
            boolean $$9 = $$7.isPowered();
            $$7.setPowered($$8);
            if (!$$9 && !$$7.isAutomatic() && $$7.getMode() != CommandBlockEntity.Mode.SEQUENCE) {
               if ($$8) {
                  $$7.markConditionMet();
                  p_51839_.scheduleTick(p_51840_, this, 1);
               }
            }
         }
      }
   }

   @Override
   protected void tick(BlockState p_221005_, ServerLevel p_221006_, BlockPos p_221007_, RandomSource p_221008_) {
      if (p_221006_.getBlockEntity(p_221007_) instanceof CommandBlockEntity $$5) {
         BaseCommandBlock $$6 = $$5.getCommandBlock();
         boolean $$7 = !StringUtil.isNullOrEmpty($$6.getCommand());
         CommandBlockEntity.Mode $$8 = $$5.getMode();
         boolean $$9 = $$5.wasConditionMet();
         if ($$8 == CommandBlockEntity.Mode.AUTO) {
            $$5.markConditionMet();
            if ($$9) {
               this.execute(p_221005_, p_221006_, p_221007_, $$6, $$7);
            } else if ($$5.isConditional()) {
               $$6.setSuccessCount(0);
            }

            if ($$5.isPowered() || $$5.isAutomatic()) {
               p_221006_.scheduleTick(p_221007_, this, 1);
            }
         } else if ($$8 == CommandBlockEntity.Mode.REDSTONE) {
            if ($$9) {
               this.execute(p_221005_, p_221006_, p_221007_, $$6, $$7);
            } else if ($$5.isConditional()) {
               $$6.setSuccessCount(0);
            }
         }

         p_221006_.updateNeighbourForOutputSignal(p_221007_, this);
      }
   }

   private void execute(BlockState p_51832_, Level p_51833_, BlockPos p_51834_, BaseCommandBlock p_51835_, boolean p_51836_) {
      if (p_51836_) {
         p_51835_.performCommand(p_51833_);
      } else {
         p_51835_.setSuccessCount(0);
      }

      executeChain(p_51833_, p_51834_, p_51832_.getValue(FACING));
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_51825_, Level p_51826_, BlockPos p_51827_, Player p_51828_, BlockHitResult p_51830_) {
      BlockEntity $$5 = p_51826_.getBlockEntity(p_51827_);
      if ($$5 instanceof CommandBlockEntity && p_51828_.canUseGameMasterBlocks()) {
         p_51828_.openCommandBlock((CommandBlockEntity)$$5);
         return InteractionResult.sidedSuccess(p_51826_.isClientSide);
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState p_51814_) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(BlockState p_51821_, Level p_51822_, BlockPos p_51823_) {
      BlockEntity $$3 = p_51822_.getBlockEntity(p_51823_);
      return $$3 instanceof CommandBlockEntity ? ((CommandBlockEntity)$$3).getCommandBlock().getSuccessCount() : 0;
   }

   @Override
   public void setPlacedBy(Level p_51804_, BlockPos p_51805_, BlockState p_51806_, LivingEntity p_51807_, ItemStack p_51808_) {
      if (p_51804_.getBlockEntity(p_51805_) instanceof CommandBlockEntity $$6) {
         BaseCommandBlock $$7 = $$6.getCommandBlock();
         if (!p_51804_.isClientSide) {
            if (!p_51808_.has(DataComponents.BLOCK_ENTITY_DATA)) {
               $$7.setTrackOutput(p_51804_.getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK));
               $$6.setAutomatic(this.automatic);
            }

            boolean $$8 = p_51804_.hasNeighborSignal(p_51805_);
            $$6.setPowered($$8);
         }
      }
   }

   @Override
   protected RenderShape getRenderShape(BlockState p_51853_) {
      return RenderShape.MODEL;
   }

   @Override
   protected BlockState rotate(BlockState p_51848_, Rotation p_51849_) {
      return p_51848_.setValue(FACING, p_51849_.rotate(p_51848_.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState p_51845_, Mirror p_51846_) {
      return p_51845_.rotate(p_51846_.getRotation(p_51845_.getValue(FACING)));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_51851_) {
      p_51851_.add(FACING, CONDITIONAL);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_51800_) {
      return this.defaultBlockState().setValue(FACING, p_51800_.getNearestLookingDirection().getOpposite());
   }

   private static void executeChain(Level p_51810_, BlockPos p_51811_, Direction p_51812_) {
      BlockPos.MutableBlockPos $$3 = p_51811_.mutable();
      GameRules $$4 = p_51810_.getGameRules();
      int $$5 = $$4.getInt(GameRules.RULE_MAX_COMMAND_CHAIN_LENGTH);

      while ($$5-- > 0) {
         $$3.move(p_51812_);
         BlockState $$6 = p_51810_.getBlockState($$3);
         Block $$7 = $$6.getBlock();
         if (!$$6.is(Blocks.CHAIN_COMMAND_BLOCK)
            || !(p_51810_.getBlockEntity($$3) instanceof CommandBlockEntity $$9)
            || $$9.getMode() != CommandBlockEntity.Mode.SEQUENCE) {
            break;
         }

         if ($$9.isPowered() || $$9.isAutomatic()) {
            BaseCommandBlock $$10 = $$9.getCommandBlock();
            if ($$9.markConditionMet()) {
               if (!$$10.performCommand(p_51810_)) {
                  break;
               }

               p_51810_.updateNeighbourForOutputSignal($$3, $$7);
            } else if ($$9.isConditional()) {
               $$10.setSuccessCount(0);
            }
         }

         p_51812_ = $$6.getValue(FACING);
      }

      if ($$5 <= 0) {
         int $$11 = Math.max($$4.getInt(GameRules.RULE_MAX_COMMAND_CHAIN_LENGTH), 0);
         LOGGER.warn("Command Block chain tried to execute more than {} steps!", $$11);
      }
   }
}
