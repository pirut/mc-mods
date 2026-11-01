package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V1451_3 extends NamespacedSchema {
   public V1451_3(int p_17444_, Schema p_17445_) {
      super(p_17444_, p_17445_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_17472_) {
      Map<String, Supplier<TypeTemplate>> $$1 = super.registerEntities(p_17472_);
      p_17472_.registerSimple($$1, "minecraft:egg");
      p_17472_.registerSimple($$1, "minecraft:ender_pearl");
      p_17472_.registerSimple($$1, "minecraft:fireball");
      p_17472_.register($$1, "minecraft:potion", p_17450_ -> DSL.optionalFields("Potion", References.ITEM_STACK.in(p_17472_)));
      p_17472_.registerSimple($$1, "minecraft:small_fireball");
      p_17472_.registerSimple($$1, "minecraft:snowball");
      p_17472_.registerSimple($$1, "minecraft:wither_skull");
      p_17472_.registerSimple($$1, "minecraft:xp_bottle");
      p_17472_.register($$1, "minecraft:arrow", () -> DSL.optionalFields("inBlockState", References.BLOCK_STATE.in(p_17472_)));
      p_17472_.register($$1, "minecraft:enderman", () -> DSL.optionalFields("carriedBlockState", References.BLOCK_STATE.in(p_17472_), V100.equipment(p_17472_)));
      p_17472_.register(
         $$1,
         "minecraft:falling_block",
         () -> DSL.optionalFields("BlockState", References.BLOCK_STATE.in(p_17472_), "TileEntityData", References.BLOCK_ENTITY.in(p_17472_))
      );
      p_17472_.register($$1, "minecraft:spectral_arrow", () -> DSL.optionalFields("inBlockState", References.BLOCK_STATE.in(p_17472_)));
      p_17472_.register(
         $$1,
         "minecraft:chest_minecart",
         () -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(p_17472_), "Items", DSL.list(References.ITEM_STACK.in(p_17472_)))
      );
      p_17472_.register($$1, "minecraft:commandblock_minecart", () -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(p_17472_)));
      p_17472_.register($$1, "minecraft:furnace_minecart", () -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(p_17472_)));
      p_17472_.register(
         $$1,
         "minecraft:hopper_minecart",
         () -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(p_17472_), "Items", DSL.list(References.ITEM_STACK.in(p_17472_)))
      );
      p_17472_.register($$1, "minecraft:minecart", () -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(p_17472_)));
      p_17472_.register(
         $$1,
         "minecraft:spawner_minecart",
         () -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(p_17472_), References.UNTAGGED_SPAWNER.in(p_17472_))
      );
      p_17472_.register($$1, "minecraft:tnt_minecart", () -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(p_17472_)));
      return $$1;
   }
}
