package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;

public class ContainerBlockEntityLockPredicateFix extends DataFix {
   public ContainerBlockEntityLockPredicateFix(Schema p_376113_) {
      super(p_376113_, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "ContainerBlockEntityLockPredicateFix",
         this.getInputSchema().findChoiceType(References.BLOCK_ENTITY),
         ContainerBlockEntityLockPredicateFix::fixBlockEntity
      );
   }

   private static Typed<?> fixBlockEntity(Typed<?> p_376180_) {
      return p_376180_.update(DSL.remainderFinder(), p_376631_ -> p_376631_.renameAndFixField("Lock", "lock", LockComponentPredicateFix::fixLock));
   }
}
