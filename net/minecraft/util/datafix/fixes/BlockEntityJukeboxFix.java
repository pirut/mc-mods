package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;

public class BlockEntityJukeboxFix extends NamedEntityFix {
   public BlockEntityJukeboxFix(Schema p_14842_, boolean p_14843_) {
      super(p_14842_, p_14843_, "BlockEntityJukeboxFix", References.BLOCK_ENTITY, "minecraft:jukebox");
   }

   @Override
   protected Typed<?> fix(Typed<?> p_14846_) {
      Type<?> $$1 = this.getInputSchema().getChoiceType(References.BLOCK_ENTITY, "minecraft:jukebox");
      Type<?> $$2 = $$1.findFieldType("RecordItem");
      OpticFinder<?> $$3 = DSL.fieldFinder("RecordItem", $$2);
      Dynamic<?> $$4 = (Dynamic<?>)p_14846_.get(DSL.remainderFinder());
      int $$5 = $$4.get("Record").asInt(0);
      if ($$5 > 0) {
         $$4.remove("Record");
         String $$6 = ItemStackTheFlatteningFix.updateItem(ItemIdFix.getItem($$5), 0);
         if ($$6 != null) {
            Dynamic<?> $$7 = $$4.emptyMap();
            $$7 = $$7.set("id", $$7.createString($$6));
            $$7 = $$7.set("Count", $$7.createByte((byte)1));
            return p_14846_.set(
                  $$3,
                  (Typed)((Pair)$$2.readTyped($$7).result().orElseThrow(() -> new IllegalStateException("Could not create record item stack."))).getFirst()
               )
               .set(DSL.remainderFinder(), $$4);
         }
      }

      return p_14846_;
   }
}
