package io.github.pirut.lib.fabric;

import io.github.pirut.lib.Loader;
import net.fabricmc.api.EnvType;

import java.nio.file.Path;
import java.util.Optional;

public final class FabricLoader implements Loader {
    private static net.fabricmc.loader.api.FabricLoader fabric() {
        return net.fabricmc.loader.api.FabricLoader.getInstance();
    }

    @Override
    public String name() {
        return "fabric";
    }

    @Override
    public Path configDir() {
        return fabric().getConfigDir();
    }

    @Override
    public Path gameDir() {
        return fabric().getGameDir();
    }

    @Override
    public boolean isClient() {
        return fabric().getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public Optional<String> modVersion(String modId) {
        return fabric().getModContainer(modId).map(c -> c.getMetadata().getVersion().getFriendlyString());
    }
}
