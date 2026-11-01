package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class LodestoneCompassComponentFix extends ItemStackComponentRemainderFix {
   public LodestoneCompassComponentFix(Schema p_332795_) {
      super(p_332795_, "LodestoneCompassComponentFix", "minecraft:lodestone_target", "minecraft:lodestone_tracker");
   }

   @Override
   protected <T> Dynamic<T> fixComponent(Dynamic<T> p_332647_) {
      Optional<Dynamic<T>> $$1 = p_332647_.get("pos").result();
      Optional<Dynamic<T>> $$2 = p_332647_.get("dimension").result();
      p_332647_ = p_332647_.remove("pos").remove("dimension");
      if ($$1.isPresent() && $$2.isPresent()) {
         p_332647_ = p_332647_.set("target", p_332647_.emptyMap().set("pos", $$1.get()).set("dimension", $$2.get()));
      }

      return p_332647_;
   }
}
