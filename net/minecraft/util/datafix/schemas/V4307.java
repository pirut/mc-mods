package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4307 extends NamespacedSchema {
   public V4307(int p_399742_, Schema p_400220_) {
      super(p_399742_, p_400220_);
   }

   public static SequencedMap<String, Supplier<TypeTemplate>> components(Schema p_399944_) {
      SequencedMap<String, Supplier<TypeTemplate>> sequencedmap = V4059.components(p_399944_);
      sequencedmap.put("minecraft:can_place_on", () -> adventureModePredicate(p_399944_));
      sequencedmap.put("minecraft:can_break", () -> adventureModePredicate(p_399944_));
      return sequencedmap;
   }

   private static TypeTemplate adventureModePredicate(Schema p_399617_) {
      TypeTemplate typetemplate = DSL.optionalFields("blocks", DSL.or(References.BLOCK_NAME.in(p_399617_), DSL.list(References.BLOCK_NAME.in(p_399617_))));
      return DSL.or(typetemplate, DSL.list(typetemplate));
   }

   public void registerTypes(Schema p_400294_, Map<String, Supplier<TypeTemplate>> p_399732_, Map<String, Supplier<TypeTemplate>> p_399997_) {
      super.registerTypes(p_400294_, p_399732_, p_399997_);
      p_400294_.registerType(true, References.DATA_COMPONENTS, () -> DSL.optionalFieldsLazy(components(p_400294_)));
   }
}
