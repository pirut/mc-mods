package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class EmptyItemInVillagerTradeFix extends DataFix {
   public EmptyItemInVillagerTradeFix(Schema p_340827_) {
      super(p_340827_, false);
   }

   public TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getInputSchema().getType(References.VILLAGER_TRADE);
      return this.writeFixAndRead("EmptyItemInVillagerTradeFix", $$0, $$0, p_342001_ -> {
         Dynamic<?> $$1 = p_342001_.get("buyB").orElseEmptyMap();
         String $$2 = NamespacedSchema.ensureNamespaced($$1.get("id").asString("minecraft:air"));
         int $$3 = $$1.get("count").asInt(0);
         return !$$2.equals("minecraft:air") && $$3 != 0 ? p_342001_ : p_342001_.remove("buyB");
      });
   }
}
