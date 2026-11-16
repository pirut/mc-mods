package io.github.pirut.lib.neoforge;

import io.github.pirut.lib.PirutLib;
import io.github.pirut.lib.ServerEvents;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Turns NeoForge's events into {@link ServerEvents}. */
@Mod(PirutLib.MOD_ID)
public final class PirutLibNeoForge {
    public PirutLibNeoForge() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> ServerEvents.Fire.tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> ServerEvents.Fire.started(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> ServerEvents.Fire.stopped(e.getServer()));
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) ->
                ServerEvents.Fire.commands(e.getDispatcher(), e.getBuildContext(), e.getCommandSelection()));
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent e) -> {
            if (e.getLevel() instanceof ServerLevel level && !ServerEvents.Fire.entityLoad(e.getEntity(), level)) e.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((LivingDeathEvent e) -> {
            if (!e.getEntity().level().isClientSide) ServerEvents.Fire.death(e.getEntity(), e.getSource());
        });
    }
}
