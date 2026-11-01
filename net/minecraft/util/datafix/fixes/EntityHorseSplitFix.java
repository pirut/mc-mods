package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Objects;
import net.minecraft.Util;

public class EntityHorseSplitFix extends EntityRenameFix {
   public EntityHorseSplitFix(Schema p_15447_, boolean p_15448_) {
      super("EntityHorseSplitFix", p_15447_, p_15448_);
   }

   @Override
   protected Pair<String, Typed<?>> fix(String p_15451_, Typed<?> p_15452_) {
      if (Objects.equals("EntityHorse", p_15451_)) {
         Dynamic<?> $$2 = (Dynamic<?>)p_15452_.get(DSL.remainderFinder());
         int $$3 = $$2.get("Type").asInt(0);

         String $$4 = switch ($$3) {
            case 1 -> "Donkey";
            case 2 -> "Mule";
            case 3 -> "ZombieHorse";
            case 4 -> "SkeletonHorse";
            default -> "Horse";
         };
         Type<?> $$5 = (Type<?>)this.getOutputSchema().findChoiceType(References.ENTITY).types().get($$4);
         return Pair.of($$4, Util.writeAndReadTypedOrThrow(p_15452_, $$5, p_341965_ -> p_341965_.remove("Type")));
      } else {
         return Pair.of(p_15451_, p_15452_);
      }
   }
}
