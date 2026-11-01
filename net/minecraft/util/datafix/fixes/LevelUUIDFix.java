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

   public LevelUUIDFix(Schema p_16360_) {
      super(p_16360_, References.LEVEL);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(this.typeReference);
      OpticFinder<?> opticfinder = type.findField("CustomBossEvents");
      OpticFinder<?> opticfinder1 = DSL.typeFinder(
         DSL.and(DSL.optional(DSL.field("Name", this.getInputSchema().getTypeRaw(References.TEXT_COMPONENT))), DSL.remainderType())
      );
      return this.fixTypeEverywhereTyped(
         "LevelUUIDFix",
         type,
         p_392870_ -> p_392870_.update(DSL.remainderFinder(), p_392865_ -> {
               p_392865_ = this.updateDragonFight(p_392865_);
               return this.updateWanderingTrader(p_392865_);
            })
            .updateTyped(
               opticfinder, p_392867_ -> p_392867_.updateTyped(opticfinder1, p_145496_ -> p_145496_.update(DSL.remainderFinder(), this::updateCustomBossEvent))
            )
      );
   }

   private Dynamic<?> updateWanderingTrader(Dynamic<?> p_16373_) {
      return replaceUUIDString(p_16373_, "WanderingTraderId", "WanderingTraderId").orElse(p_16373_);
   }

   private Dynamic<?> updateDragonFight(Dynamic<?> p_16375_) {
      return p_16375_.update(
         "DimensionData",
         p_16379_ -> p_16379_.updateMapValues(
            p_145491_ -> p_145491_.mapSecond(
               p_145506_ -> p_145506_.update("DragonFight", p_145508_ -> replaceUUIDLeastMost(p_145508_, "DragonUUID", "Dragon").orElse(p_145508_))
            )
         )
      );
   }

   private Dynamic<?> updateCustomBossEvent(Dynamic<?> p_393606_) {
      return p_393606_.update(
         "Players", p_145494_ -> p_393606_.createList(p_145494_.asStream().map(p_145502_ -> (Dynamic)createUUIDFromML((Dynamic<?>)p_145502_).orElseGet(() -> {
            LOGGER.warn("CustomBossEvents contains invalid UUIDs.");
            return (Dynamic<?>)p_145502_;
         })))
      );
   }
}
