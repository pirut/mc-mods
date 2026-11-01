package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ReorganizePoi extends DataFix {
   public ReorganizePoi(Schema p_16853_, boolean p_16854_) {
      super(p_16853_, p_16854_);
   }

   protected TypeRewriteRule makeRule() {
      Type<Pair<String, Dynamic<?>>> $$0 = DSL.named(References.POI_CHUNK.typeName(), DSL.remainderType());
      if (!Objects.equals($$0, this.getInputSchema().getType(References.POI_CHUNK))) {
         throw new IllegalStateException("Poi type is not what was expected.");
      } else {
         return this.fixTypeEverywhere("POI reorganization", $$0, p_16860_ -> p_145640_ -> p_145640_.mapSecond(ReorganizePoi::cap));
      }
   }

   private static <T> Dynamic<T> cap(Dynamic<T> p_16858_) {
      Map<Dynamic<T>, Dynamic<T>> $$1 = Maps.newHashMap();

      for (int $$2 = 0; $$2 < 16; $$2++) {
         String $$3 = String.valueOf($$2);
         Optional<Dynamic<T>> $$4 = p_16858_.get($$3).result();
         if ($$4.isPresent()) {
            Dynamic<T> $$5 = $$4.get();
            Dynamic<T> $$6 = p_16858_.createMap(ImmutableMap.of(p_16858_.createString("Records"), $$5));
            $$1.put(p_16858_.createInt($$2), $$6);
            p_16858_ = p_16858_.remove($$3);
         }
      }

      return p_16858_.set("Sections", p_16858_.createMap($$1));
   }
}
