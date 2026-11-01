package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

public class AddFieldFix extends DataFix {
   private final String name;
   private final TypeReference type;
   private final String fieldName;
   private final String[] path;
   private final Function<Dynamic<?>, Dynamic<?>> fieldGenerator;

   public AddFieldFix(Schema p_442505_, TypeReference p_442489_, String p_442686_, Function<Dynamic<?>, Dynamic<?>> p_442742_, String... p_442513_) {
      super(p_442505_, false);
      this.name = "Adding field `" + p_442686_ + "` to type `" + p_442489_.typeName().toLowerCase(Locale.ROOT) + "`";
      this.type = p_442489_;
      this.fieldName = p_442686_;
      this.path = p_442513_;
      this.fieldGenerator = p_442742_;
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         this.name,
         this.getInputSchema().getType(this.type),
         this.getOutputSchema().getType(this.type),
         p_443276_ -> p_443276_.update(DSL.remainderFinder(), p_443221_ -> this.addField(p_443221_, 0))
      );
   }

   private Dynamic<?> addField(Dynamic<?> p_442884_, int p_442687_) {
      if (p_442687_ >= this.path.length) {
         return p_442884_.set(this.fieldName, this.fieldGenerator.apply(p_442884_));
      } else {
         Optional<? extends Dynamic<?>> optional = p_442884_.get(this.path[p_442687_]).result();
         return optional.isEmpty() ? p_442884_ : this.addField((Dynamic<?>)optional.get(), p_442687_ + 1);
      }
   }
}
