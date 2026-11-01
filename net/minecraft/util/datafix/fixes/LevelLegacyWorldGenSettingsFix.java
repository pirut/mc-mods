package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Optional;

public class LevelLegacyWorldGenSettingsFix extends DataFix {
   private static final String WORLD_GEN_SETTINGS = "WorldGenSettings";
   private static final List<String> OLD_SETTINGS_KEYS = List.of(
      "RandomSeed", "generatorName", "generatorOptions", "generatorVersion", "legacy_custom_options", "MapFeatures", "BonusChest"
   );

   public LevelLegacyWorldGenSettingsFix(Schema p_307356_) {
      super(p_307356_, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "LevelLegacyWorldGenSettingsFix", this.getInputSchema().getType(References.LEVEL), p_307516_ -> p_307516_.update(DSL.remainderFinder(), p_307487_ -> {
            Dynamic<?> $$1 = p_307487_.get("WorldGenSettings").orElseEmptyMap();

            for (String $$2 : OLD_SETTINGS_KEYS) {
               Optional<? extends Dynamic<?>> $$3 = p_307487_.get($$2).result();
               if ($$3.isPresent()) {
                  p_307487_ = p_307487_.remove($$2);
                  $$1 = $$1.set($$2, $$3.get());
               }
            }

            return p_307487_.set("WorldGenSettings", $$1);
         })
      );
   }
}
