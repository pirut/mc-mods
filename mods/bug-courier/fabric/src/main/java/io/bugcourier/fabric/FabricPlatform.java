package io.bugcourier.fabric;

import io.bugcourier.platform.Platform;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.ModOrigin;

public final class FabricPlatform implements Platform {
    @Override
    public List<ModEntry> allMods() {
        return FabricLoader.getInstance().getAllMods().stream().map(FabricPlatform::entry).toList();
    }

    private static ModEntry entry(ModContainer mod) {
        ModMetadata meta = mod.getMetadata();
        // bugcourier_* keys live in fabric.mod.json's "custom" block.
        Map<String, Object> props = new LinkedHashMap<>();
        meta.getCustomValues().forEach((key, value) -> {
            if (!key.startsWith("bugcourier_")) return;
            if (value.getType() == CustomValue.CvType.STRING) props.put(key, value.getAsString());
            else if (value.getType() == CustomValue.CvType.BOOLEAN) props.put(key, value.getAsBoolean());
        });
        // The game, Java and the loader are "builtin"; mods nested in another mod's jar (such as Fabric API's
        // modules) are reported to the mod that carries them.
        boolean reportable = !"builtin".equals(meta.getType()) && mod.getContainingMod().isEmpty();
        return new ModEntry(meta.getId(), meta.getName(), meta.getVersion().getFriendlyString(),
                meta.getContact().get("issues").orElse(null), props, fileName(mod), reportable);
    }

    private static String fileName(ModContainer mod) {
        try {
            ModOrigin origin = mod.getOrigin();
            return switch (origin.getKind()) {
                case PATH -> origin.getPaths().isEmpty() ? "?" : origin.getPaths().get(0).getFileName().toString();
                case NESTED -> origin.getParentSubLocation();
                default -> "?";
            };
        } catch (RuntimeException e) {
            return "?";
        }
    }

    @Override
    public Path gameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public String loaderName() {
        return "Fabric";
    }

    @Override
    public String loaderVersion() {
        return FabricLoader.getInstance().getModContainer("fabricloader").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
    }

    @Override
    public boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }
}
