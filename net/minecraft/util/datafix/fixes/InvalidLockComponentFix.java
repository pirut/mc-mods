package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

public class InvalidLockComponentFix extends DataComponentRemainderFix {
   private static final Optional<String> INVALID_LOCK_CUSTOM_NAME = Optional.of("\"\"");

   public InvalidLockComponentFix(Schema p_388236_) {
      super(p_388236_, "InvalidLockComponentPredicateFix", "minecraft:lock");
   }

   @Nullable
   @Override
   protected <T> Dynamic<T> fixComponent(Dynamic<T> p_388620_) {
      return fixLock(p_388620_);
   }

   @Nullable
   public static <T> Dynamic<T> fixLock(Dynamic<T> p_387816_) {
      return isBrokenLock(p_387816_) ? null : p_387816_;
   }

   private static <T> boolean isBrokenLock(Dynamic<T> p_388895_) {
      return isMapWithOneField(
         p_388895_,
         "components",
         p_387987_ -> isMapWithOneField(p_387987_, "minecraft:custom_name", p_386809_ -> p_386809_.asString().result().equals(INVALID_LOCK_CUSTOM_NAME))
      );
   }

   private static <T> boolean isMapWithOneField(Dynamic<T> p_388919_, String p_388326_, Predicate<Dynamic<T>> p_387152_) {
      Optional<Map<Dynamic<T>, Dynamic<T>>> optional = p_388919_.getMapValues().result();
      return !optional.isEmpty() && optional.get().size() == 1 ? p_388919_.get(p_388326_).result().filter(p_387152_).isPresent() : false;
   }
}
