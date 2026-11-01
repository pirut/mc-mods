package net.minecraft.util.datafix.fixes;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.function.Supplier;
import net.minecraft.Util;

public class EntityZombieSplitFix extends EntityRenameFix {
   private final Supplier<Type<?>> zombieVillagerType = Suppliers.memoize(() -> this.getOutputSchema().getChoiceType(References.ENTITY, "ZombieVillager"));

   public EntityZombieSplitFix(Schema p_15798_) {
      super("EntityZombieSplitFix", p_15798_, true);
   }

   @Override
   protected Pair<String, Typed<?>> fix(String p_341651_, Typed<?> p_341608_) {
      if (!p_341651_.equals("Zombie")) {
         return Pair.of(p_341651_, p_341608_);
      } else {
         Dynamic<?> $$2 = (Dynamic<?>)p_341608_.getOptional(DSL.remainderFinder()).orElseThrow();
         int $$3 = $$2.get("ZombieType").asInt(0);
         String $$4;
         Typed<?> $$5;
         switch ($$3) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
               $$4 = "ZombieVillager";
               $$5 = this.changeSchemaToZombieVillager(p_341608_, $$3 - 1);
               break;
            case 6:
               $$4 = "Husk";
               $$5 = p_341608_;
               break;
            default:
               $$4 = "Zombie";
               $$5 = p_341608_;
         }

         return Pair.of($$4, $$5.update(DSL.remainderFinder(), p_341600_ -> p_341600_.remove("ZombieType")));
      }
   }

   private Typed<?> changeSchemaToZombieVillager(Typed<?> p_341612_, int p_341675_) {
      return Util.writeAndReadTypedOrThrow(p_341612_, this.zombieVillagerType.get(), p_341611_ -> p_341611_.set("Profession", p_341611_.createInt(p_341675_)));
   }
}
