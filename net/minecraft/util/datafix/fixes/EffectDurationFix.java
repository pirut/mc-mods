package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import java.util.Set;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class EffectDurationFix extends DataFix {
   private static final Set<String> ITEM_TYPES = Set.of("minecraft:potion", "minecraft:splash_potion", "minecraft:lingering_potion", "minecraft:tipped_arrow");

   public EffectDurationFix(Schema p_267976_) {
      super(p_267976_, false);
   }

   protected TypeRewriteRule makeRule() {
      Schema $$0 = this.getInputSchema();
      Type<?> $$1 = this.getInputSchema().getType(References.ITEM_STACK);
      OpticFinder<Pair<String, String>> $$2 = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
      OpticFinder<?> $$3 = $$1.findField("tag");
      return TypeRewriteRule.seq(
         this.fixTypeEverywhereTyped(
            "EffectDurationEntity", $$0.getType(References.ENTITY), p_268118_ -> p_268118_.update(DSL.remainderFinder(), this::updateEntity)
         ),
         new TypeRewriteRule[]{
            this.fixTypeEverywhereTyped(
               "EffectDurationPlayer", $$0.getType(References.PLAYER), p_268326_ -> p_268326_.update(DSL.remainderFinder(), this::updateEntity)
            ),
            this.fixTypeEverywhereTyped("EffectDurationItem", $$1, p_268235_ -> {
               Optional<Pair<String, String>> $$3x = p_268235_.getOptional($$2);
               if ($$3x.filter(ITEM_TYPES::contains).isPresent()) {
                  Optional<? extends Typed<?>> $$4 = p_268235_.getOptionalTyped($$3);
                  if ($$4.isPresent()) {
                     Dynamic<?> $$5 = (Dynamic<?>)$$4.get().get(DSL.remainderFinder());
                     Typed<?> $$6 = $$4.get().set(DSL.remainderFinder(), $$5.update("CustomPotionEffects", this::fix));
                     return p_268235_.set($$3, $$6);
                  }
               }

               return p_268235_;
            })
         }
      );
   }

   private Dynamic<?> fixEffect(Dynamic<?> p_267989_) {
      return p_267989_.update("FactorCalculationData", p_268051_ -> {
         int $$2 = p_268051_.get("effect_changed_timestamp").asInt(-1);
         p_268051_ = p_268051_.remove("effect_changed_timestamp");
         int $$3 = p_267989_.get("Duration").asInt(-1);
         int $$4 = $$2 - $$3;
         return p_268051_.set("ticks_active", p_268051_.createInt($$4));
      });
   }

   private Dynamic<?> fix(Dynamic<?> p_268201_) {
      return p_268201_.createList(p_268201_.asStream().map(this::fixEffect));
   }

   private Dynamic<?> updateEntity(Dynamic<?> p_268005_) {
      p_268005_ = p_268005_.update("Effects", this::fix);
      p_268005_ = p_268005_.update("ActiveEffects", this::fix);
      return p_268005_.update("CustomPotionEffects", this::fix);
   }
}
