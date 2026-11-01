package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import java.util.Map;

public class MapIdFix extends DataFix {
   public MapIdFix(Schema p_16396_) {
      super(p_16396_, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "Map id fix",
         this.getInputSchema().getType(References.SAVED_DATA_MAP_INDEX),
         p_428142_ -> p_428142_.update(DSL.remainderFinder(), p_392880_ -> p_392880_.createMap(Map.of(p_392880_.createString("data"), p_392880_)))
      );
   }
}
