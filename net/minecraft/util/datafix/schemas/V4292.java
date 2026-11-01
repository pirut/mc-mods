package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4292 extends NamespacedSchema {
   public V4292(int p_394622_, Schema p_394504_) {
      super(p_394622_, p_394504_);
   }

   public void registerTypes(Schema p_394453_, Map<String, Supplier<TypeTemplate>> p_394165_, Map<String, Supplier<TypeTemplate>> p_394549_) {
      super.registerTypes(p_394453_, p_394165_, p_394549_);
      p_394453_.registerType(
         true,
         References.TEXT_COMPONENT,
         () -> DSL.or(
            DSL.or(DSL.constType(DSL.string()), DSL.list(References.TEXT_COMPONENT.in(p_394453_))),
            DSL.optionalFields(
               "extra",
               DSL.list(References.TEXT_COMPONENT.in(p_394453_)),
               "separator",
               References.TEXT_COMPONENT.in(p_394453_),
               "hover_event",
               DSL.taggedChoice(
                  "action",
                  DSL.string(),
                  Map.of(
                     "show_text",
                     DSL.optionalFields("value", References.TEXT_COMPONENT.in(p_394453_)),
                     "show_item",
                     References.ITEM_STACK.in(p_394453_),
                     "show_entity",
                     DSL.optionalFields("id", References.ENTITY_NAME.in(p_394453_), "name", References.TEXT_COMPONENT.in(p_394453_))
                  )
               )
            )
         )
      );
   }
}
