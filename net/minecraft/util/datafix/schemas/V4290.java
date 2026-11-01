package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4290 extends NamespacedSchema {
   public V4290(int p_393582_, Schema p_394408_) {
      super(p_393582_, p_394408_);
   }

   public void registerTypes(Schema p_393996_, Map<String, Supplier<TypeTemplate>> p_393833_, Map<String, Supplier<TypeTemplate>> p_393752_) {
      super.registerTypes(p_393996_, p_393833_, p_393752_);
      p_393996_.registerType(
         true,
         References.TEXT_COMPONENT,
         () -> DSL.or(
            DSL.or(DSL.constType(DSL.string()), DSL.list(References.TEXT_COMPONENT.in(p_393996_))),
            DSL.optionalFields(
               "extra",
               DSL.list(References.TEXT_COMPONENT.in(p_393996_)),
               "separator",
               References.TEXT_COMPONENT.in(p_393996_),
               "hoverEvent",
               DSL.taggedChoice(
                  "action",
                  DSL.string(),
                  Map.of(
                     "show_text",
                     DSL.optionalFields("contents", References.TEXT_COMPONENT.in(p_393996_)),
                     "show_item",
                     DSL.optionalFields("contents", DSL.or(References.ITEM_STACK.in(p_393996_), References.ITEM_NAME.in(p_393996_))),
                     "show_entity",
                     DSL.optionalFields("type", References.ENTITY_NAME.in(p_393996_), "name", References.TEXT_COMPONENT.in(p_393996_))
                  )
               )
            )
         )
      );
   }
}
