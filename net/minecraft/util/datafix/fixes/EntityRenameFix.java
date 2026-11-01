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
import java.util.function.Function;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public abstract class EntityRenameFix extends DataFix {
   protected final String name;

   public EntityRenameFix(String name, Schema outputSchema, boolean changesType) {
      super(outputSchema, changesType);
      this.name = name;
   }

   public TypeRewriteRule makeRule() {
      TaggedChoiceType<String> taggedChoiceType = this.getInputSchema().findChoiceType(References.ENTITY);
      TaggedChoiceType<String> taggedChoiceType2 = this.getOutputSchema().findChoiceType(References.ENTITY);
      Function<String, Type<?>> function = Util.memoize(string -> {
         Type<?> type = (Type<?>)taggedChoiceType.types().get(string);
         return ExtraDataFixUtils.patchSubType(type, taggedChoiceType, taggedChoiceType2);
      });
      return this.fixTypeEverywhere(
         this.name,
         taggedChoiceType,
         taggedChoiceType2,
         dynamicOps -> pair -> {
            String string = (String)pair.getFirst();
            Type<?> type = function.apply(string);
            Pair<String, Typed<?>> pair2 = this.fix(string, this.getEntity(pair.getSecond(), dynamicOps, type));
            Type<?> type2 = (Type<?>)taggedChoiceType2.types().get(pair2.getFirst());
            if (!type2.equals(((Typed)pair2.getSecond()).getType(), true, true)) {
               throw new IllegalStateException(
                  String.format(Locale.ROOT, "Dynamic type check failed: %s not equal to %s", type2, ((Typed)pair2.getSecond()).getType())
               );
            } else {
               return Pair.of((String)pair2.getFirst(), ((Typed)pair2.getSecond()).getValue());
            }
         }
      );
   }

   private <A> Typed<A> getEntity(Object value, DynamicOps<?> ops, Type<A> type) {
      return new Typed(type, ops, value);
   }

   protected abstract Pair<String, Typed<?>> fix(String entityName, Typed<?> typed);
}
