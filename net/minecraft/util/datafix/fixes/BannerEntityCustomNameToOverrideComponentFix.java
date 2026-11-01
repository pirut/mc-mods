package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.Optional;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;

public class BannerEntityCustomNameToOverrideComponentFix extends DataFix {
   public BannerEntityCustomNameToOverrideComponentFix(Schema outputSchema) {
      super(outputSchema, false);
   }

   public TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.BLOCK_ENTITY);
      TaggedChoiceType<?> taggedChoiceType = this.getInputSchema().findChoiceType(References.BLOCK_ENTITY);
      OpticFinder<?> opticFinder = type.findField("CustomName");
      OpticFinder<Pair<String, String>> opticFinder2 = DSL.typeFinder(this.getInputSchema().getType(References.TEXT_COMPONENT));
      return this.fixTypeEverywhereTyped("Banner entity custom_name to item_name component fix", type, typed -> {
         Object object = ((Pair)typed.get(taggedChoiceType.finder())).getFirst();
         return object.equals("minecraft:banner") ? this.fix(typed, opticFinder2, opticFinder) : typed;
      });
   }

   private Typed<?> fix(Typed<?> data, OpticFinder<Pair<String, String>> textComponentTypeOptic, OpticFinder<?> customNameOptic) {
      Optional<String> optional = data.getOptionalTyped(customNameOptic).flatMap(typed -> typed.getOptional(textComponentTypeOptic).map(Pair::getSecond));
      boolean bl = optional.flatMap(LegacyComponentDataFixUtils::extractTranslationString)
         .filter(string -> string.equals("block.minecraft.ominous_banner"))
         .isPresent();
      return bl
         ? Util.writeAndReadTypedOrThrow(
            data,
            data.getType(),
            dynamic -> {
               Dynamic<?> dynamic2 = dynamic.createMap(
                  Map.of(
                     dynamic.createString("minecraft:item_name"),
                     dynamic.createString(optional.get()),
                     dynamic.createString("minecraft:hide_additional_tooltip"),
                     dynamic.emptyMap()
                  )
               );
               return dynamic.set("components", dynamic2).remove("CustomName");
            }
         )
         : data;
   }
}
