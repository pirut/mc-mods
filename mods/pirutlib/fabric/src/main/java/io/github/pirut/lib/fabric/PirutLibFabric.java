package io.github.pirut.lib.fabric;

import io.github.pirut.lib.ServerEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/** Turns Fabric API's events into {@link ServerEvents}. */
public final class PirutLibFabric implements ModInitializer {
    /** Fabric can't refuse an entity while it is being added, so refused ones go on the next tick. */
    private static final List<Entity> REFUSED = new ArrayList<>();

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!REFUSED.isEmpty()) {
                for (Entity e : REFUSED) if (!e.isRemoved()) e.discard();
                REFUSED.clear();
            }
            ServerEvents.Fire.tick(server);
        });
        ServerLifecycleEvents.SERVER_STARTED.register(ServerEvents.Fire::started);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            REFUSED.clear();
            ServerEvents.Fire.stopped(server);
        });
        CommandRegistrationCallback.EVENT.register(ServerEvents.Fire::commands);
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (!ServerEvents.Fire.entityLoad(entity, level)) REFUSED.add(entity);
        });
        ServerLivingEntityEvents.AFTER_DEATH.register(ServerEvents.Fire::death);
        io.github.pirut.lib.Net.bridge(new FabricNet());
        net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.register((player, level, hand) -> {
            net.minecraft.world.item.ItemStack stack = player.getItemInHand(hand);
            if (player instanceof net.minecraft.server.level.ServerPlayer p && ServerEvents.Fire.useItem(p, hand, stack)) {
                return net.minecraft.world.InteractionResultHolder.success(stack);
            }
            return net.minecraft.world.InteractionResultHolder.pass(stack);
        });
    }
}
