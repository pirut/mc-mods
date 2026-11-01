package io.bugcourier.fabric;

import java.util.List;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

/** Fabric API calls whose names changed in 26.1 (when Fabric moved to Mojang's names). This copy: Minecraft 26.x. */
final class FabricCompat {
    private FabricCompat() {
    }

    static void registerKey(KeyMapping key) {
        KeyMappingHelper.registerKeyMapping(key);
    }

    static List<AbstractWidget> widgets(Screen screen) {
        return Screens.getWidgets(screen);
    }
}
