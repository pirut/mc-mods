package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3439 extends NamespacedSchema {
   public V3439(int p_394422_, Schema p_393805_) {
      super(p_394422_, p_393805_);
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema p_394240_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(p_394240_);
      this.register(map, "minecraft:sign", () -> sign(p_394240_));
      return map;
   }

   public static TypeTemplate sign(Schema p_393656_) {
      return DSL.optionalFields(
         "front_text",
         DSL.optionalFields(
            "messages", DSL.list(References.TEXT_COMPONENT.in(p_393656_)), "filtered_messages", DSL.list(References.TEXT_COMPONENT.in(p_393656_))
         ),
         "back_text",
         DSL.optionalFields(
            "messages", DSL.list(References.TEXT_COMPONENT.in(p_393656_)), "filtered_messages", DSL.list(References.TEXT_COMPONENT.in(p_393656_))
         )
      );
   }
}
