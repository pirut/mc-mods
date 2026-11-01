package net.minecraft.world.level.block;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AttachedStemBlock extends VegetationBlock {
   public static final MapCodec<AttachedStemBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_432600_ -> p_432600_.group(
            ResourceKey.codec(Registries.BLOCK).fieldOf("fruit").forGetter(p_304711_ -> p_304711_.fruit),
            ResourceKey.codec(Registries.BLOCK).fieldOf("stem").forGetter(p_304975_ -> p_304975_.stem),
            ResourceKey.codec(Registries.ITEM).fieldOf("seed").forGetter(p_304775_ -> p_304775_.seed),
            propertiesCodec()
         )
         .apply(p_432600_, AttachedStemBlock::new)
   );
   public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
   private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Block.boxZ(4.0, 0.0, 10.0, 0.0, 10.0));
   private final ResourceKey<Block> fruit;
   private final ResourceKey<Block> stem;
   private final ResourceKey<Item> seed;

   @Override
   public MapCodec<AttachedStemBlock> codec() {
      return CODEC;
   }

   public AttachedStemBlock(ResourceKey<Block> p_304582_, ResourceKey<Block> p_304450_, ResourceKey<Item> p_304828_, BlockBehaviour.Properties p_152062_) {
      super(p_152062_);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
      this.stem = p_304582_;
      this.fruit = p_304450_;
      this.seed = p_304828_;
   }

   @Override
   protected VoxelShape getShape(BlockState p_48858_, BlockGetter p_48859_, BlockPos p_48860_, CollisionContext p_48861_) {
      return SHAPES.get(p_48858_.getValue(FACING));
   }

   @Override
   protected BlockState updateShape(
      BlockState p_48848_,
      LevelReader p_374143_,
      ScheduledTickAccess p_374241_,
      BlockPos p_48852_,
      Direction p_48849_,
      BlockPos p_48853_,
      BlockState p_48850_,
      RandomSource p_374410_
   ) {
      if (!p_48850_.is(this.fruit) && p_48849_ == p_48848_.getValue(FACING)) {
         Optional<Block> optional = p_374143_.registryAccess().lookupOrThrow(Registries.BLOCK).getOptional(this.stem);
         if (optional.isPresent()) {
            return optional.get().defaultBlockState().trySetValue(StemBlock.AGE, 7);
         }
      }

      return super.updateShape(p_48848_, p_374143_, p_374241_, p_48852_, p_48849_, p_48853_, p_48850_, p_374410_);
   }

   @Override
   protected boolean mayPlaceOn(BlockState p_48863_, BlockGetter p_48864_, BlockPos p_48865_) {
      return p_48863_.getBlock() instanceof FarmBlock;
   }

   @Override
   protected ItemStack getCloneItemStack(LevelReader p_304950_, BlockPos p_48839_, BlockState p_48840_, boolean p_388189_) {
      return new ItemStack((ItemLike)DataFixUtils.orElse(p_304950_.registryAccess().lookupOrThrow(Registries.ITEM).getOptional(this.seed), this));
   }

   @Override
   protected BlockState rotate(BlockState p_48845_, Rotation p_48846_) {
      return p_48845_.setValue(FACING, p_48846_.rotate(p_48845_.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState p_48842_, Mirror p_48843_) {
      return p_48842_.rotate(p_48843_.getRotation(p_48842_.getValue(FACING)));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_48855_) {
      p_48855_.add(FACING);
   }
}
