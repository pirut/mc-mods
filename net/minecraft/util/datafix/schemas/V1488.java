package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V1488 extends NamespacedSchema {
   public V1488(int p_394468_, Schema p_393648_) {
      super(p_394468_, p_393648_);
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema p_393872_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(p_393872_);
      p_393872_.register(
         map,
         "minecraft:command_block",
         () -> DSL.optionalFields("CustomName", References.TEXT_COMPONENT.in(p_393872_), "LastOutput", References.TEXT_COMPONENT.in(p_393872_))
      );
      return map;
   }
}
