package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.function.TriConsumer;
import org.jspecify.annotations.Nullable;

public class TransportItemsBetweenContainers extends Behavior<PathfinderMob> {
   public static final int TARGET_INTERACTION_TIME = 60;
   private static final int VISITED_POSITIONS_MEMORY_TIME = 6000;
   private static final int TRANSPORTED_ITEM_MAX_STACK_SIZE = 16;
   private static final int MAX_VISITED_POSITIONS = 10;
   private static final int MAX_UNREACHABLE_POSITIONS = 50;
   private static final int PASSENGER_MOB_TARGET_SEARCH_DISTANCE = 1;
   private static final int IDLE_COOLDOWN = 140;
   private static final double CLOSE_ENOUGH_TO_START_QUEUING_DISTANCE = 3.0;
   private static final double CLOSE_ENOUGH_TO_START_INTERACTING_WITH_TARGET_DISTANCE = 0.5;
   private static final double CLOSE_ENOUGH_TO_START_INTERACTING_WITH_TARGET_PATH_END_DISTANCE = 1.0;
   private static final double CLOSE_ENOUGH_TO_CONTINUE_INTERACTING_WITH_TARGET = 2.0;
   private final float speedModifier;
   private final int horizontalSearchDistance;
   private final int verticalSearchDistance;
   private final Predicate<BlockState> sourceBlockType;
   private final Predicate<BlockState> destinationBlockType;
   private final Predicate<TransportItemsBetweenContainers.TransportItemTarget> shouldQueueForTarget;
   private final Consumer<PathfinderMob> onStartTravelling;
   private final Map<TransportItemsBetweenContainers.ContainerInteractionState, TransportItemsBetweenContainers.OnTargetReachedInteraction> onTargetInteractionActions;
   @Nullable
   private TransportItemsBetweenContainers.TransportItemTarget target = null;
   private TransportItemsBetweenContainers.TransportItemState state;
   @Nullable
   private TransportItemsBetweenContainers.ContainerInteractionState interactionState;
   private int ticksSinceReachingTarget;

   public TransportItemsBetweenContainers(
      float speedModifier,
      Predicate<BlockState> sourceBlockType,
      Predicate<BlockState> destinationBlockType,
      int horizontalSearchDistance,
      int verticalSearchDistance,
      Map<TransportItemsBetweenContainers.ContainerInteractionState, TransportItemsBetweenContainers.OnTargetReachedInteraction> onTargetInteractionActions,
      Consumer<PathfinderMob> onStartTravelling,
      Predicate<TransportItemsBetweenContainers.TransportItemTarget> shouldQueueForTarget
   ) {
      super(
         ImmutableMap.of(
            MemoryModuleType.VISITED_BLOCK_POSITIONS,
            MemoryStatus.REGISTERED,
            MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS,
            MemoryStatus.REGISTERED,
            MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.IS_PANICKING,
            MemoryStatus.VALUE_ABSENT
         )
      );
      this.speedModifier = speedModifier;
      this.sourceBlockType = sourceBlockType;
      this.destinationBlockType = destinationBlockType;
      this.horizontalSearchDistance = horizontalSearchDistance;
      this.verticalSearchDistance = verticalSearchDistance;
      this.onStartTravelling = onStartTravelling;
      this.shouldQueueForTarget = shouldQueueForTarget;
      this.onTargetInteractionActions = onTargetInteractionActions;
      this.state = TransportItemsBetweenContainers.TransportItemState.TRAVELLING;
   }

   protected void start(ServerLevel serverLevel, PathfinderMob pathfinderMob, long l) {
      if (pathfinderMob.getNavigation() instanceof GroundPathNavigation groundPathNavigation) {
         groundPathNavigation.setCanPathToTargetsBelowSurface(true);
      }
   }

   protected boolean checkExtraStartConditions(ServerLevel serverLevel, PathfinderMob pathfinderMob) {
      return !pathfinderMob.isLeashed();
   }

   protected boolean canStillUse(ServerLevel serverLevel, PathfinderMob pathfinderMob, long l) {
      return pathfinderMob.getBrain().getMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS).isEmpty()
         && !pathfinderMob.isPanicking()
         && !pathfinderMob.isLeashed();
   }

   @Override
   protected boolean timedOut(long gameTime) {
      return false;
   }

   protected void tick(ServerLevel serverLevel, PathfinderMob pathfinderMob, long l) {
      boolean bl = this.updateInvalidTarget(serverLevel, pathfinderMob);
      if (this.target == null) {
         this.stop(serverLevel, pathfinderMob, l);
      } else if (!bl) {
         if (this.state.equals(TransportItemsBetweenContainers.TransportItemState.QUEUING)) {
            this.onQueuingForTarget(this.target, serverLevel, pathfinderMob);
         }

         if (this.state.equals(TransportItemsBetweenContainers.TransportItemState.TRAVELLING)) {
            this.onTravelToTarget(this.target, serverLevel, pathfinderMob);
         }

         if (this.state.equals(TransportItemsBetweenContainers.TransportItemState.INTERACTING)) {
            this.onReachedTarget(this.target, serverLevel, pathfinderMob);
         }
      }
   }

   private boolean updateInvalidTarget(ServerLevel level, PathfinderMob mob) {
      if (!this.hasValidTarget(level, mob)) {
         this.stopTargetingCurrentTarget(mob);
         Optional<TransportItemsBetweenContainers.TransportItemTarget> optional = this.getTransportTarget(level, mob);
         if (optional.isPresent()) {
            this.target = optional.get();
            this.onStartTravelling(mob);
            this.setVisitedBlockPos(mob, level, this.target.pos);
            return true;
         } else {
            this.enterCooldownAfterNoMatchingTargetFound(mob);
            return true;
         }
      } else {
         return false;
      }
   }

   private void onQueuingForTarget(TransportItemsBetweenContainers.TransportItemTarget target, Level level, PathfinderMob mob) {
      if (!this.isAnotherMobInteractingWithTarget(target, level)) {
         this.resumeTravelling(mob);
      }
   }

   protected void onTravelToTarget(TransportItemsBetweenContainers.TransportItemTarget target, Level level, PathfinderMob mob) {
      if (this.isWithinTargetDistance(3.0, target, level, mob, this.getCenterPos(mob)) && this.isAnotherMobInteractingWithTarget(target, level)) {
         this.startQueuing(mob);
      } else if (this.isWithinTargetDistance(getInteractionRange(mob), target, level, mob, this.getCenterPos(mob))) {
         this.startOnReachedTargetInteraction(target, mob);
      } else {
         this.walkTowardsTarget(mob);
      }
   }

   private Vec3 getCenterPos(PathfinderMob mob) {
      return this.setMiddleYPosition(mob, mob.position());
   }

   protected void onReachedTarget(TransportItemsBetweenContainers.TransportItemTarget target, Level level, PathfinderMob mob) {
      if (!this.isWithinTargetDistance(2.0, target, level, mob, this.getCenterPos(mob))) {
         this.onStartTravelling(mob);
      } else {
         this.ticksSinceReachingTarget++;
         this.onTargetInteraction(target, mob);
         if (this.ticksSinceReachingTarget >= 60) {
            this.doReachedTargetInteraction(
               mob,
               target.container,
               this::pickUpItems,
               (pathfinderMob2, container) -> this.stopTargetingCurrentTarget(mob),
               this::putDownItem,
               (pathfinderMob2, container) -> this.stopTargetingCurrentTarget(mob)
            );
            this.onStartTravelling(mob);
         }
      }
   }

   private void startQueuing(PathfinderMob mob) {
      this.stopInPlace(mob);
      this.setTransportingState(TransportItemsBetweenContainers.TransportItemState.QUEUING);
   }

   private void resumeTravelling(PathfinderMob mob) {
      this.setTransportingState(TransportItemsBetweenContainers.TransportItemState.TRAVELLING);
      this.walkTowardsTarget(mob);
   }

   private void walkTowardsTarget(PathfinderMob mob) {
      if (this.target != null) {
         BehaviorUtils.setWalkAndLookTargetMemories(mob, this.target.pos, this.speedModifier, 0);
      }
   }

   private void startOnReachedTargetInteraction(TransportItemsBetweenContainers.TransportItemTarget target, PathfinderMob mob) {
      this.doReachedTargetInteraction(
         mob,
         target.container,
         this.onReachedInteraction(TransportItemsBetweenContainers.ContainerInteractionState.PICKUP_ITEM),
         this.onReachedInteraction(TransportItemsBetweenContainers.ContainerInteractionState.PICKUP_NO_ITEM),
         this.onReachedInteraction(TransportItemsBetweenContainers.ContainerInteractionState.PLACE_ITEM),
         this.onReachedInteraction(TransportItemsBetweenContainers.ContainerInteractionState.PLACE_NO_ITEM)
      );
      this.setTransportingState(TransportItemsBetweenContainers.TransportItemState.INTERACTING);
   }

   private void onStartTravelling(PathfinderMob mob) {
      this.onStartTravelling.accept(mob);
      this.setTransportingState(TransportItemsBetweenContainers.TransportItemState.TRAVELLING);
      this.interactionState = null;
      this.ticksSinceReachingTarget = 0;
   }

   private BiConsumer<PathfinderMob, Container> onReachedInteraction(TransportItemsBetweenContainers.ContainerInteractionState interactionState) {
      return (pathfinderMob, container) -> this.setInteractionState(interactionState);
   }

   private void setTransportingState(TransportItemsBetweenContainers.TransportItemState transportingState) {
      this.state = transportingState;
   }

   private void setInteractionState(TransportItemsBetweenContainers.ContainerInteractionState interactionState) {
      this.interactionState = interactionState;
   }

   private void onTargetInteraction(TransportItemsBetweenContainers.TransportItemTarget target, PathfinderMob mob) {
      mob.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(target.pos));
      this.stopInPlace(mob);
      if (this.interactionState != null) {
         Optional.ofNullable(this.onTargetInteractionActions.get(this.interactionState))
            .ifPresent(onTargetReachedInteraction -> onTargetReachedInteraction.accept(mob, target, this.ticksSinceReachingTarget));
      }
   }

   private void doReachedTargetInteraction(
      PathfinderMob mob,
      Container container,
      BiConsumer<PathfinderMob, Container> pickupItem,
      BiConsumer<PathfinderMob, Container> pickupNoItem,
      BiConsumer<PathfinderMob, Container> placeItem,
      BiConsumer<PathfinderMob, Container> placeNoItem
   ) {
      if (isPickingUpItems(mob)) {
         if (matchesGettingItemsRequirement(container)) {
            pickupItem.accept(mob, container);
         } else {
            pickupNoItem.accept(mob, container);
         }
      } else if (matchesLeavingItemsRequirement(mob, container)) {
         placeItem.accept(mob, container);
      } else {
         placeNoItem.accept(mob, container);
      }
   }

   private Optional<TransportItemsBetweenContainers.TransportItemTarget> getTransportTarget(ServerLevel level, PathfinderMob mob) {
      AABB aABB = this.getTargetSearchArea(mob);
      Set<GlobalPos> set = getVisitedPositions(mob);
      Set<GlobalPos> set2 = getUnreachablePositions(mob);
      List<ChunkPos> list = ChunkPos.rangeClosed(new ChunkPos(mob.blockPosition()), Math.floorDiv(this.getHorizontalSearchDistance(mob), 16) + 1).toList();
      TransportItemsBetweenContainers.TransportItemTarget transportItemTarget = null;
      double d = Float.MAX_VALUE;

      for (ChunkPos chunkPos : list) {
         LevelChunk levelChunk = level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
         if (levelChunk != null) {
            for (BlockEntity blockEntity : levelChunk.getBlockEntities().values()) {
               if (blockEntity instanceof ChestBlockEntity chestBlockEntity) {
                  double e = chestBlockEntity.getBlockPos().distToCenterSqr(mob.position());
                  if (e < d) {
                     TransportItemsBetweenContainers.TransportItemTarget transportItemTarget2 = this.isTargetValidToPick(
                        mob, level, chestBlockEntity, set, set2, aABB
                     );
                     if (transportItemTarget2 != null) {
                        transportItemTarget = transportItemTarget2;
                        d = e;
                     }
                  }
               }
            }
         }
      }

      return transportItemTarget == null ? Optional.empty() : Optional.of(transportItemTarget);
   }

   @Nullable
   private TransportItemsBetweenContainers.TransportItemTarget isTargetValidToPick(
      PathfinderMob mob, Level level, BlockEntity blockEntity, Set<GlobalPos> visited, Set<GlobalPos> unreachable, AABB searchArea
   ) {
      BlockPos blockPos = blockEntity.getBlockPos();
      boolean bl = searchArea.contains(blockPos.getX(), blockPos.getY(), blockPos.getZ());
      if (!bl) {
         return null;
      } else {
         TransportItemsBetweenContainers.TransportItemTarget transportItemTarget = TransportItemsBetweenContainers.TransportItemTarget.tryCreatePossibleTarget(
            blockEntity, level
         );
         if (transportItemTarget == null) {
            return null;
         } else {
            boolean bl2 = this.isWantedBlock(mob, transportItemTarget.state)
               && !this.isPositionAlreadyVisited(visited, unreachable, transportItemTarget, level)
               && !this.isContainerLocked(transportItemTarget);
            return bl2 ? transportItemTarget : null;
         }
      }
   }

   private boolean isContainerLocked(TransportItemsBetweenContainers.TransportItemTarget target) {
      return target.blockEntity instanceof BaseContainerBlockEntity baseContainerBlockEntity && baseContainerBlockEntity.isLocked();
   }

   private boolean hasValidTarget(Level level, PathfinderMob mob) {
      boolean bl = this.target != null && this.isWantedBlock(mob, this.target.state) && this.targetHasNotChanged(level, this.target);
      if (bl && !this.isTargetBlocked(level, this.target)) {
         if (!this.state.equals(TransportItemsBetweenContainers.TransportItemState.TRAVELLING)) {
            return true;
         }

         if (this.hasValidTravellingPath(level, this.target, mob)) {
            return true;
         }

         this.markVisitedBlockPosAsUnreachable(mob, level, this.target.pos);
      }

      return false;
   }

   private boolean hasValidTravellingPath(Level level, TransportItemsBetweenContainers.TransportItemTarget target, PathfinderMob mob) {
      Path path = mob.getNavigation().getPath() == null ? mob.getNavigation().createPath(target.pos, 0) : mob.getNavigation().getPath();
      Vec3 vec3 = this.getPositionToReachTargetFrom(path, mob);
      boolean bl = this.isWithinTargetDistance(getInteractionRange(mob), target, level, mob, vec3);
      boolean bl2 = path == null && !bl;
      return bl2 || this.targetIsReachableFromPosition(level, bl, vec3, target, mob);
   }

   private Vec3 getPositionToReachTargetFrom(@Nullable Path path, PathfinderMob mob) {
      boolean bl = path == null || path.getEndNode() == null;
      Vec3 vec3 = bl ? mob.position() : path.getEndNode().asBlockPos().getBottomCenter();
      return this.setMiddleYPosition(mob, vec3);
   }

   private Vec3 setMiddleYPosition(PathfinderMob mob, Vec3 pos) {
      return pos.add(0.0, mob.getBoundingBox().getYsize() / 2.0, 0.0);
   }

   private boolean isTargetBlocked(Level level, TransportItemsBetweenContainers.TransportItemTarget target) {
      return ChestBlock.isChestBlockedAt(level, target.pos);
   }

   private boolean targetHasNotChanged(Level level, TransportItemsBetweenContainers.TransportItemTarget target) {
      return target.blockEntity.equals(level.getBlockEntity(target.pos));
   }

   private Stream<TransportItemsBetweenContainers.TransportItemTarget> getConnectedTargets(
      TransportItemsBetweenContainers.TransportItemTarget target, Level level
   ) {
      if (target.state.getValueOrElse(ChestBlock.TYPE, ChestType.SINGLE) != ChestType.SINGLE) {
         TransportItemsBetweenContainers.TransportItemTarget transportItemTarget = TransportItemsBetweenContainers.TransportItemTarget.tryCreatePossibleTarget(
            ChestBlock.getConnectedBlockPos(target.pos, target.state), level
         );
         return transportItemTarget != null ? Stream.of(target, transportItemTarget) : Stream.of(target);
      } else {
         return Stream.of(target);
      }
   }

   private AABB getTargetSearchArea(PathfinderMob mob) {
      int i = this.getHorizontalSearchDistance(mob);
      return new AABB(mob.blockPosition()).inflate(i, this.getVerticalSearchDistance(mob), i);
   }

   private int getHorizontalSearchDistance(PathfinderMob mob) {
      return mob.isPassenger() ? 1 : this.horizontalSearchDistance;
   }

   private int getVerticalSearchDistance(PathfinderMob mob) {
      return mob.isPassenger() ? 1 : this.verticalSearchDistance;
   }

   private static Set<GlobalPos> getVisitedPositions(PathfinderMob mob) {
      return mob.getBrain().getMemory(MemoryModuleType.VISITED_BLOCK_POSITIONS).orElse(Set.of());
   }

   private static Set<GlobalPos> getUnreachablePositions(PathfinderMob mob) {
      return mob.getBrain().getMemory(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS).orElse(Set.of());
   }

   private boolean isPositionAlreadyVisited(
      Set<GlobalPos> visited, Set<GlobalPos> unreachable, TransportItemsBetweenContainers.TransportItemTarget target, Level level
   ) {
      return this.getConnectedTargets(target, level)
         .map(transportItemTarget -> new GlobalPos(level.dimension(), transportItemTarget.pos))
         .anyMatch(globalPos -> visited.contains(globalPos) || unreachable.contains(globalPos));
   }

   private static boolean hasFinishedPath(PathfinderMob mob) {
      return mob.getNavigation().getPath() != null && mob.getNavigation().getPath().isDone();
   }

   protected void setVisitedBlockPos(PathfinderMob mob, Level level, BlockPos pos) {
      Set<GlobalPos> set = new HashSet<>(getVisitedPositions(mob));
      set.add(new GlobalPos(level.dimension(), pos));
      if (set.size() > 10) {
         this.enterCooldownAfterNoMatchingTargetFound(mob);
      } else {
         mob.getBrain().setMemoryWithExpiry(MemoryModuleType.VISITED_BLOCK_POSITIONS, set, 6000L);
      }
   }

   protected void markVisitedBlockPosAsUnreachable(PathfinderMob mob, Level level, BlockPos pos) {
      Set<GlobalPos> set = new HashSet<>(getVisitedPositions(mob));
      set.remove(new GlobalPos(level.dimension(), pos));
      Set<GlobalPos> set2 = new HashSet<>(getUnreachablePositions(mob));
      set2.add(new GlobalPos(level.dimension(), pos));
      if (set2.size() > 50) {
         this.enterCooldownAfterNoMatchingTargetFound(mob);
      } else {
         mob.getBrain().setMemoryWithExpiry(MemoryModuleType.VISITED_BLOCK_POSITIONS, set, 6000L);
         mob.getBrain().setMemoryWithExpiry(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS, set2, 6000L);
      }
   }

   private boolean isWantedBlock(PathfinderMob mob, BlockState state) {
      return isPickingUpItems(mob) ? this.sourceBlockType.test(state) : this.destinationBlockType.test(state);
   }

   private static double getInteractionRange(PathfinderMob mob) {
      return hasFinishedPath(mob) ? 1.0 : 0.5;
   }

   private boolean isWithinTargetDistance(
      double distance, TransportItemsBetweenContainers.TransportItemTarget target, Level level, PathfinderMob mob, Vec3 center
   ) {
      AABB aABB = mob.getBoundingBox();
      AABB aABB2 = AABB.ofSize(center, aABB.getXsize(), aABB.getYsize(), aABB.getZsize());
      return target.state.getCollisionShape(level, target.pos).bounds().inflate(distance, 0.5, distance).move(target.pos).intersects(aABB2);
   }

   private boolean targetIsReachableFromPosition(
      Level level, boolean withinDistance, Vec3 targetPos, TransportItemsBetweenContainers.TransportItemTarget target, PathfinderMob mob
   ) {
      return withinDistance && this.canSeeAnyTargetSide(target, level, mob, targetPos);
   }

   private boolean canSeeAnyTargetSide(TransportItemsBetweenContainers.TransportItemTarget target, Level level, PathfinderMob mob, Vec3 pos) {
      Vec3 vec3 = target.pos.getCenter();
      return Direction.stream()
         .map(direction -> vec3.add(0.5 * direction.getStepX(), 0.5 * direction.getStepY(), 0.5 * direction.getStepZ()))
         .map(vec32 -> level.clip(new ClipContext(pos, vec32, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob)))
         .anyMatch(blockHitResult -> blockHitResult.getType() == HitResult.Type.BLOCK && blockHitResult.getBlockPos().equals(target.pos));
   }

   private boolean isAnotherMobInteractingWithTarget(TransportItemsBetweenContainers.TransportItemTarget target, Level level) {
      return this.getConnectedTargets(target, level).anyMatch(this.shouldQueueForTarget);
   }

   private static boolean isPickingUpItems(PathfinderMob mob) {
      return mob.getMainHandItem().isEmpty();
   }

   private static boolean matchesGettingItemsRequirement(Container container) {
      return !container.isEmpty();
   }

   private static boolean matchesLeavingItemsRequirement(PathfinderMob mob, Container container) {
      return container.isEmpty() || hasItemMatchingHandItem(mob, container);
   }

   private static boolean hasItemMatchingHandItem(PathfinderMob mob, Container container) {
      ItemStack itemStack = mob.getMainHandItem();

      for (ItemStack itemStack2 : container) {
         if (ItemStack.isSameItem(itemStack2, itemStack)) {
            return true;
         }
      }

      return false;
   }

   private void pickUpItems(PathfinderMob mob, Container container) {
      mob.setItemSlot(EquipmentSlot.MAINHAND, pickupItemFromContainer(container));
      mob.setGuaranteedDrop(EquipmentSlot.MAINHAND);
      container.setChanged();
      this.clearMemoriesAfterMatchingTargetFound(mob);
   }

   private void putDownItem(PathfinderMob mob, Container container) {
      ItemStack itemStack = addItemsToContainer(mob, container);
      container.setChanged();
      mob.setItemSlot(EquipmentSlot.MAINHAND, itemStack);
      if (itemStack.isEmpty()) {
         this.clearMemoriesAfterMatchingTargetFound(mob);
      } else {
         this.stopTargetingCurrentTarget(mob);
      }
   }

   private static ItemStack pickupItemFromContainer(Container container) {
      int i = 0;

      for (ItemStack itemStack : container) {
         if (!itemStack.isEmpty()) {
            int j = Math.min(itemStack.getCount(), 16);
            return container.removeItem(i, j);
         }

         i++;
      }

      return ItemStack.EMPTY;
   }

   private static ItemStack addItemsToContainer(PathfinderMob mob, Container container) {
      int i = 0;
      ItemStack itemStack = mob.getMainHandItem();

      for (ItemStack itemStack2 : container) {
         if (itemStack2.isEmpty()) {
            container.setItem(i, itemStack);
            return ItemStack.EMPTY;
         }

         if (ItemStack.isSameItemSameComponents(itemStack2, itemStack) && itemStack2.getCount() < itemStack2.getMaxStackSize()) {
            int j = itemStack2.getMaxStackSize() - itemStack2.getCount();
            int k = Math.min(j, itemStack.getCount());
            itemStack2.setCount(itemStack2.getCount() + k);
            itemStack.setCount(itemStack.getCount() - j);
            container.setItem(i, itemStack2);
            if (itemStack.isEmpty()) {
               return ItemStack.EMPTY;
            }
         }

         i++;
      }

      return itemStack;
   }

   protected void stopTargetingCurrentTarget(PathfinderMob mob) {
      this.ticksSinceReachingTarget = 0;
      this.target = null;
      mob.getNavigation().stop();
      mob.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
   }

   protected void clearMemoriesAfterMatchingTargetFound(PathfinderMob mob) {
      this.stopTargetingCurrentTarget(mob);
      mob.getBrain().eraseMemory(MemoryModuleType.VISITED_BLOCK_POSITIONS);
      mob.getBrain().eraseMemory(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS);
   }

   private void enterCooldownAfterNoMatchingTargetFound(PathfinderMob mob) {
      this.stopTargetingCurrentTarget(mob);
      mob.getBrain().setMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS, 140);
      mob.getBrain().eraseMemory(MemoryModuleType.VISITED_BLOCK_POSITIONS);
      mob.getBrain().eraseMemory(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS);
   }

   protected void stop(ServerLevel serverLevel, PathfinderMob pathfinderMob, long l) {
      this.onStartTravelling(pathfinderMob);
      if (pathfinderMob.getNavigation() instanceof GroundPathNavigation groundPathNavigation) {
         groundPathNavigation.setCanPathToTargetsBelowSurface(false);
      }
   }

   private void stopInPlace(PathfinderMob mob) {
      mob.getNavigation().stop();
      mob.setXxa(0.0F);
      mob.setYya(0.0F);
      mob.setSpeed(0.0F);
      mob.setDeltaMovement(0.0, mob.getDeltaMovement().y, 0.0);
   }

   public static enum ContainerInteractionState {
      PICKUP_ITEM,
      PICKUP_NO_ITEM,
      PLACE_ITEM,
      PLACE_NO_ITEM;
   }

   @FunctionalInterface
   public interface OnTargetReachedInteraction extends TriConsumer<PathfinderMob, TransportItemsBetweenContainers.TransportItemTarget, Integer> {
   }

   public static enum TransportItemState {
      TRAVELLING,
      QUEUING,
      INTERACTING;
   }

   public record TransportItemTarget(BlockPos pos, Container container, BlockEntity blockEntity, BlockState state) {

      @Nullable
      public static TransportItemsBetweenContainers.TransportItemTarget tryCreatePossibleTarget(BlockEntity blockEntity, Level level) {
         BlockPos blockPos = blockEntity.getBlockPos();
         BlockState blockState = blockEntity.getBlockState();
         Container container = getBlockEntityContainer(blockEntity, blockState, level, blockPos);
         return container != null ? new TransportItemsBetweenContainers.TransportItemTarget(blockPos, container, blockEntity, blockState) : null;
      }

      @Nullable
      public static TransportItemsBetweenContainers.TransportItemTarget tryCreatePossibleTarget(BlockPos pos, Level level) {
         BlockEntity blockEntity = level.getBlockEntity(pos);
         return blockEntity == null ? null : tryCreatePossibleTarget(blockEntity, level);
      }

      @Nullable
      private static Container getBlockEntityContainer(BlockEntity blockEntity, BlockState state, Level level, BlockPos pos) {
         if (state.getBlock() instanceof ChestBlock chestBlock) {
            return ChestBlock.getContainer(chestBlock, state, level, pos, false);
         } else {
            return blockEntity instanceof Container container ? container : null;
         }
      }
   }
}
