package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3813 extends NamespacedSchema {
   public V3813(int p_393662_, Schema p_393895_) {
      super(p_393662_, p_393895_);
   }

   public void registerTypes(Schema p_394531_, Map<String, Supplier<TypeTemplate>> p_394304_, Map<String, Supplier<TypeTemplate>> p_394148_) {
      super.registerTypes(p_394531_, p_394304_, p_394148_);
      p_394531_.registerType(
         false,
         References.SAVED_DATA_MAP_DATA,
         () -> DSL.optionalFields("data", DSL.optionalFields("banners", DSL.list(DSL.optionalFields("name", References.TEXT_COMPONENT.in(p_394531_)))))
      );
   }
}
