package io.bugcourier.client.version;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Everything Bug Courier's shared client code calls that changed between Minecraft versions, for Minecraft
 * 1.21.8. Every supported version has its own copy of this class in common/src/mc&lt;version&gt;; they all have the
 * same methods. To add a Minecraft version, copy the nearest one and fix what no longer compiles.
 */
public final class Mc {
    private Mc() {
    }

    /** The "open Bug Courier" key: unbound until the player picks one. */
    public static KeyMapping keyMapping(String name) {
        return new KeyMapping(name, InputConstants.UNKNOWN.getValue(), "key.categories.bugcourier");
    }

    /** Runs {@code task} on the render thread on a later tick, even when called from it. */
    public static void later(Runnable task) {
        Minecraft.getInstance().schedule(task);
    }

    public static Screen screen() {
        return Minecraft.getInstance().screen;
    }

    public static void setScreen(Screen screen) {
        Minecraft.getInstance().setScreen(screen);
    }

    public static void openUri(URI uri) {
        Util.getPlatform().openUri(uri);
    }

    public static void openFolder(Path dir) {
        Util.getPlatform().openFile(dir.toFile());
    }

    /** The game's version name, such as "1.21.8". */
    public static String gameVersion() {
        return SharedConstants.getCurrentVersion().name();
    }

    public static String cpu() {
        return GLX._getCpuInfo();
    }

    public static String gpu() {
        var device = RenderSystem.getDevice();
        return device.getRenderer() + " (" + device.getVendor() + "), " + device.getBackendName() + " " + device.getVersion();
    }

    public static <T> CycleButton<T> cycleButton(Function<T, Component> name, List<T> values, T initial, int x, int y, int width,
                                                 int height, Component label, Consumer<T> onChange) {
        return CycleButton.<T>builder(name).withValues(values).withInitialValue(initial)
                .create(x, y, width, height, label, (button, value) -> onChange.accept(value));
    }

    public static MultiLineEditBox multiLineEditBox(Font font, int x, int y, int width, int height, Component placeholder,
                                                    Component message) {
        return MultiLineEditBox.builder().setX(x).setY(y).setPlaceholder(placeholder).build(font, width, height, message);
    }
}
