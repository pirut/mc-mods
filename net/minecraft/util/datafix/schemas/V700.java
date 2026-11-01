package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V700 extends Schema {
   public V700(int p_17985_, Schema p_17986_) {
      super(p_17985_, p_17986_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_17994_) {
      Map<String, Supplier<TypeTemplate>> $$1 = super.registerEntities(p_17994_);
      p_17994_.register($$1, "ElderGuardian", () -> V100.equipment(p_17994_));
      return $$1;
   }
}
