package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import java.util.Optional;

public class ZombieVillagerRebuildXpFix extends NamedEntityFix {
   public ZombieVillagerRebuildXpFix(Schema p_17298_, boolean p_17299_) {
      super(p_17298_, p_17299_, "Zombie Villager XP rebuild", References.ENTITY, "minecraft:zombie_villager");
   }

   @Override
   protected Typed<?> fix(Typed<?> p_17301_) {
      return p_17301_.update(DSL.remainderFinder(), p_337684_ -> {
         Optional<Number> $$1 = p_337684_.get("Xp").asNumber().result();
         if ($$1.isEmpty()) {
            int $$2 = p_337684_.get("VillagerData").get("level").asInt(1);
            return p_337684_.set("Xp", p_337684_.createInt(VillagerRebuildLevelAndXpFix.getMinXpPerLevel($$2)));
         } else {
            return p_337684_;
         }
      });
   }
}
