package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public abstract class NamedEntityWriteReadFix extends DataFix {
   private final String name;
   private final String entityName;
   private final TypeReference type;

   public NamedEntityWriteReadFix(Schema outputSchema, boolean changesType, String name, TypeReference type, String entityName) {
      super(outputSchema, changesType);
      this.name = name;
      this.type = type;
      this.entityName = entityName;
   }

   public TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(this.type);
      Type<?> type2 = this.getInputSchema().getChoiceType(this.type, this.entityName);
      Type<?> type3 = this.getOutputSchema().getType(this.type);
      OpticFinder<?> opticFinder = DSL.namedChoice(this.entityName, type2);
      Type<?> type4 = ExtraDataFixUtils.patchSubType(type, type, type3);
      return this.fix(type, type3, type4, opticFinder);
   }

   private <S, T, A> TypeRewriteRule fix(Type<S> inputType, Type<T> outputType, Type<?> type, OpticFinder<A> optic) {
      return this.fixTypeEverywhereTyped(this.name, inputType, outputType, typed -> {
         if (typed.getOptional(optic).isEmpty()) {
            return ExtraDataFixUtils.cast(outputType, typed);
         } else {
            Typed<?> typed2 = ExtraDataFixUtils.cast(type, typed);
            return Util.writeAndReadTypedOrThrow((Typed<A>)typed2, outputType, this::fix);
         }
      });
   }

   protected abstract <T> Dynamic<T> fix(Dynamic<T> tag);
}
