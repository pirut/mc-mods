package net.minecraft.world.level.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class WeightedPressurePlateBlock extends BasePressurePlateBlock {
   public static final MapCodec<WeightedPressurePlateBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_308853_ -> p_308853_.group(
            Codec.intRange(1, 1024).fieldOf("max_weight").forGetter(p_304500_ -> p_304500_.maxWeight),
            BlockSetType.CODEC.fieldOf("block_set_type").forGetter(p_304629_ -> p_304629_.type),
            propertiesCodec()
         )
         .apply(p_308853_, WeightedPressurePlateBlock::new)
   );
   public static final IntegerProperty POWER = BlockStateProperties.POWER;
   private final int maxWeight;

   @Override
   public MapCodec<WeightedPressurePlateBlock> codec() {
      return CODEC;
   }

   public WeightedPressurePlateBlock(int p_273669_, BlockSetType p_272868_, BlockBehaviour.Properties p_273512_) {
      super(p_273512_, p_272868_);
      this.registerDefaultState(this.stateDefinition.any().setValue(POWER, 0));
      this.maxWeight = p_273669_;
   }

   @Override
   protected int getSignalStrength(Level p_58213_, BlockPos p_58214_) {
      int $$2 = Math.min(getEntityCount(p_58213_, TOUCH_AABB.move(p_58214_), Entity.class), this.maxWeight);
      if ($$2 > 0) {
         float $$3 = (float)Math.min(this.maxWeight, $$2) / this.maxWeight;
         return Mth.ceil($$3 * 15.0F);
      } else {
         return 0;
      }
   }

   @Override
   protected int getSignalForState(BlockState p_58220_) {
      return p_58220_.getValue(POWER);
   }

   @Override
   protected BlockState setSignalForState(BlockState p_58208_, int p_58209_) {
      return p_58208_.setValue(POWER, p_58209_);
   }

   @Override
   protected int getPressedTime() {
      return 10;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_58211_) {
      p_58211_.add(POWER);
   }
}
