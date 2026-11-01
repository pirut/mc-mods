package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class TippedArrowPotionToItemFix extends NamedEntityWriteReadFix {
   public TippedArrowPotionToItemFix(Schema p_330412_) {
      super(p_330412_, false, "TippedArrowPotionToItemFix", References.ENTITY, "minecraft:arrow");
   }

   @Override
   protected <T> Dynamic<T> fix(Dynamic<T> p_330283_) {
      Optional<Dynamic<T>> $$1 = p_330283_.get("Potion").result();
      Optional<Dynamic<T>> $$2 = p_330283_.get("custom_potion_effects").result();
      Optional<Dynamic<T>> $$3 = p_330283_.get("Color").result();
      return $$1.isEmpty() && $$2.isEmpty() && $$3.isEmpty()
         ? p_330283_
         : p_330283_.remove("Potion").remove("custom_potion_effects").remove("Color").update("item", p_331889_ -> {
            Dynamic<?> $$4 = p_331889_.get("tag").orElseEmptyMap();
            if ($$1.isPresent()) {
               $$4 = $$4.set("Potion", $$1.get());
            }

            if ($$2.isPresent()) {
               $$4 = $$4.set("custom_potion_effects", $$2.get());
            }

            if ($$3.isPresent()) {
               $$4 = $$4.set("CustomPotionColor", $$3.get());
            }

            return p_331889_.set("tag", $$4);
         });
   }
}
