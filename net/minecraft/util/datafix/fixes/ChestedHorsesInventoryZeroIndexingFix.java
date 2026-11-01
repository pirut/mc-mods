package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;

public class ChestedHorsesInventoryZeroIndexingFix extends DataFix {
   public ChestedHorsesInventoryZeroIndexingFix(Schema p_324298_) {
      super(p_324298_, false);
   }

   protected TypeRewriteRule makeRule() {
      OpticFinder<Pair<String, Pair<Either<Pair<String, String>, Unit>, Pair<Either<?, Unit>, Dynamic<?>>>>> $$0 = DSL.typeFinder(
         this.getInputSchema().getType(References.ITEM_STACK)
      );
      Type<?> $$1 = this.getInputSchema().getType(References.ENTITY);
      return TypeRewriteRule.seq(
         this.horseLikeInventoryIndexingFixer($$0, $$1, "minecraft:llama"),
         new TypeRewriteRule[]{
            this.horseLikeInventoryIndexingFixer($$0, $$1, "minecraft:trader_llama"),
            this.horseLikeInventoryIndexingFixer($$0, $$1, "minecraft:mule"),
            this.horseLikeInventoryIndexingFixer($$0, $$1, "minecraft:donkey")
         }
      );
   }

   private TypeRewriteRule horseLikeInventoryIndexingFixer(
      OpticFinder<Pair<String, Pair<Either<Pair<String, String>, Unit>, Pair<Either<?, Unit>, Dynamic<?>>>>> p_324299_, Type<?> p_323519_, String p_324132_
   ) {
      Type<?> $$3 = this.getInputSchema().getChoiceType(References.ENTITY, p_324132_);
      OpticFinder<?> $$4 = DSL.namedChoice(p_324132_, $$3);
      OpticFinder<?> $$5 = $$3.findField("Items");
      return this.fixTypeEverywhereTyped(
         "Fix non-zero indexing in chest horse type " + p_324132_,
         p_323519_,
         p_324100_ -> p_324100_.updateTyped(
            $$4,
            p_323809_ -> p_323809_.updateTyped(
               $$5,
               p_324159_ -> p_324159_.update(
                  p_324299_,
                  p_323642_ -> p_323642_.mapSecond(
                     p_324444_ -> p_324444_.mapSecond(
                        p_323677_ -> p_323677_.mapSecond(
                           p_323925_ -> p_323925_.update("Slot", p_324395_ -> p_324395_.createByte((byte)(p_324395_.asInt(2) - 2)))
                        )
                     )
                  )
               )
            )
         )
      );
   }
}
