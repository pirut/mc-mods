package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V4071 extends NamespacedSchema {
   public V4071(int p_380159_, Schema p_379344_) {
      super(p_380159_, p_379344_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_379997_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_379997_);
      p_379997_.registerSimple(map, "minecraft:creaking");
      p_379997_.registerSimple(map, "minecraft:creaking_transient");
      return map;
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema p_379562_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(p_379562_);
      this.registerSimple(map, "minecraft:creaking_heart");
      return map;
   }
}
