package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.Util;

public class StructureSettingsFlattenFix extends DataFix {
   public StructureSettingsFlattenFix(Schema p_204000_) {
      super(p_204000_, false);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getInputSchema().getType(References.WORLD_GEN_SETTINGS);
      OpticFinder<?> $$1 = $$0.findField("dimensions");
      return this.fixTypeEverywhereTyped(
         "StructureSettingsFlatten",
         $$0,
         p_204003_ -> p_204003_.updateTyped(
            $$1,
            p_311570_ -> Util.writeAndReadTypedOrThrow(p_311570_, $$1.type(), p_311568_ -> p_311568_.updateMapValues(StructureSettingsFlattenFix::fixDimension))
         )
      );
   }

   private static Pair<Dynamic<?>, Dynamic<?>> fixDimension(Pair<Dynamic<?>, Dynamic<?>> p_204005_) {
      Dynamic<?> $$1 = (Dynamic<?>)p_204005_.getSecond();
      return Pair.of(
         (Dynamic)p_204005_.getFirst(),
         $$1.update(
            "generator", p_204018_ -> p_204018_.update("settings", p_204020_ -> p_204020_.update("structures", StructureSettingsFlattenFix::fixStructures))
         )
      );
   }

   private static Dynamic<?> fixStructures(Dynamic<?> p_204007_) {
      Dynamic<?> $$1 = p_204007_.get("structures")
         .orElseEmptyMap()
         .updateMapValues(p_204010_ -> p_204010_.mapSecond(p_204013_ -> p_204013_.set("type", p_204007_.createString("minecraft:random_spread"))));
      return (Dynamic<?>)DataFixUtils.orElse(
         p_204007_.get("stronghold")
            .result()
            .map(p_207675_ -> $$1.set("minecraft:stronghold", p_207675_.set("type", p_204007_.createString("minecraft:concentric_rings")))),
         $$1
      );
   }
}
