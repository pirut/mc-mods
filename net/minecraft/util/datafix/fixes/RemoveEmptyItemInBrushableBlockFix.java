package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class RemoveEmptyItemInBrushableBlockFix extends NamedEntityWriteReadFix {
   public RemoveEmptyItemInBrushableBlockFix(Schema p_342003_) {
      super(p_342003_, false, "RemoveEmptyItemInSuspiciousBlockFix", References.BLOCK_ENTITY, "minecraft:brushable_block");
   }

   @Override
   protected <T> Dynamic<T> fix(Dynamic<T> p_342013_) {
      Optional<Dynamic<T>> $$1 = p_342013_.get("item").result();
      return $$1.isPresent() && isEmptyStack($$1.get()) ? p_342013_.remove("item") : p_342013_;
   }

   private static boolean isEmptyStack(Dynamic<?> p_342006_) {
      String $$1 = NamespacedSchema.ensureNamespaced(p_342006_.get("id").asString("minecraft:air"));
      int $$2 = p_342006_.get("count").asInt(0);
      return $$1.equals("minecraft:air") || $$2 == 0;
   }
}
