package net.minecraft.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.Reader;

public class LenientJsonParser {
   public static JsonElement parse(Reader p_422300_) throws JsonIOException, JsonSyntaxException {
      return JsonParser.parseReader(p_422300_);
   }

   public static JsonElement parse(String p_421547_) throws JsonSyntaxException {
      return JsonParser.parseString(p_421547_);
   }
}
