package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V4543 extends NamespacedSchema {
   public V4543(int p_447189_, Schema p_445615_) {
      super(p_447189_, p_445615_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_446452_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_446452_);
      p_446452_.registerSimple(map, "minecraft:mannequin");
      return map;
   }
}
