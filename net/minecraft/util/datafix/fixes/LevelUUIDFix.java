package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import org.slf4j.Logger;

public class LevelUUIDFix extends AbstractUUIDFix {
   private static final Logger LOGGER = LogUtils.getLogger();

   public LevelUUIDFix(Schema outputSchema) {
      super(outputSchema, References.LEVEL);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(this.typeReference);
      OpticFinder<?> opticFinder = type.findField("CustomBossEvents");
      OpticFinder<?> opticFinder2 = DSL.typeFinder(
         DSL.and(DSL.optional(DSL.field("Name", this.getInputSchema().getTypeRaw(References.TEXT_COMPONENT))), DSL.remainderType())
      );
      return this.fixTypeEverywhereTyped("LevelUUIDFix", type, typed -> typed.update(DSL.remainderFinder(), dynamic -> {
         dynamic = this.updateDragonFight(dynamic);
         return this.updateWanderingTrader(dynamic);
      }).updateTyped(opticFinder, typedx -> typedx.updateTyped(opticFinder2, typedxx -> typedxx.update(DSL.remainderFinder(), this::updateCustomBossEvent))));
   }

   private Dynamic<?> updateWanderingTrader(Dynamic<?> data) {
      return replaceUUIDString(data, "WanderingTraderId", "WanderingTraderId").orElse(data);
   }

   private Dynamic<?> updateDragonFight(Dynamic<?> data) {
      return data.update(
         "DimensionData",
         dynamic -> dynamic.updateMapValues(
            pair -> pair.mapSecond(
               dynamicx -> dynamicx.update("DragonFight", dynamicxx -> replaceUUIDLeastMost(dynamicxx, "DragonUUID", "Dragon").orElse(dynamicxx))
            )
         )
      );
   }

   private Dynamic<?> updateCustomBossEvent(Dynamic<?> data) {
      return data.update("Players", dynamic2 -> data.createList(dynamic2.asStream().map(dynamicx -> (Dynamic)createUUIDFromML(dynamicx).orElseGet(() -> {
         LOGGER.warn("CustomBossEvents contains invalid UUIDs.");
         return dynamicx;
      }))));
   }
}
