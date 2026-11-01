package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4533 extends NamespacedSchema {
   public V4533(int p_435726_, Schema p_432755_) {
      super(p_435726_, p_432755_);
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema p_435845_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(p_435845_);
      p_435845_.register(map, "minecraft:shelf", () -> DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(p_435845_))));
      return map;
   }
}
