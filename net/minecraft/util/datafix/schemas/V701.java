package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V701 extends Schema {
   public V701(int p_17996_, Schema p_17997_) {
      super(p_17996_, p_17997_);
   }

   protected static void registerMob(Schema p_18001_, Map<String, Supplier<TypeTemplate>> p_18002_, String p_18003_) {
      p_18001_.register(p_18002_, p_18003_, () -> V100.equipment(p_18001_));
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_18005_) {
      Map<String, Supplier<TypeTemplate>> $$1 = super.registerEntities(p_18005_);
      registerMob(p_18005_, $$1, "WitherSkeleton");
      registerMob(p_18005_, $$1, "Stray");
      return $$1;
   }
}
