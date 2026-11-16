package io.github.pirut.bloodfeud.game;

import io.github.pirut.bloodfeud.BloodFeud;
import io.github.pirut.bloodfeud.feud.Brute;
import io.github.pirut.bloodfeud.feud.Family;
import io.github.pirut.bloodfeud.feud.Pack;
import io.github.pirut.bloodfeud.feud.Rank;
import io.github.pirut.bloodfeud.feud.Scar;
import io.github.pirut.bloodfeud.feud.Trait;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.ArrayList;
import java.util.List;

/**
 * Relics: the weapons Bosses, Kings and strong Brutes carry, dropped when they fall. Each is named after its owner,
 * enchanted past what an enchanting table can do, and shaped by the owner's traits and scars.
 */
public final class Relics {
    private Relics() {
    }

    /** Whether this Brute drops a relic when a player kills it. */
    public static boolean dropsRelic(Rank rank, Brute b) {
        return rank != Rank.BRUTE || b.level >= 10;
    }

    public static ItemStack make(ServerLevel level, Brute b, Rank rank, Pack pack) {
        boolean ranged = b.family == Family.BONES || b.entityType.equals("minecraft:pillager");
        boolean crossbow = b.entityType.equals("minecraft:pillager");
        boolean king = rank == Rank.KING;
        Item item = ranged ? (crossbow ? Items.CROSSBOW : Items.BOW)
                : king || b.level >= 14 ? (b.level % 2 == 0 ? Items.NETHERITE_AXE : Items.NETHERITE_SWORD)
                : (b.level % 2 == 0 ? Items.DIAMOND_AXE : Items.DIAMOND_SWORD);
        ItemStack s = new ItemStack(item);
        // Power: Brute 4-5, Boss 6, King 8, plus a little for its level.
        int power = (king ? 8 : rank == Rank.BOSS ? 6 : 4) + Math.min(2, b.level / 10);
        if (ranged) {
            enchant(level, s, crossbow ? Enchantments.QUICK_CHARGE : Enchantments.POWER, crossbow ? 3 : power);
            if (crossbow) enchant(level, s, Enchantments.MULTISHOT, 1);
            else enchant(level, s, Enchantments.INFINITY, 1);
            if (b.traits.contains(Trait.BONEBREAKER) || b.traits.contains(Trait.UNSHAKABLE)) {
                enchant(level, s, crossbow ? Enchantments.PIERCING : Enchantments.PUNCH, crossbow ? 4 : 3);
            }
        } else {
            enchant(level, s, Enchantments.SHARPNESS, power);
            enchant(level, s, Enchantments.SWEEPING_EDGE, 3);
            if (b.traits.contains(Trait.BONEBREAKER) || b.traits.contains(Trait.UNSHAKABLE)) enchant(level, s, Enchantments.KNOCKBACK, 3);
            if (b.traits.contains(Trait.THIEF)) enchant(level, s, Enchantments.LOOTING, 4);
            if (b.traits.contains(Trait.SMITE_WEAK)) enchant(level, s, Enchantments.SMITE, 6);
        }
        if (b.traits.contains(Trait.FIREPROOF) || b.scars.contains(Scar.BURNED)) {
            enchant(level, s, ranged ? Enchantments.FLAME : Enchantments.FIRE_ASPECT, ranged ? 1 : 3);
        }
        enchant(level, s, Enchantments.UNBREAKING, 5);
        enchant(level, s, Enchantments.MENDING, 1);
        if (!ranged) {
            // Hits harder and faster than any weapon a player can make: more for Kings and Berserkers.
            double damage = (king ? 4 : rank == Rank.BOSS ? 2.5 : 1.5) + (b.traits.contains(Trait.BERSERKER) ? 2 : 0);
            ItemAttributeModifiers mods = s.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
                    .withModifierAdded(Attributes.ATTACK_DAMAGE, new AttributeModifier(BloodFeud.id("relic_damage"), damage,
                            AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                    .withModifierAdded(Attributes.ATTACK_SPEED, new AttributeModifier(BloodFeud.id("relic_speed"), 0.15,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.MAINHAND);
            s.set(DataComponents.ATTRIBUTE_MODIFIERS, mods);
        }
        if (b.traits.contains(Trait.BLINK) || b.traits.contains(Trait.NIGHT_STALKER)) {
            ItemAttributeModifiers mods = s.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
                    .withModifierAdded(Attributes.MOVEMENT_SPEED, new AttributeModifier(BloodFeud.id("relic_swift"), 0.15,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND);
            s.set(DataComponents.ATTRIBUTE_MODIFIERS, mods);
        }
        if (b.traits.contains(Trait.REGENERATING) || b.traits.contains(Trait.SHIELDBEARER)) {
            ItemAttributeModifiers mods = s.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
                    .withModifierAdded(Attributes.MAX_HEALTH, new AttributeModifier(BloodFeud.id("relic_hearty"), king ? 8 : 4,
                            AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
            s.set(DataComponents.ATTRIBUTE_MODIFIERS, mods);
        }
        String kind = ranged ? (crossbow ? "Crossbow" : "Bow") : item == Items.NETHERITE_AXE || item == Items.DIAMOND_AXE ? "Cleaver" : "Blade";
        String title = b.name + "'s " + (king ? "Crown " : "") + kind;
        s.set(DataComponents.CUSTOM_NAME, Component.literal(title).withStyle(king ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GOLD));
        s.set(DataComponents.RARITY, Rarity.EPIC);
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("Taken from " + b.fullName() + ",").withStyle(ChatFormatting.GRAY));
        lore.add(Component.literal("level " + b.level + " " + rank.title + (pack != null ? " of " + pack.title() : "") + ".")
                .withStyle(ChatFormatting.GRAY));
        for (Trait t : b.traits) {
            if (t.kind == Trait.Kind.STRENGTH || t.kind == Trait.Kind.IMMUNITY) {
                lore.add(Component.literal("Remembers: " + t.title + (t == Trait.VENOM_BLADE ? " (poisons)" : t == Trait.BONEBREAKER ? " (slows)" : ""))
                        .withStyle(ChatFormatting.DARK_PURPLE));
            }
        }
        s.set(DataComponents.LORE, new ItemLore(lore));
        CompoundTag tag = new CompoundTag();
        ListTag kept = new ListTag();
        for (Trait t : b.traits) if (t == Trait.VENOM_BLADE || t == Trait.BONEBREAKER) kept.add(StringTag.valueOf(t.key()));
        tag.put(TRAITS, kept);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return s;
    }

    private static final String TRAITS = "bloodfeud_relic_traits";

    /** A relic that remembers Venom Blade poisons what it hits; one that remembers Bonebreaker slows it. */
    public static void onHit(ItemStack weapon, LivingEntity target, LivingEntity holder) {
        CustomData data = weapon.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.contains(TRAITS)) return;
        for (Tag t : data.copyTag().getList(TRAITS, Tag.TAG_STRING)) {
            Trait trait = Trait.byKey(t.getAsString());
            if (trait == Trait.VENOM_BLADE) target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1), holder);
            if (trait == Trait.BONEBREAKER) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1), holder);
        }
    }

    private static void enchant(ServerLevel level, ItemStack s, ResourceKey<Enchantment> key, int lvl) {
        Holder<Enchantment> h = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(key).orElse(null);
        if (h != null) s.enchant(h, lvl);
    }
}
