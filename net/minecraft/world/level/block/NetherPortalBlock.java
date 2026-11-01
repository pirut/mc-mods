package net.minecraft.world.level.block;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.BlockUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class NetherPortalBlock extends Block implements Portal {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final MapCodec<NetherPortalBlock> CODEC = simpleCodec(NetherPortalBlock::new);
   public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
   private static final Map<Direction.Axis, VoxelShape> SHAPES = Shapes.rotateHorizontalAxis(Block.column(4.0, 16.0, 0.0, 16.0));

   @Override
   public MapCodec<NetherPortalBlock> codec() {
      return CODEC;
   }

   public NetherPortalBlock(BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPES.get(state.getValue(AXIS));
   }

   @Override
   protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      if (level.isSpawningMonsters()
         && level.environmentAttributes().getValue(EnvironmentAttributes.NETHER_PORTAL_SPAWNS_PIGLINS, pos)
         && random.nextInt(2000) < level.getDifficulty().getId()
         && level.anyPlayerCloseEnoughForSpawning(pos)) {
         while (level.getBlockState(pos).is(this)) {
            pos = pos.below();
         }

         if (level.getBlockState(pos).isValidSpawn(level, pos, EntityType.ZOMBIFIED_PIGLIN)) {
            Entity entity = EntityType.ZOMBIFIED_PIGLIN.spawn(level, pos.above(), EntitySpawnReason.STRUCTURE);
            if (entity != null) {
               entity.setPortalCooldown();
               Entity entity2 = entity.getVehicle();
               if (entity2 != null) {
                  entity2.setPortalCooldown();
               }
            }
         }
      }
   }

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
      Direction.Axis axis = direction.getAxis();
      Direction.Axis axis2 = state.getValue(AXIS);
      boolean bl = axis2 != axis && axis.isHorizontal();
      return !bl && !neighborState.is(this) && !PortalShape.findAnyShape(level, pos, axis2).isComplete()
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
   }

   @Override
   protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier applier, boolean intersects) {
      if (entity.canUsePortal(false)) {
         entity.setAsInsidePortal(this, pos);
      }
   }

   @Override
   public int getPortalTransitionTime(ServerLevel level, Entity entity) {
      return entity instanceof Player player
         ? Math.max(
            0,
            level.getGameRules()
               .get(player.getAbilities().invulnerable ? GameRules.PLAYERS_NETHER_PORTAL_CREATIVE_DELAY : GameRules.PLAYERS_NETHER_PORTAL_DEFAULT_DELAY)
         )
         : 0;
   }

   @Nullable
   @Override
   public TeleportTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
      ResourceKey<Level> resourceKey = level.dimension() == Level.NETHER ? Level.OVERWORLD : Level.NETHER;
      ServerLevel serverLevel = level.getServer().getLevel(resourceKey);
      if (serverLevel == null) {
         return null;
      } else {
         boolean bl = serverLevel.dimension() == Level.NETHER;
         WorldBorder worldBorder = serverLevel.getWorldBorder();
         double d = DimensionType.getTeleportationScale(level.dimensionType(), serverLevel.dimensionType());
         BlockPos blockPos = worldBorder.clampToBounds(entity.getX() * d, entity.getY(), entity.getZ() * d);
         return this.getExitPortal(serverLevel, entity, pos, blockPos, bl, worldBorder);
      }
   }

   @Nullable
   private TeleportTransition getExitPortal(ServerLevel level, Entity entity, BlockPos pos, BlockPos exitPos, boolean isNether, WorldBorder worldBorder) {
      Optional<BlockPos> optional = level.getPortalForcer().findClosestPortalPosition(exitPos, isNether, worldBorder);
      BlockUtil.FoundRectangle foundRectangle;
      TeleportTransition.PostTeleportTransition postTeleportTransition;
      if (optional.isPresent()) {
         BlockPos blockPos = optional.get();
         BlockState blockState = level.getBlockState(blockPos);
         foundRectangle = BlockUtil.getLargestRectangleAround(
            blockPos,
            blockState.getValue(BlockStateProperties.HORIZONTAL_AXIS),
            21,
            Direction.Axis.Y,
            21,
            blockPosx -> level.getBlockState(blockPosx) == blockState
         );
         postTeleportTransition = TeleportTransition.PLAY_PORTAL_SOUND.then(entityx -> entityx.placePortalTicket(blockPos));
      } else {
         Direction.Axis axis = entity.level().getBlockState(pos).getOptionalValue(AXIS).orElse(Direction.Axis.X);
         Optional<BlockUtil.FoundRectangle> optional2 = level.getPortalForcer().createPortal(exitPos, axis);
         if (optional2.isEmpty()) {
            LOGGER.error("Unable to create a portal, likely target out of worldborder");
            return null;
         }

         foundRectangle = optional2.get();
         postTeleportTransition = TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET);
      }

      return getDimensionTransitionFromExit(entity, pos, foundRectangle, level, postTeleportTransition);
   }

   private static TeleportTransition getDimensionTransitionFromExit(
      Entity entity, BlockPos pos, BlockUtil.FoundRectangle rectangle, ServerLevel level, TeleportTransition.PostTeleportTransition postTeleportTransition
   ) {
      BlockState blockState = entity.level().getBlockState(pos);
      Direction.Axis axis;
      Vec3 vec3;
      if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
         axis = blockState.getValue(BlockStateProperties.HORIZONTAL_AXIS);
         BlockUtil.FoundRectangle foundRectangle = BlockUtil.getLargestRectangleAround(
            pos, axis, 21, Direction.Axis.Y, 21, blockPos -> entity.level().getBlockState(blockPos) == blockState
         );
         vec3 = entity.getRelativePortalPosition(axis, foundRectangle);
      } else {
         axis = Direction.Axis.X;
         vec3 = new Vec3(0.5, 0.0, 0.0);
      }

      return createDimensionTransition(level, rectangle, axis, vec3, entity, postTeleportTransition);
   }

   private static TeleportTransition createDimensionTransition(
      ServerLevel level,
      BlockUtil.FoundRectangle rectangle,
      Direction.Axis axis,
      Vec3 offset,
      Entity entity,
      TeleportTransition.PostTeleportTransition postTeleportTransition
   ) {
      BlockPos blockPos = rectangle.minCorner;
      BlockState blockState = level.getBlockState(blockPos);
      Direction.Axis axis2 = blockState.getOptionalValue(BlockStateProperties.HORIZONTAL_AXIS).orElse(Direction.Axis.X);
      double d = rectangle.axis1Size;
      double e = rectangle.axis2Size;
      EntityDimensions entityDimensions = entity.getDimensions(entity.getPose());
      int i = axis == axis2 ? 0 : 90;
      double f = entityDimensions.width() / 2.0 + (d - entityDimensions.width()) * offset.x();
      double g = (e - entityDimensions.height()) * offset.y();
      double h = 0.5 + offset.z();
      boolean bl = axis2 == Direction.Axis.X;
      Vec3 vec3 = new Vec3(blockPos.getX() + (bl ? f : h), blockPos.getY() + g, blockPos.getZ() + (bl ? h : f));
      Vec3 vec32 = PortalShape.findCollisionFreePosition(vec3, level, entity, entityDimensions);
      return new TeleportTransition(level, vec32, Vec3.ZERO, i, 0.0F, Relative.union(Relative.DELTA, Relative.ROTATION), postTeleportTransition);
   }

   @Override
   public Portal.Transition getLocalTransition() {
      return Portal.Transition.CONFUSION;
   }

   @Override
   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
      if (random.nextInt(100) == 0) {
         level.playLocalSound(
            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.5F, random.nextFloat() * 0.4F + 0.8F, false
         );
      }

      for (int i = 0; i < 4; i++) {
         double d = pos.getX() + random.nextDouble();
         double e = pos.getY() + random.nextDouble();
         double f = pos.getZ() + random.nextDouble();
         double g = (random.nextFloat() - 0.5) * 0.5;
         double h = (random.nextFloat() - 0.5) * 0.5;
         double j = (random.nextFloat() - 0.5) * 0.5;
         int k = random.nextInt(2) * 2 - 1;
         if (!level.getBlockState(pos.west()).is(this) && !level.getBlockState(pos.east()).is(this)) {
            d = pos.getX() + 0.5 + 0.25 * k;
            g = random.nextFloat() * 2.0F * k;
         } else {
            f = pos.getZ() + 0.5 + 0.25 * k;
            j = random.nextFloat() * 2.0F * k;
         }

         level.addParticle(ParticleTypes.PORTAL, d, e, f, g, h, j);
      }
   }

   @Override
   protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
      return ItemStack.EMPTY;
   }

   @Override
   protected BlockState rotate(BlockState state, Rotation rotation) {
      switch (rotation) {
         case COUNTERCLOCKWISE_90:
         case CLOCKWISE_90:
            switch ((Direction.Axis)state.getValue(AXIS)) {
               case X:
                  return state.setValue(AXIS, Direction.Axis.Z);
               case Z:
                  return state.setValue(AXIS, Direction.Axis.X);
               default:
                  return state;
            }
         default:
            return state;
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(AXIS);
   }
}
