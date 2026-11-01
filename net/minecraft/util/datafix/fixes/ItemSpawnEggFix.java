package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class ItemSpawnEggFix extends DataFix {
   private static final String[] ID_TO_ENTITY = (String[])DataFixUtils.make(new String[256], p_341874_ -> {
      p_341874_[1] = "Item";
      p_341874_[2] = "XPOrb";
      p_341874_[7] = "ThrownEgg";
      p_341874_[8] = "LeashKnot";
      p_341874_[9] = "Painting";
      p_341874_[10] = "Arrow";
      p_341874_[11] = "Snowball";
      p_341874_[12] = "Fireball";
      p_341874_[13] = "SmallFireball";
      p_341874_[14] = "ThrownEnderpearl";
      p_341874_[15] = "EyeOfEnderSignal";
      p_341874_[16] = "ThrownPotion";
      p_341874_[17] = "ThrownExpBottle";
      p_341874_[18] = "ItemFrame";
      p_341874_[19] = "WitherSkull";
      p_341874_[20] = "PrimedTnt";
      p_341874_[21] = "FallingSand";
      p_341874_[22] = "FireworksRocketEntity";
      p_341874_[23] = "TippedArrow";
      p_341874_[24] = "SpectralArrow";
      p_341874_[25] = "ShulkerBullet";
      p_341874_[26] = "DragonFireball";
      p_341874_[30] = "ArmorStand";
      p_341874_[41] = "Boat";
      p_341874_[42] = "MinecartRideable";
      p_341874_[43] = "MinecartChest";
      p_341874_[44] = "MinecartFurnace";
      p_341874_[45] = "MinecartTNT";
      p_341874_[46] = "MinecartHopper";
      p_341874_[47] = "MinecartSpawner";
      p_341874_[40] = "MinecartCommandBlock";
      p_341874_[50] = "Creeper";
      p_341874_[51] = "Skeleton";
      p_341874_[52] = "Spider";
      p_341874_[53] = "Giant";
      p_341874_[54] = "Zombie";
      p_341874_[55] = "Slime";
      p_341874_[56] = "Ghast";
      p_341874_[57] = "PigZombie";
      p_341874_[58] = "Enderman";
      p_341874_[59] = "CaveSpider";
      p_341874_[60] = "Silverfish";
      p_341874_[61] = "Blaze";
      p_341874_[62] = "LavaSlime";
      p_341874_[63] = "EnderDragon";
      p_341874_[64] = "WitherBoss";
      p_341874_[65] = "Bat";
      p_341874_[66] = "Witch";
      p_341874_[67] = "Endermite";
      p_341874_[68] = "Guardian";
      p_341874_[69] = "Shulker";
      p_341874_[90] = "Pig";
      p_341874_[91] = "Sheep";
      p_341874_[92] = "Cow";
      p_341874_[93] = "Chicken";
      p_341874_[94] = "Squid";
      p_341874_[95] = "Wolf";
      p_341874_[96] = "MushroomCow";
      p_341874_[97] = "SnowMan";
      p_341874_[98] = "Ozelot";
      p_341874_[99] = "VillagerGolem";
      p_341874_[100] = "EntityHorse";
      p_341874_[101] = "Rabbit";
      p_341874_[120] = "Villager";
      p_341874_[200] = "EnderCrystal";
   });

   public ItemSpawnEggFix(Schema p_16034_, boolean p_16035_) {
      super(p_16034_, p_16035_);
   }

   public TypeRewriteRule makeRule() {
      Schema $$0 = this.getInputSchema();
      Type<?> $$1 = $$0.getType(References.ITEM_STACK);
      OpticFinder<Pair<String, String>> $$2 = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
      OpticFinder<String> $$3 = DSL.fieldFinder("id", DSL.string());
      OpticFinder<?> $$4 = $$1.findField("tag");
      OpticFinder<?> $$5 = $$4.type().findField("EntityTag");
      OpticFinder<?> $$6 = DSL.typeFinder($$0.getTypeRaw(References.ENTITY));
      Type<?> $$7 = this.getOutputSchema().getTypeRaw(References.ENTITY);
      return this.fixTypeEverywhereTyped("ItemSpawnEggFix", $$1, p_311557_ -> {
         Optional<Pair<String, String>> $$7x = p_311557_.getOptional($$2);
         if ($$7x.isPresent() && Objects.equals($$7x.get().getSecond(), "minecraft:spawn_egg")) {
            Dynamic<?> $$8 = (Dynamic<?>)p_311557_.get(DSL.remainderFinder());
            short $$9 = $$8.get("Damage").asShort((short)0);
            Optional<? extends Typed<?>> $$10 = p_311557_.getOptionalTyped($$4);
            Optional<? extends Typed<?>> $$11 = $$10.flatMap(p_145417_ -> p_145417_.getOptionalTyped($$5));
            Optional<? extends Typed<?>> $$12 = $$11.flatMap(p_145414_ -> p_145414_.getOptionalTyped($$6));
            Optional<String> $$13 = $$12.flatMap(p_145406_ -> p_145406_.getOptional($$3));
            Typed<?> $$14 = p_311557_;
            String $$15 = ID_TO_ENTITY[$$9 & 255];
            if ($$15 != null && ($$13.isEmpty() || !Objects.equals($$13.get(), $$15))) {
               Typed<?> $$16 = p_311557_.getOrCreateTyped($$4);
               Typed<?> $$17 = $$16.getOrCreateTyped($$5);
               Typed<?> $$18 = $$17.getOrCreateTyped($$6);
               Typed<?> $$20 = Util.writeAndReadTypedOrThrow($$18, $$7, p_311560_ -> p_311560_.set("id", $$8.createString($$15)));
               $$14 = p_311557_.set($$4, $$16.set($$5, $$17.set($$6, $$20)));
            }

            if ($$9 != 0) {
               $$8 = $$8.set("Damage", $$8.createShort((short)0));
               $$14 = $$14.set(DSL.remainderFinder(), $$8);
            }

            return $$14;
         } else {
            return p_311557_;
         }
      });
   }
}
