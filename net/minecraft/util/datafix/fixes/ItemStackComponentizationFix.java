package net.minecraft.util.datafix.fixes;

import com.google.common.base.Splitter;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.OptionalDynamic;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.ComponentDataFixUtils;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class ItemStackComponentizationFix extends DataFix {
   private static final int HIDE_ENCHANTMENTS = 1;
   private static final int HIDE_MODIFIERS = 2;
   private static final int HIDE_UNBREAKABLE = 4;
   private static final int HIDE_CAN_DESTROY = 8;
   private static final int HIDE_CAN_PLACE = 16;
   private static final int HIDE_ADDITIONAL = 32;
   private static final int HIDE_DYE = 64;
   private static final int HIDE_UPGRADES = 128;
   private static final Set<String> POTION_HOLDER_IDS = Set.of(
      "minecraft:potion", "minecraft:splash_potion", "minecraft:lingering_potion", "minecraft:tipped_arrow"
   );
   private static final Set<String> BUCKETED_MOB_IDS = Set.of(
      "minecraft:pufferfish_bucket",
      "minecraft:salmon_bucket",
      "minecraft:cod_bucket",
      "minecraft:tropical_fish_bucket",
      "minecraft:axolotl_bucket",
      "minecraft:tadpole_bucket"
   );
   private static final List<String> BUCKETED_MOB_TAGS = List.of(
      "NoAI", "Silent", "NoGravity", "Glowing", "Invulnerable", "Health", "Age", "Variant", "HuntingCooldown", "BucketVariantTag"
   );
   private static final Set<String> BOOLEAN_BLOCK_STATE_PROPERTIES = Set.of(
      "attached",
      "bottom",
      "conditional",
      "disarmed",
      "drag",
      "enabled",
      "extended",
      "eye",
      "falling",
      "hanging",
      "has_bottle_0",
      "has_bottle_1",
      "has_bottle_2",
      "has_record",
      "has_book",
      "inverted",
      "in_wall",
      "lit",
      "locked",
      "occupied",
      "open",
      "persistent",
      "powered",
      "short",
      "signal_fire",
      "snowy",
      "triggered",
      "unstable",
      "waterlogged",
      "berries",
      "bloom",
      "shrieking",
      "can_summon",
      "up",
      "down",
      "north",
      "east",
      "south",
      "west",
      "slot_0_occupied",
      "slot_1_occupied",
      "slot_2_occupied",
      "slot_3_occupied",
      "slot_4_occupied",
      "slot_5_occupied",
      "cracked",
      "crafting"
   );
   private static final Splitter PROPERTY_SPLITTER = Splitter.on(',');

   public ItemStackComponentizationFix(Schema p_331673_) {
      super(p_331673_, true);
   }

   private static void fixItemStack(ItemStackComponentizationFix.ItemStackData p_332167_, Dynamic<?> p_330498_) {
      int $$2 = p_332167_.removeTag("HideFlags").asInt(0);
      p_332167_.moveTagToComponent("Damage", "minecraft:damage", p_330498_.createInt(0));
      p_332167_.moveTagToComponent("RepairCost", "minecraft:repair_cost", p_330498_.createInt(0));
      p_332167_.moveTagToComponent("CustomModelData", "minecraft:custom_model_data");
      p_332167_.removeTag("BlockStateTag")
         .result()
         .ifPresent(p_332594_ -> p_332167_.setComponent("minecraft:block_state", fixBlockStateTag((Dynamic<?>)p_332594_)));
      p_332167_.moveTagToComponent("EntityTag", "minecraft:entity_data");
      p_332167_.fixSubTag("BlockEntityTag", false, p_330655_ -> {
         String $$2x = NamespacedSchema.ensureNamespaced(p_330655_.get("id").asString(""));
         p_330655_ = fixBlockEntityTag(p_332167_, p_330655_, $$2x);
         Dynamic<?> $$3 = p_330655_.remove("id");
         return $$3.equals(p_330655_.emptyMap()) ? $$3 : p_330655_;
      });
      p_332167_.moveTagToComponent("BlockEntityTag", "minecraft:block_entity_data");
      if (p_332167_.removeTag("Unbreakable").asBoolean(false)) {
         Dynamic<?> $$3 = p_330498_.emptyMap();
         if (($$2 & 4) != 0) {
            $$3 = $$3.set("show_in_tooltip", p_330498_.createBoolean(false));
         }

         p_332167_.setComponent("minecraft:unbreakable", $$3);
      }

      fixEnchantments(p_332167_, p_330498_, "Enchantments", "minecraft:enchantments", ($$2 & 1) != 0);
      if (p_332167_.is("minecraft:enchanted_book")) {
         fixEnchantments(p_332167_, p_330498_, "StoredEnchantments", "minecraft:stored_enchantments", ($$2 & 32) != 0);
      }

      p_332167_.fixSubTag("display", false, p_331784_ -> fixDisplay(p_332167_, p_331784_, $$2));
      fixAdventureModeChecks(p_332167_, p_330498_, $$2);
      fixAttributeModifiers(p_332167_, p_330498_, $$2);
      Optional<? extends Dynamic<?>> $$4 = p_332167_.removeTag("Trim").result();
      if ($$4.isPresent()) {
         Dynamic<?> $$5 = (Dynamic<?>)$$4.get();
         if (($$2 & 128) != 0) {
            $$5 = $$5.set("show_in_tooltip", $$5.createBoolean(false));
         }

         p_332167_.setComponent("minecraft:trim", $$5);
      }

      if (($$2 & 32) != 0) {
         p_332167_.setComponent("minecraft:hide_additional_tooltip", p_330498_.emptyMap());
      }

      if (p_332167_.is("minecraft:crossbow")) {
         p_332167_.removeTag("Charged");
         p_332167_.moveTagToComponent("ChargedProjectiles", "minecraft:charged_projectiles", p_330498_.createList(Stream.empty()));
      }

      if (p_332167_.is("minecraft:bundle")) {
         p_332167_.moveTagToComponent("Items", "minecraft:bundle_contents", p_330498_.createList(Stream.empty()));
      }

      if (p_332167_.is("minecraft:filled_map")) {
         p_332167_.moveTagToComponent("map", "minecraft:map_id");
         Map<? extends Dynamic<?>, ? extends Dynamic<?>> $$6 = p_332167_.removeTag("Decorations")
            .asStream()
            .map(ItemStackComponentizationFix::fixMapDecoration)
            .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond, (p_332591_, p_339605_) -> p_332591_));
         if (!$$6.isEmpty()) {
            p_332167_.setComponent("minecraft:map_decorations", p_330498_.createMap($$6));
         }
      }

      if (p_332167_.is(POTION_HOLDER_IDS)) {
         fixPotionContents(p_332167_, p_330498_);
      }

      if (p_332167_.is("minecraft:writable_book")) {
         fixWritableBook(p_332167_, p_330498_);
      }

      if (p_332167_.is("minecraft:written_book")) {
         fixWrittenBook(p_332167_, p_330498_);
      }

      if (p_332167_.is("minecraft:suspicious_stew")) {
         p_332167_.moveTagToComponent("effects", "minecraft:suspicious_stew_effects");
      }

      if (p_332167_.is("minecraft:debug_stick")) {
         p_332167_.moveTagToComponent("DebugProperty", "minecraft:debug_stick_state");
      }

      if (p_332167_.is(BUCKETED_MOB_IDS)) {
         fixBucketedMobData(p_332167_, p_330498_);
      }

      if (p_332167_.is("minecraft:goat_horn")) {
         p_332167_.moveTagToComponent("instrument", "minecraft:instrument");
      }

      if (p_332167_.is("minecraft:knowledge_book")) {
         p_332167_.moveTagToComponent("Recipes", "minecraft:recipes");
      }

      if (p_332167_.is("minecraft:compass")) {
         fixLodestoneTracker(p_332167_, p_330498_);
      }

      if (p_332167_.is("minecraft:firework_rocket")) {
         fixFireworkRocket(p_332167_);
      }

      if (p_332167_.is("minecraft:firework_star")) {
         fixFireworkStar(p_332167_);
      }

      if (p_332167_.is("minecraft:player_head")) {
         p_332167_.removeTag("SkullOwner").result().ifPresent(p_330565_ -> p_332167_.setComponent("minecraft:profile", fixProfile((Dynamic<?>)p_330565_)));
      }
   }

   private static Dynamic<?> fixBlockStateTag(Dynamic<?> p_332829_) {
      return (Dynamic<?>)DataFixUtils.orElse(p_332829_.asMapOpt().result().map(p_339491_ -> p_339491_.collect(Collectors.toMap(Pair::getFirst, p_339490_ -> {
         String $$1 = ((Dynamic)p_339490_.getFirst()).asString("");
         Dynamic<?> $$2 = (Dynamic<?>)p_339490_.getSecond();
         if (BOOLEAN_BLOCK_STATE_PROPERTIES.contains($$1)) {
            Optional<Boolean> $$3 = $$2.asBoolean().result();
            if ($$3.isPresent()) {
               return $$2.createString(String.valueOf($$3.get()));
            }
         }

         Optional<Number> $$4 = $$2.asNumber().result();
         return $$4.isPresent() ? $$2.createString($$4.get().toString()) : $$2;
      }))).map(p_332829_::createMap), p_332829_);
   }

   private static Dynamic<?> fixDisplay(ItemStackComponentizationFix.ItemStackData p_331302_, Dynamic<?> p_331703_, int p_331793_) {
      p_331302_.setComponent("minecraft:custom_name", p_331703_.get("Name"));
      p_331302_.setComponent("minecraft:lore", p_331703_.get("Lore"));
      Optional<Integer> $$3 = p_331703_.get("color").asNumber().result().map(Number::intValue);
      boolean $$4 = (p_331793_ & 64) != 0;
      if ($$3.isPresent() || $$4) {
         Dynamic<?> $$5 = p_331703_.emptyMap().set("rgb", p_331703_.createInt($$3.orElse(10511680)));
         if ($$4) {
            $$5 = $$5.set("show_in_tooltip", p_331703_.createBoolean(false));
         }

         p_331302_.setComponent("minecraft:dyed_color", $$5);
      }

      Optional<String> $$6 = p_331703_.get("LocName").asString().result();
      if ($$6.isPresent()) {
         p_331302_.setComponent("minecraft:item_name", ComponentDataFixUtils.createTranslatableComponent(p_331703_.getOps(), $$6.get()));
      }

      if (p_331302_.is("minecraft:filled_map")) {
         p_331302_.setComponent("minecraft:map_color", p_331703_.get("MapColor"));
         p_331703_ = p_331703_.remove("MapColor");
      }

      return p_331703_.remove("Name").remove("Lore").remove("color").remove("LocName");
   }

   private static <T> Dynamic<T> fixBlockEntityTag(ItemStackComponentizationFix.ItemStackData p_330721_, Dynamic<T> p_331427_, String p_331421_) {
      p_330721_.setComponent("minecraft:lock", p_331427_.get("Lock"));
      p_331427_ = p_331427_.remove("Lock");
      Optional<Dynamic<T>> $$3 = p_331427_.get("LootTable").result();
      if ($$3.isPresent()) {
         Dynamic<T> $$4 = p_331427_.emptyMap().set("loot_table", $$3.get());
         long $$5 = p_331427_.get("LootTableSeed").asLong(0L);
         if ($$5 != 0L) {
            $$4 = $$4.set("seed", p_331427_.createLong($$5));
         }

         p_330721_.setComponent("minecraft:container_loot", $$4);
         p_331427_ = p_331427_.remove("LootTable").remove("LootTableSeed");
      }
      return switch (p_331421_) {
         case "minecraft:skull" -> {
            p_330721_.setComponent("minecraft:note_block_sound", p_331427_.get("note_block_sound"));
            yield p_331427_.remove("note_block_sound");
         }
         case "minecraft:decorated_pot" -> {
            p_330721_.setComponent("minecraft:pot_decorations", p_331427_.get("sherds"));
            Optional<Dynamic<T>> $$6 = p_331427_.get("item").result();
            if ($$6.isPresent()) {
               p_330721_.setComponent(
                  "minecraft:container", p_331427_.createList(Stream.of(p_331427_.emptyMap().set("slot", p_331427_.createInt(0)).set("item", $$6.get())))
               );
            }

            yield p_331427_.remove("sherds").remove("item");
         }
         case "minecraft:banner" -> {
            p_330721_.setComponent("minecraft:banner_patterns", p_331427_.get("patterns"));
            Optional<Number> $$7 = p_331427_.get("Base").asNumber().result();
            if ($$7.isPresent()) {
               p_330721_.setComponent("minecraft:base_color", p_331427_.createString(BannerPatternFormatFix.fixColor($$7.get().intValue())));
            }

            yield p_331427_.remove("patterns").remove("Base");
         }
         case "minecraft:shulker_box", "minecraft:chest", "minecraft:trapped_chest", "minecraft:furnace", "minecraft:ender_chest", "minecraft:dispenser", "minecraft:dropper", "minecraft:brewing_stand", "minecraft:hopper", "minecraft:barrel", "minecraft:smoker", "minecraft:blast_furnace", "minecraft:campfire", "minecraft:chiseled_bookshelf", "minecraft:crafter" -> {
            List<Dynamic<T>> $$8 = p_331427_.get("Items")
               .asList(
                  p_332590_ -> p_332590_.emptyMap()
                     .set("slot", p_332590_.createInt(p_332590_.get("Slot").asByte((byte)0) & 255))
                     .set("item", p_332590_.remove("Slot"))
               );
            if (!$$8.isEmpty()) {
               p_330721_.setComponent("minecraft:container", p_331427_.createList($$8.stream()));
            }

            yield p_331427_.remove("Items");
         }
         case "minecraft:beehive" -> {
            p_330721_.setComponent("minecraft:bees", p_331427_.get("bees"));
            yield p_331427_.remove("bees");
         }
         default -> p_331427_;
      };
   }

   private static void fixEnchantments(
      ItemStackComponentizationFix.ItemStackData p_331903_, Dynamic<?> p_330744_, String p_331654_, String p_331804_, boolean p_331955_
   ) {
      OptionalDynamic<?> $$5 = p_331903_.removeTag(p_331654_);
      List<Pair<String, Integer>> $$6 = $$5.asList(Function.identity())
         .stream()
         .flatMap(p_330659_ -> parseEnchantment((Dynamic<?>)p_330659_).stream())
         .toList();
      if (!$$6.isEmpty() || p_331955_) {
         Dynamic<?> $$7 = p_330744_.emptyMap();
         Dynamic<?> $$8 = p_330744_.emptyMap();

         for (Pair<String, Integer> $$9 : $$6) {
            $$8 = $$8.set((String)$$9.getFirst(), p_330744_.createInt((Integer)$$9.getSecond()));
         }

         $$7 = $$7.set("levels", $$8);
         if (p_331955_) {
            $$7 = $$7.set("show_in_tooltip", p_330744_.createBoolean(false));
         }

         p_331903_.setComponent(p_331804_, $$7);
      }

      if ($$5.result().isPresent() && $$6.isEmpty()) {
         p_331903_.setComponent("minecraft:enchantment_glint_override", p_330744_.createBoolean(true));
      }
   }

   private static Optional<Pair<String, Integer>> parseEnchantment(Dynamic<?> p_332205_) {
      return p_332205_.get("id")
         .asString()
         .apply2stable((p_331946_, p_330581_) -> Pair.of(p_331946_, Mth.clamp(p_330581_.intValue(), 0, 255)), p_332205_.get("lvl").asNumber())
         .result();
   }

   private static void fixAdventureModeChecks(ItemStackComponentizationFix.ItemStackData p_332034_, Dynamic<?> p_332091_, int p_331042_) {
      fixBlockStatePredicates(p_332034_, p_332091_, "CanDestroy", "minecraft:can_break", (p_331042_ & 8) != 0);
      fixBlockStatePredicates(p_332034_, p_332091_, "CanPlaceOn", "minecraft:can_place_on", (p_331042_ & 16) != 0);
   }

   private static void fixBlockStatePredicates(
      ItemStackComponentizationFix.ItemStackData p_331487_, Dynamic<?> p_331061_, String p_330321_, String p_332110_, boolean p_332166_
   ) {
      Optional<? extends Dynamic<?>> $$5 = p_331487_.removeTag(p_330321_).result();
      if (!$$5.isEmpty()) {
         Dynamic<?> $$6 = p_331061_.emptyMap()
            .set(
               "predicates",
               p_331061_.createList(
                  $$5.get()
                     .asStream()
                     .map(
                        p_337638_ -> (Dynamic)DataFixUtils.orElse(
                           p_337638_.asString().map(p_330959_ -> fixBlockStatePredicate((Dynamic<?>)p_337638_, p_330959_)).result(), p_337638_
                        )
                     )
               )
            );
         if (p_332166_) {
            $$6 = $$6.set("show_in_tooltip", p_331061_.createBoolean(false));
         }

         p_331487_.setComponent(p_332110_, $$6);
      }
   }

   private static Dynamic<?> fixBlockStatePredicate(Dynamic<?> p_331862_, String p_332123_) {
      int $$2 = p_332123_.indexOf(91);
      int $$3 = p_332123_.indexOf(123);
      int $$4 = p_332123_.length();
      if ($$2 != -1) {
         $$4 = $$2;
      }

      if ($$3 != -1) {
         $$4 = Math.min($$4, $$3);
      }

      String $$5 = p_332123_.substring(0, $$4);
      Dynamic<?> $$6 = p_331862_.emptyMap().set("blocks", p_331862_.createString($$5.trim()));
      int $$7 = p_332123_.indexOf(93);
      if ($$2 != -1 && $$7 != -1) {
         Dynamic<?> $$8 = p_331862_.emptyMap();

         for (String $$10 : PROPERTY_SPLITTER.split(p_332123_.substring($$2 + 1, $$7))) {
            int $$11 = $$10.indexOf(61);
            if ($$11 != -1) {
               String $$12 = $$10.substring(0, $$11).trim();
               String $$13 = $$10.substring($$11 + 1).trim();
               $$8 = $$8.set($$12, p_331862_.createString($$13));
            }
         }

         $$6 = $$6.set("state", $$8);
      }

      int $$14 = p_332123_.indexOf(125);
      if ($$3 != -1 && $$14 != -1) {
         $$6 = $$6.set("nbt", p_331862_.createString(p_332123_.substring($$3, $$14 + 1)));
      }

      return $$6;
   }

   private static void fixAttributeModifiers(ItemStackComponentizationFix.ItemStackData p_330771_, Dynamic<?> p_331387_, int p_330887_) {
      OptionalDynamic<?> $$3 = p_330771_.removeTag("AttributeModifiers");
      if (!$$3.result().isEmpty()) {
         boolean $$4 = (p_330887_ & 2) != 0;
         List<? extends Dynamic<?>> $$5 = $$3.asList(ItemStackComponentizationFix::fixAttributeModifier);
         Dynamic<?> $$6 = p_331387_.emptyMap().set("modifiers", p_331387_.createList($$5.stream()));
         if ($$4) {
            $$6 = $$6.set("show_in_tooltip", p_331387_.createBoolean(false));
         }

         p_330771_.setComponent("minecraft:attribute_modifiers", $$6);
      }
   }

   private static Dynamic<?> fixAttributeModifier(Dynamic<?> p_331035_) {
      Dynamic<?> $$1 = p_331035_.emptyMap()
         .set("name", p_331035_.createString(""))
         .set("amount", p_331035_.createDouble(0.0))
         .set("operation", p_331035_.createString("add_value"));
      $$1 = Dynamic.copyField(p_331035_, "AttributeName", $$1, "type");
      $$1 = Dynamic.copyField(p_331035_, "Slot", $$1, "slot");
      $$1 = Dynamic.copyField(p_331035_, "UUID", $$1, "uuid");
      $$1 = Dynamic.copyField(p_331035_, "Name", $$1, "name");
      $$1 = Dynamic.copyField(p_331035_, "Amount", $$1, "amount");
      return Dynamic.copyAndFixField(p_331035_, "Operation", $$1, "operation", p_330453_ -> {
         return p_330453_.createString(switch (p_330453_.asInt(0)) {
            case 1 -> "add_multiplied_base";
            case 2 -> "add_multiplied_total";
            default -> "add_value";
         });
      });
   }

   private static Pair<Dynamic<?>, Dynamic<?>> fixMapDecoration(Dynamic<?> p_332095_) {
      Dynamic<?> $$1 = (Dynamic<?>)DataFixUtils.orElseGet(p_332095_.get("id").result(), () -> p_332095_.createString(""));
      Dynamic<?> $$2 = p_332095_.emptyMap()
         .set("type", p_332095_.createString(fixMapDecorationType(p_332095_.get("type").asInt(0))))
         .set("x", p_332095_.createDouble(p_332095_.get("x").asDouble(0.0)))
         .set("z", p_332095_.createDouble(p_332095_.get("z").asDouble(0.0)))
         .set("rotation", p_332095_.createFloat((float)p_332095_.get("rot").asDouble(0.0)));
      return Pair.of($$1, $$2);
   }

   private static String fixMapDecorationType(int p_330596_) {
      return switch (p_330596_) {
         case 1 -> "frame";
         case 2 -> "red_marker";
         case 3 -> "blue_marker";
         case 4 -> "target_x";
         case 5 -> "target_point";
         case 6 -> "player_off_map";
         case 7 -> "player_off_limits";
         case 8 -> "mansion";
         case 9 -> "monument";
         case 10 -> "banner_white";
         case 11 -> "banner_orange";
         case 12 -> "banner_magenta";
         case 13 -> "banner_light_blue";
         case 14 -> "banner_yellow";
         case 15 -> "banner_lime";
         case 16 -> "banner_pink";
         case 17 -> "banner_gray";
         case 18 -> "banner_light_gray";
         case 19 -> "banner_cyan";
         case 20 -> "banner_purple";
         case 21 -> "banner_blue";
         case 22 -> "banner_brown";
         case 23 -> "banner_green";
         case 24 -> "banner_red";
         case 25 -> "banner_black";
         case 26 -> "red_x";
         case 27 -> "village_desert";
         case 28 -> "village_plains";
         case 29 -> "village_savanna";
         case 30 -> "village_snowy";
         case 31 -> "village_taiga";
         case 32 -> "jungle_temple";
         case 33 -> "swamp_hut";
         default -> "player";
      };
   }

   private static void fixPotionContents(ItemStackComponentizationFix.ItemStackData p_330477_, Dynamic<?> p_330723_) {
      Dynamic<?> $$2 = p_330723_.emptyMap();
      Optional<String> $$3 = p_330477_.removeTag("Potion").asString().result().filter(p_330426_ -> !p_330426_.equals("minecraft:empty"));
      if ($$3.isPresent()) {
         $$2 = $$2.set("potion", p_330723_.createString($$3.get()));
      }

      $$2 = p_330477_.moveTagInto("CustomPotionColor", $$2, "custom_color");
      $$2 = p_330477_.moveTagInto("custom_potion_effects", $$2, "custom_effects");
      if (!$$2.equals(p_330723_.emptyMap())) {
         p_330477_.setComponent("minecraft:potion_contents", $$2);
      }
   }

   private static void fixWritableBook(ItemStackComponentizationFix.ItemStackData p_330306_, Dynamic<?> p_331455_) {
      Dynamic<?> $$2 = fixBookPages(p_330306_, p_331455_);
      if ($$2 != null) {
         p_330306_.setComponent("minecraft:writable_book_content", p_331455_.emptyMap().set("pages", $$2));
      }
   }

   private static void fixWrittenBook(ItemStackComponentizationFix.ItemStackData p_330209_, Dynamic<?> p_332113_) {
      Dynamic<?> $$2 = fixBookPages(p_330209_, p_332113_);
      String $$3 = p_330209_.removeTag("title").asString("");
      Optional<String> $$4 = p_330209_.removeTag("filtered_title").asString().result();
      Dynamic<?> $$5 = p_332113_.emptyMap();
      $$5 = $$5.set("title", createFilteredText(p_332113_, $$3, $$4));
      $$5 = p_330209_.moveTagInto("author", $$5, "author");
      $$5 = p_330209_.moveTagInto("resolved", $$5, "resolved");
      $$5 = p_330209_.moveTagInto("generation", $$5, "generation");
      if ($$2 != null) {
         $$5 = $$5.set("pages", $$2);
      }

      p_330209_.setComponent("minecraft:written_book_content", $$5);
   }

   @Nullable
   private static Dynamic<?> fixBookPages(ItemStackComponentizationFix.ItemStackData p_332071_, Dynamic<?> p_330407_) {
      List<String> $$2 = p_332071_.removeTag("pages").asList(p_331677_ -> p_331677_.asString(""));
      Map<String, String> $$3 = p_332071_.removeTag("filtered_pages").asMap(p_332151_ -> p_332151_.asString("0"), p_330471_ -> p_330471_.asString(""));
      if ($$2.isEmpty()) {
         return null;
      } else {
         List<Dynamic<?>> $$4 = new ArrayList<>($$2.size());

         for (int $$5 = 0; $$5 < $$2.size(); $$5++) {
            String $$6 = $$2.get($$5);
            String $$7 = $$3.get(String.valueOf($$5));
            $$4.add(createFilteredText(p_330407_, $$6, Optional.ofNullable($$7)));
         }

         return p_330407_.createList($$4.stream());
      }
   }

   private static Dynamic<?> createFilteredText(Dynamic<?> p_331589_, String p_330423_, Optional<String> p_330385_) {
      Dynamic<?> $$3 = p_331589_.emptyMap().set("raw", p_331589_.createString(p_330423_));
      if (p_330385_.isPresent()) {
         $$3 = $$3.set("filtered", p_331589_.createString(p_330385_.get()));
      }

      return $$3;
   }

   private static void fixBucketedMobData(ItemStackComponentizationFix.ItemStackData p_331570_, Dynamic<?> p_330855_) {
      Dynamic<?> $$2 = p_330855_.emptyMap();

      for (String $$3 : BUCKETED_MOB_TAGS) {
         $$2 = p_331570_.moveTagInto($$3, $$2, $$3);
      }

      if (!$$2.equals(p_330855_.emptyMap())) {
         p_331570_.setComponent("minecraft:bucket_entity_data", $$2);
      }
   }

   private static void fixLodestoneTracker(ItemStackComponentizationFix.ItemStackData p_332824_, Dynamic<?> p_332755_) {
      Optional<? extends Dynamic<?>> $$2 = p_332824_.removeTag("LodestonePos").result();
      Optional<? extends Dynamic<?>> $$3 = p_332824_.removeTag("LodestoneDimension").result();
      if (!$$2.isEmpty() || !$$3.isEmpty()) {
         boolean $$4 = p_332824_.removeTag("LodestoneTracked").asBoolean(true);
         Dynamic<?> $$5 = p_332755_.emptyMap();
         if ($$2.isPresent() && $$3.isPresent()) {
            $$5 = $$5.set("target", p_332755_.emptyMap().set("pos", $$2.get()).set("dimension", $$3.get()));
         }

         if (!$$4) {
            $$5 = $$5.set("tracked", p_332755_.createBoolean(false));
         }

         p_332824_.setComponent("minecraft:lodestone_tracker", $$5);
      }
   }

   private static void fixFireworkStar(ItemStackComponentizationFix.ItemStackData p_330447_) {
      p_330447_.fixSubTag("Explosion", true, p_331995_ -> {
         p_330447_.setComponent("minecraft:firework_explosion", fixFireworkExplosion(p_331995_));
         return p_331995_.remove("Type").remove("Colors").remove("FadeColors").remove("Trail").remove("Flicker");
      });
   }

   private static void fixFireworkRocket(ItemStackComponentizationFix.ItemStackData p_330858_) {
      p_330858_.fixSubTag(
         "Fireworks",
         true,
         p_331577_ -> {
            Stream<? extends Dynamic<?>> $$2 = p_331577_.get("Explosions").asStream().map(ItemStackComponentizationFix::fixFireworkExplosion);
            int $$3 = p_331577_.get("Flight").asInt(0);
            p_330858_.setComponent(
               "minecraft:fireworks", p_331577_.emptyMap().set("explosions", p_331577_.createList($$2)).set("flight_duration", p_331577_.createByte((byte)$$3))
            );
            return p_331577_.remove("Explosions").remove("Flight");
         }
      );
   }

   private static Dynamic<?> fixFireworkExplosion(Dynamic<?> p_332063_) {
      p_332063_ = p_332063_.set("shape", p_332063_.createString(switch (p_332063_.get("Type").asInt(0)) {
         case 1 -> "large_ball";
         case 2 -> "star";
         case 3 -> "creeper";
         case 4 -> "burst";
         default -> "small_ball";
      })).remove("Type");
      p_332063_ = p_332063_.renameField("Colors", "colors");
      p_332063_ = p_332063_.renameField("FadeColors", "fade_colors");
      p_332063_ = p_332063_.renameField("Trail", "has_trail");
      return p_332063_.renameField("Flicker", "has_twinkle");
   }

   public static Dynamic<?> fixProfile(Dynamic<?> p_330375_) {
      Optional<String> $$1 = p_330375_.asString().result();
      if ($$1.isPresent()) {
         return isValidPlayerName($$1.get()) ? p_330375_.emptyMap().set("name", p_330375_.createString($$1.get())) : p_330375_.emptyMap();
      } else {
         String $$2 = p_330375_.get("Name").asString("");
         Optional<? extends Dynamic<?>> $$3 = p_330375_.get("Id").result();
         Dynamic<?> $$4 = fixProfileProperties(p_330375_.get("Properties"));
         Dynamic<?> $$5 = p_330375_.emptyMap();
         if (isValidPlayerName($$2)) {
            $$5 = $$5.set("name", p_330375_.createString($$2));
         }

         if ($$3.isPresent()) {
            $$5 = $$5.set("id", $$3.get());
         }

         if ($$4 != null) {
            $$5 = $$5.set("properties", $$4);
         }

         return $$5;
      }
   }

   private static boolean isValidPlayerName(String p_332666_) {
      return p_332666_.length() > 16 ? false : p_332666_.chars().filter(p_332597_ -> p_332597_ <= 32 || p_332597_ >= 127).findAny().isEmpty();
   }

   @Nullable
   private static Dynamic<?> fixProfileProperties(OptionalDynamic<?> p_330875_) {
      Map<String, List<Pair<String, Optional<String>>>> $$1 = p_330875_.asMap(p_331855_ -> p_331855_.asString(""), p_331384_ -> p_331384_.asList(p_337640_ -> {
         String $$1x = p_337640_.get("Value").asString("");
         Optional<String> $$2 = p_337640_.get("Signature").asString().result();
         return Pair.of($$1x, $$2);
      }));
      return $$1.isEmpty()
         ? null
         : p_330875_.createList(
            $$1.entrySet()
               .stream()
               .flatMap(
                  p_331925_ -> p_331925_.getValue()
                     .stream()
                     .map(
                        p_331949_ -> {
                           Dynamic<?> $$3 = p_330875_.emptyMap()
                              .set("name", p_330875_.createString(p_331925_.getKey()))
                              .set("value", p_330875_.createString((String)p_331949_.getFirst()));
                           Optional<String> $$4 = (Optional<String>)p_331949_.getSecond();
                           return $$4.isPresent() ? $$3.set("signature", p_330875_.createString($$4.get())) : $$3;
                        }
                     )
               )
         );
   }

   protected TypeRewriteRule makeRule() {
      return this.writeFixAndRead(
         "ItemStack componentization",
         this.getInputSchema().getType(References.ITEM_STACK),
         this.getOutputSchema().getType(References.ITEM_STACK),
         p_331180_ -> {
            Optional<? extends Dynamic<?>> $$1 = ItemStackComponentizationFix.ItemStackData.read(p_331180_).map(p_330696_ -> {
               fixItemStack(p_330696_, p_330696_.tag);
               return p_330696_.write();
            });
            return (Dynamic)DataFixUtils.orElse($$1, p_331180_);
         }
      );
   }

   static class ItemStackData {
      private final String item;
      private final int count;
      private Dynamic<?> components;
      private final Dynamic<?> remainder;
      Dynamic<?> tag;

      private ItemStackData(String p_330523_, int p_331147_, Dynamic<?> p_331630_) {
         this.item = NamespacedSchema.ensureNamespaced(p_330523_);
         this.count = p_331147_;
         this.components = p_331630_.emptyMap();
         this.tag = p_331630_.get("tag").orElseEmptyMap();
         this.remainder = p_331630_.remove("tag");
      }

      public static Optional<ItemStackComponentizationFix.ItemStackData> read(Dynamic<?> p_330273_) {
         return p_330273_.get("id")
            .asString()
            .apply2stable(
               (p_331191_, p_330701_) -> new ItemStackComponentizationFix.ItemStackData(p_331191_, p_330701_.intValue(), p_330273_.remove("id").remove("Count")),
               p_330273_.get("Count").asNumber()
            )
            .result();
      }

      public OptionalDynamic<?> removeTag(String p_330602_) {
         OptionalDynamic<?> $$1 = this.tag.get(p_330602_);
         this.tag = this.tag.remove(p_330602_);
         return $$1;
      }

      public void setComponent(String p_330861_, Dynamic<?> p_331399_) {
         this.components = this.components.set(p_330861_, p_331399_);
      }

      public void setComponent(String p_331000_, OptionalDynamic<?> p_332145_) {
         p_332145_.result().ifPresent(p_332105_ -> this.components = this.components.set(p_331000_, p_332105_));
      }

      public Dynamic<?> moveTagInto(String p_330240_, Dynamic<?> p_330972_, String p_331818_) {
         Optional<? extends Dynamic<?>> $$3 = this.removeTag(p_330240_).result();
         return $$3.isPresent() ? p_330972_.set(p_331818_, $$3.get()) : p_330972_;
      }

      public void moveTagToComponent(String p_331198_, String p_330485_, Dynamic<?> p_330981_) {
         Optional<? extends Dynamic<?>> $$3 = this.removeTag(p_331198_).result();
         if ($$3.isPresent() && !$$3.get().equals(p_330981_)) {
            this.setComponent(p_330485_, (Dynamic<?>)$$3.get());
         }
      }

      public void moveTagToComponent(String p_330961_, String p_330509_) {
         this.removeTag(p_330961_).result().ifPresent(p_330514_ -> this.setComponent(p_330509_, (Dynamic<?>)p_330514_));
      }

      public void fixSubTag(String p_330776_, boolean p_330566_, UnaryOperator<Dynamic<?>> p_330246_) {
         OptionalDynamic<?> $$3 = this.tag.get(p_330776_);
         if (!p_330566_ || !$$3.result().isEmpty()) {
            Dynamic<?> $$4 = $$3.orElseEmptyMap();
            $$4 = p_330246_.apply($$4);
            if ($$4.equals($$4.emptyMap())) {
               this.tag = this.tag.remove(p_330776_);
            } else {
               this.tag = this.tag.set(p_330776_, $$4);
            }
         }
      }

      public Dynamic<?> write() {
         Dynamic<?> $$0 = this.tag.emptyMap().set("id", this.tag.createString(this.item)).set("count", this.tag.createInt(this.count));
         if (!this.tag.equals(this.tag.emptyMap())) {
            this.components = this.components.set("minecraft:custom_data", this.tag);
         }

         if (!this.components.equals(this.tag.emptyMap())) {
            $$0 = $$0.set("components", this.components);
         }

         return mergeRemainder($$0, this.remainder);
      }

      private static <T> Dynamic<T> mergeRemainder(Dynamic<T> p_331175_, Dynamic<?> p_330435_) {
         DynamicOps<T> $$2 = p_331175_.getOps();
         return $$2.getMap(p_331175_.getValue())
            .flatMap(p_330670_ -> $$2.mergeToMap(p_330435_.convert($$2).getValue(), p_330670_))
            .map(p_331482_ -> new Dynamic($$2, p_331482_))
            .result()
            .orElse(p_331175_);
      }

      public boolean is(String p_330700_) {
         return this.item.equals(p_330700_);
      }

      public boolean is(Set<String> p_330784_) {
         return p_330784_.contains(this.item);
      }

      public boolean hasComponent(String p_332677_) {
         return this.components.get(p_332677_).result().isPresent();
      }
   }
}
