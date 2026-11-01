package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.Map.Entry;

public class EntityFieldsRenameFix extends NamedEntityFix {
   private final Map<String, String> renames;

   public EntityFieldsRenameFix(Schema p_382802_, String p_383185_, String p_383122_, Map<String, String> p_382950_) {
      super(p_382802_, false, p_383185_, References.ENTITY, p_383122_);
      this.renames = p_382950_;
   }

   public Dynamic<?> fixTag(Dynamic<?> p_383186_) {
      for (Entry<String, String> entry : this.renames.entrySet()) {
         p_383186_ = p_383186_.renameField(entry.getKey(), entry.getValue());
      }

      return p_383186_;
   }

   @Override
   protected Typed<?> fix(Typed<?> p_382900_) {
      return p_382900_.update(DSL.remainderFinder(), this::fixTag);
   }
}
