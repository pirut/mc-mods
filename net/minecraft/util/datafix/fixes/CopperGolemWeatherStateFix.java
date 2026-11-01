package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class CopperGolemWeatherStateFix extends NamedEntityFix {
   public CopperGolemWeatherStateFix(Schema p_436670_) {
      super(p_436670_, false, "CopperGolemWeatherStateFix", References.ENTITY, "minecraft:copper_golem");
   }

   @Override
   protected Typed<?> fix(Typed<?> p_436642_) {
      return p_436642_.update(DSL.remainderFinder(), p_436673_ -> p_436673_.update("weather_state", CopperGolemWeatherStateFix::fixWeatherState));
   }

   private static Dynamic<?> fixWeatherState(Dynamic<?> p_436616_) {
      return switch (p_436616_.asInt(0)) {
         case 1 -> p_436616_.createString("exposed");
         case 2 -> p_436616_.createString("weathered");
         case 3 -> p_436616_.createString("oxidized");
         default -> p_436616_.createString("unaffected");
      };
   }
}
