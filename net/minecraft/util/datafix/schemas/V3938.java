package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3938 extends NamespacedSchema {
   public V3938(int p_345954_, Schema p_345207_) {
      super(p_345954_, p_345207_);
   }

   protected static TypeTemplate abstractArrow(Schema p_344799_) {
      return DSL.optionalFields(
         "inBlockState", References.BLOCK_STATE.in(p_344799_), "item", References.ITEM_STACK.in(p_344799_), "weapon", References.ITEM_STACK.in(p_344799_)
      );
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_346323_) {
      Map<String, Supplier<TypeTemplate>> $$1 = super.registerEntities(p_346323_);
      p_346323_.register($$1, "minecraft:spectral_arrow", () -> abstractArrow(p_346323_));
      p_346323_.register($$1, "minecraft:arrow", () -> abstractArrow(p_346323_));
      return $$1;
   }
}
