package net.minecraft.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.mojang.brigadier.StringReader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.lang.reflect.Field;
import net.minecraft.CharPredicate;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;

public class ParserUtils {
   private static final Field JSON_READER_POS = Util.make(() -> {
      try {
         Field $$0 = JsonReader.class.getDeclaredField("pos");
         $$0.setAccessible(true);
         return $$0;
      } catch (NoSuchFieldException var1) {
         throw new IllegalStateException("Couldn't get field 'pos' for JsonReader", var1);
      }
   });
   private static final Field JSON_READER_LINESTART = Util.make(() -> {
      try {
         Field $$0 = JsonReader.class.getDeclaredField("lineStart");
         $$0.setAccessible(true);
         return $$0;
      } catch (NoSuchFieldException var1) {
         throw new IllegalStateException("Couldn't get field 'lineStart' for JsonReader", var1);
      }
   });

   private static int getPos(JsonReader p_313929_) {
      try {
         return JSON_READER_POS.getInt(p_313929_) - JSON_READER_LINESTART.getInt(p_313929_);
      } catch (IllegalAccessException var2) {
         throw new IllegalStateException("Couldn't read position of JsonReader", var2);
      }
   }

   public static <T> T parseJson(HolderLookup.Provider p_323581_, StringReader p_313786_, Codec<T> p_313876_) {
      JsonReader $$3 = new JsonReader(new java.io.StringReader(p_313786_.getRemaining()));
      $$3.setLenient(false);

      Object var5;
      try {
         JsonElement $$4 = Streams.parse($$3);
         var5 = p_313876_.parse(p_323581_.createSerializationContext(JsonOps.INSTANCE), $$4).getOrThrow(JsonParseException::new);
      } catch (StackOverflowError var9) {
         throw new JsonParseException(var9);
      } finally {
         p_313786_.setCursor(p_313786_.getCursor() + getPos($$3));
      }

      return (T)var5;
   }

   public static String readWhile(StringReader p_332772_, CharPredicate p_332664_) {
      int $$2 = p_332772_.getCursor();

      while (p_332772_.canRead() && p_332664_.test(p_332772_.peek())) {
         p_332772_.skip();
      }

      return p_332772_.getString().substring($$2, p_332772_.getCursor());
   }
}
