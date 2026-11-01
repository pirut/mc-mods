package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class ValueObject {
   @Override
   public String toString() {
      StringBuilder $$0 = new StringBuilder("{");

      for (Field $$1 : this.getClass().getFields()) {
         if (!isStatic($$1)) {
            try {
               $$0.append(getName($$1)).append("=").append($$1.get(this)).append(" ");
            } catch (IllegalAccessException var7) {
            }
         }
      }

      $$0.deleteCharAt($$0.length() - 1);
      $$0.append('}');
      return $$0.toString();
   }

   private static String getName(Field p_87714_) {
      SerializedName $$1 = p_87714_.getAnnotation(SerializedName.class);
      return $$1 != null ? $$1.value() : p_87714_.getName();
   }

   private static boolean isStatic(Field p_87716_) {
      return Modifier.isStatic(p_87716_.getModifiers());
   }
}
