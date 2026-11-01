package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V4648 extends NamespacedSchema {
   public V4648(int p_455145_, Schema p_455468_) {
      super(p_455145_, p_455468_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_454785_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_454785_);
      p_454785_.registerSimple(map, "minecraft:nautilus");
      p_454785_.registerSimple(map, "minecraft:zombie_nautilus");
      return map;
   }
}
