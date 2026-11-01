package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List.ListType;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public class BeehiveFieldRenameFix extends DataFix {
   public BeehiveFieldRenameFix(Schema p_331407_) {
      super(p_331407_, true);
   }

   private Dynamic<?> fixBeehive(Dynamic<?> p_331749_) {
      return p_331749_.remove("Bees");
   }

   private Dynamic<?> fixBee(Dynamic<?> p_331694_) {
      p_331694_ = p_331694_.remove("EntityData");
      p_331694_ = p_331694_.renameField("TicksInHive", "ticks_in_hive");
      return p_331694_.renameField("MinOccupationTicks", "min_ticks_in_hive");
   }

   public TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getInputSchema().getChoiceType(References.BLOCK_ENTITY, "minecraft:beehive");
      OpticFinder<?> $$1 = DSL.namedChoice("minecraft:beehive", $$0);
      ListType<?> $$2 = (ListType<?>)$$0.findFieldType("Bees");
      Type<?> $$3 = $$2.getElement();
      OpticFinder<?> $$4 = DSL.fieldFinder("Bees", $$2);
      OpticFinder<?> $$5 = DSL.typeFinder($$3);
      Type<?> $$6 = this.getInputSchema().getType(References.BLOCK_ENTITY);
      Type<?> $$7 = this.getOutputSchema().getType(References.BLOCK_ENTITY);
      return this.fixTypeEverywhereTyped(
         "BeehiveFieldRenameFix",
         $$6,
         $$7,
         p_330576_ -> ExtraDataFixUtils.cast(
            $$7,
            p_330576_.updateTyped(
               $$1,
               p_331994_ -> p_331994_.update(DSL.remainderFinder(), this::fixBeehive)
                  .updateTyped($$4, p_331926_ -> p_331926_.updateTyped($$5, p_331349_ -> p_331349_.update(DSL.remainderFinder(), this::fixBee)))
            )
         )
      );
   }
}
