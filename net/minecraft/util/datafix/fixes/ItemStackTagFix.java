package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public abstract class ItemStackTagFix extends DataFix {
   private final String name;
   private final Predicate<String> idFilter;

   public ItemStackTagFix(Schema p_216682_, String p_216683_, Predicate<String> p_216684_) {
      super(p_216682_, false);
      this.name = p_216683_;
      this.idFilter = p_216684_;
   }

   public final TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.ITEM_STACK);
      return this.fixTypeEverywhereTyped(this.name, type, createFixer(type, this.idFilter, this::fixItemStackTag));
   }

   public static UnaryOperator<Typed<?>> createFixer(Type<?> p_326099_, Predicate<String> p_326164_, UnaryOperator<Typed<?>> p_326258_) {
      OpticFinder<Pair<String, String>> opticfinder = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
      OpticFinder<?> opticfinder1 = p_326099_.findField("tag");
      return p_392864_ -> {
         Optional<Pair<String, String>> optional = p_392864_.getOptional(opticfinder);
         return optional.isPresent() && p_326164_.test((String)optional.get().getSecond()) ? p_392864_.updateTyped(opticfinder1, p_326258_) : p_392864_;
      };
   }

   protected abstract Typed<?> fixItemStackTag(Typed<?> var1);
}
