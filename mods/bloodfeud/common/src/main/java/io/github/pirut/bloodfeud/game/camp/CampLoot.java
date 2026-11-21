package io.github.pirut.bloodfeud.game.camp;

import io.github.pirut.bloodfeud.feud.Family;
import io.github.pirut.bloodfeud.feud.SettlementTier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Deterministic, balanced, tier-scaled and family-themed loot for pack settlement chests. */
public final class CampLoot {
    private CampLoot() {
    }

    public static SettlementTier tierForChest(CampBlueprint.Material mat) {
        return switch (mat) {
            case CHEST_TOWN -> SettlementTier.TOWN;
            case CHEST_STRONGHOLD -> SettlementTier.STRONGHOLD;
            case CHEST_OUTPOST -> SettlementTier.OUTPOST;
            default -> SettlementTier.CAMP;
        };
    }

    public static Component chestTitle(String packTitle, SettlementTier tier) {
        String label = switch (tier) {
            case TOWN -> "King's Treasury";
            case STRONGHOLD -> "Stronghold Stash";
            case OUTPOST -> "Boss Stash";
            default -> "Camp Stash";
        };
        return Component.literal(label + " (" + packTitle + ")").withStyle(ChatFormatting.GOLD);
    }

    public static void fill(Container container, Family family, SettlementTier tier, long seed) {
        Random rng = new Random(seed ^ (tier.level * 0x9E3779B97F4A7C15L) ^ (family.ordinal() * 0xBF58476D1CE4E5B9L));
        List<ItemStack> items = generate(family, tier, rng);
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < container.getContainerSize(); i++) slots.add(i);
        Collections.shuffle(slots, rng);
        for (int i = 0; i < Math.min(items.size(), slots.size()); i++) {
            container.setItem(slots.get(i), items.get(i));
        }
    }

    private static List<ItemStack> generate(Family family, SettlementTier tier, Random rng) {
        List<ItemStack> out = new ArrayList<>();
        // Everyday camp provisions scaled by tier
        out.add(new ItemStack(Items.TORCH, 4 + rng.nextInt(5) + tier.level * 2));
        out.add(new ItemStack(Items.BREAD, 2 + rng.nextInt(4) + tier.level));
        out.add(new ItemStack(Items.IRON_INGOT, 1 + rng.nextInt(3) + tier.level * 2));

        // Tier-scaled valuables
        switch (tier) {
            case CAMP -> {
                out.add(new ItemStack(Items.COAL, 4 + rng.nextInt(7)));
                out.add(new ItemStack(Items.GOLD_NUGGET, 4 + rng.nextInt(9)));
                if (rng.nextBoolean()) out.add(new ItemStack(Items.COOKED_BEEF, 2 + rng.nextInt(3)));
            }
            case OUTPOST -> {
                out.add(new ItemStack(Items.GOLD_INGOT, 2 + rng.nextInt(5)));
                out.add(new ItemStack(Items.EMERALD, 2 + rng.nextInt(4)));
                out.add(new ItemStack(Items.IRON_NUGGET, 8 + rng.nextInt(10)));
                out.add(new ItemStack(Items.COOKED_BEEF, 3 + rng.nextInt(4)));
            }
            case STRONGHOLD -> {
                out.add(new ItemStack(Items.GOLD_INGOT, 4 + rng.nextInt(6)));
                out.add(new ItemStack(Items.EMERALD, 4 + rng.nextInt(7)));
                out.add(new ItemStack(Items.DIAMOND, 1 + rng.nextInt(3)));
                out.add(new ItemStack(Items.GOLDEN_APPLE, 1 + rng.nextInt(2)));
                out.add(new ItemStack(Items.EXPERIENCE_BOTTLE, 4 + rng.nextInt(6)));
            }
            case TOWN -> {
                out.add(new ItemStack(Items.GOLD_INGOT, 8 + rng.nextInt(9)));
                out.add(new ItemStack(Items.EMERALD, 8 + rng.nextInt(11)));
                out.add(new ItemStack(Items.DIAMOND, 2 + rng.nextInt(5)));
                out.add(new ItemStack(Items.GOLDEN_APPLE, 2 + rng.nextInt(3)));
                out.add(new ItemStack(Items.EXPERIENCE_BOTTLE, 8 + rng.nextInt(9)));
                if (rng.nextInt(3) == 0) out.add(new ItemStack(Items.NETHERITE_SCRAP, 1));
            }
            default -> {
            }
        }

        // Family-themed spoils
        switch (family) {
            case ROTTEN -> {
                out.add(new ItemStack(Items.ROTTEN_FLESH, 6 + rng.nextInt(9)));
                out.add(new ItemStack(Items.CARROT, 3 + rng.nextInt(5)));
                out.add(new ItemStack(tier.level >= 3 ? Items.IRON_AXE : Items.IRON_SWORD));
            }
            case BONES -> {
                out.add(new ItemStack(Items.BONE, 6 + rng.nextInt(9)));
                out.add(new ItemStack(Items.ARROW, 12 + rng.nextInt(13) + tier.level * 6));
                out.add(new ItemStack(Items.BOW));
                if (tier.level >= 3) out.add(new ItemStack(Items.SPECTRAL_ARROW, 6 + rng.nextInt(7)));
            }
            case ILLAGERS -> {
                out.add(new ItemStack(Items.EMERALD, 3 + rng.nextInt(5) * tier.level));
                out.add(new ItemStack(Items.CROSSBOW));
                out.add(new ItemStack(Items.ARROW, 8 + rng.nextInt(12)));
                if (tier.level >= 3) out.add(new ItemStack(Items.IRON_AXE));
            }
            case NETHER -> {
                out.add(new ItemStack(Items.GOLD_INGOT, 3 + rng.nextInt(5) * tier.level));
                out.add(new ItemStack(Items.QUARTZ, 6 + rng.nextInt(9)));
                out.add(new ItemStack(Items.FIRE_CHARGE, 2 + rng.nextInt(4)));
                if (tier.level >= 2) out.add(new ItemStack(Items.BLAZE_POWDER, 2 + rng.nextInt(4)));
                if (tier.level >= 3) out.add(new ItemStack(Items.GOLDEN_CARROT, 4 + rng.nextInt(5)));
            }
            case SPIDERS -> {
                out.add(new ItemStack(Items.STRING, 8 + rng.nextInt(11)));
                out.add(new ItemStack(Items.SPIDER_EYE, 3 + rng.nextInt(5)));
                out.add(new ItemStack(Items.COBWEB, 2 + rng.nextInt(4)));
                if (tier.level >= 2) out.add(new ItemStack(Items.FERMENTED_SPIDER_EYE, 1 + rng.nextInt(3)));
            }
            case WITCHES -> {
                out.add(new ItemStack(Items.GLASS_BOTTLE, 4 + rng.nextInt(6)));
                out.add(new ItemStack(Items.REDSTONE, 4 + rng.nextInt(7)));
                out.add(new ItemStack(Items.GLOWSTONE_DUST, 4 + rng.nextInt(7)));
                out.add(new ItemStack(Items.GUNPOWDER, 3 + rng.nextInt(5)));
                if (tier.level >= 2) out.add(new ItemStack(Items.NETHER_WART, 2 + rng.nextInt(4)));
            }
        }
        return out;
    }

    /** Optional minor flavor loot for camp barrels. */
    public static void fillBarrel(Container container, Family family, long seed) {
        Random rng = new Random(seed);
        if (rng.nextInt(3) != 0) return; // most barrels stay light/empty so players don't get flooded
        Item[] pool = switch (family) {
            case ROTTEN -> new Item[]{Items.ROTTEN_FLESH, Items.BONE, Items.POTATO, Items.COAL};
            case BONES -> new Item[]{Items.BONE, Items.ARROW, Items.FLINT, Items.STRING};
            case ILLAGERS -> new Item[]{Items.WHEAT, Items.BREAD, Items.ARROW, Items.DARK_OAK_LOG};
            case NETHER -> new Item[]{Items.GOLD_NUGGET, Items.COAL, Items.MAGMA_CREAM, Items.BLACKSTONE};
            case SPIDERS -> new Item[]{Items.STRING, Items.SPIDER_EYE, Items.BONE, Items.COAL};
            case WITCHES -> new Item[]{Items.GLASS_BOTTLE, Items.SUGAR, Items.RED_MUSHROOM, Items.SPIDER_EYE};
        };
        Item pick = pool[rng.nextInt(pool.length)];
        container.setItem(rng.nextInt(container.getContainerSize()), new ItemStack(pick, 2 + rng.nextInt(5)));
    }
}
