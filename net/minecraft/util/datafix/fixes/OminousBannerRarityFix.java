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
   public OminousBannerRarityFix(Schema p_364363_) {
      super(p_364363_, false);
   }

   public TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.BLOCK_ENTITY);
      Type<?> type1 = this.getInputSchema().getType(References.ITEM_STACK);
      TaggedChoiceType<?> taggedchoicetype = this.getInputSchema().findChoiceType(References.BLOCK_ENTITY);
      OpticFinder<Pair<String, String>> opticfinder = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
      OpticFinder<?> opticfinder1 = type.findField("components");
      OpticFinder<?> opticfinder2 = type1.findField("components");
      OpticFinder<?> opticfinder3 = opticfinder1.type().findField("minecraft:item_name");
      OpticFinder<Pair<String, String>> opticfinder4 = DSL.typeFinder(this.getInputSchema().getType(References.TEXT_COMPONENT));
      return TypeRewriteRule.seq(this.fixTypeEverywhereTyped("Ominous Banner block entity common rarity to uncommon rarity fix", type, p_392898_ -> {
         Object object = ((Pair)p_392898_.get(taggedchoicetype.finder())).getFirst();
         return object.equals("minecraft:banner") ? this.fix(p_392898_, opticfinder1, opticfinder3, opticfinder4) : p_392898_;
      }), this.fixTypeEverywhereTyped("Ominous Banner item stack common rarity to uncommon rarity fix", type1, p_392890_ -> {
         String s = p_392890_.getOptional(opticfinder).<String>map(Pair::getSecond).orElse("");
         return s.equals("minecraft:white_banner") ? this.fix(p_392890_, opticfinder2, opticfinder3, opticfinder4) : p_392890_;
      }));
   }

   private Typed<?> fix(Typed<?> p_363320_, OpticFinder<?> p_362991_, OpticFinder<?> p_393815_, OpticFinder<Pair<String, String>> p_393787_) {
      return p_363320_.updateTyped(
         p_362991_,
         p_392893_ -> {
            boolean flag = p_392893_.getOptionalTyped(p_393815_)
               .flatMap(p_392882_ -> p_392882_.getOptional(p_393787_))
               .<String>map(Pair::getSecond)
               .flatMap(LegacyComponentDataFixUtils::extractTranslationString)
               .filter(p_363017_ -> p_363017_.equals("block.minecraft.ominous_banner"))
               .isPresent();
            return flag
               ? p_392893_.updateTyped(
                     p_393815_,
                     p_392885_ -> p_392885_.set(
                        p_393787_,
                        Pair.of(
                           References.TEXT_COMPONENT.typeName(), LegacyComponentDataFixUtils.createTranslatableComponentJson("block.minecraft.ominous_banner")
                        )
                     )
                  )
                  .update(DSL.remainderFinder(), p_392883_ -> p_392883_.set("minecraft:rarity", p_392883_.createString("uncommon")))
               : p_392893_;
         }
      );
   }
}
