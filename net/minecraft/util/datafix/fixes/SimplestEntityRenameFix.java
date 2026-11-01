package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;
import com.mojang.datafixers.util.Pair;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public abstract class SimplestEntityRenameFix extends DataFix {
   private final String name;

   public SimplestEntityRenameFix(String p_16911_, Schema p_16912_, boolean p_16913_) {
      super(p_16912_, p_16913_);
      this.name = p_16911_;
   }

   public TypeRewriteRule makeRule() {
      TaggedChoiceType<String> $$0 = this.getInputSchema().findChoiceType(References.ENTITY);
      TaggedChoiceType<String> $$1 = this.getOutputSchema().findChoiceType(References.ENTITY);
      Type<Pair<String, String>> $$2 = DSL.named(References.ENTITY_NAME.typeName(), NamespacedSchema.namespacedString());
      if (!Objects.equals(this.getOutputSchema().getType(References.ENTITY_NAME), $$2)) {
         throw new IllegalStateException("Entity name type is not what was expected.");
      } else {
         return TypeRewriteRule.seq(this.fixTypeEverywhere(this.name, $$0, $$1, p_16921_ -> p_145688_ -> p_145688_.mapFirst(p_145692_ -> {
            String $$3 = this.rename(p_145692_);
            Type<?> $$4 = (Type<?>)$$0.types().get(p_145692_);
            Type<?> $$5 = (Type<?>)$$1.types().get($$3);
            if (!$$5.equals($$4, true, true)) {
               throw new IllegalStateException(String.format(Locale.ROOT, "Dynamic type check failed: %s not equal to %s", $$5, $$4));
            } else {
               return $$3;
            }
         })), this.fixTypeEverywhere(this.name + " for entity name", $$2, p_16929_ -> p_145694_ -> p_145694_.mapSecond(this::rename)));
      }
   }

   protected abstract String rename(String var1);
}
