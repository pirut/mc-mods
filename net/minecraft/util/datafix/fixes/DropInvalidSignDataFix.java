package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Streams;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;

public class DropInvalidSignDataFix extends DataFix {
   private final String entityName;

   public DropInvalidSignDataFix(Schema outputSchema, String entityName) {
      super(outputSchema, false);
      this.entityName = entityName;
   }

   private <T> Dynamic<T> fix(Dynamic<T> data) {
      data = data.update("front_text", DropInvalidSignDataFix::fixText);
      data = data.update("back_text", DropInvalidSignDataFix::fixText);

      for (String string : BlockEntitySignDoubleSidedEditableTextFix.FIELDS_TO_DROP) {
         data = data.remove(string);
      }

      return data;
   }

   private static <T> Dynamic<T> fixText(Dynamic<T> textDynamic) {
      Optional<Stream<Dynamic<T>>> optional = textDynamic.get("filtered_messages").asStreamOpt().result();
      if (optional.isEmpty()) {
         return textDynamic;
      } else {
         Dynamic<T> dynamic = LegacyComponentDataFixUtils.createEmptyComponent(textDynamic.getOps());
         List<Dynamic<T>> list = textDynamic.get("messages").asStreamOpt().result().orElse(Stream.of()).toList();
         List<Dynamic<T>> list2 = Streams.mapWithIndex(optional.get(), (dynamic2, l) -> {
            Dynamic<T> dynamic3 = l < list.size() ? list.get((int)l) : dynamic;
            return dynamic2.equals(dynamic) ? dynamic3 : dynamic2;
         }).toList();
         return list2.equals(list) ? textDynamic.remove("filtered_messages") : textDynamic.set("filtered_messages", textDynamic.createList(list2.stream()));
      }
   }

   public TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.BLOCK_ENTITY);
      Type<?> type2 = this.getInputSchema().getChoiceType(References.BLOCK_ENTITY, this.entityName);
      OpticFinder<?> opticFinder = DSL.namedChoice(this.entityName, type2);
      return this.fixTypeEverywhereTyped(
         "DropInvalidSignDataFix for " + this.entityName,
         type,
         typed -> typed.updateTyped(
            opticFinder,
            type2,
            typedx -> {
               boolean bl = ((Dynamic)typedx.get(DSL.remainderFinder())).get("_filtered_correct").asBoolean(false);
               return bl
                  ? typedx.update(DSL.remainderFinder(), dynamic -> dynamic.remove("_filtered_correct"))
                  : Util.writeAndReadTypedOrThrow(typedx, type2, this::fix);
            }
         )
      );
   }
}
