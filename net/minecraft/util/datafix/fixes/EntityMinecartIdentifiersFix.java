package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.Util;

public class EntityMinecartIdentifiersFix extends EntityRenameFix {
   public EntityMinecartIdentifiersFix(Schema p_15479_) {
      super("EntityMinecartIdentifiersFix", p_15479_, true);
   }

   @Override
   protected Pair<String, Typed<?>> fix(String p_341981_, Typed<?> p_341975_) {
      if (!p_341981_.equals("Minecart")) {
         return Pair.of(p_341981_, p_341975_);
      } else {
         int $$2 = ((Dynamic)p_341975_.getOrCreate(DSL.remainderFinder())).get("Type").asInt(0);

         String $$3 = switch ($$2) {
            case 1 -> "MinecartChest";
            case 2 -> "MinecartFurnace";
            default -> "MinecartRideable";
         };
         Type<?> $$4 = (Type<?>)this.getOutputSchema().findChoiceType(References.ENTITY).types().get($$3);
         return Pair.of($$3, Util.writeAndReadTypedOrThrow(p_341975_, $$4, p_341966_ -> p_341966_.remove("Type")));
      }
   }
}
