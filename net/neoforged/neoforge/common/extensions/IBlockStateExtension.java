package net.neoforged.neoforge.common.extensions;

import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.enums.BubbleColumnDirection;
import net.neoforged.neoforge.event.EventHooks;
import org.jspecify.annotations.Nullable;

public interface IBlockStateExtension {
   private BlockState self() {
      return (BlockState)this;
   }

   default float getFriction(LevelReader level, BlockPos pos, @Nullable Entity entity) {
      return this.self().getBlock().getFriction(this.self(), level, pos, entity);
   }

   default boolean hasDynamicLightEmission() {
      return this.self().getBlock().hasDynamicLightEmission(this.self());
   }

   default int getLightEmission(BlockGetter level, BlockPos pos) {
      return this.self().getBlock().getLightEmission(this.self(), level, pos);
   }

   default boolean ignitedByLava(BlockGetter level, BlockPos pos, Direction face) {
      return this.self().getBlock().ignitedByLava(this.self(), level, pos, face);
   }

   default boolean isLadder(LevelReader level, BlockPos pos, LivingEntity entity) {
      return this.self().getBlock().isLadder(this.self(), level, pos, entity);
   }

   default boolean canHarvestBlock(BlockGetter level, BlockPos pos, Player player) {
      return this.self().getBlock().canHarvestBlock(this.self(), level, pos, player);
   }

   default boolean onDestroyedByPlayer(Level level, BlockPos pos, Player player, ItemStack toolStack, boolean willHarvest, FluidState fluid) {
      return this.self().getBlock().onDestroyedByPlayer(this.self(), level, pos, player, toolStack, willHarvest, fluid);
   }

   default void onDestroyedByPushReaction(Level level, BlockPos pos, Direction pushDirection, FluidState fluid) {
      this.self().getBlock().onDestroyedByPushReaction(this.self(), level, pos, pushDirection, fluid);
   }

   default boolean isBed(BlockGetter level, BlockPos pos, LivingEntity sleeper) {
      return this.self().getBlock().isBed(this.self(), level, pos, sleeper);
   }

   default Optional<ServerPlayer.RespawnPosAngle> getRespawnPosition(EntityType<?> type, LevelReader level, BlockPos pos, float orientation) {
      return this.self().getBlock().getRespawnPosition(this.self(), type, level, pos, orientation);
   }

   default void setBedOccupied(Level level, BlockPos pos, LivingEntity sleeper, boolean occupied) {
      this.self().getBlock().setBedOccupied(this.self(), level, pos, sleeper, occupied);
   }

   default Direction getBedDirection(LevelReader level, BlockPos pos) {
      return this.self().getBlock().getBedDirection(this.self(), level, pos);
   }

   default float getExplosionResistance(BlockGetter level, BlockPos pos, Explosion explosion) {
      return this.self().getBlock().getExplosionResistance(this.self(), level, pos, explosion);
   }

   default ItemStack getCloneItemStack(BlockPos pos, LevelReader level, boolean includeData, Player player) {
      return this.self().getBlock().getCloneItemStack(level, pos, this.self(), includeData, player);
   }

   default boolean addLandingEffects(ServerLevel level, BlockPos pos, BlockState state2, LivingEntity entity, int numberOfParticles) {
      return this.self().getBlock().addLandingEffects(this.self(), level, pos, state2, entity, numberOfParticles);
   }

   default boolean addRunningEffects(Level level, BlockPos pos, Entity entity) {
      return this.self().getBlock().addRunningEffects(this.self(), level, pos, entity);
   }

   default void playFallSound(Level level, BlockPos pos, LivingEntity entity) {
      this.self().getBlock().playFallSound(this.self(), level, pos, entity);
   }

   default void playStepSound(Level level, BlockPos pos, Entity entity, float volumeMultiplier, float pitchMultiplier) {
      this.self().getBlock().playStepSound(this.self(), level, pos, entity, volumeMultiplier, pitchMultiplier);
   }

   default TriState canSustainPlant(BlockGetter level, BlockPos soilPosition, Direction facing, BlockState plant) {
      return this.self().getBlock().canSustainPlant(this.self(), level, soilPosition, facing, plant);
   }

   default boolean onTreeGrow(
      LevelReader level, BiConsumer<BlockPos, BlockState> placeFunction, RandomSource randomSource, BlockPos pos, TreeConfiguration config
   ) {
      return this.self().getBlock().onTreeGrow(this.self(), level, placeFunction, randomSource, pos, config);
   }

   default boolean isFertile(BlockGetter level, BlockPos pos) {
      return this.self().getBlock().isFertile(this.self(), level, pos);
   }

   default boolean isConduitFrame(LevelReader level, BlockPos pos, BlockPos conduit) {
      return this.self().getBlock().isConduitFrame(this.self(), level, pos, conduit);
   }

   default boolean isPortalFrame(BlockGetter level, BlockPos pos) {
      return this.self().getBlock().isPortalFrame(this.self(), level, pos);
   }

   default int getExpDrop(LevelAccessor level, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable Entity breaker, ItemStack tool) {
      return this.self().getBlock().getExpDrop(this.self(), level, pos, blockEntity, breaker, tool);
   }

   default BlockState rotate(LevelAccessor level, BlockPos pos, Rotation direction) {
      return this.self().getBlock().rotate(this.self(), level, pos, direction);
   }

   default float getEnchantPowerBonus(LevelReader level, BlockPos pos) {
      return this.self().getBlock().getEnchantPowerBonus(this.self(), level, pos);
   }

   default void onNeighborChange(LevelReader level, BlockPos pos, BlockPos neighbor) {
      this.self().getBlock().onNeighborChange(this.self(), level, pos, neighbor);
   }

   default boolean shouldCheckWeakPower(SignalGetter level, BlockPos pos, Direction side) {
      return this.self().getBlock().shouldCheckWeakPower(this.self(), level, pos, side);
   }

   default boolean getWeakChanges(LevelReader level, BlockPos pos) {
      return this.self().getBlock().getWeakChanges(this.self(), level, pos);
   }

   default SoundType getSoundType(LevelReader level, BlockPos pos, @Nullable Entity entity) {
      return this.self().getBlock().getSoundType(this.self(), level, pos, entity);
   }

   @Nullable
   default Integer getBeaconColorMultiplier(LevelReader level, BlockPos pos, BlockPos beacon) {
      return this.self().getBlock().getBeaconColorMultiplier(this.self(), level, pos, beacon);
   }

   default BlockState getStateAtViewpoint(BlockGetter level, BlockPos pos, Vec3 viewpoint) {
      return this.self().getBlock().getStateAtViewpoint(this.self(), level, pos, viewpoint);
   }

   default boolean isSlimeBlock() {
      return this.self().getBlock().isSlimeBlock(this.self());
   }

   default boolean isStickyBlock() {
      return this.self().getBlock().isStickyBlock(this.self());
   }

   default boolean canStickTo(BlockState other) {
      return this.self().getBlock().canStickTo(this.self(), other);
   }

   default int getFlammability(BlockGetter level, BlockPos pos, Direction face) {
      return this.self().getBlock().getFlammability(this.self(), level, pos, face);
   }

   default boolean isFlammable(BlockGetter level, BlockPos pos, Direction face) {
      return this.self().getBlock().isFlammable(this.self(), level, pos, face);
   }

   default boolean onCaughtFire(Level level, BlockPos pos, @Nullable Direction face, @Nullable LivingEntity igniter) {
      return this.self().getBlock().onCaughtFire(this.self(), level, pos, face, igniter);
   }

   default int getFireSpreadSpeed(BlockGetter level, BlockPos pos, Direction face) {
      return this.self().getBlock().getFireSpreadSpeed(this.self(), level, pos, face);
   }

   default boolean isFireSource(LevelReader level, BlockPos pos, Direction side) {
      return this.self().getBlock().isFireSource(this.self(), level, pos, side);
   }

   default boolean canEntityDestroy(BlockGetter level, BlockPos pos, Entity entity) {
      return this.self().getBlock().canEntityDestroy(this.self(), level, pos, entity);
   }

   default boolean isBurning(BlockGetter level, BlockPos pos) {
      return this.self().getBlock().isBurning(this.self(), level, pos);
   }

   @Nullable
   default PathType getBlockPathType(BlockGetter level, BlockPos pos, @Nullable Mob mob) {
      return this.self().getBlock().getBlockPathType(this.self(), level, pos, mob);
   }

   @Nullable
   default PathType getAdjacentBlockPathType(BlockGetter level, BlockPos pos, @Nullable Mob mob, PathType originalType) {
      return this.self().getBlock().getAdjacentBlockPathType(this.self(), level, pos, mob, originalType);
   }

   default boolean canDropFromExplosion(BlockGetter level, BlockPos pos, Explosion explosion) {
      return this.self().getBlock().canDropFromExplosion(this.self(), level, pos, explosion);
   }

   default void onBlockExploded(ServerLevel level, BlockPos pos, Explosion explosion) {
      this.self().getBlock().onBlockExploded(this.self(), level, pos, explosion);
   }

   default boolean collisionExtendsVertically(BlockGetter level, BlockPos pos, Entity collidingEntity) {
      return this.self().getBlock().collisionExtendsVertically(this.self(), level, pos, collidingEntity);
   }

   default boolean shouldDisplayFluidOverlay(BlockAndTintGetter level, BlockPos pos, FluidState fluidState) {
      return this.self().getBlock().shouldDisplayFluidOverlay(this.self(), level, pos, fluidState);
   }

   @Nullable
   default BlockState getToolModifiedState(UseOnContext context, ItemAbility itemAbility, boolean simulate) {
      BlockState eventState = EventHooks.onToolUse(this.self(), context, itemAbility, simulate);
      return eventState != this.self() ? eventState : this.self().getBlock().getToolModifiedState(this.self(), context, itemAbility, simulate);
   }

   default boolean isScaffolding(LivingEntity entity) {
      return this.self().getBlock().isScaffolding(this.self(), entity.level(), entity.blockPosition(), entity);
   }

   default boolean canRedstoneConnectTo(BlockGetter level, BlockPos pos, @Nullable Direction direction) {
      return this.self().getBlock().canConnectRedstone(this.self(), level, pos, direction);
   }

   default boolean hidesNeighborFace(BlockGetter level, BlockPos pos, BlockState neighborState, Direction dir) {
      return this.self().getBlock().hidesNeighborFace(level, pos, this.self(), neighborState, dir);
   }

   default boolean supportsExternalFaceHiding() {
      return this.self().getBlock().supportsExternalFaceHiding(this.self());
   }

   default void onBlockStateChange(LevelReader level, BlockPos pos, BlockState oldState) {
      this.self().getBlock().onBlockStateChange(level, pos, oldState, this.self());
   }

   default boolean canBeHydrated(BlockGetter getter, BlockPos pos, FluidState fluid, BlockPos fluidPos) {
      return this.self().getBlock().canBeHydrated(this.self(), getter, pos, fluid, fluidPos);
   }

   default BlockState getAppearance(BlockAndTintGetter level, BlockPos pos, Direction side, @Nullable BlockState queryState, @Nullable BlockPos queryPos) {
      return this.self().getBlock().getAppearance(this.self(), level, pos, side, queryState, queryPos);
   }

   default boolean isEmpty() {
      return this.self().getBlock().isEmpty(this.self());
   }

   default BubbleColumnDirection getBubbleColumnDirection() {
      return this.self().getBlock().getBubbleColumnDirection(this.self());
   }

   default boolean shouldHideAdjacentFluidFace(Direction selfFace, FluidState adjacentFluid) {
      return this.self().getBlock().shouldHideAdjacentFluidFace(this.self(), selfFace, adjacentFluid);
   }
}
