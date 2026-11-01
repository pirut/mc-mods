package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V4421 extends NamespacedSchema {
   public V4421(int p_415966_, Schema p_416078_) {
      super(p_415966_, p_416078_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_416676_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_416676_);
      p_416676_.registerSimple(map, "minecraft:happy_ghast");
      return map;
   }
}
