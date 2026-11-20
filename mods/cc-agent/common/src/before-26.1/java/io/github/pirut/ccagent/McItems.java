package io.github.pirut.ccagent;

import dan200.computercraft.api.upgrades.UpgradeBase;
import net.minecraft.core.registries.BuiltInRegistries;

/** Items, for Minecraft before 26.1 (CC: Tweaked's upgrades give an ItemStack). */
final class McItems {
    private McItems() {
    }

    /** The id of the item that crafts this upgrade, or null. */
    static String upgradeItem(UpgradeBase upgrade) {
        var item = upgrade.getCraftingItem();
        return item.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
    }
}
