package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class AreaEffectCloudPotionFix extends NamedEntityFix {
   public AreaEffectCloudPotionFix(Schema p_331376_) {
      super(p_331376_, false, "AreaEffectCloudPotionFix", References.ENTITY, "minecraft:area_effect_cloud");
   }

   @Override
   protected Typed<?> fix(Typed<?> p_330255_) {
      return p_330255_.update(DSL.remainderFinder(), this::fix);
   }

   private <T> Dynamic<T> fix(Dynamic<T> p_330468_) {
      Optional<Dynamic<T>> $$1 = p_330468_.get("Color").result();
      Optional<Dynamic<T>> $$2 = p_330468_.get("effects").result();
      Optional<Dynamic<T>> $$3 = p_330468_.get("Potion").result();
      p_330468_ = p_330468_.remove("Color").remove("effects").remove("Potion");
      if ($$1.isEmpty() && $$2.isEmpty() && $$3.isEmpty()) {
         return p_330468_;
      } else {
         Dynamic<T> $$4 = p_330468_.emptyMap();
         if ($$1.isPresent()) {
            $$4 = $$4.set("custom_color", $$1.get());
         }

         if ($$2.isPresent()) {
            $$4 = $$4.set("custom_effects", $$2.get());
         }

         if ($$3.isPresent()) {
            $$4 = $$4.set("potion", $$3.get());
         }

         return p_330468_.set("potion_contents", $$4);
      }
   }
}
