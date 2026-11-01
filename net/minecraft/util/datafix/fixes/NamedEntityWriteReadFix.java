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

   public NamedEntityWriteReadFix(Schema p_307236_, boolean p_307467_, String p_307246_, TypeReference p_307497_, String p_307636_) {
      super(p_307236_, p_307467_);
      this.name = p_307246_;
      this.type = p_307497_;
      this.entityName = p_307636_;
   }

   public TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(this.type);
      Type<?> type1 = this.getInputSchema().getChoiceType(this.type, this.entityName);
      Type<?> type2 = this.getOutputSchema().getType(this.type);
      OpticFinder<?> opticfinder = DSL.namedChoice(this.entityName, type1);
      Type<?> type3 = ExtraDataFixUtils.patchSubType(type, type, type2);
      return this.fix(type, type2, type3, opticfinder);
   }

   private <S, T, A> TypeRewriteRule fix(Type<S> p_324369_, Type<T> p_323537_, Type<?> p_324518_, OpticFinder<A> p_324194_) {
      return this.fixTypeEverywhereTyped(this.name, p_324369_, p_323537_, p_466467_ -> {
         if (p_466467_.getOptional(p_324194_).isEmpty()) {
            return ExtraDataFixUtils.cast(p_323537_, p_466467_);
         } else {
            Typed<?> typed = ExtraDataFixUtils.cast(p_324518_, p_466467_);
            return Util.writeAndReadTypedOrThrow((Typed<A>)typed, p_323537_, this::fix);
         }
      });
   }

   protected abstract <T> Dynamic<T> fix(Dynamic<T> var1);
}
