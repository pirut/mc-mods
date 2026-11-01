package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Optional;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class EntityCustomNameToComponentFix extends DataFix {
   public EntityCustomNameToComponentFix(Schema outputSchema) {
      super(outputSchema, true);
   }

   public TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.ENTITY);
      Type<?> type2 = this.getOutputSchema().getType(References.ENTITY);
      OpticFinder<String> opticFinder = DSL.fieldFinder("id", NamespacedSchema.namespacedString());
      OpticFinder<String> opticFinder2 = type.findField("CustomName");
      Type<?> type3 = type2.findFieldType("CustomName");
      return this.fixTypeEverywhereTyped("EntityCustomNameToComponentFix", type, type2, typed -> fixEntity(typed, type2, opticFinder, opticFinder2, type3));
   }

   private static <T> Typed<?> fixEntity(Typed<?> data, Type<?> entityType, OpticFinder<String> customNameOptic, OpticFinder<String> idOptic, Type<T> newType) {
      Optional<String> optional = data.getOptional(idOptic);
      if (optional.isEmpty()) {
         return ExtraDataFixUtils.cast(entityType, (Typed<T>)data);
      } else if (optional.get().isEmpty()) {
         return Util.writeAndReadTypedOrThrow(data, entityType, dynamicx -> dynamicx.remove("CustomName"));
      } else {
         String string = data.getOptional(customNameOptic).orElse("");
         Dynamic<?> dynamic = fixCustomName(data.getOps(), optional.get(), string);
         return data.set(idOptic, Util.readTypedOrThrow(newType, dynamic));
      }
   }

   private static <T> Dynamic<T> fixCustomName(DynamicOps<T> ops, String customName, String id) {
      return "minecraft:commandblock_minecart".equals(id)
         ? new Dynamic(ops, ops.createString(customName))
         : LegacyComponentDataFixUtils.createPlainTextComponent(ops, customName);
   }
}
