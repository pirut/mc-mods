package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V1451_5 extends NamespacedSchema {
   public V1451_5(int p_17527_, Schema p_17528_) {
      super(p_17527_, p_17528_);
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema p_17530_) {
      Map<String, Supplier<TypeTemplate>> $$1 = super.registerBlockEntities(p_17530_);
      $$1.remove("minecraft:flower_pot");
      $$1.remove("minecraft:noteblock");
      return $$1;
   }
}
