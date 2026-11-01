package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class JigsawPropertiesFix extends NamedEntityFix {
   public JigsawPropertiesFix(Schema p_16182_, boolean p_16183_) {
      super(p_16182_, p_16183_, "JigsawPropertiesFix", References.BLOCK_ENTITY, "minecraft:jigsaw");
   }

   private static Dynamic<?> fixTag(Dynamic<?> p_16187_) {
      String $$1 = p_16187_.get("attachement_type").asString("minecraft:empty");
      String $$2 = p_16187_.get("target_pool").asString("minecraft:empty");
      return p_16187_.set("name", p_16187_.createString($$1))
         .set("target", p_16187_.createString($$1))
         .remove("attachement_type")
         .set("pool", p_16187_.createString($$2))
         .remove("target_pool");
   }

   @Override
   protected Typed<?> fix(Typed<?> p_16185_) {
      return p_16185_.update(DSL.remainderFinder(), JigsawPropertiesFix::fixTag);
   }
}
