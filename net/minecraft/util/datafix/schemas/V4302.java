package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4302 extends NamespacedSchema {
   public V4302(int p_397269_, Schema p_397662_) {
      super(p_397269_, p_397662_);
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema p_397880_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(p_397880_);
      p_397880_.registerSimple(map, "minecraft:test_block");
      p_397880_.register(
         map,
         "minecraft:test_instance_block",
         () -> DSL.optionalFields(
            "data",
            DSL.optionalFields("error_message", References.TEXT_COMPONENT.in(p_397880_)),
            "errors",
            DSL.list(DSL.optionalFields("text", References.TEXT_COMPONENT.in(p_397880_)))
         )
      );
      return map;
   }
}
