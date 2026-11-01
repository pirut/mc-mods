package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.datafix.ComponentDataFixUtils;

public class ItemCustomNameToComponentFix extends DataFix {
   public ItemCustomNameToComponentFix(Schema p_15927_, boolean p_15928_) {
      super(p_15927_, p_15928_);
   }

   private Dynamic<?> fixTag(Dynamic<?> p_15935_) {
      Optional<? extends Dynamic<?>> $$1 = p_15935_.get("display").result();
      if ($$1.isPresent()) {
         Dynamic<?> $$2 = (Dynamic<?>)$$1.get();
         Optional<String> $$3 = $$2.get("Name").asString().result();
         if ($$3.isPresent()) {
            $$2 = $$2.set("Name", ComponentDataFixUtils.createPlainTextComponent($$2.getOps(), $$3.get()));
         }

         return p_15935_.set("display", $$2);
      } else {
         return p_15935_;
      }
   }

   public TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getInputSchema().getType(References.ITEM_STACK);
      OpticFinder<?> $$1 = $$0.findField("tag");
      return this.fixTypeEverywhereTyped(
         "ItemCustomNameToComponentFix", $$0, p_15931_ -> p_15931_.updateTyped($$1, p_145384_ -> p_145384_.update(DSL.remainderFinder(), this::fixTag))
      );
   }
}
