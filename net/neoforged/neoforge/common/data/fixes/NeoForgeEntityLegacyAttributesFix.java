package net.neoforged.neoforge.common.data.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.util.datafix.fixes.References;

public class NeoForgeEntityLegacyAttributesFix extends DataFix {
   private final Set<String> legacyAttributes = new HashSet<>();
   private final String name;

   public NeoForgeEntityLegacyAttributesFix(String name, Schema outputSchema, List<String> attributesToRemove) {
      super(outputSchema, false);
      this.name = name;
      this.legacyAttributes.addAll(attributesToRemove);
   }

   protected TypeRewriteRule makeRule() {
      Schema schema = this.getInputSchema();
      Type<?> itemStackType = schema.getType(References.ITEM_STACK);
      OpticFinder<?> itemStackTag = itemStackType.findField("tag");
      return TypeRewriteRule.seq(
         this.fixTypeEverywhereTyped(this.name + " (ItemStack)", itemStackType, typed -> typed.updateTyped(itemStackTag, this::fixItemStackTag)),
         new TypeRewriteRule[]{
            this.fixTypeEverywhereTyped(this.name + " (Entity)", schema.getType(References.ENTITY), this::removeLegacyAttributes),
            this.fixTypeEverywhereTyped(this.name + " (Player)", schema.getType(References.PLAYER), this::removeLegacyAttributes)
         }
      );
   }

   private Typed<?> fixItemStackTag(Typed<?> typed) {
      return typed.update(
         DSL.remainderFinder(),
         tagDynamic -> tagDynamic.update(
            "AttributeModifiers",
            modifiersDynamic -> (Dynamic)DataFixUtils.orElse(
               modifiersDynamic.asStreamOpt()
                  .result()
                  .map(
                     dynamics -> dynamics.filter(
                        dynamic -> dynamic.get("AttributeName").asString().result().map(str -> !this.legacyAttributes.contains(str)).orElse(true)
                     )
                  )
                  .map(modifiersDynamic::createList),
               modifiersDynamic
            )
         )
      );
   }

   private Typed<?> removeLegacyAttributes(Typed<?> typed) {
      return typed.update(
         DSL.remainderFinder(),
         entityDynamic -> entityDynamic.update(
            "Attributes",
            attributesDynamic -> (Dynamic)DataFixUtils.orElse(
               attributesDynamic.asStreamOpt()
                  .result()
                  .map(
                     dynamics -> dynamics.filter(
                        dynamic -> dynamic.get("Name").asString().result().map(str -> !this.legacyAttributes.contains(str)).orElse(true)
                     )
                  )
                  .map(attributesDynamic::createList),
               attributesDynamic
            )
         )
      );
   }
}
