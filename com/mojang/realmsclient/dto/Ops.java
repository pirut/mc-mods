package com.mojang.realmsclient.dto;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Set;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Ops extends ValueObject {
   public Set<String> ops = Sets.newHashSet();

   public static Ops parse(String p_87421_) {
      Ops $$1 = new Ops();
      JsonParser $$2 = new JsonParser();

      try {
         JsonElement $$3 = $$2.parse(p_87421_);
         JsonObject $$4 = $$3.getAsJsonObject();
         JsonElement $$5 = $$4.get("ops");
         if ($$5.isJsonArray()) {
            for (JsonElement $$6 : $$5.getAsJsonArray()) {
               $$1.ops.add($$6.getAsString());
            }
         }
      } catch (Exception var8) {
      }

      return $$1;
   }
}
