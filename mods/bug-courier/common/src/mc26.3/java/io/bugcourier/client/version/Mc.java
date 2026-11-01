package io.bugcourier.client.version;

import com.mojang.blaze3d.Blaze3D;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.SharedConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.debug.DebugEntrySystemSpecs;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Everything Bug Courier's shared client code calls that changed between Minecraft versions, for Minecraft
 * 26.3. Every supported version has its own copy of this class in common/src/mc&lt;version&gt;; they all have the
 * same methods. To add a Minecraft version, copy the nearest one and fix what no longer compiles.
 */
public final class Mc {
    private Mc() {
    }

    /** The "open Bug Courier" key: unbound until the player picks one. */
    public static KeyMapping keyMapping(String name) {
        // 1.21.9 made categories registered objects; Miscellaneous needs no registering on either loader.
        return new KeyMapping(name, InputConstants.UNKNOWN.getValue(), KeyMapping.Category.MISC);
    }

    /** Runs {@code task} on the render thread on a later tick, even when called from it. */
    public static void later(Runnable task) {
        Minecraft.getInstance().schedule(task);
    }

    public static Screen screen() {
        return Minecraft.getInstance().gui.screen();
    }

    public static void setScreen(Screen screen) {
        Minecraft.getInstance().gui.setScreen(screen);
    }

    public static void openUri(URI uri) {
        Blaze3D.openUri(uri);
    }

    public static void openFolder(Path dir) {
        Blaze3D.openPath(dir);
    }

    /** The game's version name, such as "26.3". */
    public static String gameVersion() {
        return SharedConstants.getCurrentVersion().name();
    }

    public static String cpu() {
        return DebugEntrySystemSpecs.getCpuInfo();
    }

    public static String gpu() {
        var info = RenderSystem.getDevice().getDeviceInfo();
        return info.name() + " (" + info.vendorName() + "), " + info.backendName() + " " + info.driverInfo();
    }

    public static <T> CycleButton<T> cycleButton(Function<T, Component> name, List<T> values, T initial, int x, int y, int width,
                                                 int height, Component label, Consumer<T> onChange) {
        return CycleButton.builder(name, initial).withValues(values)
                .create(x, y, width, height, label, (button, value) -> onChange.accept(value));
    }

    public static MultiLineEditBox multiLineEditBox(Font font, int x, int y, int width, int height, Component placeholder,
                                                    Component message) {
        return MultiLineEditBox.builder().setX(x).setY(y).setPlaceholder(placeholder).build(font, width, height, message);
    }
}
