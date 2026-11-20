package io.github.pirut.chunks3.neoforge;

import io.github.pirut.chunks3.Cubic;
import io.github.pirut.chunks3.CubicEvents;
import io.github.pirut.chunks3.CubicPoiClaims;
import io.github.pirut.chunks3.CubicSpawns;
import io.github.pirut.chunks3.compat.PortalReturn;
import io.github.pirut.chunks3.net.CubePayloads;
import io.github.pirut.chunks3.portal.CubicTravel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** NeoForge's events, on both sides, handed to the engine. */
@EventBusSubscriber(modid = Cubic.MOD_ID)
public final class NeoForgeEvents {
    private NeoForgeEvents() {}

    /** Setup and registration, on the mod's own event bus. */
    //#if MC >= 1.21.9
    //$$ @EventBusSubscriber(modid = Cubic.MOD_ID)
    //#else
    @EventBusSubscriber(modid = Cubic.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    //#endif
    public static final class Setup {
        private Setup() {}

        @SubscribeEvent public static void payloads(RegisterPayloadHandlersEvent event) {
            CubePayloads.register(new NeoForgePayloads(event.registrar(CubePayloads.VERSION)));
        }

        @SubscribeEvent public static void placements(RegisterSpawnPlacementsEvent event) {
            CubicSpawns.placements(new CubicSpawns.Rules() {
                @Override public <T extends Entity> void or(EntityType<T> type, SpawnPlacements.SpawnPredicate<T> rule) {
                    event.register(type, rule, RegisterSpawnPlacementsEvent.Operation.OR);
                }
            });
        }
    }

    @SubscribeEvent public static void load(LevelEvent.Load event) { CubicEvents.load(event.getLevel()); }
    @SubscribeEvent public static void save(LevelEvent.Save event) { CubicEvents.save(event.getLevel()); }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) { CubicEvents.unload(event.getLevel()); }
    @SubscribeEvent public static void levelTick(LevelTickEvent.Post event) { CubicEvents.tick(event.getLevel()); }

    @SubscribeEvent public static void join(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk()) CubicPoiClaims.join(event.getEntity(), event.getLevel());
    }

    @SubscribeEvent public static void travel(EntityTravelToDimensionEvent event) {
        if (PortalReturn.fallFromAether(event.getEntity(), event.getDimension())) event.setCanceled(true);
    }

    @SubscribeEvent public static void commands(RegisterCommandsEvent event) { CubicEvents.commands(event.getDispatcher()); }

    @SubscribeEvent public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) CubicTravel.changedDimension(player, event.getFrom(), event.getTo());
    }

    @SubscribeEvent public static void respawnPosition(PlayerRespawnPositionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            event.setDimensionTransition(CubicTravel.home(player, event.getDimensionTransition()));
    }
}
