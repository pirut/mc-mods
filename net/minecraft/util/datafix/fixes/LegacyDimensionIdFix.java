package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;

public class LegacyDimensionIdFix extends DataFix {
   public LegacyDimensionIdFix(Schema outputSchema) {
      super(outputSchema, false);
   }

   public TypeRewriteRule makeRule() {
      TypeRewriteRule typeRewriteRule = this.fixTypeEverywhereTyped(
         "PlayerLegacyDimensionFix", this.getInputSchema().getType(References.PLAYER), typed -> typed.update(DSL.remainderFinder(), this::fixPlayer)
      );
      Type<?> type = this.getInputSchema().getType(References.SAVED_DATA_MAP_DATA);
      OpticFinder<?> opticFinder = type.findField("data");
      TypeRewriteRule typeRewriteRule2 = this.fixTypeEverywhereTyped(
         "MapLegacyDimensionFix", type, typed -> typed.updateTyped(opticFinder, typedx -> typedx.update(DSL.remainderFinder(), this::fixMap))
      );
      return TypeRewriteRule.seq(typeRewriteRule, typeRewriteRule2);
   }

   private <T> Dynamic<T> fixMap(Dynamic<T> data) {
      return data.update("dimension", this::fixDimensionId);
   }

   private <T> Dynamic<T> fixPlayer(Dynamic<T> data) {
      return data.update("Dimension", this::fixDimensionId);
   }

   private <T> Dynamic<T> fixDimensionId(Dynamic<T> data) {
      return (Dynamic<T>)DataFixUtils.orElse(data.asNumber().result().map(number -> {
         return switch (number.intValue()) {
            case -1 -> data.createString("minecraft:the_nether");
            case 1 -> data.createString("minecraft:the_end");
            default -> data.createString("minecraft:overworld");
         };
      }), data);
   }
}
