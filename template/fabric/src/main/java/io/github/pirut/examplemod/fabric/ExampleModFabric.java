package io.github.pirut.examplemod.fabric;

import io.github.pirut.examplemod.ExampleMod;
import net.fabricmc.api.ModInitializer;

public final class ExampleModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ExampleMod.init("fabric");
    }
}
