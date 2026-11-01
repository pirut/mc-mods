package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V1458 extends NamespacedSchema {
   public V1458(int p_394362_, Schema p_393484_) {
      super(p_394362_, p_393484_);
   }

   public void registerTypes(Schema p_393569_, Map<String, Supplier<TypeTemplate>> p_394292_, Map<String, Supplier<TypeTemplate>> p_394498_) {
      super.registerTypes(p_393569_, p_394292_, p_394498_);
      p_393569_.registerType(
         true,
         References.ENTITY,
         () -> DSL.and(
            References.ENTITY_EQUIPMENT.in(p_393569_),
            DSL.optionalFields("CustomName", References.TEXT_COMPONENT.in(p_393569_), DSL.taggedChoiceLazy("id", namespacedString(), p_394292_))
         )
      );
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema p_393778_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(p_393778_);
      p_393778_.register(map, "minecraft:beacon", () -> nameable(p_393778_));
      p_393778_.register(map, "minecraft:banner", () -> nameable(p_393778_));
      p_393778_.register(map, "minecraft:brewing_stand", () -> nameableInventory(p_393778_));
      p_393778_.register(map, "minecraft:chest", () -> nameableInventory(p_393778_));
      p_393778_.register(map, "minecraft:trapped_chest", () -> nameableInventory(p_393778_));
      p_393778_.register(map, "minecraft:dispenser", () -> nameableInventory(p_393778_));
      p_393778_.register(map, "minecraft:dropper", () -> nameableInventory(p_393778_));
      p_393778_.register(map, "minecraft:enchanting_table", () -> nameable(p_393778_));
      p_393778_.register(map, "minecraft:furnace", () -> nameableInventory(p_393778_));
      p_393778_.register(map, "minecraft:hopper", () -> nameableInventory(p_393778_));
      p_393778_.register(map, "minecraft:shulker_box", () -> nameableInventory(p_393778_));
      return map;
   }

   public static TypeTemplate nameableInventory(Schema p_393480_) {
      return DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(p_393480_)), "CustomName", References.TEXT_COMPONENT.in(p_393480_));
   }

   public static TypeTemplate nameable(Schema p_393518_) {
      return DSL.optionalFields("CustomName", References.TEXT_COMPONENT.in(p_393518_));
   }
}
