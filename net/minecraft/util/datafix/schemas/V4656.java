package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V4656 extends NamespacedSchema {
   public V4656(int p_461029_, Schema p_461219_) {
      super(p_461029_, p_461219_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_461203_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_461203_);
      p_461203_.registerSimple(map, "minecraft:camel_husk");
      p_461203_.registerSimple(map, "minecraft:parched");
      return map;
   }
}
