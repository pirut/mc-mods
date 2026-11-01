package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V100 extends Schema {
   public V100(int p_17328_, Schema p_17329_) {
      super(p_17328_, p_17329_);
   }

   protected static TypeTemplate equipment(Schema p_17331_) {
      return DSL.optionalFields(
         "ArmorItems",
         DSL.list(References.ITEM_STACK.in(p_17331_)),
         "HandItems",
         DSL.list(References.ITEM_STACK.in(p_17331_)),
         "body_armor_item",
         References.ITEM_STACK.in(p_17331_)
      );
   }

   protected static void registerMob(Schema p_17336_, Map<String, Supplier<TypeTemplate>> p_17337_, String p_17338_) {
      p_17336_.register(p_17337_, p_17338_, () -> equipment(p_17336_));
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_17350_) {
      Map<String, Supplier<TypeTemplate>> $$1 = super.registerEntities(p_17350_);
      registerMob(p_17350_, $$1, "ArmorStand");
      registerMob(p_17350_, $$1, "Creeper");
      registerMob(p_17350_, $$1, "Skeleton");
      registerMob(p_17350_, $$1, "Spider");
      registerMob(p_17350_, $$1, "Giant");
      registerMob(p_17350_, $$1, "Zombie");
      registerMob(p_17350_, $$1, "Slime");
      registerMob(p_17350_, $$1, "Ghast");
      registerMob(p_17350_, $$1, "PigZombie");
      p_17350_.register($$1, "Enderman", p_17348_ -> DSL.optionalFields("carried", References.BLOCK_NAME.in(p_17350_), equipment(p_17350_)));
      registerMob(p_17350_, $$1, "CaveSpider");
      registerMob(p_17350_, $$1, "Silverfish");
      registerMob(p_17350_, $$1, "Blaze");
      registerMob(p_17350_, $$1, "LavaSlime");
      registerMob(p_17350_, $$1, "EnderDragon");
      registerMob(p_17350_, $$1, "WitherBoss");
      registerMob(p_17350_, $$1, "Bat");
      registerMob(p_17350_, $$1, "Witch");
      registerMob(p_17350_, $$1, "Endermite");
      registerMob(p_17350_, $$1, "Guardian");
      registerMob(p_17350_, $$1, "Pig");
      registerMob(p_17350_, $$1, "Sheep");
      registerMob(p_17350_, $$1, "Cow");
      registerMob(p_17350_, $$1, "Chicken");
      registerMob(p_17350_, $$1, "Squid");
      registerMob(p_17350_, $$1, "Wolf");
      registerMob(p_17350_, $$1, "MushroomCow");
      registerMob(p_17350_, $$1, "SnowMan");
      registerMob(p_17350_, $$1, "Ozelot");
      registerMob(p_17350_, $$1, "VillagerGolem");
      p_17350_.register(
         $$1,
         "EntityHorse",
         p_17343_ -> DSL.optionalFields(
            "Items",
            DSL.list(References.ITEM_STACK.in(p_17350_)),
            "ArmorItem",
            References.ITEM_STACK.in(p_17350_),
            "SaddleItem",
            References.ITEM_STACK.in(p_17350_),
            equipment(p_17350_)
         )
      );
      registerMob(p_17350_, $$1, "Rabbit");
      p_17350_.register(
         $$1,
         "Villager",
         p_340681_ -> DSL.optionalFields(
            "Inventory",
            DSL.list(References.ITEM_STACK.in(p_17350_)),
            "Offers",
            DSL.optionalFields("Recipes", DSL.list(References.VILLAGER_TRADE.in(p_17350_))),
            equipment(p_17350_)
         )
      );
      registerMob(p_17350_, $$1, "Shulker");
      p_17350_.register($$1, "AreaEffectCloud", p_340679_ -> DSL.optionalFields("Particle", References.PARTICLE.in(p_17350_)));
      p_17350_.registerSimple($$1, "ShulkerBullet");
      return $$1;
   }

   public void registerTypes(Schema p_17352_, Map<String, Supplier<TypeTemplate>> p_17353_, Map<String, Supplier<TypeTemplate>> p_17354_) {
      super.registerTypes(p_17352_, p_17353_, p_17354_);
      p_17352_.registerType(
         false,
         References.STRUCTURE,
         () -> DSL.optionalFields(
            "entities",
            DSL.list(DSL.optionalFields("nbt", References.ENTITY_TREE.in(p_17352_))),
            "blocks",
            DSL.list(DSL.optionalFields("nbt", References.BLOCK_ENTITY.in(p_17352_))),
            "palette",
            DSL.list(References.BLOCK_STATE.in(p_17352_))
         )
      );
      p_17352_.registerType(false, References.BLOCK_STATE, DSL::remainder);
      p_17352_.registerType(false, References.FLAT_BLOCK_STATE, DSL::remainder);
   }
}
