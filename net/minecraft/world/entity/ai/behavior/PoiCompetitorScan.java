package net.minecraft.world.entity.ai.behavior;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

public class PoiCompetitorScan {
   public static BehaviorControl<Villager> create() {
      return BehaviorBuilder.create(
         instance -> instance.group(instance.present(MemoryModuleType.JOB_SITE), instance.present(MemoryModuleType.NEAREST_LIVING_ENTITIES))
            .apply(
               instance,
               (memoryAccessor, memoryAccessor2) -> (serverLevel, villager, l) -> {
                  GlobalPos globalPos = instance.get(memoryAccessor);
                  serverLevel.getPoiManager()
                     .getType(globalPos.pos())
                     .ifPresent(
                        holder -> instance.<List<LivingEntity>>get(memoryAccessor2)
                           .stream()
                           .filter(livingEntity -> livingEntity instanceof Villager && livingEntity != villager)
                           .map(livingEntity -> (Villager)livingEntity)
                           .filter(LivingEntity::isAlive)
                           .filter(villagerxx -> competesForSameJobsite(globalPos, holder, villagerxx))
                           .reduce(villager, PoiCompetitorScan::selectWinner)
                     );
                  return true;
               }
            )
      );
   }

   private static Villager selectWinner(Villager villager1, Villager villager2) {
      Villager villager;
      Villager villager3;
      if (villager1.getVillagerXp() > villager2.getVillagerXp()) {
         villager = villager1;
         villager3 = villager2;
      } else {
         villager = villager2;
         villager3 = villager1;
      }

      villager3.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
      return villager;
   }

   private static boolean competesForSameJobsite(GlobalPos pos, Holder<PoiType> poiType, Villager villager) {
      Optional<GlobalPos> optional = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
      return optional.isPresent() && pos.equals(optional.get()) && hasMatchingProfession(poiType, villager.getVillagerData().profession());
   }

   private static boolean hasMatchingProfession(Holder<PoiType> poiType, Holder<VillagerProfession> profession) {
      return profession.value().heldJobSite().test(poiType);
   }
}
