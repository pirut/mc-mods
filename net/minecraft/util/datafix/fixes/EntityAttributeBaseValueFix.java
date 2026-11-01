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

   public EntityAttributeBaseValueFix(Schema p_390654_, String p_390649_, String p_390648_, String p_390655_, DoubleUnaryOperator p_390659_) {
      super(p_390654_, false, p_390649_, References.ENTITY, p_390648_);
      this.attributeId = p_390655_;
      this.valueFixer = p_390659_;
   }

   @Override
   protected Typed<?> fix(Typed<?> p_390650_) {
      return p_390650_.update(DSL.remainderFinder(), this::fixValue);
   }

   private Dynamic<?> fixValue(Dynamic<?> p_390652_) {
      return p_390652_.update("attributes", p_390656_ -> p_390652_.createList(p_390656_.asStream().map(p_390653_ -> {
         String s = NamespacedSchema.ensureNamespaced(p_390653_.get("id").asString(""));
         if (!s.equals(this.attributeId)) {
            return (Dynamic)p_390653_;
         } else {
            double d0 = p_390653_.get("base").asDouble(0.0);
            return p_390653_.set("base", p_390653_.createDouble(this.valueFixer.applyAsDouble(d0)));
         }
      })));
   }
}
