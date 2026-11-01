package net.minecraft.world.level.block;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;

public class NetherPortalBlock extends Block implements Portal {
   public static final MapCodec<NetherPortalBlock> CODEC = simpleCodec(NetherPortalBlock::new);
   public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
   private static final Logger LOGGER = LogUtils.getLogger();
   protected static final int AABB_OFFSET = 2;
   protected static final VoxelShape X_AXIS_AABB = Block.box(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);
   protected static final VoxelShape Z_AXIS_AABB = Block.box(6.0, 0.0, 0.0, 10.0, 16.0, 16.0);

   @Override
   public MapCodec<NetherPortalBlock> codec() {
      return CODEC;
   }

   public NetherPortalBlock(BlockBehaviour.Properties p_54909_) {
      super(p_54909_);
      this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
   }

   @Override
   protected VoxelShape getShape(BlockState p_54942_, BlockGetter p_54943_, BlockPos p_54944_, CollisionContext p_54945_) {
      switch ((Direction.Axis)p_54942_.getValue(AXIS)) {
         case Z:
            return Z_AXIS_AABB;
         case X:
         default:
            return X_AXIS_AABB;
      }
   }

   @Override
   protected void randomTick(BlockState p_221799_, ServerLevel p_221800_, BlockPos p_221801_, RandomSource p_221802_) {
      if (p_221800_.dimensionType().natural()
         && p_221800_.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)
         && p_221802_.nextInt(2000) < p_221800_.getDifficulty().getId()) {
         while (p_221800_.getBlockState(p_221801_).is(this)) {
            p_221801_ = p_221801_.below();
         }

         if (p_221800_.getBlockState(p_221801_).isValidSpawn(p_221800_, p_221801_, EntityType.ZOMBIFIED_PIGLIN)) {
            Entity $$4 = EntityType.ZOMBIFIED_PIGLIN.spawn(p_221800_, p_221801_.above(), MobSpawnType.STRUCTURE);
            if ($$4 != null) {
               $$4.setPortalCooldown();
            }
         }
      }
   }

   @Override
   protected BlockState updateShape(BlockState p_54928_, Direction p_54929_, BlockState p_54930_, LevelAccessor p_54931_, BlockPos p_54932_, BlockPos p_54933_) {
      Direction.Axis $$6 = p_54929_.getAxis();
      Direction.Axis $$7 = p_54928_.getValue(AXIS);
      boolean $$8 = $$7 != $$6 && $$6.isHorizontal();
      return !$$8 && !p_54930_.is(this) && !new PortalShape(p_54931_, p_54932_, $$7).isComplete()
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(p_54928_, p_54929_, p_54930_, p_54931_, p_54932_, p_54933_);
   }

   @Override
   protected void entityInside(BlockState p_54915_, Level p_54916_, BlockPos p_54917_, Entity p_54918_) {
      if (p_54918_.canUsePortal(false)) {
         p_54918_.setAsInsidePortal(this, p_54917_);
      }
   }

   @Override
   public int getPortalTransitionTime(ServerLevel p_350689_, Entity p_350280_) {
      return p_350280_ instanceof Player $$2
         ? Math.max(
            1,
            p_350689_.getGameRules()
               .getInt(
                  $$2.getAbilities().invulnerable ? GameRules.RULE_PLAYERS_NETHER_PORTAL_CREATIVE_DELAY : GameRules.RULE_PLAYERS_NETHER_PORTAL_DEFAULT_DELAY
               )
         )
         : 0;
   }

   @Nullable
   @Override
   public DimensionTransition getPortalDestination(ServerLevel p_350444_, Entity p_350334_, BlockPos p_350764_) {
      ResourceKey<Level> $$3 = p_350444_.dimension() == Level.NETHER ? Level.OVERWORLD : Level.NETHER;
      ServerLevel $$4 = p_350444_.getServer().getLevel($$3);
      if ($$4 == null) {
         return null;
      } else {
         boolean $$5 = $$4.dimension() == Level.NETHER;
         WorldBorder $$6 = $$4.getWorldBorder();
         double $$7 = DimensionType.getTeleportationScale(p_350444_.dimensionType(), $$4.dimensionType());
         BlockPos $$8 = $$6.clampToBounds(p_350334_.getX() * $$7, p_350334_.getY(), p_350334_.getZ() * $$7);
         return this.getExitPortal($$4, p_350334_, p_350764_, $$8, $$5, $$6);
      }
   }

   @Nullable
   private DimensionTransition getExitPortal(
      ServerLevel p_350564_, Entity p_350493_, BlockPos p_350379_, BlockPos p_350747_, boolean p_350326_, WorldBorder p_350718_
   ) {
      Optional<BlockPos> $$6 = p_350564_.getPortalForcer().findClosestPortalPosition(p_350747_, p_350326_, p_350718_);
      BlockUtil.FoundRectangle $$9;
      DimensionTransition.PostDimensionTransition $$10;
      if ($$6.isPresent()) {
         BlockPos $$7 = $$6.get();
         BlockState $$8 = p_350564_.getBlockState($$7);
         $$9 = BlockUtil.getLargestRectangleAround(
            $$7, $$8.getValue(BlockStateProperties.HORIZONTAL_AXIS), 21, Direction.Axis.Y, 21, p_351970_ -> p_350564_.getBlockState(p_351970_) == $$8
         );
         $$10 = DimensionTransition.PLAY_PORTAL_SOUND.then(p_351967_ -> p_351967_.placePortalTicket($$7));
      } else {
         Direction.Axis $$11 = p_350493_.level().getBlockState(p_350379_).getOptionalValue(AXIS).orElse(Direction.Axis.X);
         Optional<BlockUtil.FoundRectangle> $$12 = p_350564_.getPortalForcer().createPortal(p_350747_, $$11);
         if ($$12.isEmpty()) {
            LOGGER.error("Unable to create a portal, likely target out of worldborder");
            return null;
         }

         $$9 = $$12.get();
         $$10 = DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET);
      }

      return getDimensionTransitionFromExit(p_350493_, p_350379_, $$9, p_350564_, $$10);
   }

   private static DimensionTransition getDimensionTransitionFromExit(
      Entity p_350906_, BlockPos p_350376_, BlockUtil.FoundRectangle p_350428_, ServerLevel p_350928_, DimensionTransition.PostDimensionTransition p_352093_
   ) {
      BlockState $$5 = p_350906_.level().getBlockState(p_350376_);
      Direction.Axis $$6;
      Vec3 $$8;
      if ($$5.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
         $$6 = $$5.getValue(BlockStateProperties.HORIZONTAL_AXIS);
         BlockUtil.FoundRectangle $$7 = BlockUtil.getLargestRectangleAround(
            p_350376_, $$6, 21, Direction.Axis.Y, 21, p_351016_ -> p_350906_.level().getBlockState(p_351016_) == $$5
         );
         $$8 = p_350906_.getRelativePortalPosition($$6, $$7);
      } else {
         $$6 = Direction.Axis.X;
         $$8 = new Vec3(0.5, 0.0, 0.0);
      }

      return createDimensionTransition(
         p_350928_, p_350428_, $$6, $$8, p_350906_, p_350906_.getDeltaMovement(), p_350906_.getYRot(), p_350906_.getXRot(), p_352093_
      );
   }

   private static DimensionTransition createDimensionTransition(
      ServerLevel p_350955_,
      BlockUtil.FoundRectangle p_350865_,
      Direction.Axis p_351013_,
      Vec3 p_351020_,
      Entity p_350578_,
      Vec3 p_350266_,
      float p_350648_,
      float p_350338_,
      DimensionTransition.PostDimensionTransition p_352441_
   ) {
      BlockPos $$9 = p_350865_.minCorner;
      BlockState $$10 = p_350955_.getBlockState($$9);
      Direction.Axis $$11 = $$10.getOptionalValue(BlockStateProperties.HORIZONTAL_AXIS).orElse(Direction.Axis.X);
      double $$12 = p_350865_.axis1Size;
      double $$13 = p_350865_.axis2Size;
      EntityDimensions $$14 = p_350578_.getDimensions(p_350578_.getPose());
      int $$15 = p_351013_ == $$11 ? 0 : 90;
      Vec3 $$16 = p_351013_ == $$11 ? p_350266_ : new Vec3(p_350266_.z, p_350266_.y, -p_350266_.x);
      double $$17 = $$14.width() / 2.0 + ($$12 - $$14.width()) * p_351020_.x();
      double $$18 = ($$13 - $$14.height()) * p_351020_.y();
      double $$19 = 0.5 + p_351020_.z();
      boolean $$20 = $$11 == Direction.Axis.X;
      Vec3 $$21 = new Vec3($$9.getX() + ($$20 ? $$17 : $$19), $$9.getY() + $$18, $$9.getZ() + ($$20 ? $$19 : $$17));
      Vec3 $$22 = PortalShape.findCollisionFreePosition($$21, p_350955_, p_350578_, $$14);
      return new DimensionTransition(p_350955_, $$22, $$16, p_350648_ + $$15, p_350338_, p_352441_);
   }

   @Override
   public Portal.Transition getLocalTransition() {
      return Portal.Transition.CONFUSION;
   }

   @Override
   public void animateTick(BlockState p_221794_, Level p_221795_, BlockPos p_221796_, RandomSource p_221797_) {
      if (p_221797_.nextInt(100) == 0) {
         p_221795_.playLocalSound(
            p_221796_.getX() + 0.5,
            p_221796_.getY() + 0.5,
            p_221796_.getZ() + 0.5,
            SoundEvents.PORTAL_AMBIENT,
            SoundSource.BLOCKS,
            0.5F,
            p_221797_.nextFloat() * 0.4F + 0.8F,
            false
         );
      }

      for (int $$4 = 0; $$4 < 4; $$4++) {
         double $$5 = p_221796_.getX() + p_221797_.nextDouble();
         double $$6 = p_221796_.getY() + p_221797_.nextDouble();
         double $$7 = p_221796_.getZ() + p_221797_.nextDouble();
         double $$8 = (p_221797_.nextFloat() - 0.5) * 0.5;
         double $$9 = (p_221797_.nextFloat() - 0.5) * 0.5;
         double $$10 = (p_221797_.nextFloat() - 0.5) * 0.5;
         int $$11 = p_221797_.nextInt(2) * 2 - 1;
         if (!p_221795_.getBlockState(p_221796_.west()).is(this) && !p_221795_.getBlockState(p_221796_.east()).is(this)) {
            $$5 = p_221796_.getX() + 0.5 + 0.25 * $$11;
            $$8 = p_221797_.nextFloat() * 2.0F * $$11;
         } else {
            $$7 = p_221796_.getZ() + 0.5 + 0.25 * $$11;
            $$10 = p_221797_.nextFloat() * 2.0F * $$11;
         }

         p_221795_.addParticle(ParticleTypes.PORTAL, $$5, $$6, $$7, $$8, $$9, $$10);
      }
   }

   @Override
   public ItemStack getCloneItemStack(LevelReader p_304402_, BlockPos p_54912_, BlockState p_54913_) {
      return ItemStack.EMPTY;
   }

   @Override
   protected BlockState rotate(BlockState p_54925_, Rotation p_54926_) {
      switch (p_54926_) {
         case COUNTERCLOCKWISE_90:
         case CLOCKWISE_90:
            switch ((Direction.Axis)p_54925_.getValue(AXIS)) {
               case Z:
                  return p_54925_.setValue(AXIS, Direction.Axis.X);
               case X:
                  return p_54925_.setValue(AXIS, Direction.Axis.Z);
               default:
                  return p_54925_;
            }
         default:
            return p_54925_;
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_54935_) {
      p_54935_.add(AXIS);
   }
}
