package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4306 extends NamespacedSchema {
   public V4306(int p_399939_, Schema p_399481_) {
      super(p_399939_, p_399481_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_400054_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_400054_);
      map.remove("minecraft:potion");
      p_400054_.register(map, "minecraft:splash_potion", () -> DSL.optionalFields("Item", References.ITEM_STACK.in(p_400054_)));
      p_400054_.register(map, "minecraft:lingering_potion", () -> DSL.optionalFields("Item", References.ITEM_STACK.in(p_400054_)));
      return map;
   }
}
