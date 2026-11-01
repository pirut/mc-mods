package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TrialSpawnerConfigFix extends NamedEntityWriteReadFix {
   public TrialSpawnerConfigFix(Schema p_338323_) {
      super(p_338323_, true, "Trial Spawner config tag fixer", References.BLOCK_ENTITY, "minecraft:trial_spawner");
   }

   private static <T> Dynamic<T> moveToConfigTag(Dynamic<T> p_338221_) {
      List<String> $$1 = List.of(
         "spawn_range",
         "total_mobs",
         "simultaneous_mobs",
         "total_mobs_added_per_player",
         "simultaneous_mobs_added_per_player",
         "ticks_between_spawn",
         "spawn_potentials",
         "loot_tables_to_eject",
         "items_to_drop_when_ominous"
      );
      Map<Dynamic<T>, Dynamic<T>> $$2 = new HashMap<>($$1.size());

      for (String $$3 : $$1) {
         Optional<Dynamic<T>> $$4 = p_338221_.get($$3).get().result();
         if ($$4.isPresent()) {
            $$2.put(p_338221_.createString($$3), $$4.get());
            p_338221_ = p_338221_.remove($$3);
         }
      }

      return $$2.isEmpty() ? p_338221_ : p_338221_.set("normal_config", p_338221_.createMap($$2));
   }

   @Override
   protected <T> Dynamic<T> fix(Dynamic<T> p_338437_) {
      return moveToConfigTag(p_338437_);
   }
}
