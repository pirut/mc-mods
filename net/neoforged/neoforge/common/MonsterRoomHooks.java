package net.neoforged.neoforge.common;

import java.util.List;
import java.util.Objects;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.datamaps.DataMapsUpdatedEvent;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

@EventBusSubscriber(
   modid = "neoforge"
)
public class MonsterRoomHooks {
   private static List<MonsterRoomHooks.MobEntry> monsterRoomMobs = List.of();

   @SubscribeEvent
   public static void onDataMapsUpdated(DataMapsUpdatedEvent event) {
      event.ifRegistry(
         Registries.ENTITY_TYPE, registry -> monsterRoomMobs = registry.getDataMap(NeoForgeDataMaps.MONSTER_ROOM_MOBS).entrySet().stream().map(entry -> {
            EntityType<?> type = Objects.requireNonNull(registry.get(entry.getKey()), "Nonexistent entity " + entry.getKey() + " in monster room datamap!");
            return new MonsterRoomHooks.MobEntry(type, entry.getValue().weight());
         }).toList()
      );
   }

   public static EntityType<?> getRandomMonsterRoomMob(RandomSource rand) {
      MonsterRoomHooks.MobEntry mob = WeightedRandom.getRandomItem(rand, monsterRoomMobs).orElseThrow();
      return mob.type;
   }

   public record MobEntry(EntityType<?> type, Weight weight) implements WeightedEntry {
      @Override
      public Weight getWeight() {
         return this.weight;
      }
   }
}
