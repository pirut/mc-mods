package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.Sets;
import com.mojang.datafixers.kinds.OptionalBox.Mu;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
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

public class InteractWithDoor {
   private static final int COOLDOWN_BEFORE_RERUNNING_IN_SAME_NODE = 20;
   private static final double SKIP_CLOSING_DOOR_IF_FURTHER_AWAY_THAN = 3.0;
   private static final double MAX_DISTANCE_TO_HOLD_DOOR_OPEN_FOR_OTHER_MOBS = 2.0;

   public static BehaviorControl<LivingEntity> create() {
      MutableObject<Node> $$0 = new MutableObject(null);
      MutableInt $$1 = new MutableInt(0);
      return BehaviorBuilder.create(
         p_258474_ -> p_258474_.group(
               p_258474_.present(MemoryModuleType.PATH),
               p_258474_.registered(MemoryModuleType.DOORS_TO_CLOSE),
               p_258474_.registered(MemoryModuleType.NEAREST_LIVING_ENTITIES)
            )
            .apply(
               p_258474_,
               (p_258460_, p_258461_, p_258462_) -> (p_258469_, p_258470_, p_258471_) -> {
                  Path $$9 = p_258474_.get(p_258460_);
                  Optional<Set<GlobalPos>> $$10 = p_258474_.tryGet(p_258461_);
                  if (!$$9.notStarted() && !$$9.isDone()) {
                     if (Objects.equals($$0.getValue(), $$9.getNextNode())) {
                        $$1.setValue(20);
                     } else if ($$1.decrementAndGet() > 0) {
                        return false;
                     }

                     $$0.setValue($$9.getNextNode());
                     Node $$11 = $$9.getPreviousNode();
                     Node $$12 = $$9.getNextNode();
                     BlockPos $$13 = $$11.asBlockPos();
                     BlockState $$14 = p_258469_.getBlockState($$13);
                     if ($$14.is(BlockTags.MOB_INTERACTABLE_DOORS, p_201959_ -> p_201959_.getBlock() instanceof DoorBlock)) {
                        DoorBlock $$15 = (DoorBlock)$$14.getBlock();
                        if (!$$15.isOpen($$14)) {
                           $$15.setOpen(p_258470_, p_258469_, $$14, $$13, true);
                        }

                        $$10 = rememberDoorToClose(p_258461_, $$10, p_258469_, $$13);
                     }

                     BlockPos $$16 = $$12.asBlockPos();
                     BlockState $$17 = p_258469_.getBlockState($$16);
                     if ($$17.is(BlockTags.MOB_INTERACTABLE_DOORS, p_201957_ -> p_201957_.getBlock() instanceof DoorBlock)) {
                        DoorBlock $$18 = (DoorBlock)$$17.getBlock();
                        if (!$$18.isOpen($$17)) {
                           $$18.setOpen(p_258470_, p_258469_, $$17, $$16, true);
                           $$10 = rememberDoorToClose(p_258461_, $$10, p_258469_, $$16);
                        }
                     }

                     $$10.ifPresent(
                        p_258452_ -> closeDoorsThatIHaveOpenedOrPassedThrough(
                           p_258469_, p_258470_, $$11, $$12, (Set<GlobalPos>)p_258452_, p_258474_.tryGet(p_258462_)
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
      ServerLevel p_260343_,
      LivingEntity p_259371_,
      @Nullable Node p_259408_,
      @Nullable Node p_260013_,
      Set<GlobalPos> p_259401_,
      Optional<List<LivingEntity>> p_260015_
   ) {
      Iterator<GlobalPos> $$6 = p_259401_.iterator();

      while ($$6.hasNext()) {
         GlobalPos $$7 = $$6.next();
         BlockPos $$8 = $$7.pos();
         if ((p_259408_ == null || !p_259408_.asBlockPos().equals($$8)) && (p_260013_ == null || !p_260013_.asBlockPos().equals($$8))) {
            if (isDoorTooFarAway(p_260343_, p_259371_, $$7)) {
               $$6.remove();
            } else {
               BlockState $$9 = p_260343_.getBlockState($$8);
               if (!$$9.is(BlockTags.MOB_INTERACTABLE_DOORS, p_201952_ -> p_201952_.getBlock() instanceof DoorBlock)) {
                  $$6.remove();
               } else {
                  DoorBlock $$10 = (DoorBlock)$$9.getBlock();
                  if (!$$10.isOpen($$9)) {
                     $$6.remove();
                  } else if (areOtherMobsComingThroughDoor(p_259371_, $$8, p_260015_)) {
                     $$6.remove();
                  } else {
                     $$10.setOpen(p_259371_, p_260343_, $$9, $$8, false);
                     $$6.remove();
                  }
               }
            }
         }
      }
   }

   private static boolean areOtherMobsComingThroughDoor(LivingEntity p_260091_, BlockPos p_259764_, Optional<List<LivingEntity>> p_259365_) {
      return p_259365_.isEmpty()
         ? false
         : p_259365_.get()
            .stream()
            .filter(p_348184_ -> p_348184_.getType() == p_260091_.getType())
            .filter(p_352722_ -> p_259764_.closerToCenterThan(p_352722_.position(), 2.0))
            .anyMatch(p_258454_ -> isMobComingThroughDoor(p_258454_.getBrain(), p_259764_));
   }

   private static boolean isMobComingThroughDoor(Brain<?> p_259548_, BlockPos p_259146_) {
      if (!p_259548_.hasMemoryValue(MemoryModuleType.PATH)) {
         return false;
      } else {
         Path $$2 = p_259548_.getMemory(MemoryModuleType.PATH).get();
         if ($$2.isDone()) {
            return false;
         } else {
            Node $$3 = $$2.getPreviousNode();
            if ($$3 == null) {
               return false;
            } else {
               Node $$4 = $$2.getNextNode();
               return p_259146_.equals($$3.asBlockPos()) || p_259146_.equals($$4.asBlockPos());
            }
         }
      }
   }

   private static boolean isDoorTooFarAway(ServerLevel p_23308_, LivingEntity p_23309_, GlobalPos p_23310_) {
      return p_23310_.dimension() != p_23308_.dimension() || !p_23310_.pos().closerToCenterThan(p_23309_.position(), 3.0);
   }

   private static Optional<Set<GlobalPos>> rememberDoorToClose(
      MemoryAccessor<Mu, Set<GlobalPos>> p_262178_, Optional<Set<GlobalPos>> p_261639_, ServerLevel p_261528_, BlockPos p_261874_
   ) {
      GlobalPos $$4 = GlobalPos.of(p_261528_.dimension(), p_261874_);
      return Optional.of(p_261639_.<Set<GlobalPos>>map(p_261437_ -> {
         p_261437_.add($$4);
         return p_261437_;
      }).orElseGet(() -> {
         Set<GlobalPos> $$2 = Sets.newHashSet(new GlobalPos[]{$$4});
         p_262178_.set($$2);
         return $$2;
      }));
   }
}
