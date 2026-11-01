package net.minecraft.util.datafix.fixes;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public class LockComponentPredicateFix extends DataComponentRemainderFix {
   public static final Escaper ESCAPER = Escapers.builder().addEscape('"', "\\\"").addEscape('\\', "\\\\").build();

   public LockComponentPredicateFix(Schema p_376234_) {
      super(p_376234_, "LockComponentPredicateFix", "minecraft:lock");
   }

   @Nullable
   @Override
   protected <T> Dynamic<T> fixComponent(Dynamic<T> p_376828_) {
      return fixLock(p_376828_);
   }

   @Nullable
   public static <T> Dynamic<T> fixLock(Dynamic<T> p_376818_) {
      Optional<String> optional = p_376818_.asString().result();
      if (optional.isEmpty()) {
         return null;
      } else if (optional.get().isEmpty()) {
         return null;
      } else {
         Dynamic<T> dynamic = p_376818_.createString("\"" + ESCAPER.escape(optional.get()) + "\"");
         Dynamic<T> dynamic1 = p_376818_.emptyMap().set("minecraft:custom_name", dynamic);
         return p_376818_.emptyMap().set("components", dynamic1);
      }
   }
}
