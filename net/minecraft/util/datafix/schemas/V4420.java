package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4420 extends NamespacedSchema {
   public V4420(int p_416209_, Schema p_416517_) {
      super(p_416209_, p_416517_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_416579_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_416579_);
      p_416579_.register(map, "minecraft:area_effect_cloud", p_416525_ -> DSL.optionalFields("custom_particle", References.PARTICLE.in(p_416579_)));
      return map;
   }
}
