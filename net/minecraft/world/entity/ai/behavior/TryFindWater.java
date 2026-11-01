package net.minecraft.world.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableLong;

public class TryFindWater {
   public static BehaviorControl<PathfinderMob> create(int p_259298_, float p_259140_) {
      MutableLong $$2 = new MutableLong(0L);
      return BehaviorBuilder.create(
         p_260101_ -> p_260101_.group(
               p_260101_.absent(MemoryModuleType.ATTACK_TARGET),
               p_260101_.absent(MemoryModuleType.WALK_TARGET),
               p_260101_.registered(MemoryModuleType.LOOK_TARGET)
            )
            .apply(p_260101_, (p_259124_, p_259692_, p_259819_) -> (p_260228_, p_259212_, p_260041_) -> {
               if (p_260228_.getFluidState(p_259212_.blockPosition()).is(FluidTags.WATER)) {
                  return false;
               } else if (p_260041_ < $$2.getValue()) {
                  $$2.setValue(p_260041_ + 20L + 2L);
                  return true;
               } else {
                  BlockPos $$8 = null;
                  BlockPos $$9 = null;
                  BlockPos $$10 = p_259212_.blockPosition();

                  for (BlockPos $$12 : BlockPos.withinManhattan($$10, p_259298_, p_259298_, p_259298_)) {
                     if ($$12.getX() != $$10.getX() || $$12.getZ() != $$10.getZ()) {
                        BlockState $$13 = p_259212_.level().getBlockState($$12.above());
                        BlockState $$14 = p_259212_.level().getBlockState($$12);
                        if ($$14.is(Blocks.WATER)) {
                           if ($$13.isAir()) {
                              $$8 = $$12.immutable();
                              break;
                           }

                           if ($$9 == null && !$$12.closerToCenterThan(p_259212_.position(), 1.5)) {
                              $$9 = $$12.immutable();
                           }
                        }
                     }
                  }

                  if ($$8 == null) {
                     $$8 = $$9;
                  }

                  if ($$8 != null) {
                     p_259819_.set(new BlockPosTracker($$8));
                     p_259692_.set(new WalkTarget(new BlockPosTracker($$8), p_259140_, 0));
                  }

                  $$2.setValue(p_260041_ + 40L);
                  return true;
               }
            })
      );
   }
}
