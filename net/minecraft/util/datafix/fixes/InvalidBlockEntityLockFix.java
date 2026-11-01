package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class InvalidBlockEntityLockFix extends DataFix {
   public InvalidBlockEntityLockFix(Schema p_387843_) {
      super(p_387843_, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "BlockEntityLockToComponentFix",
         this.getInputSchema().getType(References.BLOCK_ENTITY),
         p_387403_ -> p_387403_.update(DSL.remainderFinder(), p_387307_ -> {
            Optional<? extends Dynamic<?>> optional = p_387307_.get("lock").result();
            if (optional.isEmpty()) {
               return p_387307_;
            } else {
               Dynamic<?> dynamic = InvalidLockComponentFix.fixLock(optional.get());
               return dynamic != null ? p_387307_.set("lock", dynamic) : p_387307_.remove("lock");
            }
         })
      );
   }
}
