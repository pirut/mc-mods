package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Optional;

public class OverreachingTickFix extends DataFix {
   public OverreachingTickFix(Schema p_207654_) {
      super(p_207654_, false);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getInputSchema().getType(References.CHUNK);
      OpticFinder<?> $$1 = $$0.findField("block_ticks");
      return this.fixTypeEverywhereTyped("Handle ticks saved in the wrong chunk", $$0, p_337660_ -> {
         Optional<? extends Typed<?>> $$2 = p_337660_.getOptionalTyped($$1);
         Optional<? extends Dynamic<?>> $$3 = $$2.isPresent() ? $$2.get().write().result() : Optional.empty();
         return p_337660_.update(DSL.remainderFinder(), p_337658_ -> {
            int $$2x = p_337658_.get("xPos").asInt(0);
            int $$3x = p_337658_.get("zPos").asInt(0);
            Optional<? extends Dynamic<?>> $$4 = p_337658_.get("fluid_ticks").get().result();
            p_337658_ = extractOverreachingTicks(p_337658_, $$2x, $$3x, $$3, "neighbor_block_ticks");
            return extractOverreachingTicks(p_337658_, $$2x, $$3x, $$4, "neighbor_fluid_ticks");
         });
      });
   }

   private static Dynamic<?> extractOverreachingTicks(
      Dynamic<?> p_207663_, int p_207664_, int p_207665_, Optional<? extends Dynamic<?>> p_207666_, String p_207667_
   ) {
      if (p_207666_.isPresent()) {
         List<? extends Dynamic<?>> $$5 = p_207666_.get().asStream().filter(p_207658_ -> {
            int $$3 = p_207658_.get("x").asInt(0);
            int $$4 = p_207658_.get("z").asInt(0);
            int $$5x = Math.abs(p_207664_ - ($$3 >> 4));
            int $$6 = Math.abs(p_207665_ - ($$4 >> 4));
            return ($$5x != 0 || $$6 != 0) && $$5x <= 1 && $$6 <= 1;
         }).toList();
         if (!$$5.isEmpty()) {
            p_207663_ = p_207663_.set("UpgradeData", p_207663_.get("UpgradeData").orElseEmptyMap().set(p_207667_, p_207663_.createList($$5.stream())));
         }
      }

      return p_207663_;
   }
}
