package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class VillagerDataFix extends NamedEntityFix {
   public VillagerDataFix(Schema p_17056_, String p_17057_) {
      super(p_17056_, false, "Villager profession data fix (" + p_17057_ + ")", References.ENTITY, p_17057_);
   }

   @Override
   protected Typed<?> fix(Typed<?> p_17062_) {
      Dynamic<?> $$1 = (Dynamic<?>)p_17062_.get(DSL.remainderFinder());
      return p_17062_.set(
         DSL.remainderFinder(),
         $$1.remove("Profession")
            .remove("Career")
            .remove("CareerLevel")
            .set(
               "VillagerData",
               $$1.createMap(
                  ImmutableMap.of(
                     $$1.createString("type"),
                     $$1.createString("minecraft:plains"),
                     $$1.createString("profession"),
                     $$1.createString(upgradeData($$1.get("Profession").asInt(0), $$1.get("Career").asInt(0))),
                     $$1.createString("level"),
                     (Dynamic)DataFixUtils.orElse($$1.get("CareerLevel").result(), $$1.createInt(1))
                  )
               )
            )
      );
   }

   private static String upgradeData(int p_17059_, int p_17060_) {
      if (p_17059_ == 0) {
         if (p_17060_ == 2) {
            return "minecraft:fisherman";
         } else if (p_17060_ == 3) {
            return "minecraft:shepherd";
         } else {
            return p_17060_ == 4 ? "minecraft:fletcher" : "minecraft:farmer";
         }
      } else if (p_17059_ == 1) {
         return p_17060_ == 2 ? "minecraft:cartographer" : "minecraft:librarian";
      } else if (p_17059_ == 2) {
         return "minecraft:cleric";
      } else if (p_17059_ == 3) {
         if (p_17060_ == 2) {
            return "minecraft:weaponsmith";
         } else {
            return p_17060_ == 3 ? "minecraft:toolsmith" : "minecraft:armorer";
         }
      } else if (p_17059_ == 4) {
         return p_17060_ == 2 ? "minecraft:leatherworker" : "minecraft:butcher";
      } else {
         return p_17059_ == 5 ? "minecraft:nitwit" : "minecraft:none";
      }
   }
}
