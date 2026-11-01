package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

public class CopperChestBlock extends ChestBlock {
   public static final MapCodec<CopperChestBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_434693_ -> p_434693_.group(
            WeatheringCopper.WeatherState.CODEC.fieldOf("weathering_state").forGetter(CopperChestBlock::getState),
            BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("open_sound").forGetter(ChestBlock::getOpenChestSound),
            BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("close_sound").forGetter(ChestBlock::getCloseChestSound),
            propertiesCodec()
         )
         .apply(p_434693_, CopperChestBlock::new)
   );
   private static final Map<Block, Supplier<Block>> COPPER_TO_COPPER_CHEST_MAPPING = Map.of(
      Blocks.COPPER_BLOCK,
      () -> Blocks.COPPER_CHEST,
      Blocks.EXPOSED_COPPER,
      () -> Blocks.EXPOSED_COPPER_CHEST,
      Blocks.WEATHERED_COPPER,
      () -> Blocks.WEATHERED_COPPER_CHEST,
      Blocks.OXIDIZED_COPPER,
      () -> Blocks.OXIDIZED_COPPER_CHEST,
      Blocks.WAXED_COPPER_BLOCK,
      () -> Blocks.COPPER_CHEST,
      Blocks.WAXED_EXPOSED_COPPER,
      () -> Blocks.EXPOSED_COPPER_CHEST,
      Blocks.WAXED_WEATHERED_COPPER,
      () -> Blocks.WEATHERED_COPPER_CHEST,
      Blocks.WAXED_OXIDIZED_COPPER,
      () -> Blocks.OXIDIZED_COPPER_CHEST
   );
   private final WeatheringCopper.WeatherState weatherState;

   @Override
   public MapCodec<? extends CopperChestBlock> codec() {
      return CODEC;
   }

   public CopperChestBlock(WeatheringCopper.WeatherState p_434044_, SoundEvent p_433319_, SoundEvent p_433572_, BlockBehaviour.Properties p_434849_) {
      super(() -> BlockEntityType.CHEST, p_433319_, p_433572_, p_434849_);
      this.weatherState = p_434044_;
   }

   @Override
   public boolean chestCanConnectTo(BlockState p_436019_) {
      return p_436019_.is(BlockTags.COPPER_CHESTS) && p_436019_.hasProperty(ChestBlock.TYPE);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_435006_) {
      BlockState blockstate = super.getStateForPlacement(p_435006_);
      return getLeastOxidizedChestOfConnectedBlocks(blockstate, p_435006_.getLevel(), p_435006_.getClickedPos());
   }

   private static BlockState getLeastOxidizedChestOfConnectedBlocks(BlockState p_443572_, Level p_442796_, BlockPos p_443618_) {
      BlockState blockstate = p_442796_.getBlockState(p_443618_.relative(getConnectedDirection(p_443572_)));
      if (!p_443572_.getValue(ChestBlock.TYPE).equals(ChestType.SINGLE)
         && p_443572_.getBlock() instanceof CopperChestBlock copperchestblock
         && blockstate.getBlock() instanceof CopperChestBlock copperchestblock1) {
         BlockState blockstate2 = p_443572_;
         BlockState blockstate1 = blockstate;
         if (copperchestblock.isWaxed() != copperchestblock1.isWaxed()) {
            blockstate2 = unwaxBlock(copperchestblock, p_443572_).orElse(p_443572_);
            blockstate1 = unwaxBlock(copperchestblock1, blockstate).orElse(blockstate);
         }

         Block block = copperchestblock.weatherState.ordinal() <= copperchestblock1.weatherState.ordinal() ? blockstate2.getBlock() : blockstate1.getBlock();
         return block.withPropertiesOf(blockstate2);
      } else {
         return p_443572_;
      }
   }

   @Override
   protected BlockState updateShape(
      BlockState p_434501_,
      LevelReader p_435363_,
      ScheduledTickAccess p_435571_,
      BlockPos p_434601_,
      Direction p_435158_,
      BlockPos p_434233_,
      BlockState p_435964_,
      RandomSource p_432879_
   ) {
      BlockState blockstate = super.updateShape(p_434501_, p_435363_, p_435571_, p_434601_, p_435158_, p_434233_, p_435964_, p_432879_);
      if (this.chestCanConnectTo(p_435964_)) {
         ChestType chesttype = blockstate.getValue(ChestBlock.TYPE);
         if (!chesttype.equals(ChestType.SINGLE) && getConnectedDirection(blockstate) == p_435158_) {
            return p_435964_.getBlock().withPropertiesOf(blockstate);
         }
      }

      return blockstate;
   }

   private static Optional<BlockState> unwaxBlock(CopperChestBlock p_435693_, BlockState p_434614_) {
      return !p_435693_.isWaxed()
         ? Optional.of(p_434614_)
         : Optional.ofNullable((Block)HoneycombItem.WAX_OFF_BY_BLOCK.get().get(p_434614_.getBlock()))
            .map(p_435226_ -> ((Block)p_435226_).withPropertiesOf(p_434614_));
   }

   public WeatheringCopper.WeatherState getState() {
      return this.weatherState;
   }

   public static BlockState getFromCopperBlock(Block p_434003_, Direction p_442692_, Level p_442654_, BlockPos p_443072_) {
      CopperChestBlock copperchestblock = (CopperChestBlock)COPPER_TO_COPPER_CHEST_MAPPING.getOrDefault(p_434003_, Blocks.COPPER_CHEST::asBlock).get();
      ChestType chesttype = copperchestblock.getChestType(p_442654_, p_443072_, p_442692_);
      BlockState blockstate = copperchestblock.defaultBlockState().setValue(FACING, p_442692_).setValue(TYPE, chesttype);
      return getLeastOxidizedChestOfConnectedBlocks(blockstate, p_442654_, p_443072_);
   }

   public boolean isWaxed() {
      return true;
   }

   @Override
   public boolean shouldChangedStateKeepBlockEntity(BlockState p_433440_) {
      return p_433440_.is(BlockTags.COPPER_CHESTS);
   }
}
