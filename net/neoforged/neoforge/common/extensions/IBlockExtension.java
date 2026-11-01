package net.neoforged.neoforge.common.extensions;

import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.common.DataMapHooks;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.enums.BubbleColumnDirection;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

public interface IBlockExtension {
   private Block self() {
      return (Block)this;
   }

   default float getFriction(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
      return this.self().getFriction();
   }

   default boolean hasDynamicLightEmission(BlockState state) {
      return false;
   }

   default int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
      return state.getLightEmission();
   }

   default boolean ignitedByLava(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
      return state.ignitedByLava();
   }

   default boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
      return state.is(BlockTags.CLIMBABLE);
   }

   default boolean makesOpenTrapdoorAboveClimbable(BlockState state, LevelReader level, BlockPos pos, BlockState trapdoorState) {
      return state.getBlock() instanceof LadderBlock && state.getValue(LadderBlock.FACING) == trapdoorState.getValue(TrapDoorBlock.FACING);
   }

   default boolean isBurning(BlockState state, BlockGetter level, BlockPos pos) {
      return this == Blocks.FIRE || this == Blocks.LAVA;
   }

   default boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
      return EventHooks.doPlayerHarvestCheck(player, state, level, pos);
   }

   default boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
      return level.isClientSide() ? level.setBlock(pos, fluid.createLegacyBlock(), 11) : level.removeBlock(pos, false);
   }

   default void onDestroyedByPushReaction(BlockState state, Level level, BlockPos pos, Direction pushDirection, FluidState fluid) {
      level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
      level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(state));
   }

   default boolean isBed(BlockState state, BlockGetter level, BlockPos pos, LivingEntity sleeper) {
      return this.self() instanceof BedBlock;
   }

   default Optional<ServerPlayer.RespawnPosAngle> getRespawnPosition(
      BlockState state, EntityType<?> type, LevelReader levelReader, BlockPos pos, float orientation
   ) {
      return Optional.empty();
   }

   default void setBedOccupied(BlockState state, Level level, BlockPos pos, LivingEntity sleeper, boolean occupied) {
      level.setBlock(pos, state.setValue(BedBlock.OCCUPIED, occupied), 3);
   }

   default Direction getBedDirection(BlockState state, LevelReader level, BlockPos pos) {
      return state.getValue(HorizontalDirectionalBlock.FACING);
   }

   default float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
      return this.self().getExplosionResistance();
   }

   default ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
      return this.self().getCloneItemStack(level, pos, state);
   }

   default boolean addLandingEffects(BlockState state1, ServerLevel level, BlockPos pos, BlockState state2, LivingEntity entity, int numberOfParticles) {
      return false;
   }

   default boolean addRunningEffects(BlockState state, Level level, BlockPos pos, Entity entity) {
      return false;
   }

   default TriState canSustainPlant(BlockState state, BlockGetter level, BlockPos soilPosition, Direction facing, BlockState plant) {
      return TriState.DEFAULT;
   }

   default boolean onTreeGrow(
      BlockState state, LevelReader level, BiConsumer<BlockPos, BlockState> placeFunction, RandomSource randomSource, BlockPos pos, TreeConfiguration config
   ) {
      return false;
   }

   default boolean isFertile(BlockState state, BlockGetter level, BlockPos pos) {
      return state.getBlock() instanceof FarmBlock ? state.getValue(FarmBlock.MOISTURE) > 0 : false;
   }

   default boolean isConduitFrame(BlockState state, LevelReader level, BlockPos pos, BlockPos conduit) {
      return state.getBlock() == Blocks.PRISMARINE
         || state.getBlock() == Blocks.PRISMARINE_BRICKS
         || state.getBlock() == Blocks.SEA_LANTERN
         || state.getBlock() == Blocks.DARK_PRISMARINE;
   }

   default boolean isPortalFrame(BlockState state, BlockGetter level, BlockPos pos) {
      return state.is(Blocks.OBSIDIAN);
   }

   default int getExpDrop(BlockState state, LevelAccessor level, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable Entity breaker, ItemStack tool) {
      return 0;
   }

   default BlockState rotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation direction) {
      return state.rotate(direction);
   }

   default float getEnchantPowerBonus(BlockState state, LevelReader level, BlockPos pos) {
      return state.is(BlockTags.ENCHANTMENT_POWER_PROVIDER) ? 1.0F : 0.0F;
   }

   default void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {
   }

   default boolean shouldCheckWeakPower(BlockState state, SignalGetter level, BlockPos pos, Direction side) {
      return state.isRedstoneConductor(level, pos);
   }

   default boolean getWeakChanges(BlockState state, LevelReader level, BlockPos pos) {
      return false;
   }

   default SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
      return state.getSoundType();
   }

   @Nullable
   default Integer getBeaconColorMultiplier(BlockState state, LevelReader level, BlockPos pos, BlockPos beaconPos) {
      return this.self() instanceof BeaconBeamBlock ? ((BeaconBeamBlock)this.self()).getColor().getTextureDiffuseColor() : null;
   }

   default BlockState getStateAtViewpoint(BlockState state, BlockGetter level, BlockPos pos, Vec3 viewpoint) {
      return state;
   }

   @Nullable
   default PathType getBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob) {
      return state.getBlock() == Blocks.LAVA ? PathType.LAVA : (state.isBurning(level, pos) ? PathType.DAMAGE_FIRE : null);
   }

   @Nullable
   default PathType getAdjacentBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob, PathType originalType) {
      if (state.is(Blocks.SWEET_BERRY_BUSH)) {
         return PathType.DANGER_OTHER;
      } else {
         return WalkNodeEvaluator.isBurningBlock(state) ? PathType.DANGER_FIRE : null;
      }
   }

   default boolean isSlimeBlock(BlockState state) {
      return state.getBlock() == Blocks.SLIME_BLOCK;
   }

   default boolean isStickyBlock(BlockState state) {
      return state.getBlock() == Blocks.SLIME_BLOCK || state.getBlock() == Blocks.HONEY_BLOCK;
   }

   default boolean canStickTo(BlockState state, BlockState other) {
      if (state.getBlock() == Blocks.HONEY_BLOCK && other.getBlock() == Blocks.SLIME_BLOCK) {
         return false;
      } else {
         return state.getBlock() == Blocks.SLIME_BLOCK && other.getBlock() == Blocks.HONEY_BLOCK ? false : state.isStickyBlock() || other.isStickyBlock();
      }
   }

   default int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
      return ((FireBlock)Blocks.FIRE).getBurnOdds(state);
   }

   default boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
      return state.getFlammability(level, pos, direction) > 0;
   }

   default void onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction direction, @Nullable LivingEntity igniter) {
   }

   default int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
      return ((FireBlock)Blocks.FIRE).getIgniteOdds(state);
   }

   default boolean isFireSource(BlockState state, LevelReader level, BlockPos pos, Direction direction) {
      return state.is(level.dimensionType().infiniburn());
   }

   default boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
      if (entity instanceof EnderDragon) {
         return !this.self().defaultBlockState().is(BlockTags.DRAGON_IMMUNE);
      } else {
         return !(entity instanceof WitherBoss) && !(entity instanceof WitherSkull) ? true : state.isAir() || WitherBoss.canDestroy(state);
      }
   }

   default boolean canDropFromExplosion(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
      return state.getBlock().dropFromExplosion(explosion);
   }

   default void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
      level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
      this.self().wasExploded(level, pos, explosion);
   }

   default boolean collisionExtendsVertically(BlockState state, BlockGetter level, BlockPos pos, Entity collidingEntity) {
      return state.is(BlockTags.FENCES) || state.is(BlockTags.WALLS) || this.self() instanceof FenceGateBlock;
   }

   default boolean shouldDisplayFluidOverlay(BlockState state, BlockAndTintGetter level, BlockPos pos, FluidState fluidState) {
      return state.getBlock() instanceof HalfTransparentBlock || state.getBlock() instanceof LeavesBlock;
   }

   @Nullable
   default BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility itemAbility, boolean simulate) {
      ItemStack itemStack = context.getItemInHand();
      if (!itemStack.canPerformAction(itemAbility)) {
         return null;
      } else if (ItemAbilities.AXE_STRIP == itemAbility) {
         return AxeItem.getAxeStrippingState(state);
      } else if (ItemAbilities.AXE_SCRAPE == itemAbility) {
         return WeatheringCopper.getPrevious(state).orElse(null);
      } else if (ItemAbilities.AXE_WAX_OFF == itemAbility) {
         Block waxOffBlock = DataMapHooks.getBlockUnwaxed(state.getBlock());
         return Optional.ofNullable(waxOffBlock).map(block -> block.withPropertiesOf(state)).orElse(null);
      } else if (ItemAbilities.SHOVEL_FLATTEN == itemAbility) {
         return ShovelItem.getShovelPathingState(state);
      } else {
         if (ItemAbilities.HOE_TILL == itemAbility) {
            Block block = state.getBlock();
            if (block == Blocks.ROOTED_DIRT) {
               if (!simulate && !context.getLevel().isClientSide) {
                  Block.popResourceFromFace(context.getLevel(), context.getClickedPos(), context.getClickedFace(), new ItemStack(Items.HANGING_ROOTS));
               }

               return Blocks.DIRT.defaultBlockState();
            }

            if ((block == Blocks.GRASS_BLOCK || block == Blocks.DIRT_PATH || block == Blocks.DIRT || block == Blocks.COARSE_DIRT)
               && context.getLevel().getBlockState(context.getClickedPos().above()).isAir()) {
               return block == Blocks.COARSE_DIRT ? Blocks.DIRT.defaultBlockState() : Blocks.FARMLAND.defaultBlockState();
            }
         } else if (ItemAbilities.SHEARS_TRIM == itemAbility) {
            if (state.getBlock() instanceof GrowingPlantHeadBlock growingPlant && !growingPlant.isMaxAge(state)) {
               if (!simulate) {
                  context.getLevel().playSound(context.getPlayer(), context.getClickedPos(), SoundEvents.GROWING_PLANT_CROP, SoundSource.BLOCKS, 1.0F, 1.0F);
               }

               return growingPlant.getMaxAgeState(state);
            }
         } else if (ItemAbilities.SHOVEL_DOUSE == itemAbility) {
            if (state.getBlock() instanceof CampfireBlock && state.getValue(CampfireBlock.LIT)) {
               if (!simulate) {
                  CampfireBlock.dowse(context.getPlayer(), context.getLevel(), context.getClickedPos(), state);
               }

               return state.setValue(CampfireBlock.LIT, false);
            }
         } else if (ItemAbilities.FIRESTARTER_LIGHT == itemAbility
            && (CampfireBlock.canLight(state) || CandleBlock.canLight(state) || CandleCakeBlock.canLight(state))) {
            return state.setValue(BlockStateProperties.LIT, true);
         }

         return null;
      }
   }

   default boolean isScaffolding(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
      return state.is(Blocks.SCAFFOLDING);
   }

   default boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
      if (state.is(Blocks.REDSTONE_WIRE)) {
         return true;
      } else if (state.is(Blocks.REPEATER)) {
         Direction facing = state.getValue(RepeaterBlock.FACING);
         return facing == direction || facing.getOpposite() == direction;
      } else {
         return state.is(Blocks.OBSERVER) ? direction == state.getValue(ObserverBlock.FACING) : state.isSignalSource() && direction != null;
      }
   }

   default boolean hidesNeighborFace(BlockGetter level, BlockPos pos, BlockState state, BlockState neighborState, Direction dir) {
      return false;
   }

   default boolean supportsExternalFaceHiding(BlockState state) {
      return FMLEnvironment.dist.isClient() ? !ClientHooks.isBlockInSolidLayer(state) : true;
   }

   default void onBlockStateChange(LevelReader level, BlockPos pos, BlockState oldState, BlockState newState) {
   }

   default boolean canBeHydrated(BlockState state, BlockGetter getter, BlockPos pos, FluidState fluid, BlockPos fluidPos) {
      return fluid.canHydrate(getter, fluidPos, state, pos);
   }

   default MapColor getMapColor(BlockState state, BlockGetter level, BlockPos pos, MapColor defaultColor) {
      return defaultColor;
   }

   default BlockState getAppearance(
      BlockState state, BlockAndTintGetter level, BlockPos pos, Direction side, @Nullable BlockState queryState, @Nullable BlockPos queryPos
   ) {
      return state;
   }

   @Nullable
   default PushReaction getPistonPushReaction(BlockState state) {
      return null;
   }

   default boolean isEmpty(BlockState state) {
      return state.is(Blocks.AIR) || state.is(Blocks.CAVE_AIR) || state.is(Blocks.VOID_AIR);
   }

   default BubbleColumnDirection getBubbleColumnDirection(BlockState state) {
      if (state.is(Blocks.SOUL_SAND)) {
         return BubbleColumnDirection.UPWARD;
      } else {
         return state.is(Blocks.MAGMA_BLOCK) ? BubbleColumnDirection.DOWNWARD : BubbleColumnDirection.NONE;
      }
   }

   default boolean shouldHideAdjacentFluidFace(BlockState state, Direction selfFace, FluidState adjacentFluid) {
      return state.getFluidState().getType().isSame(adjacentFluid.getType());
   }
}
