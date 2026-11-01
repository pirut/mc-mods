package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V1906 extends NamespacedSchema {
   public V1906(int p_17768_, Schema p_17769_) {
      super(p_17768_, p_17769_);
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema p_17780_) {
      Map<String, Supplier<TypeTemplate>> $$1 = super.registerBlockEntities(p_17780_);
      registerInventory(p_17780_, $$1, "minecraft:barrel");
      registerInventory(p_17780_, $$1, "minecraft:smoker");
      registerInventory(p_17780_, $$1, "minecraft:blast_furnace");
      p_17780_.register($$1, "minecraft:lectern", p_17774_ -> DSL.optionalFields("Book", References.ITEM_STACK.in(p_17780_)));
      p_17780_.registerSimple($$1, "minecraft:bell");
      return $$1;
   }

   protected static void registerInventory(Schema p_17776_, Map<String, Supplier<TypeTemplate>> p_17777_, String p_17778_) {
      p_17776_.register(p_17777_, p_17778_, () -> DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(p_17776_))));
   }
}
