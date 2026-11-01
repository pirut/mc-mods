package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class CarvingStepRemoveFix extends DataFix {
   public CarvingStepRemoveFix(Schema outputSchema) {
      super(outputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped("CarvingStepRemoveFix", this.getInputSchema().getType(References.CHUNK), CarvingStepRemoveFix::fixChunk);
   }

   private static Typed<?> fixChunk(Typed<?> chunkTag) {
      return chunkTag.update(DSL.remainderFinder(), dynamic -> {
         Dynamic<?> dynamic2 = dynamic;
         Optional<? extends Dynamic<?>> optional = dynamic.get("CarvingMasks").result();
         if (optional.isPresent()) {
            Optional<? extends Dynamic<?>> optional2 = optional.get().get("AIR").result();
            if (optional2.isPresent()) {
               dynamic2 = dynamic.set("carving_mask", optional2.get());
            }
         }

         return dynamic2.remove("CarvingMasks");
      });
   }
}
