package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3325 extends NamespacedSchema {
   public V3325(int p_270387_, Schema p_270374_) {
      super(p_270387_, p_270374_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_270629_) {
      Map<String, Supplier<TypeTemplate>> $$1 = super.registerEntities(p_270629_);
      p_270629_.register($$1, "minecraft:item_display", p_270589_ -> DSL.optionalFields("item", References.ITEM_STACK.in(p_270629_)));
      p_270629_.register($$1, "minecraft:block_display", p_270174_ -> DSL.optionalFields("block_state", References.BLOCK_STATE.in(p_270629_)));
      p_270629_.registerSimple($$1, "minecraft:text_display");
      return $$1;
   }
}
