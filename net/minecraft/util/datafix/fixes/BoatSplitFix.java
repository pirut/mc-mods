package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class BoatSplitFix extends DataFix {
   public BoatSplitFix(Schema p_376293_) {
      super(p_376293_, true);
   }

   private static boolean isNormalBoat(String p_376165_) {
      return p_376165_.equals("minecraft:boat");
   }

   private static boolean isChestBoat(String p_376209_) {
      return p_376209_.equals("minecraft:chest_boat");
   }

   private static boolean isAnyBoat(String p_376775_) {
      return isNormalBoat(p_376775_) || isChestBoat(p_376775_);
   }

   private static String mapVariantToNormalBoat(String p_376825_) {
      return switch (p_376825_) {
         case "spruce" -> "minecraft:spruce_boat";
         case "birch" -> "minecraft:birch_boat";
         case "jungle" -> "minecraft:jungle_boat";
         case "acacia" -> "minecraft:acacia_boat";
         case "cherry" -> "minecraft:cherry_boat";
         case "dark_oak" -> "minecraft:dark_oak_boat";
         case "mangrove" -> "minecraft:mangrove_boat";
         case "bamboo" -> "minecraft:bamboo_raft";
         default -> "minecraft:oak_boat";
      };
   }

   private static String mapVariantToChestBoat(String p_376447_) {
      return switch (p_376447_) {
         case "spruce" -> "minecraft:spruce_chest_boat";
         case "birch" -> "minecraft:birch_chest_boat";
         case "jungle" -> "minecraft:jungle_chest_boat";
         case "acacia" -> "minecraft:acacia_chest_boat";
         case "cherry" -> "minecraft:cherry_chest_boat";
         case "dark_oak" -> "minecraft:dark_oak_chest_boat";
         case "mangrove" -> "minecraft:mangrove_chest_boat";
         case "bamboo" -> "minecraft:bamboo_chest_raft";
         default -> "minecraft:oak_chest_boat";
      };
   }

   public TypeRewriteRule makeRule() {
      OpticFinder<String> opticfinder = DSL.fieldFinder("id", NamespacedSchema.namespacedString());
      Type<?> type = this.getInputSchema().getType(References.ENTITY);
      Type<?> type1 = this.getOutputSchema().getType(References.ENTITY);
      return this.fixTypeEverywhereTyped("BoatSplitFix", type, type1, p_376134_ -> {
         Optional<String> optional = p_376134_.getOptional(opticfinder);
         if (optional.isPresent() && isAnyBoat(optional.get())) {
            Dynamic<?> dynamic = (Dynamic<?>)p_376134_.getOrCreate(DSL.remainderFinder());
            Optional<String> optional1 = dynamic.get("Type").asString().result();
            String s;
            if (isChestBoat(optional.get())) {
               s = optional1.map(BoatSplitFix::mapVariantToChestBoat).orElse("minecraft:oak_chest_boat");
            } else {
               s = optional1.map(BoatSplitFix::mapVariantToNormalBoat).orElse("minecraft:oak_boat");
            }

            return ExtraDataFixUtils.cast(type1, p_376134_).update(DSL.remainderFinder(), p_376755_ -> p_376755_.remove("Type")).set(opticfinder, s);
         } else {
            return ExtraDataFixUtils.cast(type1, p_376134_);
         }
      });
   }
}
