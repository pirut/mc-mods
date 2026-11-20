package io.github.pirut.bloodfeud.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.pirut.bloodfeud.game.Screens;
import io.github.pirut.lib.ClientHooks;
import io.github.pirut.lib.Net;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import org.lwjgl.glfw.GLFW;

/** The client half of Blood Feud: only the Ledger and Map screens. Everything else runs on the server. */
public final class FeudClient {
    static final KeyMapping LEDGER = new KeyMapping("key.bloodfeud.ledger", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J,
            "key.categories.bloodfeud");

    private FeudClient() {
    }

    public static void init() {
        ClientHooks.key(LEDGER, () -> ask("ledger", ""));
        ClientHooks.livingLayer(ScarLayer::forRenderer);
    }

    /** Asks the server for a fresh snapshot, which opens (or refreshes) the screens. */
    static void ask(String tab, String focus) {
        CompoundTag t = new CompoundTag();
        t.putString("open", tab);
        t.putString("focus", focus);
        Net.sendToServer(Screens.ASK, t);
    }

    /** The server sent a snapshot. */
    public static void show(CompoundTag tag) {
        FeudView view = FeudView.read(tag);
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof LedgerScreen s) s.update(view);
        else mc.setScreen(new LedgerScreen(view));
    }
}
