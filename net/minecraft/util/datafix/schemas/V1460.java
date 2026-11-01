package net.minecraft.util.datafix.schemas;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.types.templates.Hook.HookFunction;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V1460 extends NamespacedSchema {
   public V1460(int p_17553_, Schema p_17554_) {
      super(p_17553_, p_17554_);
   }

   protected static void registerMob(Schema p_17561_, Map<String, Supplier<TypeTemplate>> p_17562_, String p_17563_) {
      p_17561_.register(p_17562_, p_17563_, () -> V100.equipment(p_17561_));
   }

   protected static void registerInventory(Schema p_17576_, Map<String, Supplier<TypeTemplate>> p_17577_, String p_17578_) {
      p_17576_.register(p_17577_, p_17578_, () -> DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(p_17576_))));
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_17658_) {
      Map<String, Supplier<TypeTemplate>> $$1 = Maps.newHashMap();
      p_17658_.register($$1, "minecraft:area_effect_cloud", p_340685_ -> DSL.optionalFields("Particle", References.PARTICLE.in(p_17658_)));
      registerMob(p_17658_, $$1, "minecraft:armor_stand");
      p_17658_.register($$1, "minecraft:arrow", p_17677_ -> DSL.optionalFields("inBlockState", References.BLOCK_STATE.in(p_17658_)));
      registerMob(p_17658_, $$1, "minecraft:bat");
      registerMob(p_17658_, $$1, "minecraft:blaze");
      p_17658_.registerSimple($$1, "minecraft:boat");
      registerMob(p_17658_, $$1, "minecraft:cave_spider");
      p_17658_.register(
         $$1,
         "minecraft:chest_minecart",
         p_17680_ -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(p_17658_), "Items", DSL.list(References.ITEM_STACK.in(p_17658_)))
      );
      registerMob(p_17658_, $$1, "minecraft:chicken");
      p_17658_.register($$1, "minecraft:commandblock_minecart", p_17654_ -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(p_17658_)));
      registerMob(p_17658_, $$1, "minecraft:cow");
      registerMob(p_17658_, $$1, "minecraft:creeper");
      p_17658_.register(
         $$1,
         "minecraft:donkey",
         p_17674_ -> DSL.optionalFields(
            "Items", DSL.list(References.ITEM_STACK.in(p_17658_)), "SaddleItem", References.ITEM_STACK.in(p_17658_), V100.equipment(p_17658_)
         )
      );
      p_17658_.registerSimple($$1, "minecraft:dragon_fireball");
      p_17658_.registerSimple($$1, "minecraft:egg");
      registerMob(p_17658_, $$1, "minecraft:elder_guardian");
      p_17658_.registerSimple($$1, "minecraft:ender_crystal");
      registerMob(p_17658_, $$1, "minecraft:ender_dragon");
      p_17658_.register(
         $$1, "minecraft:enderman", p_17671_ -> DSL.optionalFields("carriedBlockState", References.BLOCK_STATE.in(p_17658_), V100.equipment(p_17658_))
      );
      registerMob(p_17658_, $$1, "minecraft:endermite");
      p_17658_.registerSimple($$1, "minecraft:ender_pearl");
      p_17658_.registerSimple($$1, "minecraft:evocation_fangs");
      registerMob(p_17658_, $$1, "minecraft:evocation_illager");
      p_17658_.registerSimple($$1, "minecraft:eye_of_ender_signal");
      p_17658_.register(
         $$1,
         "minecraft:falling_block",
         p_17668_ -> DSL.optionalFields("BlockState", References.BLOCK_STATE.in(p_17658_), "TileEntityData", References.BLOCK_ENTITY.in(p_17658_))
      );
      p_17658_.registerSimple($$1, "minecraft:fireball");
      p_17658_.register($$1, "minecraft:fireworks_rocket", p_17665_ -> DSL.optionalFields("FireworksItem", References.ITEM_STACK.in(p_17658_)));
      p_17658_.register($$1, "minecraft:furnace_minecart", p_17634_ -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(p_17658_)));
      registerMob(p_17658_, $$1, "minecraft:ghast");
      registerMob(p_17658_, $$1, "minecraft:giant");
      registerMob(p_17658_, $$1, "minecraft:guardian");
      p_17658_.register(
         $$1,
         "minecraft:hopper_minecart",
         p_17651_ -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(p_17658_), "Items", DSL.list(References.ITEM_STACK.in(p_17658_)))
      );
      p_17658_.register(
         $$1,
         "minecraft:horse",
         p_17648_ -> DSL.optionalFields(
            "ArmorItem", References.ITEM_STACK.in(p_17658_), "SaddleItem", References.ITEM_STACK.in(p_17658_), V100.equipment(p_17658_)
         )
      );
      registerMob(p_17658_, $$1, "minecraft:husk");
      p_17658_.registerSimple($$1, "minecraft:illusion_illager");
      p_17658_.register($$1, "minecraft:item", p_17645_ -> DSL.optionalFields("Item", References.ITEM_STACK.in(p_17658_)));
      p_17658_.register($$1, "minecraft:item_frame", p_17642_ -> DSL.optionalFields("Item", References.ITEM_STACK.in(p_17658_)));
      p_17658_.registerSimple($$1, "minecraft:leash_knot");
      p_17658_.register(
         $$1,
         "minecraft:llama",
         p_17639_ -> DSL.optionalFields(
            "Items",
            DSL.list(References.ITEM_STACK.in(p_17658_)),
            "SaddleItem",
            References.ITEM_STACK.in(p_17658_),
            "DecorItem",
            References.ITEM_STACK.in(p_17658_),
            V100.equipment(p_17658_)
         )
      );
      p_17658_.registerSimple($$1, "minecraft:llama_spit");
      registerMob(p_17658_, $$1, "minecraft:magma_cube");
      p_17658_.register($$1, "minecraft:minecart", p_17683_ -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(p_17658_)));
      registerMob(p_17658_, $$1, "minecraft:mooshroom");
      p_17658_.register(
         $$1,
         "minecraft:mule",
         p_17629_ -> DSL.optionalFields(
            "Items", DSL.list(References.ITEM_STACK.in(p_17658_)), "SaddleItem", References.ITEM_STACK.in(p_17658_), V100.equipment(p_17658_)
         )
      );
      registerMob(p_17658_, $$1, "minecraft:ocelot");
      p_17658_.registerSimple($$1, "minecraft:painting");
      p_17658_.registerSimple($$1, "minecraft:parrot");
      registerMob(p_17658_, $$1, "minecraft:pig");
      registerMob(p_17658_, $$1, "minecraft:polar_bear");
      p_17658_.register($$1, "minecraft:potion", p_17624_ -> DSL.optionalFields("Potion", References.ITEM_STACK.in(p_17658_)));
      registerMob(p_17658_, $$1, "minecraft:rabbit");
      registerMob(p_17658_, $$1, "minecraft:sheep");
      registerMob(p_17658_, $$1, "minecraft:shulker");
      p_17658_.registerSimple($$1, "minecraft:shulker_bullet");
      registerMob(p_17658_, $$1, "minecraft:silverfish");
      registerMob(p_17658_, $$1, "minecraft:skeleton");
      p_17658_.register(
         $$1, "minecraft:skeleton_horse", p_17619_ -> DSL.optionalFields("SaddleItem", References.ITEM_STACK.in(p_17658_), V100.equipment(p_17658_))
      );
      registerMob(p_17658_, $$1, "minecraft:slime");
      p_17658_.registerSimple($$1, "minecraft:small_fireball");
      p_17658_.registerSimple($$1, "minecraft:snowball");
      registerMob(p_17658_, $$1, "minecraft:snowman");
      p_17658_.register(
         $$1,
         "minecraft:spawner_minecart",
         p_17614_ -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(p_17658_), References.UNTAGGED_SPAWNER.in(p_17658_))
      );
      p_17658_.register($$1, "minecraft:spectral_arrow", p_17609_ -> DSL.optionalFields("inBlockState", References.BLOCK_STATE.in(p_17658_)));
      registerMob(p_17658_, $$1, "minecraft:spider");
      registerMob(p_17658_, $$1, "minecraft:squid");
      registerMob(p_17658_, $$1, "minecraft:stray");
      p_17658_.registerSimple($$1, "minecraft:tnt");
      p_17658_.register($$1, "minecraft:tnt_minecart", p_17604_ -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(p_17658_)));
      registerMob(p_17658_, $$1, "minecraft:vex");
      p_17658_.register(
         $$1,
         "minecraft:villager",
         p_340687_ -> DSL.optionalFields(
            "Inventory",
            DSL.list(References.ITEM_STACK.in(p_17658_)),
            "Offers",
            DSL.optionalFields("Recipes", DSL.list(References.VILLAGER_TRADE.in(p_17658_))),
            V100.equipment(p_17658_)
         )
      );
      registerMob(p_17658_, $$1, "minecraft:villager_golem");
      registerMob(p_17658_, $$1, "minecraft:vindication_illager");
      registerMob(p_17658_, $$1, "minecraft:witch");
      registerMob(p_17658_, $$1, "minecraft:wither");
      registerMob(p_17658_, $$1, "minecraft:wither_skeleton");
      p_17658_.registerSimple($$1, "minecraft:wither_skull");
      registerMob(p_17658_, $$1, "minecraft:wolf");
      p_17658_.registerSimple($$1, "minecraft:xp_bottle");
      p_17658_.registerSimple($$1, "minecraft:xp_orb");
      registerMob(p_17658_, $$1, "minecraft:zombie");
      p_17658_.register(
         $$1, "minecraft:zombie_horse", p_17592_ -> DSL.optionalFields("SaddleItem", References.ITEM_STACK.in(p_17658_), V100.equipment(p_17658_))
      );
      registerMob(p_17658_, $$1, "minecraft:zombie_pigman");
      p_17658_.register(
         $$1,
         "minecraft:zombie_villager",
         p_340683_ -> DSL.optionalFields("Offers", DSL.optionalFields("Recipes", DSL.list(References.VILLAGER_TRADE.in(p_17658_))), V100.equipment(p_17658_))
      );
      return $$1;
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema p_17656_) {
      Map<String, Supplier<TypeTemplate>> $$1 = Maps.newHashMap();
      registerInventory(p_17656_, $$1, "minecraft:furnace");
      registerInventory(p_17656_, $$1, "minecraft:chest");
      registerInventory(p_17656_, $$1, "minecraft:trapped_chest");
      p_17656_.registerSimple($$1, "minecraft:ender_chest");
      p_17656_.register($$1, "minecraft:jukebox", p_17586_ -> DSL.optionalFields("RecordItem", References.ITEM_STACK.in(p_17656_)));
      registerInventory(p_17656_, $$1, "minecraft:dispenser");
      registerInventory(p_17656_, $$1, "minecraft:dropper");
      p_17656_.registerSimple($$1, "minecraft:sign");
      p_17656_.register($$1, "minecraft:mob_spawner", p_17574_ -> References.UNTAGGED_SPAWNER.in(p_17656_));
      p_17656_.register($$1, "minecraft:piston", p_17559_ -> DSL.optionalFields("blockState", References.BLOCK_STATE.in(p_17656_)));
      registerInventory(p_17656_, $$1, "minecraft:brewing_stand");
      p_17656_.registerSimple($$1, "minecraft:enchanting_table");
      p_17656_.registerSimple($$1, "minecraft:end_portal");
      p_17656_.registerSimple($$1, "minecraft:beacon");
      p_17656_.registerSimple($$1, "minecraft:skull");
      p_17656_.registerSimple($$1, "minecraft:daylight_detector");
      registerInventory(p_17656_, $$1, "minecraft:hopper");
      p_17656_.registerSimple($$1, "minecraft:comparator");
      p_17656_.registerSimple($$1, "minecraft:banner");
      p_17656_.registerSimple($$1, "minecraft:structure_block");
      p_17656_.registerSimple($$1, "minecraft:end_gateway");
      p_17656_.registerSimple($$1, "minecraft:command_block");
      registerInventory(p_17656_, $$1, "minecraft:shulker_box");
      p_17656_.registerSimple($$1, "minecraft:bed");
      return $$1;
   }

   public void registerTypes(Schema p_17660_, Map<String, Supplier<TypeTemplate>> p_17661_, Map<String, Supplier<TypeTemplate>> p_17662_) {
      p_17660_.registerType(false, References.LEVEL, DSL::remainder);
      p_17660_.registerType(false, References.RECIPE, () -> DSL.constType(namespacedString()));
      p_17660_.registerType(
         false,
         References.PLAYER,
         () -> DSL.optionalFields(
            new Pair[]{
               Pair.of("RootVehicle", DSL.optionalFields("Entity", References.ENTITY_TREE.in(p_17660_))),
               Pair.of("Inventory", DSL.list(References.ITEM_STACK.in(p_17660_))),
               Pair.of("EnderItems", DSL.list(References.ITEM_STACK.in(p_17660_))),
               Pair.of("ShoulderEntityLeft", References.ENTITY_TREE.in(p_17660_)),
               Pair.of("ShoulderEntityRight", References.ENTITY_TREE.in(p_17660_)),
               Pair.of(
                  "recipeBook",
                  DSL.optionalFields("recipes", DSL.list(References.RECIPE.in(p_17660_)), "toBeDisplayed", DSL.list(References.RECIPE.in(p_17660_)))
               )
            }
         )
      );
      p_17660_.registerType(
         false,
         References.CHUNK,
         () -> DSL.fields(
            "Level",
            DSL.optionalFields(
               "Entities",
               DSL.list(References.ENTITY_TREE.in(p_17660_)),
               "TileEntities",
               DSL.list(DSL.or(References.BLOCK_ENTITY.in(p_17660_), DSL.remainder())),
               "TileTicks",
               DSL.list(DSL.fields("i", References.BLOCK_NAME.in(p_17660_))),
               "Sections",
               DSL.list(DSL.optionalFields("Palette", DSL.list(References.BLOCK_STATE.in(p_17660_))))
            )
         )
      );
      p_17660_.registerType(
         true,
         References.BLOCK_ENTITY,
         () -> DSL.optionalFields("components", References.DATA_COMPONENTS.in(p_17660_), DSL.taggedChoiceLazy("id", namespacedString(), p_17662_))
      );
      p_17660_.registerType(
         true, References.ENTITY_TREE, () -> DSL.optionalFields("Passengers", DSL.list(References.ENTITY_TREE.in(p_17660_)), References.ENTITY.in(p_17660_))
      );
      p_17660_.registerType(true, References.ENTITY, () -> DSL.taggedChoiceLazy("id", namespacedString(), p_17661_));
      p_17660_.registerType(
         true,
         References.ITEM_STACK,
         () -> DSL.hook(
            DSL.optionalFields(
               "id",
               References.ITEM_NAME.in(p_17660_),
               "tag",
               DSL.optionalFields(
                  new Pair[]{
                     Pair.of("EntityTag", References.ENTITY_TREE.in(p_17660_)),
                     Pair.of("BlockEntityTag", References.BLOCK_ENTITY.in(p_17660_)),
                     Pair.of("CanDestroy", DSL.list(References.BLOCK_NAME.in(p_17660_))),
                     Pair.of("CanPlaceOn", DSL.list(References.BLOCK_NAME.in(p_17660_))),
                     Pair.of("Items", DSL.list(References.ITEM_STACK.in(p_17660_))),
                     Pair.of("ChargedProjectiles", DSL.list(References.ITEM_STACK.in(p_17660_)))
                  }
               )
            ),
            V705.ADD_NAMES,
            HookFunction.IDENTITY
         )
      );
      p_17660_.registerType(false, References.HOTBAR, () -> DSL.compoundList(DSL.list(References.ITEM_STACK.in(p_17660_))));
      p_17660_.registerType(false, References.OPTIONS, DSL::remainder);
      p_17660_.registerType(
         false,
         References.STRUCTURE,
         () -> DSL.optionalFields(
            "entities",
            DSL.list(DSL.optionalFields("nbt", References.ENTITY_TREE.in(p_17660_))),
            "blocks",
            DSL.list(DSL.optionalFields("nbt", References.BLOCK_ENTITY.in(p_17660_))),
            "palette",
            DSL.list(References.BLOCK_STATE.in(p_17660_))
         )
      );
      p_17660_.registerType(false, References.BLOCK_NAME, () -> DSL.constType(namespacedString()));
      p_17660_.registerType(false, References.ITEM_NAME, () -> DSL.constType(namespacedString()));
      p_17660_.registerType(false, References.BLOCK_STATE, DSL::remainder);
      p_17660_.registerType(false, References.FLAT_BLOCK_STATE, DSL::remainder);
      Supplier<TypeTemplate> $$3 = () -> DSL.compoundList(References.ITEM_NAME.in(p_17660_), DSL.constType(DSL.intType()));
      p_17660_.registerType(
         false,
         References.STATS,
         () -> DSL.optionalFields(
            "stats",
            DSL.optionalFields(
               new Pair[]{
                  Pair.of("minecraft:mined", DSL.compoundList(References.BLOCK_NAME.in(p_17660_), DSL.constType(DSL.intType()))),
                  Pair.of("minecraft:crafted", $$3.get()),
                  Pair.of("minecraft:used", $$3.get()),
                  Pair.of("minecraft:broken", $$3.get()),
                  Pair.of("minecraft:picked_up", $$3.get()),
                  Pair.of("minecraft:dropped", $$3.get()),
                  Pair.of("minecraft:killed", DSL.compoundList(References.ENTITY_NAME.in(p_17660_), DSL.constType(DSL.intType()))),
                  Pair.of("minecraft:killed_by", DSL.compoundList(References.ENTITY_NAME.in(p_17660_), DSL.constType(DSL.intType()))),
                  Pair.of("minecraft:custom", DSL.compoundList(DSL.constType(namespacedString()), DSL.constType(DSL.intType())))
               }
            )
         )
      );
      p_17660_.registerType(false, References.SAVED_DATA_COMMAND_STORAGE, DSL::remainder);
      p_17660_.registerType(false, References.SAVED_DATA_FORCED_CHUNKS, DSL::remainder);
      p_17660_.registerType(false, References.SAVED_DATA_MAP_DATA, DSL::remainder);
      p_17660_.registerType(false, References.SAVED_DATA_MAP_INDEX, DSL::remainder);
      p_17660_.registerType(false, References.SAVED_DATA_RAIDS, DSL::remainder);
      p_17660_.registerType(false, References.SAVED_DATA_RANDOM_SEQUENCES, DSL::remainder);
      p_17660_.registerType(
         false,
         References.SAVED_DATA_SCOREBOARD,
         () -> DSL.optionalFields(
            "data", DSL.optionalFields("Objectives", DSL.list(References.OBJECTIVE.in(p_17660_)), "Teams", DSL.list(References.TEAM.in(p_17660_)))
         )
      );
      p_17660_.registerType(
         false,
         References.SAVED_DATA_STRUCTURE_FEATURE_INDICES,
         () -> DSL.optionalFields("data", DSL.optionalFields("Features", DSL.compoundList(References.STRUCTURE_FEATURE.in(p_17660_))))
      );
      p_17660_.registerType(false, References.STRUCTURE_FEATURE, DSL::remainder);
      Map<String, Supplier<TypeTemplate>> $$4 = V1451_6.createCriterionTypes(p_17660_);
      p_17660_.registerType(
         false,
         References.OBJECTIVE,
         () -> DSL.hook(
            DSL.optionalFields("CriteriaType", DSL.taggedChoiceLazy("type", DSL.string(), $$4)), V1451_6.UNPACK_OBJECTIVE_ID, V1451_6.REPACK_OBJECTIVE_ID
         )
      );
      p_17660_.registerType(false, References.TEAM, DSL::remainder);
      p_17660_.registerType(
         true,
         References.UNTAGGED_SPAWNER,
         () -> DSL.optionalFields(
            "SpawnPotentials", DSL.list(DSL.fields("Entity", References.ENTITY_TREE.in(p_17660_))), "SpawnData", References.ENTITY_TREE.in(p_17660_)
         )
      );
      p_17660_.registerType(
         false,
         References.ADVANCEMENTS,
         () -> DSL.optionalFields(
            "minecraft:adventure/adventuring_time",
            DSL.optionalFields("criteria", DSL.compoundList(References.BIOME.in(p_17660_), DSL.constType(DSL.string()))),
            "minecraft:adventure/kill_a_mob",
            DSL.optionalFields("criteria", DSL.compoundList(References.ENTITY_NAME.in(p_17660_), DSL.constType(DSL.string()))),
            "minecraft:adventure/kill_all_mobs",
            DSL.optionalFields("criteria", DSL.compoundList(References.ENTITY_NAME.in(p_17660_), DSL.constType(DSL.string()))),
            "minecraft:husbandry/bred_all_animals",
            DSL.optionalFields("criteria", DSL.compoundList(References.ENTITY_NAME.in(p_17660_), DSL.constType(DSL.string())))
         )
      );
      p_17660_.registerType(false, References.BIOME, () -> DSL.constType(namespacedString()));
      p_17660_.registerType(false, References.ENTITY_NAME, () -> DSL.constType(namespacedString()));
      p_17660_.registerType(false, References.POI_CHUNK, DSL::remainder);
      p_17660_.registerType(false, References.WORLD_GEN_SETTINGS, DSL::remainder);
      p_17660_.registerType(false, References.ENTITY_CHUNK, () -> DSL.optionalFields("Entities", DSL.list(References.ENTITY_TREE.in(p_17660_))));
      p_17660_.registerType(true, References.DATA_COMPONENTS, DSL::remainder);
      p_17660_.registerType(
         true,
         References.VILLAGER_TRADE,
         () -> DSL.optionalFields(
            "buy", References.ITEM_STACK.in(p_17660_), "buyB", References.ITEM_STACK.in(p_17660_), "sell", References.ITEM_STACK.in(p_17660_)
         )
      );
      p_17660_.registerType(true, References.PARTICLE, () -> DSL.constType(DSL.string()));
   }
}
