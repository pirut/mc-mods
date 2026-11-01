package net.minecraft.util;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;

public class LowerCaseEnumTypeAdapterFactory implements TypeAdapterFactory {
   @Nullable
   public <T> TypeAdapter<T> create(Gson p_13982_, TypeToken<T> p_13983_) {
      Class<T> $$2 = p_13983_.getRawType();
      if (!$$2.isEnum()) {
         return null;
      } else {
         final Map<String, T> $$3 = Maps.newHashMap();

         for (T $$4 : $$2.getEnumConstants()) {
            $$3.put(this.toLowercase($$4), $$4);
         }

         return new TypeAdapter<T>() {
            public void write(JsonWriter p_13992_, T p_13993_) throws IOException {
               if (p_13993_ == null) {
                  p_13992_.nullValue();
               } else {
                  p_13992_.value(LowerCaseEnumTypeAdapterFactory.this.toLowercase(p_13993_));
               }
            }

            @Nullable
            public T read(JsonReader p_13990_) throws IOException {
               if (p_13990_.peek() == JsonToken.NULL) {
                  p_13990_.nextNull();
                  return null;
               } else {
                  return $$3.get(p_13990_.nextString());
               }
            }
         };
      }
   }

   String toLowercase(Object p_13980_) {
      return p_13980_ instanceof Enum ? ((Enum)p_13980_).name().toLowerCase(Locale.ROOT) : p_13980_.toString().toLowerCase(Locale.ROOT);
   }
}
