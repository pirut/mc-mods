package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public abstract class AbstractBlockPropertyFix extends DataFix {
   private final String name;

   public AbstractBlockPropertyFix(Schema p_394621_, String p_394285_) {
      super(p_394621_, false);
      this.name = p_394285_;
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         this.name, this.getInputSchema().getType(References.BLOCK_STATE), p_393664_ -> p_393664_.update(DSL.remainderFinder(), this::fixBlockState)
      );
   }

   private Dynamic<?> fixBlockState(Dynamic<?> p_394590_) {
      Optional<String> optional = p_394590_.get("Name").asString().result().map(NamespacedSchema::ensureNamespaced);
      return optional.isPresent() && this.shouldFix(optional.get())
         ? p_394590_.update("Properties", p_394507_ -> this.fixProperties(optional.get(), p_394507_))
         : p_394590_;
   }

   protected abstract boolean shouldFix(String var1);

   protected abstract <T> Dynamic<T> fixProperties(String var1, Dynamic<T> var2);
}
