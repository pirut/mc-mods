package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import org.jspecify.annotations.Nullable;

public class TridentAnimationFix extends DataComponentRemainderFix {
   public TridentAnimationFix(Schema p_456145_) {
      super(p_456145_, "TridentAnimationFix", "minecraft:consumable");
   }

   @Nullable
   @Override
   protected <T> Dynamic<T> fixComponent(Dynamic<T> p_456210_) {
      return p_456210_.update("animation", p_456134_ -> {
         String s = p_456134_.asString().result().orElse("");
         return "spear".equals(s) ? p_456134_.createString("trident") : p_456134_;
      });
   }
}
