package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V2511_1 extends NamespacedSchema {
   public V2511_1(int p_399579_, Schema p_399511_) {
      super(p_399579_, p_399511_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_399842_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_399842_);
      p_399842_.register(map, "minecraft:potion", p_399598_ -> DSL.optionalFields("Item", References.ITEM_STACK.in(p_399842_)));
      return map;
   }
}
