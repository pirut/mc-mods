package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import net.minecraft.util.Util;

public class TooltipDisplayComponentFix extends DataFix {
   private static final List<String> CONVERTED_ADDITIONAL_TOOLTIP_TYPES = List.of(
      "minecraft:banner_patterns",
      "minecraft:bees",
      "minecraft:block_entity_data",
      "minecraft:block_state",
      "minecraft:bundle_contents",
      "minecraft:charged_projectiles",
      "minecraft:container",
      "minecraft:container_loot",
      "minecraft:firework_explosion",
      "minecraft:fireworks",
      "minecraft:instrument",
      "minecraft:map_id",
      "minecraft:painting/variant",
      "minecraft:pot_decorations",
      "minecraft:potion_contents",
      "minecraft:tropical_fish/pattern",
      "minecraft:written_book_content"
   );

   public TooltipDisplayComponentFix(Schema outputSchema) {
      super(outputSchema, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.DATA_COMPONENTS);
      Type<?> type2 = this.getOutputSchema().getType(References.DATA_COMPONENTS);
      OpticFinder<?> opticFinder = type.findField("minecraft:can_place_on");
      OpticFinder<?> opticFinder2 = type.findField("minecraft:can_break");
      Type<?> type3 = type2.findFieldType("minecraft:can_place_on");
      Type<?> type4 = type2.findFieldType("minecraft:can_break");
      return this.fixTypeEverywhereTyped("TooltipDisplayComponentFix", type, type2, typed -> fix(typed, opticFinder, opticFinder2, type3, type4));
   }

   private static Typed<?> fix(Typed<?> data, OpticFinder<?> canPlaceOnOptic, OpticFinder<?> canBreakOptic, Type<?> canPlaceOnType, Type<?> canBreakType) {
      Set<String> set = new HashSet<>();
      data = fixAdventureModePredicate(data, canPlaceOnOptic, canPlaceOnType, "minecraft:can_place_on", set);
      data = fixAdventureModePredicate(data, canBreakOptic, canBreakType, "minecraft:can_break", set);
      return data.update(
         DSL.remainderFinder(),
         dynamic -> {
            dynamic = fixSimpleComponent(dynamic, "minecraft:trim", set);
            dynamic = fixSimpleComponent(dynamic, "minecraft:unbreakable", set);
            dynamic = fixComponentAndUnwrap(dynamic, "minecraft:dyed_color", "rgb", set);
            dynamic = fixComponentAndUnwrap(dynamic, "minecraft:attribute_modifiers", "modifiers", set);
            dynamic = fixComponentAndUnwrap(dynamic, "minecraft:enchantments", "levels", set);
            dynamic = fixComponentAndUnwrap(dynamic, "minecraft:stored_enchantments", "levels", set);
            dynamic = fixComponentAndUnwrap(dynamic, "minecraft:jukebox_playable", "song", set);
            boolean bl = dynamic.get("minecraft:hide_tooltip").result().isPresent();
            dynamic = dynamic.remove("minecraft:hide_tooltip");
            boolean bl2 = dynamic.get("minecraft:hide_additional_tooltip").result().isPresent();
            dynamic = dynamic.remove("minecraft:hide_additional_tooltip");
            if (bl2) {
               for (String string : CONVERTED_ADDITIONAL_TOOLTIP_TYPES) {
                  if (dynamic.get(string).result().isPresent()) {
                     set.add(string);
                  }
               }
            }

            return set.isEmpty() && !bl
               ? dynamic
               : dynamic.set(
                  "minecraft:tooltip_display",
                  dynamic.createMap(
                     Map.of(
                        dynamic.createString("hide_tooltip"),
                        dynamic.createBoolean(bl),
                        dynamic.createString("hidden_components"),
                        dynamic.createList(set.stream().map(dynamic::createString))
                     )
                  )
               );
         }
      );
   }

   private static Dynamic<?> fixSimpleComponent(Dynamic<?> data, String name, Set<String> processedComponents) {
      return fixRemainderComponent(data, name, processedComponents, UnaryOperator.identity());
   }

   private static Dynamic<?> fixComponentAndUnwrap(Dynamic<?> data, String name, String innerFieldName, Set<String> processedComponents) {
      return fixRemainderComponent(data, name, processedComponents, dynamic -> (Dynamic)DataFixUtils.orElse(dynamic.get(innerFieldName).result(), dynamic));
   }

   private static Dynamic<?> fixRemainderComponent(Dynamic<?> data, String name, Set<String> processedComponents, UnaryOperator<Dynamic<?>> unwrapper) {
      return data.update(name, dynamic -> {
         boolean bl = dynamic.get("show_in_tooltip").asBoolean(true);
         if (!bl) {
            processedComponents.add(name);
         }

         return unwrapper.apply(dynamic.remove("show_in_tooltip"));
      });
   }

   private static Typed<?> fixAdventureModePredicate(Typed<?> data, OpticFinder<?> optic, Type<?> type, String name, Set<String> processedComponents) {
      return data.updateTyped(optic, type, typed -> Util.writeAndReadTypedOrThrow(typed, type, dynamic -> {
         OptionalDynamic<?> optionalDynamic = dynamic.get("predicates");
         if (optionalDynamic.result().isEmpty()) {
            return dynamic;
         } else {
            boolean bl = dynamic.get("show_in_tooltip").asBoolean(true);
            if (!bl) {
               processedComponents.add(name);
            }

            return (Dynamic)optionalDynamic.result().get();
         }
      }));
   }
}
