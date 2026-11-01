package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List.ListType;
import java.util.Optional;
import java.util.Set;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class RemoveBlockEntityTagFix extends DataFix {
   private final Set<String> blockEntityIdsToDrop;

   public RemoveBlockEntityTagFix(Schema outputSchema, Set<String> blockEntityIdsToDrop) {
      super(outputSchema, true);
      this.blockEntityIdsToDrop = blockEntityIdsToDrop;
   }

   public TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.ITEM_STACK);
      OpticFinder<?> opticFinder = type.findField("tag");
      OpticFinder<?> opticFinder2 = opticFinder.type().findField("BlockEntityTag");
      Type<?> type2 = this.getInputSchema().getType(References.ENTITY);
      OpticFinder<?> opticFinder3 = DSL.namedChoice(
         "minecraft:falling_block", this.getInputSchema().getChoiceType(References.ENTITY, "minecraft:falling_block")
      );
      OpticFinder<?> opticFinder4 = opticFinder3.type().findField("TileEntityData");
      Type<?> type3 = this.getInputSchema().getType(References.STRUCTURE);
      OpticFinder<?> opticFinder5 = type3.findField("blocks");
      OpticFinder<?> opticFinder6 = DSL.typeFinder(((ListType)opticFinder5.type()).getElement());
      OpticFinder<?> opticFinder7 = opticFinder6.type().findField("nbt");
      OpticFinder<String> opticFinder8 = DSL.fieldFinder("id", NamespacedSchema.namespacedString());
      return TypeRewriteRule.seq(
         this.fixTypeEverywhereTyped(
            "ItemRemoveBlockEntityTagFix",
            type,
            typed -> typed.updateTyped(opticFinder, typedx -> this.removeBlockEntity(typedx, opticFinder2, opticFinder8, "BlockEntityTag"))
         ),
         new TypeRewriteRule[]{
            this.fixTypeEverywhereTyped(
               "FallingBlockEntityRemoveBlockEntityTagFix",
               type2,
               typed -> typed.updateTyped(opticFinder3, typedx -> this.removeBlockEntity(typedx, opticFinder4, opticFinder8, "TileEntityData"))
            ),
            this.fixTypeEverywhereTyped(
               "StructureRemoveBlockEntityTagFix",
               type3,
               typed -> typed.updateTyped(
                  opticFinder5, typedx -> typedx.updateTyped(opticFinder6, typedxx -> this.removeBlockEntity(typedxx, opticFinder7, opticFinder8, "nbt"))
               )
            ),
            this.convertUnchecked(
               "ItemRemoveBlockEntityTagFix - update block entity type",
               this.getInputSchema().getType(References.BLOCK_ENTITY),
               this.getOutputSchema().getType(References.BLOCK_ENTITY)
            )
         }
      );
   }

   private Typed<?> removeBlockEntity(Typed<?> data, OpticFinder<?> tagFinder, OpticFinder<String> idFinder, String key) {
      Optional<? extends Typed<?>> optional = data.getOptionalTyped(tagFinder);
      if (optional.isEmpty()) {
         return data;
      } else {
         String string = optional.get().getOptional(idFinder).orElse("");
         return !this.blockEntityIdsToDrop.contains(string) ? data : Util.writeAndReadTypedOrThrow(data, data.getType(), dynamic -> dynamic.remove(key));
      }
   }
}
