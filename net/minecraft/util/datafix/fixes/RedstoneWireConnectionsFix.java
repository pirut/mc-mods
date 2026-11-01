package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class RedstoneWireConnectionsFix extends DataFix {
   public RedstoneWireConnectionsFix(Schema p_16749_) {
      super(p_16749_, false);
   }

   protected TypeRewriteRule makeRule() {
      Schema $$0 = this.getInputSchema();
      return this.fixTypeEverywhereTyped(
         "RedstoneConnectionsFix", $$0.getType(References.BLOCK_STATE), p_16751_ -> p_16751_.update(DSL.remainderFinder(), this::updateRedstoneConnections)
      );
   }

   private <T> Dynamic<T> updateRedstoneConnections(Dynamic<T> p_16753_) {
      boolean $$1 = p_16753_.get("Name").asString().result().filter("minecraft:redstone_wire"::equals).isPresent();
      return !$$1
         ? p_16753_
         : p_16753_.update(
            "Properties",
            p_16760_ -> {
               String $$1x = p_16760_.get("east").asString("none");
               String $$2 = p_16760_.get("west").asString("none");
               String $$3 = p_16760_.get("north").asString("none");
               String $$4 = p_16760_.get("south").asString("none");
               boolean $$5 = isConnected($$1x) || isConnected($$2);
               boolean $$6 = isConnected($$3) || isConnected($$4);
               String $$7 = !isConnected($$1x) && !$$6 ? "side" : $$1x;
               String $$8 = !isConnected($$2) && !$$6 ? "side" : $$2;
               String $$9 = !isConnected($$3) && !$$5 ? "side" : $$3;
               String $$10 = !isConnected($$4) && !$$5 ? "side" : $$4;
               return p_16760_.update("east", p_145627_ -> p_145627_.createString($$7))
                  .update("west", p_145624_ -> p_145624_.createString($$8))
                  .update("north", p_145621_ -> p_145621_.createString($$9))
                  .update("south", p_145618_ -> p_145618_.createString($$10));
            }
         );
   }

   private static boolean isConnected(String p_16755_) {
      return !"none".equals(p_16755_);
   }
}
