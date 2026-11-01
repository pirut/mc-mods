package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.function.UnaryOperator;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public class AttributesRenameFix extends DataFix {
   private final String name;
   private final UnaryOperator<String> renames;

   public AttributesRenameFix(Schema p_365475_, String p_365479_, UnaryOperator<String> p_364539_) {
      super(p_365475_, false);
      this.name = p_365479_;
      this.renames = p_364539_;
   }

   protected TypeRewriteRule makeRule() {
      return TypeRewriteRule.seq(
         this.fixTypeEverywhereTyped(this.name + " (Components)", this.getInputSchema().getType(References.DATA_COMPONENTS), this::fixDataComponents),
         new TypeRewriteRule[]{
            this.fixTypeEverywhereTyped(this.name + " (Entity)", this.getInputSchema().getType(References.ENTITY), this::fixEntity),
            this.fixTypeEverywhereTyped(this.name + " (Player)", this.getInputSchema().getType(References.PLAYER), this::fixEntity)
         }
      );
   }

   private Typed<?> fixDataComponents(Typed<?> p_363762_) {
      return p_363762_.update(
         DSL.remainderFinder(),
         p_365366_ -> p_365366_.update(
            "minecraft:attribute_modifiers",
            p_364961_ -> p_364961_.update(
               "modifiers",
               p_360482_ -> (Dynamic)DataFixUtils.orElse(
                  p_360482_.asStreamOpt().result().map(p_364944_ -> p_364944_.map(this::fixTypeField)).map(p_360482_::createList), p_360482_
               )
            )
         )
      );
   }

   private Typed<?> fixEntity(Typed<?> p_362622_) {
      return p_362622_.update(
         DSL.remainderFinder(),
         p_363349_ -> p_363349_.update(
            "attributes",
            p_365176_ -> (Dynamic)DataFixUtils.orElse(
               p_365176_.asStreamOpt().result().map(p_360801_ -> p_360801_.map(this::fixIdField)).map(p_365176_::createList), p_365176_
            )
         )
      );
   }

   private Dynamic<?> fixIdField(Dynamic<?> p_360783_) {
      return ExtraDataFixUtils.fixStringField(p_360783_, "id", this.renames);
   }

   private Dynamic<?> fixTypeField(Dynamic<?> p_360280_) {
      return ExtraDataFixUtils.fixStringField(p_360280_, "type", this.renames);
   }
}
