package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V2509 extends NamespacedSchema {
   public V2509(int p_17881_, Schema p_17882_) {
      super(p_17881_, p_17882_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_17890_) {
      Map<String, Supplier<TypeTemplate>> $$1 = super.registerEntities(p_17890_);
      $$1.remove("minecraft:zombie_pigman");
      p_17890_.register($$1, "minecraft:zombified_piglin", () -> V100.equipment(p_17890_));
      return $$1;
   }
}
