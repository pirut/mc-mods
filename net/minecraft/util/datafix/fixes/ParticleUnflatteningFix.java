package net.minecraft.util.datafix.fixes;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.schemas.NamespacedSchema;
import org.slf4j.Logger;

public class ParticleUnflatteningFix extends DataFix {
   private static final Logger LOGGER = LogUtils.getLogger();

   public ParticleUnflatteningFix(Schema p_340896_) {
      super(p_340896_, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getInputSchema().getType(References.PARTICLE);
      Type<?> $$1 = this.getOutputSchema().getType(References.PARTICLE);
      return this.writeFixAndRead("ParticleUnflatteningFix", $$0, $$1, this::fix);
   }

   private <T> Dynamic<T> fix(Dynamic<T> p_341243_) {
      Optional<String> $$1 = p_341243_.asString().result();
      if ($$1.isEmpty()) {
         return p_341243_;
      } else {
         String $$2 = $$1.get();
         String[] $$3 = $$2.split(" ", 2);
         String $$4 = NamespacedSchema.ensureNamespaced($$3[0]);
         Dynamic<T> $$5 = p_341243_.createMap(Map.of(p_341243_.createString("type"), p_341243_.createString($$4)));

         return switch ($$4) {
            case "minecraft:item" -> $$3.length > 1 ? this.updateItem($$5, $$3[1]) : $$5;
            case "minecraft:block", "minecraft:block_marker", "minecraft:falling_dust", "minecraft:dust_pillar" -> $$3.length > 1
               ? this.updateBlock($$5, $$3[1])
               : $$5;
            case "minecraft:dust" -> $$3.length > 1 ? this.updateDust($$5, $$3[1]) : $$5;
            case "minecraft:dust_color_transition" -> $$3.length > 1 ? this.updateDustTransition($$5, $$3[1]) : $$5;
            case "minecraft:sculk_charge" -> $$3.length > 1 ? this.updateSculkCharge($$5, $$3[1]) : $$5;
            case "minecraft:vibration" -> $$3.length > 1 ? this.updateVibration($$5, $$3[1]) : $$5;
            case "minecraft:shriek" -> $$3.length > 1 ? this.updateShriek($$5, $$3[1]) : $$5;
            default -> $$5;
         };
      }
   }

   private <T> Dynamic<T> updateItem(Dynamic<T> p_340826_, String p_340894_) {
      int $$2 = p_340894_.indexOf("{");
      Dynamic<T> $$3 = p_340826_.createMap(Map.of(p_340826_.createString("Count"), p_340826_.createInt(1)));
      if ($$2 == -1) {
         $$3 = $$3.set("id", p_340826_.createString(p_340894_));
      } else {
         $$3 = $$3.set("id", p_340826_.createString(p_340894_.substring(0, $$2)));
         CompoundTag $$4 = parseTag(p_340894_.substring($$2));
         if ($$4 != null) {
            $$3 = $$3.set("tag", new Dynamic(NbtOps.INSTANCE, $$4).convert(p_340826_.getOps()));
         }
      }

      return p_340826_.set("item", $$3);
   }

   @Nullable
   private static CompoundTag parseTag(String p_341175_) {
      try {
         return TagParser.parseTag(p_341175_);
      } catch (Exception var2) {
         LOGGER.warn("Failed to parse tag: {}", p_341175_, var2);
         return null;
      }
   }

   private <T> Dynamic<T> updateBlock(Dynamic<T> p_341247_, String p_341412_) {
      int $$2 = p_341412_.indexOf("[");
      Dynamic<T> $$3 = p_341247_.emptyMap();
      if ($$2 == -1) {
         $$3 = $$3.set("Name", p_341247_.createString(NamespacedSchema.ensureNamespaced(p_341412_)));
      } else {
         $$3 = $$3.set("Name", p_341247_.createString(NamespacedSchema.ensureNamespaced(p_341412_.substring(0, $$2))));
         Map<Dynamic<T>, Dynamic<T>> $$4 = parseBlockProperties(p_341247_, p_341412_.substring($$2));
         if (!$$4.isEmpty()) {
            $$3 = $$3.set("Properties", p_341247_.createMap($$4));
         }
      }

      return p_341247_.set("block_state", $$3);
   }

   private static <T> Map<Dynamic<T>, Dynamic<T>> parseBlockProperties(Dynamic<T> p_341040_, String p_341381_) {
      try {
         Map<Dynamic<T>, Dynamic<T>> $$2 = new HashMap<>();
         StringReader $$3 = new StringReader(p_341381_);
         $$3.expect('[');
         $$3.skipWhitespace();

         while ($$3.canRead() && $$3.peek() != ']') {
            $$3.skipWhitespace();
            String $$4 = $$3.readString();
            $$3.skipWhitespace();
            $$3.expect('=');
            $$3.skipWhitespace();
            String $$5 = $$3.readString();
            $$3.skipWhitespace();
            $$2.put(p_341040_.createString($$4), p_341040_.createString($$5));
            if ($$3.canRead()) {
               if ($$3.peek() != ',') {
                  break;
               }

               $$3.skip();
            }
         }

         $$3.expect(']');
         return $$2;
      } catch (Exception var6) {
         LOGGER.warn("Failed to parse block properties: {}", p_341381_, var6);
         return Map.of();
      }
   }

   private static <T> Dynamic<T> readVector(Dynamic<T> p_340849_, StringReader p_340969_) throws CommandSyntaxException {
      float $$2 = p_340969_.readFloat();
      p_340969_.expect(' ');
      float $$3 = p_340969_.readFloat();
      p_340969_.expect(' ');
      float $$4 = p_340969_.readFloat();
      return p_340849_.createList(Stream.of($$2, $$3, $$4).map(p_340849_::createFloat));
   }

   private <T> Dynamic<T> updateDust(Dynamic<T> p_340805_, String p_340908_) {
      try {
         StringReader $$2 = new StringReader(p_340908_);
         Dynamic<T> $$3 = readVector(p_340805_, $$2);
         $$2.expect(' ');
         float $$4 = $$2.readFloat();
         return p_340805_.set("color", $$3).set("scale", p_340805_.createFloat($$4));
      } catch (Exception var6) {
         LOGGER.warn("Failed to parse particle options: {}", p_340908_, var6);
         return p_340805_;
      }
   }

   private <T> Dynamic<T> updateDustTransition(Dynamic<T> p_341240_, String p_341296_) {
      try {
         StringReader $$2 = new StringReader(p_341296_);
         Dynamic<T> $$3 = readVector(p_341240_, $$2);
         $$2.expect(' ');
         float $$4 = $$2.readFloat();
         $$2.expect(' ');
         Dynamic<T> $$5 = readVector(p_341240_, $$2);
         return p_341240_.set("from_color", $$3).set("to_color", $$5).set("scale", p_341240_.createFloat($$4));
      } catch (Exception var7) {
         LOGGER.warn("Failed to parse particle options: {}", p_341296_, var7);
         return p_341240_;
      }
   }

   private <T> Dynamic<T> updateSculkCharge(Dynamic<T> p_340987_, String p_340846_) {
      try {
         StringReader $$2 = new StringReader(p_340846_);
         float $$3 = $$2.readFloat();
         return p_340987_.set("roll", p_340987_.createFloat($$3));
      } catch (Exception var5) {
         LOGGER.warn("Failed to parse particle options: {}", p_340846_, var5);
         return p_340987_;
      }
   }

   private <T> Dynamic<T> updateVibration(Dynamic<T> p_340954_, String p_341325_) {
      try {
         StringReader $$2 = new StringReader(p_341325_);
         float $$3 = (float)$$2.readDouble();
         $$2.expect(' ');
         float $$4 = (float)$$2.readDouble();
         $$2.expect(' ');
         float $$5 = (float)$$2.readDouble();
         $$2.expect(' ');
         int $$6 = $$2.readInt();
         Dynamic<T> $$7 = p_340954_.createIntList(IntStream.of(Mth.floor($$3), Mth.floor($$4), Mth.floor($$5)));
         Dynamic<T> $$8 = p_340954_.createMap(
            Map.of(p_340954_.createString("type"), p_340954_.createString("minecraft:block"), p_340954_.createString("pos"), $$7)
         );
         return p_340954_.set("destination", $$8).set("arrival_in_ticks", p_340954_.createInt($$6));
      } catch (Exception var10) {
         LOGGER.warn("Failed to parse particle options: {}", p_341325_, var10);
         return p_340954_;
      }
   }

   private <T> Dynamic<T> updateShriek(Dynamic<T> p_340938_, String p_341343_) {
      try {
         StringReader $$2 = new StringReader(p_341343_);
         int $$3 = $$2.readInt();
         return p_340938_.set("delay", p_340938_.createInt($$3));
      } catch (Exception var5) {
         LOGGER.warn("Failed to parse particle options: {}", p_341343_, var5);
         return p_340938_;
      }
   }
}
