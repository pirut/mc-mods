package net.minecraft.util.datafix.fixes;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.schemas.NamespacedSchema;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ParticleUnflatteningFix extends DataFix {
   private static final Logger LOGGER = LogUtils.getLogger();

   public ParticleUnflatteningFix(Schema p_340896_) {
      super(p_340896_, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.PARTICLE);
      Type<?> type1 = this.getOutputSchema().getType(References.PARTICLE);
      return this.writeFixAndRead("ParticleUnflatteningFix", type, type1, this::fix);
   }

   private <T> Dynamic<T> fix(Dynamic<T> p_341243_) {
      Optional<String> optional = p_341243_.asString().result();
      if (optional.isEmpty()) {
         return p_341243_;
      } else {
         String s = optional.get();
         String[] astring = s.split(" ", 2);
         String s1 = NamespacedSchema.ensureNamespaced(astring[0]);
         Dynamic<T> dynamic = p_341243_.createMap(Map.of(p_341243_.createString("type"), p_341243_.createString(s1)));

         return switch (s1) {
            case "minecraft:item" -> astring.length > 1 ? this.updateItem(dynamic, astring[1]) : dynamic;
            case "minecraft:block", "minecraft:block_marker", "minecraft:falling_dust", "minecraft:dust_pillar" -> astring.length > 1
               ? this.updateBlock(dynamic, astring[1])
               : dynamic;
            case "minecraft:dust" -> astring.length > 1 ? this.updateDust(dynamic, astring[1]) : dynamic;
            case "minecraft:dust_color_transition" -> astring.length > 1 ? this.updateDustTransition(dynamic, astring[1]) : dynamic;
            case "minecraft:sculk_charge" -> astring.length > 1 ? this.updateSculkCharge(dynamic, astring[1]) : dynamic;
            case "minecraft:vibration" -> astring.length > 1 ? this.updateVibration(dynamic, astring[1]) : dynamic;
            case "minecraft:shriek" -> astring.length > 1 ? this.updateShriek(dynamic, astring[1]) : dynamic;
            default -> dynamic;
         };
      }
   }

   private <T> Dynamic<T> updateItem(Dynamic<T> p_340826_, String p_340894_) {
      int i = p_340894_.indexOf("{");
      Dynamic<T> dynamic = p_340826_.createMap(Map.of(p_340826_.createString("Count"), p_340826_.createInt(1)));
      if (i == -1) {
         dynamic = dynamic.set("id", p_340826_.createString(p_340894_));
      } else {
         dynamic = dynamic.set("id", p_340826_.createString(p_340894_.substring(0, i)));
         Dynamic<T> dynamic1 = parseTag(p_340826_.getOps(), p_340894_.substring(i));
         if (dynamic1 != null) {
            dynamic = dynamic.set("tag", dynamic1);
         }
      }

      return p_340826_.set("item", dynamic);
   }

   @Nullable
   private static <T> Dynamic<T> parseTag(DynamicOps<T> p_399942_, String p_341175_) {
      try {
         return new Dynamic(p_399942_, TagParser.create(p_399942_).parseFully(p_341175_));
      } catch (Exception var3) {
         LOGGER.warn("Failed to parse tag: {}", p_341175_, var3);
         return null;
      }
   }

   private <T> Dynamic<T> updateBlock(Dynamic<T> p_341247_, String p_341412_) {
      int i = p_341412_.indexOf("[");
      Dynamic<T> dynamic = p_341247_.emptyMap();
      if (i == -1) {
         dynamic = dynamic.set("Name", p_341247_.createString(NamespacedSchema.ensureNamespaced(p_341412_)));
      } else {
         dynamic = dynamic.set("Name", p_341247_.createString(NamespacedSchema.ensureNamespaced(p_341412_.substring(0, i))));
         Map<Dynamic<T>, Dynamic<T>> map = parseBlockProperties(p_341247_, p_341412_.substring(i));
         if (!map.isEmpty()) {
            dynamic = dynamic.set("Properties", p_341247_.createMap(map));
         }
      }

      return p_341247_.set("block_state", dynamic);
   }

   private static <T> Map<Dynamic<T>, Dynamic<T>> parseBlockProperties(Dynamic<T> p_341040_, String p_341381_) {
      try {
         Map<Dynamic<T>, Dynamic<T>> map = new HashMap<>();
         StringReader stringreader = new StringReader(p_341381_);
         stringreader.expect('[');
         stringreader.skipWhitespace();

         while (stringreader.canRead() && stringreader.peek() != ']') {
            stringreader.skipWhitespace();
            String s = stringreader.readString();
            stringreader.skipWhitespace();
            stringreader.expect('=');
            stringreader.skipWhitespace();
            String s1 = stringreader.readString();
            stringreader.skipWhitespace();
            map.put(p_341040_.createString(s), p_341040_.createString(s1));
            if (stringreader.canRead()) {
               if (stringreader.peek() != ',') {
                  break;
               }

               stringreader.skip();
            }
         }

         stringreader.expect(']');
         return map;
      } catch (Exception var6) {
         LOGGER.warn("Failed to parse block properties: {}", p_341381_, var6);
         return Map.of();
      }
   }

   private static <T> Dynamic<T> readVector(Dynamic<T> p_340849_, StringReader p_340969_) throws CommandSyntaxException {
      float f = p_340969_.readFloat();
      p_340969_.expect(' ');
      float f1 = p_340969_.readFloat();
      p_340969_.expect(' ');
      float f2 = p_340969_.readFloat();
      return p_340849_.createList(Stream.of(f, f1, f2).map(p_340849_::createFloat));
   }

   private <T> Dynamic<T> updateDust(Dynamic<T> p_340805_, String p_340908_) {
      try {
         StringReader stringreader = new StringReader(p_340908_);
         Dynamic<T> dynamic = readVector(p_340805_, stringreader);
         stringreader.expect(' ');
         float f = stringreader.readFloat();
         return p_340805_.set("color", dynamic).set("scale", p_340805_.createFloat(f));
      } catch (Exception var6) {
         LOGGER.warn("Failed to parse particle options: {}", p_340908_, var6);
         return p_340805_;
      }
   }

   private <T> Dynamic<T> updateDustTransition(Dynamic<T> p_341240_, String p_341296_) {
      try {
         StringReader stringreader = new StringReader(p_341296_);
         Dynamic<T> dynamic = readVector(p_341240_, stringreader);
         stringreader.expect(' ');
         float f = stringreader.readFloat();
         stringreader.expect(' ');
         Dynamic<T> dynamic1 = readVector(p_341240_, stringreader);
         return p_341240_.set("from_color", dynamic).set("to_color", dynamic1).set("scale", p_341240_.createFloat(f));
      } catch (Exception var7) {
         LOGGER.warn("Failed to parse particle options: {}", p_341296_, var7);
         return p_341240_;
      }
   }

   private <T> Dynamic<T> updateSculkCharge(Dynamic<T> p_340987_, String p_340846_) {
      try {
         StringReader stringreader = new StringReader(p_340846_);
         float f = stringreader.readFloat();
         return p_340987_.set("roll", p_340987_.createFloat(f));
      } catch (Exception var5) {
         LOGGER.warn("Failed to parse particle options: {}", p_340846_, var5);
         return p_340987_;
      }
   }

   private <T> Dynamic<T> updateVibration(Dynamic<T> p_340954_, String p_341325_) {
      try {
         StringReader stringreader = new StringReader(p_341325_);
         float f = (float)stringreader.readDouble();
         stringreader.expect(' ');
         float f1 = (float)stringreader.readDouble();
         stringreader.expect(' ');
         float f2 = (float)stringreader.readDouble();
         stringreader.expect(' ');
         int i = stringreader.readInt();
         Dynamic<T> dynamic = p_340954_.createIntList(IntStream.of(Mth.floor(f), Mth.floor(f1), Mth.floor(f2)));
         Dynamic<T> dynamic1 = p_340954_.createMap(
            Map.of(p_340954_.createString("type"), p_340954_.createString("minecraft:block"), p_340954_.createString("pos"), dynamic)
         );
         return p_340954_.set("destination", dynamic1).set("arrival_in_ticks", p_340954_.createInt(i));
      } catch (Exception var10) {
         LOGGER.warn("Failed to parse particle options: {}", p_341325_, var10);
         return p_340954_;
      }
   }

   private <T> Dynamic<T> updateShriek(Dynamic<T> p_340938_, String p_341343_) {
      try {
         StringReader stringreader = new StringReader(p_341343_);
         int i = stringreader.readInt();
         return p_340938_.set("delay", p_340938_.createInt(i));
      } catch (Exception var5) {
         LOGGER.warn("Failed to parse particle options: {}", p_341343_, var5);
         return p_340938_;
      }
   }
}
