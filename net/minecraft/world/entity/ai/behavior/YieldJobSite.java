package net.minecraft.world.entity.ai.behavior;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.pathfinder.Path;

public class YieldJobSite {
   public static BehaviorControl<Villager> create(float p_259768_) {
      return BehaviorBuilder.create(
         p_258916_ -> p_258916_.group(
               p_258916_.present(MemoryModuleType.POTENTIAL_JOB_SITE),
               p_258916_.absent(MemoryModuleType.JOB_SITE),
               p_258916_.present(MemoryModuleType.NEAREST_LIVING_ENTITIES),
               p_258916_.registered(MemoryModuleType.WALK_TARGET),
               p_258916_.registered(MemoryModuleType.LOOK_TARGET)
            )
            .apply(
               p_258916_,
               (p_258901_, p_258902_, p_258903_, p_258904_, p_258905_) -> (p_477873_, p_477874_, p_477875_) -> {
                  if (p_477874_.isBaby()) {
                     return false;
                  } else if (!p_477874_.getVillagerData().profession().is(VillagerProfession.NONE)) {
                     return false;
                  } else {
                     BlockPos blockpos = p_258916_.<GlobalPos>get(p_258901_).pos();
                     Optional<Holder<PoiType>> optional = p_477873_.getPoiManager().getType(blockpos);
                     if (optional.isEmpty()) {
                        return true;
                     } else {
                        p_258916_.<List<LivingEntity>>get(p_258903_)
                           .stream()
                           .filter(p_477863_ -> p_477863_ instanceof Villager && p_477863_ != p_477874_)
                           .map(p_477861_ -> (Villager)p_477861_)
                           .filter(LivingEntity::isAlive)
                           .filter(p_477866_ -> nearbyWantsJobsite(optional.get(), p_477866_, blockpos))
                           .findFirst()
                           .ifPresent(p_477860_ -> {
                              p_258904_.erase();
                              p_258905_.erase();
                              p_258901_.erase();
                              if (p_477860_.getBrain().getMemory(MemoryModuleType.JOB_SITE).isEmpty()) {
                                 BehaviorUtils.setWalkAndLookTargetMemories(p_477860_, blockpos, p_259768_, 1);
                                 p_477860_.getBrain().setMemory(MemoryModuleType.POTENTIAL_JOB_SITE, GlobalPos.of(p_477873_.dimension(), blockpos));
                                 p_477873_.debugSynchronizers().updatePoi(blockpos);
                              }
                           });
                        return true;
                     }
                  }
               }
            )
      );
   }

   private static boolean nearbyWantsJobsite(Holder<PoiType> p_217511_, Villager p_480034_, BlockPos p_217513_) {
      boolean flag = p_480034_.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).isPresent();
      if (flag) {
         return false;
      } else {
         Optional<GlobalPos> optional = p_480034_.getBrain().getMemory(MemoryModuleType.JOB_SITE);
         Holder<VillagerProfession> holder = p_480034_.getVillagerData().profession();
         if (holder.value().heldJobSite().test(p_217511_)) {
            return optional.isEmpty() ? canReachPos(p_480034_, p_217513_, p_217511_.value()) : optional.get().pos().equals(p_217513_);
         } else {
            return false;
         }
      }
   }

   private static boolean canReachPos(PathfinderMob p_260080_, BlockPos p_259875_, PoiType p_259606_) {
      Path path = p_260080_.getNavigation().createPath(p_259875_, p_259606_.validRange());
      return path != null && path.canReach();
   }
}
