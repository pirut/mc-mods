package io.github.pirut.lib;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Client-only hooks. Call these only from client code (a client entry point). */
public final class ClientHooks {
    private ClientHooks() {
    }

    public record Key(KeyMapping mapping, Runnable onPress) {
    }

    /** What the loader glue provides. */
    public interface Bridge {
        void register(Key key);
    }

    /** Makes an extra layer for a mob's renderer, or returns null to leave that renderer alone. */
    public interface LayerFactory {
        RenderLayer<?, ?> create(LivingEntityRenderer<?, ?> renderer);
    }

    private static final List<LayerFactory> LAYERS = new CopyOnWriteArrayList<>();

    /**
     * An extra render layer for every mob renderer (players excluded), drawn after the mob's own layers. Call it
     * during client setup, before the game builds its renderers.
     */
    public static void livingLayer(LayerFactory factory) {
        LAYERS.add(factory);
    }

    public static List<LayerFactory> layers() {
        return LAYERS;
    }

    private static final List<Key> KEYS = new CopyOnWriteArrayList<>();
    private static Bridge bridge;

    /** A key players can rebind in Controls; pressing it in game runs {@code onPress}. */
    public static synchronized void key(KeyMapping mapping, Runnable onPress) {
        Key k = new Key(mapping, onPress);
        KEYS.add(k);
        if (bridge != null) bridge.register(k);
    }

    public static List<Key> keys() {
        return KEYS;
    }

    /** For the loader glue: runs each key's action for every press since the last tick. */
    public static void tick() {
        for (Key k : KEYS) {
            while (k.mapping().consumeClick()) {
                try {
                    k.onPress().run();
                } catch (RuntimeException e) {
                    PirutLib.LOG.error("A key action failed", e);
                }
            }
        }
    }

    /** For loader glue that registers keys as they come. Keys added earlier are handed over at once. */
    public static synchronized void bridge(Bridge b) {
        bridge = b;
        for (Key k : KEYS) b.register(k);
    }
}
