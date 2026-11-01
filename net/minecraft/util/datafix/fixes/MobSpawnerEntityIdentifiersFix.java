package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import java.util.stream.Stream;

public class MobSpawnerEntityIdentifiersFix extends DataFix {
   public MobSpawnerEntityIdentifiersFix(Schema p_16451_, boolean p_16452_) {
      super(p_16451_, p_16452_);
   }

   private Dynamic<?> fix(Dynamic<?> p_16457_) {
      if (!"MobSpawner".equals(p_16457_.get("id").asString(""))) {
         return p_16457_;
      } else {
         Optional<String> $$1 = p_16457_.get("EntityId").asString().result();
         if ($$1.isPresent()) {
            Dynamic<?> $$2 = (Dynamic<?>)DataFixUtils.orElse(p_16457_.get("SpawnData").result(), p_16457_.emptyMap());
            $$2 = $$2.set("id", $$2.createString($$1.get().isEmpty() ? "Pig" : $$1.get()));
            p_16457_ = p_16457_.set("SpawnData", $$2);
            p_16457_ = p_16457_.remove("EntityId");
         }

         Optional<? extends Stream<? extends Dynamic<?>>> $$3 = p_16457_.get("SpawnPotentials").asStreamOpt().result();
         if ($$3.isPresent()) {
            p_16457_ = p_16457_.set(
               "SpawnPotentials",
               p_16457_.createList(
                  $$3.get()
                     .map(
                        p_337650_ -> {
                           Optional<String> $$1x = p_337650_.get("Type").asString().result();
                           if ($$1x.isPresent()) {
                              Dynamic<?> $$2 = ((Dynamic)DataFixUtils.orElse(p_337650_.get("Properties").result(), p_337650_.emptyMap()))
                                 .set("id", p_337650_.createString($$1x.get()));
                              return p_337650_.set("Entity", $$2).remove("Type").remove("Properties");
                           } else {
                              return (Dynamic)p_337650_;
                           }
                        }
                     )
               )
            );
         }

         return p_16457_;
      }
   }

   public TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getOutputSchema().getType(References.UNTAGGED_SPAWNER);
      return this.fixTypeEverywhereTyped("MobSpawnerEntityIdentifiersFix", this.getInputSchema().getType(References.UNTAGGED_SPAWNER), $$0, p_337649_ -> {
         Dynamic<?> $$2 = (Dynamic<?>)p_337649_.get(DSL.remainderFinder());
         $$2 = $$2.set("id", $$2.createString("MobSpawner"));
         DataResult<? extends Pair<? extends Typed<?>, ?>> $$3 = $$0.readTyped(this.fix($$2));
         return $$3.result().isEmpty() ? p_337649_ : (Typed)((Pair)$$3.result().get()).getFirst();
      });
   }
}
