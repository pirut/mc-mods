package io.github.pirut.bloodfeud;

import io.github.pirut.bloodfeud.game.Effects;
import io.github.pirut.bloodfeud.game.FeudCommand;
import io.github.pirut.bloodfeud.game.Ledger;
import io.github.pirut.bloodfeud.game.Screens;
import io.github.pirut.bloodfeud.game.Live;
import io.github.pirut.lib.Loader;
import io.github.pirut.lib.Net;
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
        ServerEvents.onUseItem((player, hand, stack) -> {
            if (!Ledger.is(stack)) return false;
            Live live = Live.get(player.server);
            if (Screens.open(live, player, "ledger", "")) return true; // their client draws it
            Ledger.refresh(live, player, stack);
            return false;
        });
        // Players with the mod on their client get real screens; the client half lives in the client package and
        // is only touched inside these lambdas, which a dedicated server never runs.
        Net.toClient(Screens.SHOW, tag -> io.github.pirut.bloodfeud.client.FeudClient.show(tag));
        Net.toClient(Screens.SCARS, tag -> io.github.pirut.bloodfeud.client.ScarLayer.receive(tag));
        Net.toServer(Screens.ASK, (player, tag) -> Screens.open(Live.get(player.server), player,
                tag.getString("open"), tag.getString("focus")));
        ServerEvents.onRegisterCommands((dispatcher, context, selection) -> FeudCommand.register(dispatcher));
        LOG.info("{} loaded on {}", MOD_ID, loader.name());
    }

    public static Config config() {
        return config;
    }

    /** Toggles automatic pack camps/towns live and persists it to config/bloodfeud.properties. */
    public static void setBuildCamps(boolean enabled) {
        config.buildCamps = enabled;
        config.save();
        Live live = Live.current();
        if (live != null) live.onBuildCampsChanged(enabled);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private BloodFeud() {
    }
}
