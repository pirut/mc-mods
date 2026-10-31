package io.bugcourier.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.bugcourier.BugCourier;
import io.bugcourier.platform.Platform;
import io.github.pirut.mcmods.lib.PropertiesFile;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

/**
 * The ways in: a pause-menu button, a key (unbound until the player picks one), /bugreport, and a prompt after a
 * crash. Each loader's client entry point hooks these up to its own events.
 */
public final class ClientSetup {
    public static final KeyMapping OPEN = new KeyMapping("key.bugcourier.open", InputConstants.UNKNOWN.getValue(), "key.categories.bugcourier");
    private static final Duration CRASH_PROMPT_AGE = Duration.ofDays(3);
    private static boolean crashChecked;

    private ClientSetup() {
    }

    /** Call at the end of every client tick. */
    public static void onClientTick() {
        while (OPEN.consumeClick()) ReportTargets.openScreen(null, null);
    }

    /** {@code /bugreport [mod]}, for the loader's client command dispatcher, whatever its source type. */
    public static <S> LiteralArgumentBuilder<S> command() {
        return LiteralArgumentBuilder.<S>literal("bugreport")
                .executes(c -> {
                    ReportTargets.openScreen(null, null);
                    return 1;
                })
                .then(RequiredArgumentBuilder.<S, String>argument("mod", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(ReportTargets.all().stream().map(ReportTarget::modId), b))
                        .executes(c -> {
                            ReportTargets.openScreen(StringArgumentType.getString(c, "mod"), null);
                            return 1;
                        }));
    }

    /**
     * Call after a screen is set up, with its widgets so far and a way to add one: adds the pause-menu button, and
     * offers to report the last crash when the title screen first shows.
     */
    public static void onScreenInit(Screen screen, List<? extends GuiEventListener> existing, Consumer<AbstractWidget> add) {
        if (screen instanceof PauseScreen pause && pause.showsPauseMenu()) {
            Component label = Component.translatable("bugcourier.button");
            int w = Math.min(110, Minecraft.getInstance().font.width(label) + 16);
            int[] spot = freeCorner(existing, pause.width, pause.height, w);
            add.accept(Button.builder(label, b -> ReportTargets.openScreen(null, null)).bounds(spot[0], spot[1], w, 20).build());
        }
        if (screen instanceof TitleScreen && !crashChecked) {
            crashChecked = true;
            offerCrashReport();
        }
    }

    // The first corner no other button covers, so the button stays visible beside other mods' additions.
    private static int[] freeCorner(List<? extends GuiEventListener> existing, int width, int height, int w) {
        int[][] corners = {{4, height - 24}, {width - w - 4, height - 24}, {4, 4}, {width - w - 4, 4}};
        for (int[] c : corners) {
            boolean clear = true;
            for (GuiEventListener l : existing) {
                if (l instanceof AbstractWidget o && o.visible && c[0] < o.getX() + o.getWidth() && o.getX() < c[0] + w
                        && c[1] < o.getY() + o.getHeight() && o.getY() < c[1] + 20) {
                    clear = false;
                    break;
                }
            }
            if (clear) return c;
        }
        return corners[0];
    }

    // The game is gone when it crashes, so the offer comes on the next start, once per crash report.
    private static void offerCrashReport() {
        Collectors.newestCrash(CRASH_PROMPT_AGE).ifPresent(crash -> {
            Properties state = loadState();
            String seen = state.getProperty("lastCrashSeen", "");
            String name = crash.getFileName().toString();
            if (name.equals(seen) || "true".equals(state.getProperty("neverAskAboutCrashes"))) return;
            state.setProperty("lastCrashSeen", name);
            saveState(state);
            String text = Collectors.read(crash);
            if (text == null) return;
            Minecraft mc = Minecraft.getInstance();
            mc.tell(() -> {
                var title = mc.screen;
                mc.setScreen(new ConfirmScreen(yes -> {
                    if (yes) mc.setScreen(new ReportScreen(title, null, text));
                    else mc.setScreen(title);
                }, Component.translatable("bugcourier.crash.title"), Component.translatable("bugcourier.crash.body"),
                        Component.translatable("bugcourier.crash.report"), Component.translatable("bugcourier.crash.later")));
            });
        });
    }

    private static Path statePath() {
        return Platform.get().configDir().resolve("bugcourier-state.properties");
    }

    private static Properties loadState() {
        return PropertiesFile.read(statePath(), (f, e) -> BugCourier.LOG.warn("Could not read {}: {}", f.getFileName(), e.toString()));
    }

    private static void saveState(Properties p) {
        PropertiesFile.write(statePath(), p, "Bug Courier remembers which crash it already offered to report",
                (f, e) -> BugCourier.LOG.warn("Could not write {}: {}", f.getFileName(), e.toString()));
    }
}
