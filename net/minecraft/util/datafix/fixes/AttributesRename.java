package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.function.UnaryOperator;

public class AttributesRename extends DataFix {
   private final String name;
   private final UnaryOperator<String> renames;

   public AttributesRename(Schema p_14671_, String p_326430_, UnaryOperator<String> p_326062_) {
      super(p_14671_, false);
      this.name = p_326430_;
      this.renames = p_326062_;
   }

   protected TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getInputSchema().getType(References.ITEM_STACK);
      OpticFinder<?> $$1 = $$0.findField("tag");
      return TypeRewriteRule.seq(
         this.fixTypeEverywhereTyped(this.name + " (ItemStack)", $$0, p_325649_ -> p_325649_.updateTyped($$1, this::fixItemStackTag)),
         new TypeRewriteRule[]{
            this.fixTypeEverywhereTyped(this.name + " (Entity)", this.getInputSchema().getType(References.ENTITY), this::fixEntity),
            this.fixTypeEverywhereTyped(this.name + " (Player)", this.getInputSchema().getType(References.PLAYER), this::fixEntity)
         }
      );
   }

   private Dynamic<?> fixName(Dynamic<?> p_14678_) {
      return (Dynamic<?>)DataFixUtils.orElse(p_14678_.asString().result().map(this.renames).map(p_14678_::createString), p_14678_);
   }

   private Typed<?> fixItemStackTag(Typed<?> p_14676_) {
      return p_14676_.update(
         DSL.remainderFinder(),
         p_325650_ -> p_325650_.update(
            "AttributeModifiers",
            p_337594_ -> (Dynamic)DataFixUtils.orElse(
               p_337594_.asStreamOpt()
                  .result()
                  .map(p_325642_ -> p_325642_.map(p_325645_ -> p_325645_.update("AttributeName", this::fixName)))
                  .map(p_337594_::createList),
               p_337594_
            )
         )
      );
   }

   private Typed<?> fixEntity(Typed<?> p_14684_) {
      return p_14684_.update(
         DSL.remainderFinder(),
         p_325651_ -> p_325651_.update(
            "Attributes",
            p_337595_ -> (Dynamic)DataFixUtils.orElse(
               p_337595_.asStreamOpt()
                  .result()
                  .map(p_325646_ -> p_325646_.map(p_325643_ -> p_325643_.update("Name", this::fixName)))
                  .map(p_337595_::createList),
               p_337595_
            )
         )
      );
   }
}
