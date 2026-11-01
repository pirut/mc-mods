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

   public AttributesRenameFix(Schema outputSchema, String name, UnaryOperator<String> renames) {
      super(outputSchema, false);
      this.name = name;
      this.renames = renames;
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

   private Typed<?> fixDataComponents(Typed<?> dataComponents) {
      return dataComponents.update(
         DSL.remainderFinder(),
         dynamic -> dynamic.update(
            "minecraft:attribute_modifiers",
            dynamicx -> dynamicx.update(
               "modifiers",
               dynamicxx -> (Dynamic)DataFixUtils.orElse(
                  dynamicxx.asStreamOpt().result().map(stream -> stream.map(this::fixTypeField)).map(dynamicxx::createList), dynamicxx
               )
            )
         )
      );
   }

   private Typed<?> fixEntity(Typed<?> data) {
      return data.update(
         DSL.remainderFinder(),
         dynamic -> dynamic.update(
            "attributes",
            dynamicx -> (Dynamic)DataFixUtils.orElse(
               dynamicx.asStreamOpt().result().map(stream -> stream.map(this::fixIdField)).map(dynamicx::createList), dynamicx
            )
         )
      );
   }

   private Dynamic<?> fixIdField(Dynamic<?> data) {
      return ExtraDataFixUtils.fixStringField(data, "id", this.renames);
   }

   private Dynamic<?> fixTypeField(Dynamic<?> data) {
      return ExtraDataFixUtils.fixStringField(data, "type", this.renames);
   }
}
