package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

public class SetWalkTargetAwayFrom {
   public static BehaviorControl<PathfinderMob> pos(MemoryModuleType<BlockPos> p_259330_, float p_259719_, int p_259965_, boolean p_259828_) {
      return create(p_259330_, p_259719_, p_259965_, p_259828_, Vec3::atBottomCenterOf);
   }

   public static OneShot<PathfinderMob> entity(MemoryModuleType<? extends Entity> p_259598_, float p_260183_, int p_260077_, boolean p_259761_) {
      return create(p_259598_, p_260183_, p_260077_, p_259761_, Entity::position);
   }

   private static <T> OneShot<PathfinderMob> create(
      MemoryModuleType<T> p_260057_, float p_259672_, int p_259866_, boolean p_259232_, Function<T, Vec3> p_259355_
   ) {
      return BehaviorBuilder.create(
         p_259292_ -> p_259292_.group(p_259292_.registered(MemoryModuleType.WALK_TARGET), p_259292_.present(p_260057_))
            .apply(p_259292_, (p_260063_, p_260053_) -> (p_259973_, p_259323_, p_259275_) -> {
               Optional<WalkTarget> $$10 = p_259292_.tryGet(p_260063_);
               if ($$10.isPresent() && !p_259232_) {
                  return false;
               } else {
                  Vec3 $$11 = p_259323_.position();
                  Vec3 $$12 = p_259355_.apply(p_259292_.get(p_260053_));
                  if (!$$11.closerThan($$12, p_259866_)) {
                     return false;
                  } else {
                     if ($$10.isPresent() && $$10.get().getSpeedModifier() == p_259672_) {
                        Vec3 $$13 = $$10.get().getTarget().currentPosition().subtract($$11);
                        Vec3 $$14 = $$12.subtract($$11);
                        if ($$13.dot($$14) < 0.0) {
                           return false;
                        }
                     }

                     for (int $$15 = 0; $$15 < 10; $$15++) {
                        Vec3 $$16 = LandRandomPos.getPosAway(p_259323_, 16, 7, $$12);
                        if ($$16 != null) {
                           p_260063_.set(new WalkTarget($$16, p_259672_, 0));
                           break;
                        }
                     }

                     return true;
                  }
               }
            })
      );
   }
}
