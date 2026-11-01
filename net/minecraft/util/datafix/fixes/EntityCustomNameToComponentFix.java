package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.util.datafix.ComponentDataFixUtils;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class EntityCustomNameToComponentFix extends DataFix {
   public EntityCustomNameToComponentFix(Schema p_15398_, boolean p_15399_) {
      super(p_15398_, p_15399_);
   }

   public TypeRewriteRule makeRule() {
      OpticFinder<String> $$0 = DSL.fieldFinder("id", NamespacedSchema.namespacedString());
      return this.fixTypeEverywhereTyped(
         "EntityCustomNameToComponentFix", this.getInputSchema().getType(References.ENTITY), p_15402_ -> p_15402_.update(DSL.remainderFinder(), p_145277_ -> {
            Optional<String> $$3 = p_15402_.getOptional($$0);
            return $$3.isPresent() && Objects.equals($$3.get(), "minecraft:commandblock_minecart") ? p_145277_ : fixTagCustomName(p_145277_);
         })
      );
   }

   public static Dynamic<?> fixTagCustomName(Dynamic<?> p_15408_) {
      String $$1 = p_15408_.get("CustomName").asString("");
      return $$1.isEmpty() ? p_15408_.remove("CustomName") : p_15408_.set("CustomName", ComponentDataFixUtils.createPlainTextComponent(p_15408_.getOps(), $$1));
   }
}
