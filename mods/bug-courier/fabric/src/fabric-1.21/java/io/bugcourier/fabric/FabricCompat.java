package io.bugcourier.fabric;

import java.util.List;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

/** Fabric API calls whose names changed in 26.1 (when Fabric moved to Mojang's names). This copy: Minecraft 1.21.x. */
final class FabricCompat {
    private FabricCompat() {
    }

    static void registerKey(KeyMapping key) {
        KeyBindingHelper.registerKeyBinding(key);
    }

    static List<AbstractWidget> widgets(Screen screen) {
        return Screens.getButtons(screen);
    }
}
