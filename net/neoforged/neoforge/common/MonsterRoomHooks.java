package net.neoforged.neoforge.common;

import java.util.Objects;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.datamaps.DataMapsUpdatedEvent;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

@EventBusSubscriber(
   modid = "neoforge"
)
public class MonsterRoomHooks {
   private static WeightedList<EntityType<?>> monsterRoomMobs = WeightedList.of();

   @SubscribeEvent
   public static void onDataMapsUpdated(DataMapsUpdatedEvent event) {
      event.ifRegistry(
         Registries.ENTITY_TYPE,
         registry -> monsterRoomMobs = WeightedList.of(registry.getDataMap(NeoForgeDataMaps.MONSTER_ROOM_MOBS).entrySet().stream().map(entry -> {
            EntityType<?> type = Objects.requireNonNull(registry.getValue(entry.getKey()), "Nonexistent entity " + entry.getKey() + " in monster room datamap!");
            return new Weighted<>(type, entry.getValue().weight());
         }).toList())
      );
   }

   public static EntityType<?> getRandomMonsterRoomMob(RandomSource rand) {
      return monsterRoomMobs.getRandomOrThrow(rand);
   }
}
