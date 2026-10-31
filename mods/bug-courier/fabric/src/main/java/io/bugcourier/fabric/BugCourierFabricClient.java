package io.bugcourier.fabric;

import io.bugcourier.client.ClientSetup;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;

/** Reports are made on the player's computer, so Bug Courier has only a client entry point. */
public final class BugCourierFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(ClientSetup.OPEN);
        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientSetup.onClientTick());
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            var buttons = Screens.getButtons(screen);
            ClientSetup.onScreenInit(screen, buttons, buttons::add);
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) ->
                dispatcher.register(ClientSetup.<FabricClientCommandSource>command()));
    }
}
