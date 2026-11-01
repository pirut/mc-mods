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

   public RemoveBlockEntityTagFix(Schema p_425633_, Set<String> p_426286_) {
      super(p_425633_, true);
      this.blockEntityIdsToDrop = p_426286_;
   }

   public TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.ITEM_STACK);
      OpticFinder<?> opticfinder = type.findField("tag");
      OpticFinder<?> opticfinder1 = opticfinder.type().findField("BlockEntityTag");
      Type<?> type1 = this.getInputSchema().getType(References.ENTITY);
      OpticFinder<?> opticfinder2 = DSL.namedChoice(
         "minecraft:falling_block", this.getInputSchema().getChoiceType(References.ENTITY, "minecraft:falling_block")
      );
      OpticFinder<?> opticfinder3 = opticfinder2.type().findField("TileEntityData");
      Type<?> type2 = this.getInputSchema().getType(References.STRUCTURE);
      OpticFinder<?> opticfinder4 = type2.findField("blocks");
      OpticFinder<?> opticfinder5 = DSL.typeFinder(((ListType)opticfinder4.type()).getElement());
      OpticFinder<?> opticfinder6 = opticfinder5.type().findField("nbt");
      OpticFinder<String> opticfinder7 = DSL.fieldFinder("id", NamespacedSchema.namespacedString());
      return TypeRewriteRule.seq(
         this.fixTypeEverywhereTyped(
            "ItemRemoveBlockEntityTagFix",
            type,
            p_426223_ -> p_426223_.updateTyped(opticfinder, p_426122_ -> this.removeBlockEntity(p_426122_, opticfinder1, opticfinder7, "BlockEntityTag"))
         ),
         new TypeRewriteRule[]{
            this.fixTypeEverywhereTyped(
               "FallingBlockEntityRemoveBlockEntityTagFix",
               type1,
               p_425901_ -> p_425901_.updateTyped(opticfinder2, p_426084_ -> this.removeBlockEntity(p_426084_, opticfinder3, opticfinder7, "TileEntityData"))
            ),
            this.fixTypeEverywhereTyped(
               "StructureRemoveBlockEntityTagFix",
               type2,
               p_426300_ -> p_426300_.updateTyped(
                  opticfinder4,
                  p_426016_ -> p_426016_.updateTyped(opticfinder5, p_425911_ -> this.removeBlockEntity(p_425911_, opticfinder6, opticfinder7, "nbt"))
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

   private Typed<?> removeBlockEntity(Typed<?> p_425907_, OpticFinder<?> p_425929_, OpticFinder<String> p_425693_, String p_425948_) {
      Optional<? extends Typed<?>> optional = p_425907_.getOptionalTyped(p_425929_);
      if (optional.isEmpty()) {
         return p_425907_;
      } else {
         String s = optional.get().getOptional(p_425693_).orElse("");
         return !this.blockEntityIdsToDrop.contains(s)
            ? p_425907_
            : Util.writeAndReadTypedOrThrow(p_425907_, p_425907_.getType(), p_426288_ -> p_426288_.remove(p_425948_));
      }
   }
}
