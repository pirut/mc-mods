package net.minecraft.server.commands;

import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.datafixers.util.Pair;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.item.armortrim.TrimPattern;
import net.minecraft.world.item.armortrim.TrimPatterns;
import net.minecraft.world.level.Level;

public class SpawnArmorTrimsCommand {
   private static final Map<Pair<Holder<ArmorMaterial>, EquipmentSlot>, Item> MATERIAL_AND_SLOT_TO_ITEM = Util.make(Maps.newHashMap(), p_266706_ -> {
      p_266706_.put(Pair.of(ArmorMaterials.CHAIN, EquipmentSlot.HEAD), Items.CHAINMAIL_HELMET);
      p_266706_.put(Pair.of(ArmorMaterials.CHAIN, EquipmentSlot.CHEST), Items.CHAINMAIL_CHESTPLATE);
      p_266706_.put(Pair.of(ArmorMaterials.CHAIN, EquipmentSlot.LEGS), Items.CHAINMAIL_LEGGINGS);
      p_266706_.put(Pair.of(ArmorMaterials.CHAIN, EquipmentSlot.FEET), Items.CHAINMAIL_BOOTS);
      p_266706_.put(Pair.of(ArmorMaterials.IRON, EquipmentSlot.HEAD), Items.IRON_HELMET);
      p_266706_.put(Pair.of(ArmorMaterials.IRON, EquipmentSlot.CHEST), Items.IRON_CHESTPLATE);
      p_266706_.put(Pair.of(ArmorMaterials.IRON, EquipmentSlot.LEGS), Items.IRON_LEGGINGS);
      p_266706_.put(Pair.of(ArmorMaterials.IRON, EquipmentSlot.FEET), Items.IRON_BOOTS);
      p_266706_.put(Pair.of(ArmorMaterials.GOLD, EquipmentSlot.HEAD), Items.GOLDEN_HELMET);
      p_266706_.put(Pair.of(ArmorMaterials.GOLD, EquipmentSlot.CHEST), Items.GOLDEN_CHESTPLATE);
      p_266706_.put(Pair.of(ArmorMaterials.GOLD, EquipmentSlot.LEGS), Items.GOLDEN_LEGGINGS);
      p_266706_.put(Pair.of(ArmorMaterials.GOLD, EquipmentSlot.FEET), Items.GOLDEN_BOOTS);
      p_266706_.put(Pair.of(ArmorMaterials.NETHERITE, EquipmentSlot.HEAD), Items.NETHERITE_HELMET);
      p_266706_.put(Pair.of(ArmorMaterials.NETHERITE, EquipmentSlot.CHEST), Items.NETHERITE_CHESTPLATE);
      p_266706_.put(Pair.of(ArmorMaterials.NETHERITE, EquipmentSlot.LEGS), Items.NETHERITE_LEGGINGS);
      p_266706_.put(Pair.of(ArmorMaterials.NETHERITE, EquipmentSlot.FEET), Items.NETHERITE_BOOTS);
      p_266706_.put(Pair.of(ArmorMaterials.DIAMOND, EquipmentSlot.HEAD), Items.DIAMOND_HELMET);
      p_266706_.put(Pair.of(ArmorMaterials.DIAMOND, EquipmentSlot.CHEST), Items.DIAMOND_CHESTPLATE);
      p_266706_.put(Pair.of(ArmorMaterials.DIAMOND, EquipmentSlot.LEGS), Items.DIAMOND_LEGGINGS);
      p_266706_.put(Pair.of(ArmorMaterials.DIAMOND, EquipmentSlot.FEET), Items.DIAMOND_BOOTS);
      p_266706_.put(Pair.of(ArmorMaterials.TURTLE, EquipmentSlot.HEAD), Items.TURTLE_HELMET);
   });
   private static final List<ResourceKey<TrimPattern>> VANILLA_TRIM_PATTERNS = List.of(
      TrimPatterns.SENTRY,
      TrimPatterns.DUNE,
      TrimPatterns.COAST,
      TrimPatterns.WILD,
      TrimPatterns.WARD,
      TrimPatterns.EYE,
      TrimPatterns.VEX,
      TrimPatterns.TIDE,
      TrimPatterns.SNOUT,
      TrimPatterns.RIB,
      TrimPatterns.SPIRE,
      TrimPatterns.WAYFINDER,
      TrimPatterns.SHAPER,
      TrimPatterns.SILENCE,
      TrimPatterns.RAISER,
      TrimPatterns.HOST,
      TrimPatterns.FLOW,
      TrimPatterns.BOLT
   );
   private static final List<ResourceKey<TrimMaterial>> VANILLA_TRIM_MATERIALS = List.of(
      TrimMaterials.QUARTZ,
      TrimMaterials.IRON,
      TrimMaterials.NETHERITE,
      TrimMaterials.REDSTONE,
      TrimMaterials.COPPER,
      TrimMaterials.GOLD,
      TrimMaterials.EMERALD,
      TrimMaterials.DIAMOND,
      TrimMaterials.LAPIS,
      TrimMaterials.AMETHYST
   );
   private static final ToIntFunction<ResourceKey<TrimPattern>> TRIM_PATTERN_ORDER = Util.createIndexLookup(VANILLA_TRIM_PATTERNS);
   private static final ToIntFunction<ResourceKey<TrimMaterial>> TRIM_MATERIAL_ORDER = Util.createIndexLookup(VANILLA_TRIM_MATERIALS);

   public static void register(CommandDispatcher<CommandSourceStack> p_266758_) {
      p_266758_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("spawn_armor_trims").requires(p_277270_ -> p_277270_.hasPermission(2)))
            .executes(
               p_267005_ -> spawnArmorTrims((CommandSourceStack)p_267005_.getSource(), ((CommandSourceStack)p_267005_.getSource()).getPlayerOrException())
            )
      );
   }

   private static int spawnArmorTrims(CommandSourceStack p_266993_, Player p_266983_) {
      Level $$2 = p_266983_.level();
      NonNullList<ArmorTrim> $$3 = NonNullList.create();
      Registry<TrimPattern> $$4 = $$2.registryAccess().registryOrThrow(Registries.TRIM_PATTERN);
      Registry<TrimMaterial> $$5 = $$2.registryAccess().registryOrThrow(Registries.TRIM_MATERIAL);
      $$4.stream()
         .sorted(Comparator.comparing(p_266941_ -> TRIM_PATTERN_ORDER.applyAsInt($$4.getResourceKey(p_266941_).orElse(null))))
         .forEachOrdered(
            p_266759_ -> $$5.stream()
               .sorted(Comparator.comparing(p_267239_ -> TRIM_MATERIAL_ORDER.applyAsInt($$5.getResourceKey(p_267239_).orElse(null))))
               .forEachOrdered(p_267162_ -> $$3.add(new ArmorTrim($$5.wrapAsHolder(p_267162_), $$4.wrapAsHolder(p_266759_))))
         );
      BlockPos $$6 = p_266983_.blockPosition().relative(p_266983_.getDirection(), 5);
      Registry<ArmorMaterial> $$7 = p_266993_.registryAccess().registryOrThrow(Registries.ARMOR_MATERIAL);
      int $$8 = $$7.size() - 1;
      double $$9 = 3.0;
      int $$10 = 0;
      int $$11 = 0;

      for (ArmorTrim $$12 : $$3) {
         for (ArmorMaterial $$13 : $$7) {
            if ($$13 != ArmorMaterials.LEATHER.value()) {
               double $$14 = $$6.getX() + 0.5 - $$10 % $$5.size() * 3.0;
               double $$15 = $$6.getY() + 0.5 + $$11 % $$8 * 3.0;
               double $$16 = $$6.getZ() + 0.5 + $$10 / $$5.size() * 10;
               ArmorStand $$17 = new ArmorStand($$2, $$14, $$15, $$16);
               $$17.setYRot(180.0F);
               $$17.setNoGravity(true);

               for (EquipmentSlot $$18 : EquipmentSlot.values()) {
                  Item $$19 = MATERIAL_AND_SLOT_TO_ITEM.get(Pair.of($$13, $$18));
                  if ($$19 != null) {
                     ItemStack $$20 = new ItemStack($$19);
                     $$20.set(DataComponents.TRIM, $$12);
                     $$17.setItemSlot($$18, $$20);
                     if ($$19 instanceof ArmorItem $$21 && $$21.getMaterial().is(ArmorMaterials.TURTLE)) {
                        $$17.setCustomName(
                           $$12.pattern().value().copyWithStyle($$12.material()).copy().append(" ").append($$12.material().value().description())
                        );
                        $$17.setCustomNameVisible(true);
                     } else {
                        $$17.setInvisible(true);
                     }
                  }
               }

               $$2.addFreshEntity($$17);
               $$11++;
            }
         }

         $$10++;
      }

      p_266993_.sendSuccess(() -> Component.literal("Armorstands with trimmed armor spawned around you"), true);
      return 1;
   }
}
