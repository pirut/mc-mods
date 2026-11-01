package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4070 extends NamespacedSchema {
   public V4070(int p_379397_, Schema p_379851_) {
      super(p_379397_, p_379851_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_379327_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_379327_);
      p_379327_.registerSimple(map, "minecraft:pale_oak_boat");
      p_379327_.register(map, "minecraft:pale_oak_chest_boat", p_379795_ -> DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(p_379327_))));
      return map;
   }
}
