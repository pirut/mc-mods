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

   public TooltipDisplayComponentFix(Schema p_399807_) {
      super(p_399807_, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.DATA_COMPONENTS);
      Type<?> type1 = this.getOutputSchema().getType(References.DATA_COMPONENTS);
      OpticFinder<?> opticfinder = type.findField("minecraft:can_place_on");
      OpticFinder<?> opticfinder1 = type.findField("minecraft:can_break");
      Type<?> type2 = type1.findFieldType("minecraft:can_place_on");
      Type<?> type3 = type1.findFieldType("minecraft:can_break");
      return this.fixTypeEverywhereTyped("TooltipDisplayComponentFix", type, type1, p_399664_ -> fix(p_399664_, opticfinder, opticfinder1, type2, type3));
   }

   private static Typed<?> fix(Typed<?> p_399559_, OpticFinder<?> p_399761_, OpticFinder<?> p_400308_, Type<?> p_399624_, Type<?> p_399684_) {
      Set<String> set = new HashSet<>();
      p_399559_ = fixAdventureModePredicate(p_399559_, p_399761_, p_399624_, "minecraft:can_place_on", set);
      p_399559_ = fixAdventureModePredicate(p_399559_, p_400308_, p_399684_, "minecraft:can_break", set);
      return p_399559_.update(
         DSL.remainderFinder(),
         p_423221_ -> {
            p_423221_ = fixSimpleComponent(p_423221_, "minecraft:trim", set);
            p_423221_ = fixSimpleComponent(p_423221_, "minecraft:unbreakable", set);
            p_423221_ = fixComponentAndUnwrap(p_423221_, "minecraft:dyed_color", "rgb", set);
            p_423221_ = fixComponentAndUnwrap(p_423221_, "minecraft:attribute_modifiers", "modifiers", set);
            p_423221_ = fixComponentAndUnwrap(p_423221_, "minecraft:enchantments", "levels", set);
            p_423221_ = fixComponentAndUnwrap(p_423221_, "minecraft:stored_enchantments", "levels", set);
            p_423221_ = fixComponentAndUnwrap(p_423221_, "minecraft:jukebox_playable", "song", set);
            boolean flag = p_423221_.get("minecraft:hide_tooltip").result().isPresent();
            p_423221_ = p_423221_.remove("minecraft:hide_tooltip");
            boolean flag1 = p_423221_.get("minecraft:hide_additional_tooltip").result().isPresent();
            p_423221_ = p_423221_.remove("minecraft:hide_additional_tooltip");
            if (flag1) {
               for (String s : CONVERTED_ADDITIONAL_TOOLTIP_TYPES) {
                  if (p_423221_.get(s).result().isPresent()) {
                     set.add(s);
                  }
               }
            }

            return set.isEmpty() && !flag
               ? p_423221_
               : p_423221_.set(
                  "minecraft:tooltip_display",
                  p_423221_.createMap(
                     Map.of(
                        p_423221_.createString("hide_tooltip"),
                        p_423221_.createBoolean(flag),
                        p_423221_.createString("hidden_components"),
                        p_423221_.createList(set.stream().map(p_423221_::createString))
                     )
                  )
               );
         }
      );
   }

   private static Dynamic<?> fixSimpleComponent(Dynamic<?> p_400153_, String p_399904_, Set<String> p_399838_) {
      return fixRemainderComponent(p_400153_, p_399904_, p_399838_, UnaryOperator.identity());
   }

   private static Dynamic<?> fixComponentAndUnwrap(Dynamic<?> p_399467_, String p_399917_, String p_400243_, Set<String> p_400006_) {
      return fixRemainderComponent(p_399467_, p_399917_, p_400006_, p_400276_ -> (Dynamic)DataFixUtils.orElse(p_400276_.get(p_400243_).result(), p_400276_));
   }

   private static Dynamic<?> fixRemainderComponent(Dynamic<?> p_400119_, String p_400142_, Set<String> p_399576_, UnaryOperator<Dynamic<?>> p_399468_) {
      return p_400119_.update(p_400142_, p_399611_ -> {
         boolean flag = p_399611_.get("show_in_tooltip").asBoolean(true);
         if (!flag) {
            p_399576_.add(p_400142_);
         }

         return p_399468_.apply(p_399611_.remove("show_in_tooltip"));
      });
   }

   private static Typed<?> fixAdventureModePredicate(Typed<?> p_399798_, OpticFinder<?> p_399899_, Type<?> p_399691_, String p_400139_, Set<String> p_400013_) {
      return p_399798_.updateTyped(p_399899_, p_399691_, p_466487_ -> Util.writeAndReadTypedOrThrow(p_466487_, p_399691_, p_399655_ -> {
         OptionalDynamic<?> optionaldynamic = p_399655_.get("predicates");
         if (optionaldynamic.result().isEmpty()) {
            return p_399655_;
         } else {
            boolean flag = p_399655_.get("show_in_tooltip").asBoolean(true);
            if (!flag) {
               p_400013_.add(p_400139_);
            }

            return (Dynamic)optionaldynamic.result().get();
         }
      }));
   }
}
