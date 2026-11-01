package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4312 extends NamespacedSchema {
   public V4312(int p_401898_, Schema p_401801_) {
      super(p_401898_, p_401801_);
   }

   public void registerTypes(Schema p_401892_, Map<String, Supplier<TypeTemplate>> p_401819_, Map<String, Supplier<TypeTemplate>> p_401922_) {
      super.registerTypes(p_401892_, p_401819_, p_401922_);
      p_401892_.registerType(
         false,
         References.PLAYER,
         () -> DSL.and(
            References.ENTITY_EQUIPMENT.in(p_401892_),
            DSL.optionalFields(
               new Pair[]{
                  Pair.of("RootVehicle", DSL.optionalFields("Entity", References.ENTITY_TREE.in(p_401892_))),
                  Pair.of("ender_pearls", DSL.list(References.ENTITY_TREE.in(p_401892_))),
                  Pair.of("Inventory", DSL.list(References.ITEM_STACK.in(p_401892_))),
                  Pair.of("EnderItems", DSL.list(References.ITEM_STACK.in(p_401892_))),
                  Pair.of("ShoulderEntityLeft", References.ENTITY_TREE.in(p_401892_)),
                  Pair.of("ShoulderEntityRight", References.ENTITY_TREE.in(p_401892_)),
                  Pair.of(
                     "recipeBook",
                     DSL.optionalFields("recipes", DSL.list(References.RECIPE.in(p_401892_)), "toBeDisplayed", DSL.list(References.RECIPE.in(p_401892_)))
                  )
               }
            )
         )
      );
   }
}
