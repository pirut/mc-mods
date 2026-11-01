package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.Sets;
import com.mojang.datafixers.kinds.OptionalBox.Mu;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

public class InteractWithDoor {
   private static final int COOLDOWN_BEFORE_RERUNNING_IN_SAME_NODE = 20;
   private static final double SKIP_CLOSING_DOOR_IF_FURTHER_AWAY_THAN = 3.0;
   private static final double MAX_DISTANCE_TO_HOLD_DOOR_OPEN_FOR_OTHER_MOBS = 2.0;

   public static BehaviorControl<LivingEntity> create() {
      MutableObject<Node> mutableObject = new MutableObject();
      MutableInt mutableInt = new MutableInt(0);
      return BehaviorBuilder.create(
         instance -> instance.group(
               instance.present(MemoryModuleType.PATH),
               instance.registered(MemoryModuleType.DOORS_TO_CLOSE),
               instance.registered(MemoryModuleType.NEAREST_LIVING_ENTITIES)
            )
            .apply(
               instance,
               (memoryAccessor, memoryAccessor2, memoryAccessor3) -> (serverLevel, livingEntity, l) -> {
                  Path path = instance.get(memoryAccessor);
                  Optional<Set<GlobalPos>> optional = instance.tryGet(memoryAccessor2);
                  if (!path.notStarted() && !path.isDone()) {
                     if (Objects.equals(mutableObject.get(), path.getNextNode())) {
                        mutableInt.setValue(20);
                     } else if (mutableInt.decrementAndGet() > 0) {
                        return false;
                     }

                     mutableObject.setValue(path.getNextNode());
                     Node node = path.getPreviousNode();
                     Node node2 = path.getNextNode();
                     BlockPos blockPos = node.asBlockPos();
                     BlockState blockState = serverLevel.getBlockState(blockPos);
                     if (blockState.is(BlockTags.MOB_INTERACTABLE_DOORS, blockStateBase -> blockStateBase.getBlock() instanceof DoorBlock)) {
                        DoorBlock doorBlock = (DoorBlock)blockState.getBlock();
                        if (!doorBlock.isOpen(blockState)) {
                           doorBlock.setOpen(livingEntity, serverLevel, blockState, blockPos, true);
                        }

                        optional = rememberDoorToClose(memoryAccessor2, optional, serverLevel, blockPos);
                     }

                     BlockPos blockPos2 = node2.asBlockPos();
                     BlockState blockState2 = serverLevel.getBlockState(blockPos2);
                     if (blockState2.is(BlockTags.MOB_INTERACTABLE_DOORS, blockStateBase -> blockStateBase.getBlock() instanceof DoorBlock)) {
                        DoorBlock doorBlock2 = (DoorBlock)blockState2.getBlock();
                        if (!doorBlock2.isOpen(blockState2)) {
                           doorBlock2.setOpen(livingEntity, serverLevel, blockState2, blockPos2, true);
                           optional = rememberDoorToClose(memoryAccessor2, optional, serverLevel, blockPos2);
                        }
                     }

                     optional.ifPresent(
                        set -> closeDoorsThatIHaveOpenedOrPassedThrough(
                           serverLevel, livingEntity, node, node2, (Set<GlobalPos>)set, instance.tryGet(memoryAccessor3)
                        )
                     );
                     return true;
                  } else {
                     return false;
                  }
               }
            )
      );
   }

   public static void closeDoorsThatIHaveOpenedOrPassedThrough(
      ServerLevel level,
      LivingEntity entity,
      @Nullable Node previous,
      @Nullable Node next,
      Set<GlobalPos> doorPositions,
      Optional<List<LivingEntity>> nearestLivingEntities
   ) {
      Iterator<GlobalPos> iterator = doorPositions.iterator();

      while (iterator.hasNext()) {
         GlobalPos globalPos = iterator.next();
         BlockPos blockPos = globalPos.pos();
         if ((previous == null || !previous.asBlockPos().equals(blockPos)) && (next == null || !next.asBlockPos().equals(blockPos))) {
            if (isDoorTooFarAway(level, entity, globalPos)) {
               iterator.remove();
            } else {
               BlockState blockState = level.getBlockState(blockPos);
               if (!blockState.is(BlockTags.MOB_INTERACTABLE_DOORS, blockStateBase -> blockStateBase.getBlock() instanceof DoorBlock)) {
                  iterator.remove();
               } else {
                  DoorBlock doorBlock = (DoorBlock)blockState.getBlock();
                  if (!doorBlock.isOpen(blockState)) {
                     iterator.remove();
                  } else if (areOtherMobsComingThroughDoor(entity, blockPos, nearestLivingEntities)) {
                     iterator.remove();
                  } else {
                     doorBlock.setOpen(entity, level, blockState, blockPos, false);
                     iterator.remove();
                  }
               }
            }
         }
      }
   }

   private static boolean areOtherMobsComingThroughDoor(LivingEntity entity, BlockPos pos, Optional<List<LivingEntity>> nearestLivingEntities) {
      return nearestLivingEntities.isEmpty()
         ? false
         : nearestLivingEntities.get()
            .stream()
            .filter(livingEntity2 -> livingEntity2.getType() == entity.getType())
            .filter(livingEntity -> pos.closerToCenterThan(livingEntity.position(), 2.0))
            .anyMatch(livingEntity -> isMobComingThroughDoor(livingEntity.getBrain(), pos));
   }

   private static boolean isMobComingThroughDoor(Brain<?> brain, BlockPos pos) {
      if (!brain.hasMemoryValue(MemoryModuleType.PATH)) {
         return false;
      } else {
         Path path = brain.getMemory(MemoryModuleType.PATH).get();
         if (path.isDone()) {
            return false;
         } else {
            Node node = path.getPreviousNode();
            if (node == null) {
               return false;
            } else {
               Node node2 = path.getNextNode();
               return pos.equals(node.asBlockPos()) || pos.equals(node2.asBlockPos());
            }
         }
      }
   }

   private static boolean isDoorTooFarAway(ServerLevel level, LivingEntity entity, GlobalPos pos) {
      return pos.dimension() != level.dimension() || !pos.pos().closerToCenterThan(entity.position(), 3.0);
   }

   private static Optional<Set<GlobalPos>> rememberDoorToClose(
      MemoryAccessor<Mu, Set<GlobalPos>> doorsToClose, Optional<Set<GlobalPos>> doorPositions, ServerLevel level, BlockPos pos
   ) {
      GlobalPos globalPos = GlobalPos.of(level.dimension(), pos);
      return Optional.of(doorPositions.<Set<GlobalPos>>map(set -> {
         set.add(globalPos);
         return set;
      }).orElseGet(() -> {
         Set<GlobalPos> set = Sets.newHashSet(new GlobalPos[]{globalPos});
         doorsToClose.set(set);
         return set;
      }));
   }
}
