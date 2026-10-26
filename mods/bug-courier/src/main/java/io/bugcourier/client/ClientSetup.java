package io.bugcourier.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.bugcourier.BugCourier;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;

/** The ways in: a pause-menu button, a key (unbound until the player picks one), /bugreport, and a prompt after a crash. */
public final class ClientSetup {
    private static final KeyMapping OPEN = new KeyMapping("key.bugcourier.open", InputConstants.UNKNOWN.getValue(), "key.categories.bugcourier");
    private static final Duration CRASH_PROMPT_AGE = Duration.ofDays(3);
    private static boolean crashChecked;

    private ClientSetup() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(RegisterKeyMappingsEvent.class, e -> e.register(OPEN));
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> {
            while (OPEN.consumeClick()) ReportTargets.openScreen(null, null);
        });
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Init.Post.class, ClientSetup::onScreenInit);
        NeoForge.EVENT_BUS.addListener(RegisterClientCommandsEvent.class, e -> e.getDispatcher().register(Commands.literal("bugreport")
                .executes(c -> {
                    ReportTargets.openScreen(null, null);
                    return 1;
                })
                .then(Commands.argument("mod", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(ReportTargets.all().stream().map(ReportTarget::modId), b))
                        .executes(c -> {
                            ReportTargets.openScreen(StringArgumentType.getString(c, "mod"), null);
                            return 1;
                        }))));
    }

    private static void onScreenInit(ScreenEvent.Init.Post e) {
        if (e.getScreen() instanceof PauseScreen pause && pause.showsPauseMenu()) {
            Component label = Component.translatable("bugcourier.button");
            int w = Math.min(110, Minecraft.getInstance().font.width(label) + 16);
            int[] spot = freeCorner(e.getListenersList(), pause.width, pause.height, w);
            e.addListener(Button.builder(label, b -> ReportTargets.openScreen(null, null)).bounds(spot[0], spot[1], w, 20).build());
        }
        if (e.getScreen() instanceof TitleScreen && !crashChecked) {
            crashChecked = true;
            offerCrashReport();
        }
    }

    // The first corner no other button covers, so the button stays visible beside other mods' additions.
    private static int[] freeCorner(java.util.List<? extends GuiEventListener> existing, int width, int height, int w) {
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
        return FMLPaths.CONFIGDIR.get().resolve("bugcourier-state.properties");
    }

    private static Properties loadState() {
        Properties p = new Properties();
        Path path = statePath();
        if (Files.isRegularFile(path)) {
            try (Reader r = Files.newBufferedReader(path)) {
                p.load(r);
            } catch (IOException e) {
                BugCourier.LOG.warn("Could not read {}: {}", path.getFileName(), e.toString());
            }
        }
        return p;
    }

    private static void saveState(Properties p) {
        Path path = statePath();
        try {
            Files.createDirectories(path.getParent());
            try (Writer w = Files.newBufferedWriter(path)) {
                p.store(w, "Bug Courier remembers which crash it already offered to report");
            }
        } catch (IOException e) {
            BugCourier.LOG.warn("Could not write {}: {}", path.getFileName(), e.toString());
        }
    }
}
