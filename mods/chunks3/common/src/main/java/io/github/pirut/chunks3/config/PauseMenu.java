package io.github.pirut.chunks3.config;

import io.github.pirut.chunks3.client.ClientCubeWorld;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * The far terrain's distance and sharpness on the pause menu in cubic worlds, as Earth Platform has them: a row
 * under the full-width Mods button (NeoForge's, or Mod Menu's on Fabric) holds the distance slider and the
 * sharpness button, and the buttons below move down for it. Without a Mods button (Fabric without Mod Menu) the
 * row goes under "Back to Game". Both apply at once.
 */
public final class PauseMenu {
    private PauseMenu() {}

    /** Called by each loader once a screen has laid out its widgets; {@code add} adds one more. */
    public static void afterInit(Screen screen, List<? extends GuiEventListener> listeners, Consumer<AbstractWidget> add) {
        if (!(screen instanceof PauseScreen) || ClientCubeWorld.current() == null) return;
        Button back = find(listeners, "menu.returnToGame");
        // Only a Mods button as wide as Back to Game: Mod Menu may put its own as a 20-pixel icon beside another.
        Button mods = find(listeners, "fml.menu.mods", "modmenu.title");
        Button anchor = mods != null && back != null && mods.getWidth() >= back.getWidth() ? mods : back;
        if (anchor == null) return;
        int width = anchor.getWidth(), row = anchor.getHeight() + 4, y = anchor.getY() + row;
        for (var other : listeners)
            if (other instanceof AbstractWidget widget && widget.getY() > anchor.getY()) widget.setY(widget.getY() + row);
        int slider = (width - 4) * 2 / 3, rest = width - 4 - slider;
        add.accept(new FarDistanceSlider(anchor.getX(), y, slider, anchor.getHeight()));
        add.accept(Chunks3ConfigScreen.sharpness().createButton(Minecraft.getInstance().options, anchor.getX() + slider + 4, y, rest));
    }

    private static Button find(List<? extends GuiEventListener> listeners, String... keys) {
        for (var listener : listeners) {
            if (!(listener instanceof Button button) || !(button.getMessage().getContents() instanceof TranslatableContents text)) continue;
            for (String key : keys) if (text.getKey().equals(key)) return button;
        }
        return null;
    }
}
