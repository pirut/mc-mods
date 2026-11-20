package io.github.pirut.ccagent;

import dan200.computercraft.api.upgrades.UpgradeBase;
import net.minecraft.core.registries.BuiltInRegistries;

/** Items, for Minecraft 26.1 and later (CC: Tweaked's upgrades give an ItemStackTemplate, never empty). */
final class McItems {
    private McItems() {
    }

    /** The id of the item that crafts this upgrade, or null. */
    static String upgradeItem(UpgradeBase upgrade) {
        return BuiltInRegistries.ITEM.getKey(upgrade.getCraftingItem().item().value()).toString();
    }
}
