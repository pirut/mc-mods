package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SporeBlossomBlock extends Block {
   public static final MapCodec<SporeBlossomBlock> CODEC = simpleCodec(SporeBlossomBlock::new);
   private static final VoxelShape SHAPE = Block.box(2.0, 13.0, 2.0, 14.0, 16.0, 14.0);
   private static final int ADD_PARTICLE_ATTEMPTS = 14;
   private static final int PARTICLE_XZ_RADIUS = 10;
   private static final int PARTICLE_Y_MAX = 10;

   @Override
   public MapCodec<SporeBlossomBlock> codec() {
      return CODEC;
   }

   public SporeBlossomBlock(BlockBehaviour.Properties p_154697_) {
      super(p_154697_);
   }

   @Override
   protected boolean canSurvive(BlockState p_154709_, LevelReader p_154710_, BlockPos p_154711_) {
      return Block.canSupportCenter(p_154710_, p_154711_.above(), Direction.DOWN) && !p_154710_.isWaterAt(p_154711_);
   }

   @Override
   protected BlockState updateShape(
      BlockState p_154713_, Direction p_154714_, BlockState p_154715_, LevelAccessor p_154716_, BlockPos p_154717_, BlockPos p_154718_
   ) {
      return p_154714_ == Direction.UP && !this.canSurvive(p_154713_, p_154716_, p_154717_)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(p_154713_, p_154714_, p_154715_, p_154716_, p_154717_, p_154718_);
   }

   @Override
   public void animateTick(BlockState p_222503_, Level p_222504_, BlockPos p_222505_, RandomSource p_222506_) {
      int $$4 = p_222505_.getX();
      int $$5 = p_222505_.getY();
      int $$6 = p_222505_.getZ();
      double $$7 = $$4 + p_222506_.nextDouble();
      double $$8 = $$5 + 0.7;
      double $$9 = $$6 + p_222506_.nextDouble();
      p_222504_.addParticle(ParticleTypes.FALLING_SPORE_BLOSSOM, $$7, $$8, $$9, 0.0, 0.0, 0.0);
      BlockPos.MutableBlockPos $$10 = new BlockPos.MutableBlockPos();

      for (int $$11 = 0; $$11 < 14; $$11++) {
         $$10.set($$4 + Mth.nextInt(p_222506_, -10, 10), $$5 - p_222506_.nextInt(10), $$6 + Mth.nextInt(p_222506_, -10, 10));
         BlockState $$12 = p_222504_.getBlockState($$10);
         if (!$$12.isCollisionShapeFullBlock(p_222504_, $$10)) {
            p_222504_.addParticle(
               ParticleTypes.SPORE_BLOSSOM_AIR,
               $$10.getX() + p_222506_.nextDouble(),
               $$10.getY() + p_222506_.nextDouble(),
               $$10.getZ() + p_222506_.nextDouble(),
               0.0,
               0.0,
               0.0
            );
         }
      }
   }

   @Override
   protected VoxelShape getShape(BlockState p_154699_, BlockGetter p_154700_, BlockPos p_154701_, CollisionContext p_154702_) {
      return SHAPE;
   }
}
