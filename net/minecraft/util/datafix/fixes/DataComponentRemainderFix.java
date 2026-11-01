package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public abstract class DataComponentRemainderFix extends DataFix {
   private final String name;
   private final String componentId;
   private final String newComponentId;

   public DataComponentRemainderFix(Schema p_388311_, String p_387317_, String p_388665_) {
      this(p_388311_, p_387317_, p_388665_, p_388665_);
   }

   public DataComponentRemainderFix(Schema p_387789_, String p_387550_, String p_388284_, String p_388095_) {
      super(p_387789_, false);
      this.name = p_387550_;
      this.componentId = p_388284_;
      this.newComponentId = p_388095_;
   }

   public final TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.DATA_COMPONENTS);
      return this.fixTypeEverywhereTyped(this.name, type, p_386780_ -> p_386780_.update(DSL.remainderFinder(), p_387394_ -> {
         Optional<? extends Dynamic<?>> optional = p_387394_.get(this.componentId).result();
         if (optional.isEmpty()) {
            return p_387394_;
         } else {
            Dynamic<?> dynamic = this.fixComponent(optional.get());
            return p_387394_.remove(this.componentId).setFieldIfPresent(this.newComponentId, Optional.ofNullable(dynamic));
         }
      }));
   }

   @Nullable
   protected abstract <T> Dynamic<T> fixComponent(Dynamic<T> var1);
}
