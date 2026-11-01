package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class EntitySpawnerItemVariantComponentFix extends DataFix {
   public EntitySpawnerItemVariantComponentFix(Schema outputSchema) {
      super(outputSchema, false);
   }

   public final TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.ITEM_STACK);
      OpticFinder<Pair<String, String>> opticFinder = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
      OpticFinder<?> opticFinder2 = type.findField("components");
      return this.fixTypeEverywhereTyped(
         "ItemStack bucket_entity_data variants to separate components",
         type,
         typed -> {
            String string = typed.getOptional(opticFinder).<String>map(Pair::getSecond).orElse("");

            return switch (string) {
               case "minecraft:salmon_bucket" -> typed.updateTyped(opticFinder2, EntitySpawnerItemVariantComponentFix::fixSalmonBucket);
               case "minecraft:axolotl_bucket" -> typed.updateTyped(opticFinder2, EntitySpawnerItemVariantComponentFix::fixAxolotlBucket);
               case "minecraft:tropical_fish_bucket" -> typed.updateTyped(opticFinder2, EntitySpawnerItemVariantComponentFix::fixTropicalFishBucket);
               case "minecraft:painting" -> typed.updateTyped(
                  opticFinder2, typedx -> Util.writeAndReadTypedOrThrow(typedx, typedx.getType(), EntitySpawnerItemVariantComponentFix::fixPainting)
               );
               default -> typed;
            };
         }
      );
   }

   private static String getBaseColor(int variant) {
      return ExtraDataFixUtils.dyeColorIdToName(variant >> 16 & 0xFF);
   }

   private static String getPatternColor(int variant) {
      return ExtraDataFixUtils.dyeColorIdToName(variant >> 24 & 0xFF);
   }

   private static String getPattern(int variant) {
      return switch (variant & 65535) {
         case 1 -> "flopper";
         case 256 -> "sunstreak";
         case 257 -> "stripey";
         case 512 -> "snooper";
         case 513 -> "glitter";
         case 768 -> "dasher";
         case 769 -> "blockfish";
         case 1024 -> "brinely";
         case 1025 -> "betty";
         case 1280 -> "spotty";
         case 1281 -> "clayfish";
         default -> "kob";
      };
   }

   private static <T> Dynamic<T> fixTropicalFishBucket(Dynamic<T> data, Dynamic<T> entityData) {
      Optional<Number> optional = entityData.get("BucketVariantTag").asNumber().result();
      if (optional.isEmpty()) {
         return data;
      } else {
         int i = optional.get().intValue();
         String string = getPattern(i);
         String string2 = getBaseColor(i);
         String string3 = getPatternColor(i);
         return data.update("minecraft:bucket_entity_data", dynamic -> dynamic.remove("BucketVariantTag"))
            .set("minecraft:tropical_fish/pattern", data.createString(string))
            .set("minecraft:tropical_fish/base_color", data.createString(string2))
            .set("minecraft:tropical_fish/pattern_color", data.createString(string3));
      }
   }

   private static <T> Dynamic<T> fixAxolotlBucket(Dynamic<T> data, Dynamic<T> entityData) {
      Optional<Number> optional = entityData.get("Variant").asNumber().result();
      if (optional.isEmpty()) {
         return data;
      } else {
         String string = switch (optional.get().intValue()) {
            case 1 -> "wild";
            case 2 -> "gold";
            case 3 -> "cyan";
            case 4 -> "blue";
            default -> "lucy";
         };
         return data.update("minecraft:bucket_entity_data", dynamic -> dynamic.remove("Variant")).set("minecraft:axolotl/variant", data.createString(string));
      }
   }

   private static <T> Dynamic<T> fixSalmonBucket(Dynamic<T> data, Dynamic<T> entityData) {
      Optional<Dynamic<T>> optional = entityData.get("type").result();
      return optional.isEmpty()
         ? data
         : data.update("minecraft:bucket_entity_data", dynamic -> dynamic.remove("type")).set("minecraft:salmon/size", optional.get());
   }

   private static <T> Dynamic<T> fixPainting(Dynamic<T> data) {
      Optional<Dynamic<T>> optional = data.get("minecraft:entity_data").result();
      if (optional.isEmpty()) {
         return data;
      } else if (optional.get().get("id").asString().result().filter(string -> string.equals("minecraft:painting")).isEmpty()) {
         return data;
      } else {
         Optional<Dynamic<T>> optional2 = optional.get().get("variant").result();
         Dynamic<T> dynamic = optional.get().remove("variant");
         if (dynamic.remove("id").equals(dynamic.emptyMap())) {
            data = data.remove("minecraft:entity_data");
         } else {
            data = data.set("minecraft:entity_data", dynamic);
         }

         if (optional2.isPresent()) {
            data = data.set("minecraft:painting/variant", optional2.get());
         }

         return data;
      }
   }

   @FunctionalInterface
   interface Fixer extends Function<Typed<?>, Typed<?>> {
      default Typed<?> apply(Typed<?> typed) {
         return typed.update(DSL.remainderFinder(), this::fixRemainder);
      }

      default <T> Dynamic<T> fixRemainder(Dynamic<T> dynamic) {
         return dynamic.get("minecraft:bucket_entity_data").result().map(dynamic2 -> this.fixRemainder(dynamic, (Dynamic<T>)dynamic2)).orElse(dynamic);
      }

      <T> Dynamic<T> fixRemainder(Dynamic<T> dynamic, Dynamic<T> dynamic2);
   }
}
