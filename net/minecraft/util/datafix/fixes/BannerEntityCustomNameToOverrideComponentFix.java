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
import com.mojang.serialization.OptionalDynamic;
import java.util.Map;
import net.minecraft.util.datafix.ComponentDataFixUtils;

public class BannerEntityCustomNameToOverrideComponentFix extends DataFix {
   public BannerEntityCustomNameToOverrideComponentFix(Schema p_338803_) {
      super(p_338803_, false);
   }

   public TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getInputSchema().getType(References.BLOCK_ENTITY);
      TaggedChoiceType<?> $$1 = this.getInputSchema().findChoiceType(References.BLOCK_ENTITY);
      OpticFinder<?> $$2 = $$0.findField("components");
      return this.fixTypeEverywhereTyped("Banner entity custom_name to item_name component fix", $$0, p_338407_ -> {
         Object $$3 = ((Pair)p_338407_.get($$1.finder())).getFirst();
         return $$3.equals("minecraft:banner") ? this.fix(p_338407_, $$2) : p_338407_;
      });
   }

   private Typed<?> fix(Typed<?> p_338738_, OpticFinder<?> p_338876_) {
      Dynamic<?> $$2 = (Dynamic<?>)p_338738_.getOptional(DSL.remainderFinder()).orElseThrow();
      OptionalDynamic<?> $$3 = $$2.get("CustomName");
      boolean $$4 = $$3.asString()
         .result()
         .flatMap(ComponentDataFixUtils::extractTranslationString)
         .filter(p_338664_ -> p_338664_.equals("block.minecraft.ominous_banner"))
         .isPresent();
      if ($$4) {
         Typed<?> $$5 = p_338738_.getOrCreateTyped(p_338876_)
            .update(
               DSL.remainderFinder(),
               p_338230_ -> p_338230_.set("minecraft:item_name", (Dynamic)$$3.result().get())
                  .set("minecraft:hide_additional_tooltip", p_338230_.createMap(Map.of()))
            );
         return p_338738_.set(p_338876_, $$5).set(DSL.remainderFinder(), $$2.remove("CustomName"));
      } else {
         return p_338738_;
      }
   }
}
