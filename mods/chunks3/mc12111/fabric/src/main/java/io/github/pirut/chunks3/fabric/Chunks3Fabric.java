package io.github.pirut.chunks3.fabric;

import io.github.pirut.chunks3.Cubic;
import io.github.pirut.chunks3.CubicEvents;
import io.github.pirut.chunks3.CubicPoiClaims;
import io.github.pirut.chunks3.CubicSpawns;
import io.github.pirut.chunks3.fabric.axiom.AxiomCubes;
import io.github.pirut.chunks3.net.CubePayloads;
import io.github.pirut.chunks3.portal.CubicTravel;
import java.util.IdentityHashMap;
import java.util.Map;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;

/**
 * The Fabric entry point: Fabric API's events (and, where it has none, the mixins in
 * {@code io.github.pirut.chunks3.fabric.mixin}) handed to the same engine code NeoForge's events reach.
 * Chunks3 adds nothing to the game by itself: a mod makes its dimensions cubic with {@link Cubic#register}.
 */
public final class Chunks3Fabric implements ModInitializer {
    /** Extra spawn rules, OR'd with vanilla's in {@code SpawnPlacementsMixin}, as NeoForge's Operation.OR does. */
    public static final Map<EntityType<?>, SpawnPlacements.SpawnPredicate<?>> SPAWN_RULES = new IdentityHashMap<>();

    @Override
    public void onInitialize() {
        CubePayloads.register(new FabricPayloads());

        CubicSpawns.placements(new CubicSpawns.Rules() {
            @Override public <T extends Entity> void or(EntityType<T> type, SpawnPlacements.SpawnPredicate<T> rule) { SPAWN_RULES.put(type, rule); }
        });

        ServerWorldEvents.LOAD.register((server, level) -> CubicEvents.load(level));
        ServerWorldEvents.UNLOAD.register((server, level) -> CubicEvents.unload(level));
        ServerTickEvents.END_WORLD_TICK.register(CubicEvents::tick);
        ServerEntityEvents.ENTITY_LOAD.register(CubicPoiClaims::join);

        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> CubicEvents.commands(dispatcher));

        ServerLifecycleEvents.SERVER_STARTING.register(server -> FabricPlatform.server = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> FabricPlatform.server = null);
        if (FabricLoader.getInstance().isModLoaded("axiom")) ServerTickEvents.END_SERVER_TICK.register(server -> AxiomCubes.flush(false));

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, from, to) ->
                CubicTravel.changedDimension(player, from.dimension(), to.dimension()));

        Cubic.LOGGER.info("Chunks3 loaded");
    }
}
