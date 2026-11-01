package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class VillagerSetCanPickUpLootFix extends NamedEntityFix {
   private static final String CAN_PICK_UP_LOOT = "CanPickUpLoot";

   public VillagerSetCanPickUpLootFix(Schema p_366707_) {
      super(p_366707_, true, "Villager CanPickUpLoot default value", References.ENTITY, "Villager");
   }

   @Override
   protected Typed<?> fix(Typed<?> p_366663_) {
      return p_366663_.update(DSL.remainderFinder(), VillagerSetCanPickUpLootFix::fixValue);
   }

   private static Dynamic<?> fixValue(Dynamic<?> p_366760_) {
      return p_366760_.set("CanPickUpLoot", p_366760_.createBoolean(true));
   }
}
