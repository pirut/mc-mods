package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.List;

public class SpawnerDataFix extends DataFix {
   public SpawnerDataFix(Schema p_185133_) {
      super(p_185133_, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getInputSchema().getType(References.UNTAGGED_SPAWNER);
      Type<?> $$1 = this.getOutputSchema().getType(References.UNTAGGED_SPAWNER);
      OpticFinder<?> $$2 = $$0.findField("SpawnData");
      Type<?> $$3 = $$1.findField("SpawnData").type();
      OpticFinder<?> $$4 = $$0.findField("SpawnPotentials");
      Type<?> $$5 = $$1.findField("SpawnPotentials").type();
      return this.fixTypeEverywhereTyped(
         "Fix mob spawner data structure",
         $$0,
         $$1,
         p_185139_ -> p_185139_.updateTyped($$2, $$3, p_185154_ -> this.wrapEntityToSpawnData($$3, p_185154_))
            .updateTyped($$4, $$5, p_185151_ -> this.wrapSpawnPotentialsToWeightedEntries($$5, p_185151_))
      );
   }

   private <T> Typed<T> wrapEntityToSpawnData(Type<T> p_185141_, Typed<?> p_185142_) {
      DynamicOps<?> $$2 = p_185142_.getOps();
      return new Typed(p_185141_, $$2, Pair.of(p_185142_.getValue(), new Dynamic($$2)));
   }

   private <T> Typed<T> wrapSpawnPotentialsToWeightedEntries(Type<T> p_185147_, Typed<?> p_185148_) {
      DynamicOps<?> $$2 = p_185148_.getOps();
      List<?> $$3 = (List<?>)p_185148_.getValue();
      List<?> $$4 = $$3.stream().map(p_185145_ -> {
         Pair<Object, Dynamic<?>> $$2x = (Pair<Object, Dynamic<?>>)p_185145_;
         int $$3x = ((Dynamic)$$2x.getSecond()).get("Weight").asNumber().result().orElse(1).intValue();
         Dynamic<?> $$4x = new Dynamic($$2);
         $$4x = $$4x.set("weight", $$4x.createInt($$3x));
         Dynamic<?> $$5 = ((Dynamic)$$2x.getSecond()).remove("Weight").remove("Entity");
         return Pair.of(Pair.of($$2x.getFirst(), $$5), $$4x);
      }).toList();
      return new Typed(p_185147_, $$2, $$4);
   }
}
