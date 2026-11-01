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
      float p_433946_,
      Predicate<BlockState> p_433301_,
      Predicate<BlockState> p_433636_,
      int p_435457_,
      int p_434005_,
      Map<TransportItemsBetweenContainers.ContainerInteractionState, TransportItemsBetweenContainers.OnTargetReachedInteraction> p_433911_,
      Consumer<PathfinderMob> p_435307_,
      Predicate<TransportItemsBetweenContainers.TransportItemTarget> p_434953_
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
      this.speedModifier = p_433946_;
      this.sourceBlockType = p_433301_;
      this.destinationBlockType = p_433636_;
      this.horizontalSearchDistance = p_435457_;
      this.verticalSearchDistance = p_434005_;
      this.onStartTravelling = p_435307_;
      this.shouldQueueForTarget = p_434953_;
      this.onTargetInteractionActions = p_433911_;
      this.state = TransportItemsBetweenContainers.TransportItemState.TRAVELLING;
   }

   protected void start(ServerLevel p_451160_, PathfinderMob p_451431_, long p_451257_) {
      if (p_451431_.getNavigation() instanceof GroundPathNavigation groundpathnavigation) {
         groundpathnavigation.setCanPathToTargetsBelowSurface(true);
      }
   }

   protected boolean checkExtraStartConditions(ServerLevel p_443051_, PathfinderMob p_443527_) {
      return !p_443527_.isLeashed();
   }

   protected boolean canStillUse(ServerLevel p_435408_, PathfinderMob p_435654_, long p_433103_) {
      return p_435654_.getBrain().getMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS).isEmpty() && !p_435654_.isPanicking() && !p_435654_.isLeashed();
   }

   @Override
   protected boolean timedOut(long p_434291_) {
      return false;
   }

   protected void tick(ServerLevel p_434496_, PathfinderMob p_434166_, long p_434326_) {
      boolean flag = this.updateInvalidTarget(p_434496_, p_434166_);
      if (this.target == null) {
         this.stop(p_434496_, p_434166_, p_434326_);
      } else if (!flag) {
         if (this.state.equals(TransportItemsBetweenContainers.TransportItemState.QUEUING)) {
            this.onQueuingForTarget(this.target, p_434496_, p_434166_);
         }

         if (this.state.equals(TransportItemsBetweenContainers.TransportItemState.TRAVELLING)) {
            this.onTravelToTarget(this.target, p_434496_, p_434166_);
         }

         if (this.state.equals(TransportItemsBetweenContainers.TransportItemState.INTERACTING)) {
            this.onReachedTarget(this.target, p_434496_, p_434166_);
         }
      }
   }

   private boolean updateInvalidTarget(ServerLevel p_451270_, PathfinderMob p_451177_) {
      if (!this.hasValidTarget(p_451270_, p_451177_)) {
         this.stopTargetingCurrentTarget(p_451177_);
         Optional<TransportItemsBetweenContainers.TransportItemTarget> optional = this.getTransportTarget(p_451270_, p_451177_);
         if (optional.isPresent()) {
            this.target = optional.get();
            this.onStartTravelling(p_451177_);
            this.setVisitedBlockPos(p_451177_, p_451270_, this.target.pos);
            return true;
         } else {
            this.enterCooldownAfterNoMatchingTargetFound(p_451177_);
            return true;
         }
      } else {
         return false;
      }
   }

   private void onQueuingForTarget(TransportItemsBetweenContainers.TransportItemTarget p_433776_, Level p_433945_, PathfinderMob p_435369_) {
      if (!this.isAnotherMobInteractingWithTarget(p_433776_, p_433945_)) {
         this.resumeTravelling(p_435369_);
      }
   }

   protected void onTravelToTarget(TransportItemsBetweenContainers.TransportItemTarget p_433041_, Level p_433021_, PathfinderMob p_434894_) {
      if (this.isWithinTargetDistance(3.0, p_433041_, p_433021_, p_434894_, this.getCenterPos(p_434894_))
         && this.isAnotherMobInteractingWithTarget(p_433041_, p_433021_)) {
         this.startQueuing(p_434894_);
      } else if (this.isWithinTargetDistance(getInteractionRange(p_434894_), p_433041_, p_433021_, p_434894_, this.getCenterPos(p_434894_))) {
         this.startOnReachedTargetInteraction(p_433041_, p_434894_);
      } else {
         this.walkTowardsTarget(p_434894_);
      }
   }

   private Vec3 getCenterPos(PathfinderMob p_451452_) {
      return this.setMiddleYPosition(p_451452_, p_451452_.position());
   }

   protected void onReachedTarget(TransportItemsBetweenContainers.TransportItemTarget p_433814_, Level p_449662_, PathfinderMob p_434405_) {
      if (!this.isWithinTargetDistance(2.0, p_433814_, p_449662_, p_434405_, this.getCenterPos(p_434405_))) {
         this.onStartTravelling(p_434405_);
      } else {
         this.ticksSinceReachingTarget++;
         this.onTargetInteraction(p_433814_, p_434405_);
         if (this.ticksSinceReachingTarget >= 60) {
            this.doReachedTargetInteraction(
               p_434405_,
               p_433814_.container,
               this::pickUpItems,
               (p_442454_, p_442455_) -> this.stopTargetingCurrentTarget(p_434405_),
               this::putDownItem,
               (p_442440_, p_442441_) -> this.stopTargetingCurrentTarget(p_434405_)
            );
            this.onStartTravelling(p_434405_);
         }
      }
   }

   private void startQueuing(PathfinderMob p_434906_) {
      this.stopInPlace(p_434906_);
      this.setTransportingState(TransportItemsBetweenContainers.TransportItemState.QUEUING);
   }

   private void resumeTravelling(PathfinderMob p_435235_) {
      this.setTransportingState(TransportItemsBetweenContainers.TransportItemState.TRAVELLING);
      this.walkTowardsTarget(p_435235_);
   }

   private void walkTowardsTarget(PathfinderMob p_435919_) {
      if (this.target != null) {
         BehaviorUtils.setWalkAndLookTargetMemories(p_435919_, this.target.pos, this.speedModifier, 0);
      }
   }

   private void startOnReachedTargetInteraction(TransportItemsBetweenContainers.TransportItemTarget p_432845_, PathfinderMob p_434355_) {
      this.doReachedTargetInteraction(
         p_434355_,
         p_432845_.container,
         this.onReachedInteraction(TransportItemsBetweenContainers.ContainerInteractionState.PICKUP_ITEM),
         this.onReachedInteraction(TransportItemsBetweenContainers.ContainerInteractionState.PICKUP_NO_ITEM),
         this.onReachedInteraction(TransportItemsBetweenContainers.ContainerInteractionState.PLACE_ITEM),
         this.onReachedInteraction(TransportItemsBetweenContainers.ContainerInteractionState.PLACE_NO_ITEM)
      );
      this.setTransportingState(TransportItemsBetweenContainers.TransportItemState.INTERACTING);
   }

   private void onStartTravelling(PathfinderMob p_433258_) {
      this.onStartTravelling.accept(p_433258_);
      this.setTransportingState(TransportItemsBetweenContainers.TransportItemState.TRAVELLING);
      this.interactionState = null;
      this.ticksSinceReachingTarget = 0;
   }

   private BiConsumer<PathfinderMob, Container> onReachedInteraction(TransportItemsBetweenContainers.ContainerInteractionState p_435290_) {
      return (p_435151_, p_432961_) -> this.setInteractionState(p_435290_);
   }

   private void setTransportingState(TransportItemsBetweenContainers.TransportItemState p_433359_) {
      this.state = p_433359_;
   }

   private void setInteractionState(TransportItemsBetweenContainers.ContainerInteractionState p_434516_) {
      this.interactionState = p_434516_;
   }

   private void onTargetInteraction(TransportItemsBetweenContainers.TransportItemTarget p_433013_, PathfinderMob p_435148_) {
      p_435148_.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(p_433013_.pos));
      this.stopInPlace(p_435148_);
      if (this.interactionState != null) {
         Optional.ofNullable(this.onTargetInteractionActions.get(this.interactionState))
            .ifPresent(p_435805_ -> p_435805_.accept(p_435148_, p_433013_, this.ticksSinceReachingTarget));
      }
   }

   private void doReachedTargetInteraction(
      PathfinderMob p_436066_,
      Container p_432855_,
      BiConsumer<PathfinderMob, Container> p_435841_,
      BiConsumer<PathfinderMob, Container> p_433858_,
      BiConsumer<PathfinderMob, Container> p_435793_,
      BiConsumer<PathfinderMob, Container> p_433255_
   ) {
      if (isPickingUpItems(p_436066_)) {
         if (matchesGettingItemsRequirement(p_432855_)) {
            p_435841_.accept(p_436066_, p_432855_);
         } else {
            p_433858_.accept(p_436066_, p_432855_);
         }
      } else if (matchesLeavingItemsRequirement(p_436066_, p_432855_)) {
         p_435793_.accept(p_436066_, p_432855_);
      } else {
         p_433255_.accept(p_436066_, p_432855_);
      }
   }

   private Optional<TransportItemsBetweenContainers.TransportItemTarget> getTransportTarget(ServerLevel p_451641_, PathfinderMob p_451643_) {
      AABB aabb = this.getTargetSearchArea(p_451643_);
      Set<GlobalPos> set = getVisitedPositions(p_451643_);
      Set<GlobalPos> set1 = getUnreachablePositions(p_451643_);
      List<ChunkPos> list = ChunkPos.rangeClosed(new ChunkPos(p_451643_.blockPosition()), Math.floorDiv(this.getHorizontalSearchDistance(p_451643_), 16) + 1)
         .toList();
      TransportItemsBetweenContainers.TransportItemTarget transportitemsbetweencontainers$transportitemtarget = null;
      double d0 = Float.MAX_VALUE;

      for (ChunkPos chunkpos : list) {
         LevelChunk levelchunk = p_451641_.getChunkSource().getChunkNow(chunkpos.x, chunkpos.z);
         if (levelchunk != null) {
            for (BlockEntity blockentity : levelchunk.getBlockEntities().values()) {
               if (blockentity instanceof ChestBlockEntity chestblockentity) {
                  double d1 = chestblockentity.getBlockPos().distToCenterSqr(p_451643_.position());
                  if (d1 < d0) {
                     TransportItemsBetweenContainers.TransportItemTarget transportitemsbetweencontainers$transportitemtarget1 = this.isTargetValidToPick(
                        p_451643_, p_451641_, chestblockentity, set, set1, aabb
                     );
                     if (transportitemsbetweencontainers$transportitemtarget1 != null) {
                        transportitemsbetweencontainers$transportitemtarget = transportitemsbetweencontainers$transportitemtarget1;
                        d0 = d1;
                     }
                  }
               }
            }
         }
      }

      return transportitemsbetweencontainers$transportitemtarget == null ? Optional.empty() : Optional.of(transportitemsbetweencontainers$transportitemtarget);
   }

   @Nullable
   private TransportItemsBetweenContainers.TransportItemTarget isTargetValidToPick(
      PathfinderMob p_435648_, Level p_435734_, BlockEntity p_435156_, Set<GlobalPos> p_434939_, Set<GlobalPos> p_443604_, AABB p_435218_
   ) {
      BlockPos blockpos = p_435156_.getBlockPos();
      boolean flag = p_435218_.contains(blockpos.getX(), blockpos.getY(), blockpos.getZ());
      if (!flag) {
         return null;
      } else {
         TransportItemsBetweenContainers.TransportItemTarget transportitemsbetweencontainers$transportitemtarget = TransportItemsBetweenContainers.TransportItemTarget.tryCreatePossibleTarget(
            p_435156_, p_435734_
         );
         if (transportitemsbetweencontainers$transportitemtarget == null) {
            return null;
         } else {
            boolean flag1 = this.isWantedBlock(p_435648_, transportitemsbetweencontainers$transportitemtarget.state)
               && !this.isPositionAlreadyVisited(p_434939_, p_443604_, transportitemsbetweencontainers$transportitemtarget, p_435734_)
               && !this.isContainerLocked(transportitemsbetweencontainers$transportitemtarget);
            return flag1 ? transportitemsbetweencontainers$transportitemtarget : null;
         }
      }
   }

   private boolean isContainerLocked(TransportItemsBetweenContainers.TransportItemTarget p_447171_) {
      return p_447171_.blockEntity instanceof BaseContainerBlockEntity basecontainerblockentity && basecontainerblockentity.isLocked();
   }

   private boolean hasValidTarget(Level p_433204_, PathfinderMob p_433430_) {
      boolean flag = this.target != null && this.isWantedBlock(p_433430_, this.target.state) && this.targetHasNotChanged(p_433204_, this.target);
      if (flag && !this.isTargetBlocked(p_433204_, this.target)) {
         if (!this.state.equals(TransportItemsBetweenContainers.TransportItemState.TRAVELLING)) {
            return true;
         }

         if (this.hasValidTravellingPath(p_433204_, this.target, p_433430_)) {
            return true;
         }

         this.markVisitedBlockPosAsUnreachable(p_433430_, p_433204_, this.target.pos);
      }

      return false;
   }

   private boolean hasValidTravellingPath(Level p_451651_, TransportItemsBetweenContainers.TransportItemTarget p_451653_, PathfinderMob p_451654_) {
      Path path = p_451654_.getNavigation().getPath() == null ? p_451654_.getNavigation().createPath(p_451653_.pos, 0) : p_451654_.getNavigation().getPath();
      Vec3 vec3 = this.getPositionToReachTargetFrom(path, p_451654_);
      boolean flag = this.isWithinTargetDistance(getInteractionRange(p_451654_), p_451653_, p_451651_, p_451654_, vec3);
      boolean flag1 = path == null && !flag;
      return flag1 || this.targetIsReachableFromPosition(p_451651_, flag, vec3, p_451653_, p_451654_);
   }

   private Vec3 getPositionToReachTargetFrom(@Nullable Path p_451256_, PathfinderMob p_451073_) {
      boolean flag = p_451256_ == null || p_451256_.getEndNode() == null;
      Vec3 vec3 = flag ? p_451073_.position() : p_451256_.getEndNode().asBlockPos().getBottomCenter();
      return this.setMiddleYPosition(p_451073_, vec3);
   }

   private Vec3 setMiddleYPosition(PathfinderMob p_451195_, Vec3 p_451526_) {
      return p_451526_.add(0.0, p_451195_.getBoundingBox().getYsize() / 2.0, 0.0);
   }

   private boolean isTargetBlocked(Level p_449979_, TransportItemsBetweenContainers.TransportItemTarget p_449522_) {
      return ChestBlock.isChestBlockedAt(p_449979_, p_449522_.pos);
   }

   private boolean targetHasNotChanged(Level p_435073_, TransportItemsBetweenContainers.TransportItemTarget p_435138_) {
      return p_435138_.blockEntity.equals(p_435073_.getBlockEntity(p_435138_.pos));
   }

   private Stream<TransportItemsBetweenContainers.TransportItemTarget> getConnectedTargets(
      TransportItemsBetweenContainers.TransportItemTarget p_435822_, Level p_434746_
   ) {
      if (p_435822_.state.getValueOrElse(ChestBlock.TYPE, ChestType.SINGLE) != ChestType.SINGLE) {
         TransportItemsBetweenContainers.TransportItemTarget transportitemsbetweencontainers$transportitemtarget = TransportItemsBetweenContainers.TransportItemTarget.tryCreatePossibleTarget(
            ChestBlock.getConnectedBlockPos(p_435822_.pos, p_435822_.state), p_434746_
         );
         return transportitemsbetweencontainers$transportitemtarget != null
            ? Stream.of(p_435822_, transportitemsbetweencontainers$transportitemtarget)
            : Stream.of(p_435822_);
      } else {
         return Stream.of(p_435822_);
      }
   }

   private AABB getTargetSearchArea(PathfinderMob p_435860_) {
      int i = this.getHorizontalSearchDistance(p_435860_);
      return new AABB(p_435860_.blockPosition()).inflate(i, this.getVerticalSearchDistance(p_435860_), i);
   }

   private int getHorizontalSearchDistance(PathfinderMob p_451047_) {
      return p_451047_.isPassenger() ? 1 : this.horizontalSearchDistance;
   }

   private int getVerticalSearchDistance(PathfinderMob p_451124_) {
      return p_451124_.isPassenger() ? 1 : this.verticalSearchDistance;
   }

   private static Set<GlobalPos> getVisitedPositions(PathfinderMob p_434809_) {
      return p_434809_.getBrain().getMemory(MemoryModuleType.VISITED_BLOCK_POSITIONS).orElse(Set.of());
   }

   private static Set<GlobalPos> getUnreachablePositions(PathfinderMob p_442818_) {
      return p_442818_.getBrain().getMemory(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS).orElse(Set.of());
   }

   private boolean isPositionAlreadyVisited(
      Set<GlobalPos> p_433143_, Set<GlobalPos> p_442905_, TransportItemsBetweenContainers.TransportItemTarget p_435407_, Level p_433931_
   ) {
      return this.getConnectedTargets(p_435407_, p_433931_)
         .map(p_433162_ -> new GlobalPos(p_433931_.dimension(), p_433162_.pos))
         .anyMatch(p_442446_ -> p_433143_.contains(p_442446_) || p_442905_.contains(p_442446_));
   }

   private static boolean hasFinishedPath(PathfinderMob p_443529_) {
      return p_443529_.getNavigation().getPath() != null && p_443529_.getNavigation().getPath().isDone();
   }

   protected void setVisitedBlockPos(PathfinderMob p_434023_, Level p_432762_, BlockPos p_435339_) {
      Set<GlobalPos> set = new HashSet<>(getVisitedPositions(p_434023_));
      set.add(new GlobalPos(p_432762_.dimension(), p_435339_));
      if (set.size() > 10) {
         this.enterCooldownAfterNoMatchingTargetFound(p_434023_);
      } else {
         p_434023_.getBrain().setMemoryWithExpiry(MemoryModuleType.VISITED_BLOCK_POSITIONS, set, 6000L);
      }
   }

   protected void markVisitedBlockPosAsUnreachable(PathfinderMob p_443174_, Level p_443406_, BlockPos p_442631_) {
      Set<GlobalPos> set = new HashSet<>(getVisitedPositions(p_443174_));
      set.remove(new GlobalPos(p_443406_.dimension(), p_442631_));
      Set<GlobalPos> set1 = new HashSet<>(getUnreachablePositions(p_443174_));
      set1.add(new GlobalPos(p_443406_.dimension(), p_442631_));
      if (set1.size() > 50) {
         this.enterCooldownAfterNoMatchingTargetFound(p_443174_);
      } else {
         p_443174_.getBrain().setMemoryWithExpiry(MemoryModuleType.VISITED_BLOCK_POSITIONS, set, 6000L);
         p_443174_.getBrain().setMemoryWithExpiry(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS, set1, 6000L);
      }
   }

   private boolean isWantedBlock(PathfinderMob p_432985_, BlockState p_434277_) {
      return isPickingUpItems(p_432985_) ? this.sourceBlockType.test(p_434277_) : this.destinationBlockType.test(p_434277_);
   }

   private static double getInteractionRange(PathfinderMob p_442889_) {
      return hasFinishedPath(p_442889_) ? 1.0 : 0.5;
   }

   private boolean isWithinTargetDistance(
      double p_435568_, TransportItemsBetweenContainers.TransportItemTarget p_432956_, Level p_433160_, PathfinderMob p_434203_, Vec3 p_451320_
   ) {
      AABB aabb = p_434203_.getBoundingBox();
      AABB aabb1 = AABB.ofSize(p_451320_, aabb.getXsize(), aabb.getYsize(), aabb.getZsize());
      return p_432956_.state.getCollisionShape(p_433160_, p_432956_.pos).bounds().inflate(p_435568_, 0.5, p_435568_).move(p_432956_.pos).intersects(aabb1);
   }

   private boolean targetIsReachableFromPosition(
      Level p_443532_, boolean p_449582_, Vec3 p_451539_, TransportItemsBetweenContainers.TransportItemTarget p_443344_, PathfinderMob p_442773_
   ) {
      return p_449582_ && this.canSeeAnyTargetSide(p_443344_, p_443532_, p_442773_, p_451539_);
   }

   private boolean canSeeAnyTargetSide(TransportItemsBetweenContainers.TransportItemTarget p_451211_, Level p_451543_, PathfinderMob p_451255_, Vec3 p_451574_) {
      Vec3 vec3 = p_451211_.pos.getCenter();
      return Direction.stream()
         .map(p_450860_ -> vec3.add(0.5 * p_450860_.getStepX(), 0.5 * p_450860_.getStepY(), 0.5 * p_450860_.getStepZ()))
         .map(p_450864_ -> p_451543_.clip(new ClipContext(p_451574_, p_450864_, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p_451255_)))
         .anyMatch(p_450866_ -> p_450866_.getType() == HitResult.Type.BLOCK && p_450866_.getBlockPos().equals(p_451211_.pos));
   }

   private boolean isAnotherMobInteractingWithTarget(TransportItemsBetweenContainers.TransportItemTarget p_434969_, Level p_434836_) {
      return this.getConnectedTargets(p_434969_, p_434836_).anyMatch(this.shouldQueueForTarget);
   }

   private static boolean isPickingUpItems(PathfinderMob p_435190_) {
      return p_435190_.getMainHandItem().isEmpty();
   }

   private static boolean matchesGettingItemsRequirement(Container p_433519_) {
      return !p_433519_.isEmpty();
   }

   private static boolean matchesLeavingItemsRequirement(PathfinderMob p_434300_, Container p_434066_) {
      return p_434066_.isEmpty() || hasItemMatchingHandItem(p_434300_, p_434066_);
   }

   private static boolean hasItemMatchingHandItem(PathfinderMob p_433687_, Container p_435308_) {
      ItemStack itemstack = p_433687_.getMainHandItem();

      for (ItemStack itemstack1 : p_435308_) {
         if (ItemStack.isSameItem(itemstack1, itemstack)) {
            return true;
         }
      }

      return false;
   }

   private void pickUpItems(PathfinderMob p_435070_, Container p_435068_) {
      p_435070_.setItemSlot(EquipmentSlot.MAINHAND, pickupItemFromContainer(p_435068_));
      p_435070_.setGuaranteedDrop(EquipmentSlot.MAINHAND);
      p_435068_.setChanged();
      this.clearMemoriesAfterMatchingTargetFound(p_435070_);
   }

   private void putDownItem(PathfinderMob p_433032_, Container p_433491_) {
      ItemStack itemstack = addItemsToContainer(p_433032_, p_433491_);
      p_433491_.setChanged();
      p_433032_.setItemSlot(EquipmentSlot.MAINHAND, itemstack);
      if (itemstack.isEmpty()) {
         this.clearMemoriesAfterMatchingTargetFound(p_433032_);
      } else {
         this.stopTargetingCurrentTarget(p_433032_);
      }
   }

   private static ItemStack pickupItemFromContainer(Container p_434826_) {
      int i = 0;

      for (ItemStack itemstack : p_434826_) {
         if (!itemstack.isEmpty()) {
            int j = Math.min(itemstack.getCount(), 16);
            return p_434826_.removeItem(i, j);
         }

         i++;
      }

      return ItemStack.EMPTY;
   }

   private static ItemStack addItemsToContainer(PathfinderMob p_435593_, Container p_434411_) {
      int i = 0;
      ItemStack itemstack = p_435593_.getMainHandItem();

      for (ItemStack itemstack1 : p_434411_) {
         if (itemstack1.isEmpty()) {
            p_434411_.setItem(i, itemstack);
            return ItemStack.EMPTY;
         }

         if (ItemStack.isSameItemSameComponents(itemstack1, itemstack) && itemstack1.getCount() < itemstack1.getMaxStackSize()) {
            int j = itemstack1.getMaxStackSize() - itemstack1.getCount();
            int k = Math.min(j, itemstack.getCount());
            itemstack1.setCount(itemstack1.getCount() + k);
            itemstack.setCount(itemstack.getCount() - j);
            p_434411_.setItem(i, itemstack1);
            if (itemstack.isEmpty()) {
               return ItemStack.EMPTY;
            }
         }

         i++;
      }

      return itemstack;
   }

   protected void stopTargetingCurrentTarget(PathfinderMob p_443295_) {
      this.ticksSinceReachingTarget = 0;
      this.target = null;
      p_443295_.getNavigation().stop();
      p_443295_.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
   }

   protected void clearMemoriesAfterMatchingTargetFound(PathfinderMob p_433072_) {
      this.stopTargetingCurrentTarget(p_433072_);
      p_433072_.getBrain().eraseMemory(MemoryModuleType.VISITED_BLOCK_POSITIONS);
      p_433072_.getBrain().eraseMemory(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS);
   }

   private void enterCooldownAfterNoMatchingTargetFound(PathfinderMob p_435655_) {
      this.stopTargetingCurrentTarget(p_435655_);
      p_435655_.getBrain().setMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS, 140);
      p_435655_.getBrain().eraseMemory(MemoryModuleType.VISITED_BLOCK_POSITIONS);
      p_435655_.getBrain().eraseMemory(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS);
   }

   protected void stop(ServerLevel p_434127_, PathfinderMob p_435175_, long p_433064_) {
      this.onStartTravelling(p_435175_);
      if (p_435175_.getNavigation() instanceof GroundPathNavigation groundpathnavigation) {
         groundpathnavigation.setCanPathToTargetsBelowSurface(false);
      }
   }

   private void stopInPlace(PathfinderMob p_435111_) {
      p_435111_.getNavigation().stop();
      p_435111_.setXxa(0.0F);
      p_435111_.setYya(0.0F);
      p_435111_.setSpeed(0.0F);
      p_435111_.setDeltaMovement(0.0, p_435111_.getDeltaMovement().y, 0.0);
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
      public static TransportItemsBetweenContainers.TransportItemTarget tryCreatePossibleTarget(BlockEntity p_435026_, Level p_433920_) {
         BlockPos blockpos = p_435026_.getBlockPos();
         BlockState blockstate = p_435026_.getBlockState();
         Container container = getBlockEntityContainer(p_435026_, blockstate, p_433920_, blockpos);
         return container != null ? new TransportItemsBetweenContainers.TransportItemTarget(blockpos, container, p_435026_, blockstate) : null;
      }

      @Nullable
      public static TransportItemsBetweenContainers.TransportItemTarget tryCreatePossibleTarget(BlockPos p_435061_, Level p_435946_) {
         BlockEntity blockentity = p_435946_.getBlockEntity(p_435061_);
         return blockentity == null ? null : tryCreatePossibleTarget(blockentity, p_435946_);
      }

      @Nullable
      private static Container getBlockEntityContainer(BlockEntity p_434468_, BlockState p_433604_, Level p_435313_, BlockPos p_435909_) {
         if (p_433604_.getBlock() instanceof ChestBlock chestblock) {
            return ChestBlock.getContainer(chestblock, p_433604_, p_435313_, p_435909_, false);
         } else {
            return p_434468_ instanceof Container container ? container : null;
         }
      }
   }
}
