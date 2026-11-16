package io.github.pirut.bloodfeud;

import io.github.pirut.bloodfeud.game.Effects;
import io.github.pirut.bloodfeud.game.FeudCommand;
import io.github.pirut.bloodfeud.game.Live;
import io.github.pirut.lib.Loader;
import io.github.pirut.lib.ServerEvents;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Blood Feud's start. Each loader's entry point only calls {@link #init}; Pirut Lib does the rest. */
public final class BloodFeud {
    public static final String MOD_ID = "bloodfeud";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    private static Config config = new Config();

    public static void init() {
        Loader loader = Loader.get();
        config = Config.load(loader.configDir().resolve(MOD_ID + ".properties"));
        ServerEvents.onTick(server -> Live.get(server).tick());
        ServerEvents.onStopped(server -> Live.stopped());
        ServerEvents.onEntityLoad((entity, level) -> Live.get(level.getServer()).onEntityLoad(entity));
        ServerEvents.onDeath((entity, source) -> {
            if (entity.getServer() != null) Live.get(entity.getServer()).onDeath(entity, source);
        });
        ServerEvents.onIgnoreDamage((target, source, amount) -> Effects.immune(target, source));
        ServerEvents.onDamage(Effects::damage);
        ServerEvents.onRegisterCommands((dispatcher, context, selection) -> FeudCommand.register(dispatcher));
        LOG.info("{} loaded on {}", MOD_ID, loader.name());
    }

    public static Config config() {
        return config;
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private BloodFeud() {
    }
}
