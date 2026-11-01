package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4059 extends NamespacedSchema {
   public V4059(int p_366825_, Schema p_366722_) {
      super(p_366825_, p_366722_);
   }

   public static SequencedMap<String, Supplier<TypeTemplate>> components(Schema p_371581_) {
      SequencedMap<String, Supplier<TypeTemplate>> sequencedmap = V3818_3.components(p_371581_);
      sequencedmap.remove("minecraft:food");
      sequencedmap.put("minecraft:use_remainder", () -> References.ITEM_STACK.in(p_371581_));
      sequencedmap.put(
         "minecraft:equippable",
         () -> DSL.optionalFields("allowed_entities", DSL.or(References.ENTITY_NAME.in(p_371581_), DSL.list(References.ENTITY_NAME.in(p_371581_))))
      );
      return sequencedmap;
   }

   public void registerTypes(Schema p_366856_, Map<String, Supplier<TypeTemplate>> p_366843_, Map<String, Supplier<TypeTemplate>> p_366605_) {
      super.registerTypes(p_366856_, p_366843_, p_366605_);
      p_366856_.registerType(true, References.DATA_COMPONENTS, () -> DSL.optionalFieldsLazy(components(p_366856_)));
   }
}
