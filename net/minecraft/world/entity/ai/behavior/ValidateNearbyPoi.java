package net.minecraft.world.entity.ai.behavior;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class ValidateNearbyPoi {
   private static final int MAX_DISTANCE = 16;

   public static BehaviorControl<LivingEntity> create(Predicate<Holder<PoiType>> poiValidator, MemoryModuleType<GlobalPos> poiPosMemory) {
      return BehaviorBuilder.create(
         instance -> instance.group(instance.present(poiPosMemory)).apply(instance, memoryAccessor -> (serverLevel, livingEntity, l) -> {
            GlobalPos globalPos = instance.get(memoryAccessor);
            BlockPos blockPos = globalPos.pos();
            if (serverLevel.dimension() == globalPos.dimension() && blockPos.closerToCenterThan(livingEntity.position(), 16.0)) {
               ServerLevel serverLevel2 = serverLevel.getServer().getLevel(globalPos.dimension());
               if (serverLevel2 == null || !serverLevel2.getPoiManager().exists(blockPos, poiValidator)) {
                  memoryAccessor.erase();
               } else if (bedIsOccupied(serverLevel2, blockPos, livingEntity)) {
                  memoryAccessor.erase();
                  if (!bedIsOccupiedByVillager(serverLevel2, blockPos)) {
                     serverLevel.getPoiManager().release(blockPos);
                     serverLevel.debugSynchronizers().updatePoi(blockPos);
                  }
               }

               return true;
            } else {
               return false;
            }
         })
      );
   }

   private static boolean bedIsOccupied(ServerLevel level, BlockPos pos, LivingEntity entity) {
      BlockState blockState = level.getBlockState(pos);
      return blockState.is(BlockTags.BEDS) && blockState.getValue(BedBlock.OCCUPIED) && !entity.isSleeping();
   }

   private static boolean bedIsOccupiedByVillager(ServerLevel level, BlockPos pos) {
      List<Villager> list = level.getEntitiesOfClass(Villager.class, new AABB(pos), LivingEntity::isSleeping);
      return !list.isEmpty();
   }
}
