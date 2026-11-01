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
   public EntitySpawnerItemVariantComponentFix(Schema p_397140_) {
      super(p_397140_, false);
   }

   public final TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.ITEM_STACK);
      OpticFinder<Pair<String, String>> opticfinder = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
      OpticFinder<?> opticfinder1 = type.findField("components");
      return this.fixTypeEverywhereTyped(
         "ItemStack bucket_entity_data variants to separate components",
         type,
         p_397051_ -> {
            String s = p_397051_.getOptional(opticfinder).<String>map(Pair::getSecond).orElse("");

            return switch (s) {
               case "minecraft:salmon_bucket" -> p_397051_.updateTyped(opticfinder1, EntitySpawnerItemVariantComponentFix::fixSalmonBucket);
               case "minecraft:axolotl_bucket" -> p_397051_.updateTyped(opticfinder1, EntitySpawnerItemVariantComponentFix::fixAxolotlBucket);
               case "minecraft:tropical_fish_bucket" -> p_397051_.updateTyped(opticfinder1, EntitySpawnerItemVariantComponentFix::fixTropicalFishBucket);
               case "minecraft:painting" -> p_397051_.updateTyped(
                  opticfinder1, p_466459_ -> Util.writeAndReadTypedOrThrow(p_466459_, p_466459_.getType(), EntitySpawnerItemVariantComponentFix::fixPainting)
               );
               default -> p_397051_;
            };
         }
      );
   }

   private static String getBaseColor(int p_397111_) {
      return ExtraDataFixUtils.dyeColorIdToName(p_397111_ >> 16 & 0xFF);
   }

   private static String getPatternColor(int p_397624_) {
      return ExtraDataFixUtils.dyeColorIdToName(p_397624_ >> 24 & 0xFF);
   }

   private static String getPattern(int p_396995_) {
      return switch (p_396995_ & 65535) {
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

   private static <T> Dynamic<T> fixTropicalFishBucket(Dynamic<T> p_397921_, Dynamic<T> p_397256_) {
      Optional<Number> optional = p_397256_.get("BucketVariantTag").asNumber().result();
      if (optional.isEmpty()) {
         return p_397921_;
      } else {
         int i = optional.get().intValue();
         String s = getPattern(i);
         String s1 = getBaseColor(i);
         String s2 = getPatternColor(i);
         return p_397921_.update("minecraft:bucket_entity_data", p_397278_ -> p_397278_.remove("BucketVariantTag"))
            .set("minecraft:tropical_fish/pattern", p_397921_.createString(s))
            .set("minecraft:tropical_fish/base_color", p_397921_.createString(s1))
            .set("minecraft:tropical_fish/pattern_color", p_397921_.createString(s2));
      }
   }

   private static <T> Dynamic<T> fixAxolotlBucket(Dynamic<T> p_397570_, Dynamic<T> p_398033_) {
      Optional<Number> optional = p_398033_.get("Variant").asNumber().result();
      if (optional.isEmpty()) {
         return p_397570_;
      } else {
         String s = switch (optional.get().intValue()) {
            case 1 -> "wild";
            case 2 -> "gold";
            case 3 -> "cyan";
            case 4 -> "blue";
            default -> "lucy";
         };
         return p_397570_.update("minecraft:bucket_entity_data", p_397463_ -> p_397463_.remove("Variant"))
            .set("minecraft:axolotl/variant", p_397570_.createString(s));
      }
   }

   private static <T> Dynamic<T> fixSalmonBucket(Dynamic<T> p_397549_, Dynamic<T> p_397605_) {
      Optional<Dynamic<T>> optional = p_397605_.get("type").result();
      return optional.isEmpty()
         ? p_397549_
         : p_397549_.update("minecraft:bucket_entity_data", p_397679_ -> p_397679_.remove("type")).set("minecraft:salmon/size", optional.get());
   }

   private static <T> Dynamic<T> fixPainting(Dynamic<T> p_397343_) {
      Optional<Dynamic<T>> optional = p_397343_.get("minecraft:entity_data").result();
      if (optional.isEmpty()) {
         return p_397343_;
      } else if (optional.get().get("id").asString().result().filter(p_397801_ -> p_397801_.equals("minecraft:painting")).isEmpty()) {
         return p_397343_;
      } else {
         Optional<Dynamic<T>> optional1 = optional.get().get("variant").result();
         Dynamic<T> dynamic = optional.get().remove("variant");
         if (dynamic.remove("id").equals(dynamic.emptyMap())) {
            p_397343_ = p_397343_.remove("minecraft:entity_data");
         } else {
            p_397343_ = p_397343_.set("minecraft:entity_data", dynamic);
         }

         if (optional1.isPresent()) {
            p_397343_ = p_397343_.set("minecraft:painting/variant", optional1.get());
         }

         return p_397343_;
      }
   }

   @FunctionalInterface
   interface Fixer extends Function<Typed<?>, Typed<?>> {
      default Typed<?> apply(Typed<?> p_397805_) {
         return p_397805_.update(DSL.remainderFinder(), this::fixRemainder);
      }

      default <T> Dynamic<T> fixRemainder(Dynamic<T> p_397593_) {
         return p_397593_.get("minecraft:bucket_entity_data").result().map(p_397558_ -> this.fixRemainder(p_397593_, (Dynamic<T>)p_397558_)).orElse(p_397593_);
      }

      <T> Dynamic<T> fixRemainder(Dynamic<T> var1, Dynamic<T> var2);
   }
}
