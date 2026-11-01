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

   public SaddleEquipmentSlotFix(Schema p_397560_) {
      super(p_397560_, true);
   }

   protected TypeRewriteRule makeRule() {
      TaggedChoiceType<String> taggedchoicetype = this.getInputSchema().findChoiceType(References.ENTITY);
      OpticFinder<Pair<String, ?>> opticfinder = DSL.typeFinder(taggedchoicetype);
      Type<?> type = this.getInputSchema().getType(References.ENTITY);
      Type<?> type1 = this.getOutputSchema().getType(References.ENTITY);
      Type<?> type2 = ExtraDataFixUtils.patchSubType(type, type, type1);
      return this.fixTypeEverywhereTyped(
         "SaddleEquipmentSlotFix",
         type,
         type1,
         p_466473_ -> {
            String s = p_466473_.getOptional(opticfinder).<String>map(Pair::getFirst).map(NamespacedSchema::ensureNamespaced).orElse("");
            Typed<?> typed = ExtraDataFixUtils.cast(type2, p_466473_);
            if (ENTITIES_WITH_SADDLE_ITEM.contains(s)) {
               return Util.writeAndReadTypedOrThrow(typed, type1, SaddleEquipmentSlotFix::fixEntityWithSaddleItem);
            } else {
               return ENTITIES_WITH_SADDLE_FLAG.contains(s)
                  ? Util.writeAndReadTypedOrThrow(typed, type1, SaddleEquipmentSlotFix::fixEntityWithSaddleFlag)
                  : ExtraDataFixUtils.cast(type1, p_466473_);
            }
         }
      );
   }

   private static Dynamic<?> fixEntityWithSaddleItem(Dynamic<?> p_397781_) {
      return p_397781_.get("SaddleItem").result().isEmpty() ? p_397781_ : fixDropChances(p_397781_.renameField("SaddleItem", "saddle"));
   }

   private static Dynamic<?> fixEntityWithSaddleFlag(Dynamic<?> p_397827_) {
      boolean flag = p_397827_.get("Saddle").asBoolean(false);
      p_397827_ = p_397827_.remove("Saddle");
      if (!flag) {
         return p_397827_;
      } else {
         Dynamic<?> dynamic = p_397827_.emptyMap().set("id", p_397827_.createString("minecraft:saddle")).set("count", p_397827_.createInt(1));
         return fixDropChances(p_397827_.set("saddle", dynamic));
      }
   }

   private static Dynamic<?> fixDropChances(Dynamic<?> p_397439_) {
      Dynamic<?> dynamic = p_397439_.get("drop_chances").orElseEmptyMap().set("saddle", p_397439_.createFloat(2.0F));
      return p_397439_.set("drop_chances", dynamic);
   }
}
