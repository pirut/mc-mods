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
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class OminousBannerRarityFix extends DataFix {
   public OminousBannerRarityFix(Schema outputSchema) {
      super(outputSchema, false);
   }

   public TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.BLOCK_ENTITY);
      Type<?> type2 = this.getInputSchema().getType(References.ITEM_STACK);
      TaggedChoiceType<?> taggedChoiceType = this.getInputSchema().findChoiceType(References.BLOCK_ENTITY);
      OpticFinder<Pair<String, String>> opticFinder = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
      OpticFinder<?> opticFinder2 = type.findField("components");
      OpticFinder<?> opticFinder3 = type2.findField("components");
      OpticFinder<?> opticFinder4 = opticFinder2.type().findField("minecraft:item_name");
      OpticFinder<Pair<String, String>> opticFinder5 = DSL.typeFinder(this.getInputSchema().getType(References.TEXT_COMPONENT));
      return TypeRewriteRule.seq(this.fixTypeEverywhereTyped("Ominous Banner block entity common rarity to uncommon rarity fix", type, typed -> {
         Object object = ((Pair)typed.get(taggedChoiceType.finder())).getFirst();
         return object.equals("minecraft:banner") ? this.fix(typed, opticFinder2, opticFinder4, opticFinder5) : typed;
      }), this.fixTypeEverywhereTyped("Ominous Banner item stack common rarity to uncommon rarity fix", type2, typed -> {
         String string = typed.getOptional(opticFinder).<String>map(Pair::getSecond).orElse("");
         return string.equals("minecraft:white_banner") ? this.fix(typed, opticFinder3, opticFinder4, opticFinder5) : typed;
      }));
   }

   private Typed<?> fix(Typed<?> data, OpticFinder<?> componentField, OpticFinder<?> itemNameField, OpticFinder<Pair<String, String>> textComponentField) {
      return data.updateTyped(
         componentField,
         typed -> {
            boolean bl = typed.getOptionalTyped(itemNameField)
               .flatMap(typedx -> typedx.getOptional(textComponentField))
               .<String>map(Pair::getSecond)
               .flatMap(LegacyComponentDataFixUtils::extractTranslationString)
               .filter(string -> string.equals("block.minecraft.ominous_banner"))
               .isPresent();
            return bl
               ? typed.updateTyped(
                     itemNameField,
                     typedx -> typedx.set(
                        textComponentField,
                        Pair.of(
                           References.TEXT_COMPONENT.typeName(), LegacyComponentDataFixUtils.createTranslatableComponentJson("block.minecraft.ominous_banner")
                        )
                     )
                  )
                  .update(DSL.remainderFinder(), dynamic -> dynamic.set("minecraft:rarity", dynamic.createString("uncommon")))
               : typed;
         }
      );
   }
}
