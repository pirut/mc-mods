package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class HeightmapRenamingFix extends DataFix {
   public HeightmapRenamingFix(Schema p_15891_, boolean p_15892_) {
      super(p_15891_, p_15892_);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getInputSchema().getType(References.CHUNK);
      OpticFinder<?> $$1 = $$0.findField("Level");
      return this.fixTypeEverywhereTyped(
         "HeightmapRenamingFix", $$0, p_15895_ -> p_15895_.updateTyped($$1, p_145380_ -> p_145380_.update(DSL.remainderFinder(), this::fix))
      );
   }

   private Dynamic<?> fix(Dynamic<?> p_15899_) {
      Optional<? extends Dynamic<?>> $$1 = p_15899_.get("Heightmaps").result();
      if ($$1.isEmpty()) {
         return p_15899_;
      } else {
         Dynamic<?> $$2 = (Dynamic<?>)$$1.get();
         Optional<? extends Dynamic<?>> $$3 = $$2.get("LIQUID").result();
         if ($$3.isPresent()) {
            $$2 = $$2.remove("LIQUID");
            $$2 = $$2.set("WORLD_SURFACE_WG", $$3.get());
         }

         Optional<? extends Dynamic<?>> $$4 = $$2.get("SOLID").result();
         if ($$4.isPresent()) {
            $$2 = $$2.remove("SOLID");
            $$2 = $$2.set("OCEAN_FLOOR_WG", $$4.get());
            $$2 = $$2.set("OCEAN_FLOOR", $$4.get());
         }

         Optional<? extends Dynamic<?>> $$5 = $$2.get("LIGHT").result();
         if ($$5.isPresent()) {
            $$2 = $$2.remove("LIGHT");
            $$2 = $$2.set("LIGHT_BLOCKING", $$5.get());
         }

         Optional<? extends Dynamic<?>> $$6 = $$2.get("RAIN").result();
         if ($$6.isPresent()) {
            $$2 = $$2.remove("RAIN");
            $$2 = $$2.set("MOTION_BLOCKING", $$6.get());
            $$2 = $$2.set("MOTION_BLOCKING_NO_LEAVES", $$6.get());
         }

         return p_15899_.set("Heightmaps", $$2);
      }
   }
}
