package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DynamicOps;
import java.util.Locale;

public abstract class EntityRenameFix extends DataFix {
   protected final String name;

   public EntityRenameFix(String p_15618_, Schema p_15619_, boolean p_15620_) {
      super(p_15619_, p_15620_);
      this.name = p_15618_;
   }

   public TypeRewriteRule makeRule() {
      TaggedChoiceType<String> $$0 = this.getInputSchema().findChoiceType(References.ENTITY);
      TaggedChoiceType<String> $$1 = this.getOutputSchema().findChoiceType(References.ENTITY);
      return this.fixTypeEverywhere(
         this.name,
         $$0,
         $$1,
         p_15624_ -> p_145311_ -> {
            String $$4 = (String)p_145311_.getFirst();
            Type<?> $$5 = (Type<?>)$$0.types().get($$4);
            Pair<String, Typed<?>> $$6 = this.fix($$4, this.getEntity(p_145311_.getSecond(), p_15624_, $$5));
            Type<?> $$7 = (Type<?>)$$1.types().get($$6.getFirst());
            if (!$$7.equals(((Typed)$$6.getSecond()).getType(), true, true)) {
               throw new IllegalStateException(
                  String.format(Locale.ROOT, "Dynamic type check failed: %s not equal to %s", $$7, ((Typed)$$6.getSecond()).getType())
               );
            } else {
               return Pair.of((String)$$6.getFirst(), ((Typed)$$6.getSecond()).getValue());
            }
         }
      );
   }

   private <A> Typed<A> getEntity(Object p_15631_, DynamicOps<?> p_15632_, Type<A> p_15633_) {
      return new Typed(p_15633_, p_15632_, p_15631_);
   }

   protected abstract Pair<String, Typed<?>> fix(String var1, Typed<?> var2);
}
