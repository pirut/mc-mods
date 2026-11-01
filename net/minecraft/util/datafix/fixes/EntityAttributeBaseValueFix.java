package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.function.DoubleUnaryOperator;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class EntityAttributeBaseValueFix extends NamedEntityFix {
   private final String attributeId;
   private final DoubleUnaryOperator valueFixer;

   public EntityAttributeBaseValueFix(Schema outputSchema, String name, String entityName, String attributeId, DoubleUnaryOperator valueFixer) {
      super(outputSchema, false, name, References.ENTITY, entityName);
      this.attributeId = attributeId;
      this.valueFixer = valueFixer;
   }

   @Override
   protected Typed<?> fix(Typed<?> typed) {
      return typed.update(DSL.remainderFinder(), this::fixValue);
   }

   private Dynamic<?> fixValue(Dynamic<?> tag) {
      return tag.update("attributes", dynamic2 -> tag.createList(dynamic2.asStream().map(dynamicx -> {
         String string = NamespacedSchema.ensureNamespaced(dynamicx.get("id").asString(""));
         if (!string.equals(this.attributeId)) {
            return dynamicx;
         } else {
            double d = dynamicx.get("base").asDouble(0.0);
            return dynamicx.set("base", dynamicx.createDouble(this.valueFixer.applyAsDouble(d)));
         }
      })));
   }
}
