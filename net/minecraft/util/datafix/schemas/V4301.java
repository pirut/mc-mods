package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4301 extends NamespacedSchema {
   public V4301(int p_397585_, Schema p_397676_) {
      super(p_397585_, p_397676_);
   }

   public void registerTypes(Schema p_397799_, Map<String, Supplier<TypeTemplate>> p_397329_, Map<String, Supplier<TypeTemplate>> p_397128_) {
      super.registerTypes(p_397799_, p_397329_, p_397128_);
      p_397799_.registerType(
         true,
         References.ENTITY_EQUIPMENT,
         () -> DSL.optional(
            DSL.field(
               "equipment",
               DSL.optionalFields(
                  new Pair[]{
                     Pair.of("mainhand", References.ITEM_STACK.in(p_397799_)),
                     Pair.of("offhand", References.ITEM_STACK.in(p_397799_)),
                     Pair.of("feet", References.ITEM_STACK.in(p_397799_)),
                     Pair.of("legs", References.ITEM_STACK.in(p_397799_)),
                     Pair.of("chest", References.ITEM_STACK.in(p_397799_)),
                     Pair.of("head", References.ITEM_STACK.in(p_397799_)),
                     Pair.of("body", References.ITEM_STACK.in(p_397799_)),
                     Pair.of("saddle", References.ITEM_STACK.in(p_397799_))
                  }
               )
            )
         )
      );
   }
}
