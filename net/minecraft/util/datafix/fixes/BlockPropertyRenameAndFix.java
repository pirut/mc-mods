package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.function.UnaryOperator;

public class BlockPropertyRenameAndFix extends AbstractBlockPropertyFix {
   private final String blockId;
   private final String oldPropertyName;
   private final String newPropertyName;
   private final UnaryOperator<String> valueFixer;

   public BlockPropertyRenameAndFix(Schema p_394042_, String p_394125_, String p_394307_, String p_394585_, String p_394636_, UnaryOperator<String> p_393652_) {
      super(p_394042_, p_394125_);
      this.blockId = p_394307_;
      this.oldPropertyName = p_394585_;
      this.newPropertyName = p_394636_;
      this.valueFixer = p_393652_;
   }

   @Override
   protected boolean shouldFix(String p_394509_) {
      return p_394509_.equals(this.blockId);
   }

   @Override
   protected <T> Dynamic<T> fixProperties(String p_393867_, Dynamic<T> p_394529_) {
      return p_394529_.renameAndFixField(
         this.oldPropertyName, this.newPropertyName, p_394078_ -> p_394078_.createString(this.valueFixer.apply(p_394078_.asString("")))
      );
   }
}
