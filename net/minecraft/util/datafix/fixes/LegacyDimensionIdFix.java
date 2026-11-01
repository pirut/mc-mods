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
   public LegacyDimensionIdFix(Schema p_422637_) {
      super(p_422637_, false);
   }

   public TypeRewriteRule makeRule() {
      TypeRewriteRule typerewriterule = this.fixTypeEverywhereTyped(
         "PlayerLegacyDimensionFix", this.getInputSchema().getType(References.PLAYER), p_421877_ -> p_421877_.update(DSL.remainderFinder(), this::fixPlayer)
      );
      Type<?> type = this.getInputSchema().getType(References.SAVED_DATA_MAP_DATA);
      OpticFinder<?> opticfinder = type.findField("data");
      TypeRewriteRule typerewriterule1 = this.fixTypeEverywhereTyped(
         "MapLegacyDimensionFix", type, p_422008_ -> p_422008_.updateTyped(opticfinder, p_422501_ -> p_422501_.update(DSL.remainderFinder(), this::fixMap))
      );
      return TypeRewriteRule.seq(typerewriterule, typerewriterule1);
   }

   private <T> Dynamic<T> fixMap(Dynamic<T> p_421994_) {
      return p_421994_.update("dimension", this::fixDimensionId);
   }

   private <T> Dynamic<T> fixPlayer(Dynamic<T> p_422530_) {
      return p_422530_.update("Dimension", this::fixDimensionId);
   }

   private <T> Dynamic<T> fixDimensionId(Dynamic<T> p_421501_) {
      return (Dynamic<T>)DataFixUtils.orElse(p_421501_.asNumber().result().map(p_422536_ -> {
         return switch (p_422536_.intValue()) {
            case -1 -> p_421501_.createString("minecraft:the_nether");
            case 1 -> p_421501_.createString("minecraft:the_end");
            default -> p_421501_.createString("minecraft:overworld");
         };
      }), p_421501_);
   }
}
