package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Set;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class SaddleEquipmentSlotFix extends DataFix {
   private static final Set<String> ENTITIES_WITH_SADDLE_ITEM = Set.of(
      "minecraft:horse",
      "minecraft:skeleton_horse",
      "minecraft:zombie_horse",
      "minecraft:donkey",
      "minecraft:mule",
      "minecraft:camel",
      "minecraft:llama",
      "minecraft:trader_llama"
   );
   private static final Set<String> ENTITIES_WITH_SADDLE_FLAG = Set.of("minecraft:pig", "minecraft:strider");
   private static final String SADDLE_FLAG = "Saddle";
   private static final String NEW_SADDLE = "saddle";

   public SaddleEquipmentSlotFix(Schema outputSchema) {
      super(outputSchema, true);
   }

   protected TypeRewriteRule makeRule() {
      TaggedChoiceType<String> taggedChoiceType = this.getInputSchema().findChoiceType(References.ENTITY);
      OpticFinder<Pair<String, ?>> opticFinder = DSL.typeFinder(taggedChoiceType);
      Type<?> type = this.getInputSchema().getType(References.ENTITY);
      Type<?> type2 = this.getOutputSchema().getType(References.ENTITY);
      Type<?> type3 = ExtraDataFixUtils.patchSubType(type, type, type2);
      return this.fixTypeEverywhereTyped(
         "SaddleEquipmentSlotFix",
         type,
         type2,
         typed -> {
            String string = typed.getOptional(opticFinder).<String>map(Pair::getFirst).map(NamespacedSchema::ensureNamespaced).orElse("");
            Typed<?> typed2 = ExtraDataFixUtils.cast(type3, typed);
            if (ENTITIES_WITH_SADDLE_ITEM.contains(string)) {
               return Util.writeAndReadTypedOrThrow(typed2, type2, SaddleEquipmentSlotFix::fixEntityWithSaddleItem);
            } else {
               return ENTITIES_WITH_SADDLE_FLAG.contains(string)
                  ? Util.writeAndReadTypedOrThrow(typed2, type2, SaddleEquipmentSlotFix::fixEntityWithSaddleFlag)
                  : ExtraDataFixUtils.cast(type2, typed);
            }
         }
      );
   }

   private static Dynamic<?> fixEntityWithSaddleItem(Dynamic<?> data) {
      return data.get("SaddleItem").result().isEmpty() ? data : fixDropChances(data.renameField("SaddleItem", "saddle"));
   }

   private static Dynamic<?> fixEntityWithSaddleFlag(Dynamic<?> data) {
      boolean bl = data.get("Saddle").asBoolean(false);
      data = data.remove("Saddle");
      if (!bl) {
         return data;
      } else {
         Dynamic<?> dynamic = data.emptyMap().set("id", data.createString("minecraft:saddle")).set("count", data.createInt(1));
         return fixDropChances(data.set("saddle", dynamic));
      }
   }

   private static Dynamic<?> fixDropChances(Dynamic<?> data) {
      Dynamic<?> dynamic = data.get("drop_chances").orElseEmptyMap().set("saddle", data.createFloat(2.0F));
      return data.set("drop_chances", dynamic);
   }
}
