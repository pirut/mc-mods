package io.bugcourier.neoforge;

import io.bugcourier.platform.Platform;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.IModInfo;

public final class NeoForgePlatform implements Platform {
    // Bugs in the game or the loader itself belong upstream, not in a player report from a modpack.
    private static final Set<String> NOT_REPORTABLE = Set.of("minecraft", "neoforge");
    /** Set by the mod's constructor. */
    static volatile Dist dist = Dist.CLIENT;

    @Override
    public List<ModEntry> allMods() {
        return ModList.get().getMods().stream().map(NeoForgePlatform::entry).toList();
    }

    private static ModEntry entry(IModInfo mod) {
        String issues = mod.getOwningFile().getConfig().getConfigElement("issueTrackerURL").map(Object::toString).orElse(null);
        return new ModEntry(mod.getModId(), mod.getDisplayName(), mod.getVersion().toString(), issues, mod.getModProperties(),
                mod.getOwningFile().getFile().getFileName(), !NOT_REPORTABLE.contains(mod.getModId()));
    }

    @Override
    public Path gameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public String loaderName() {
        return "NeoForge";
    }

    @Override
    public String loaderVersion() {
        // FMLLoader.versionInfo() is gone in newer NeoForge; its own mod entry has the version on every version.
        return mod("neoforge").map(ModEntry::version).orElse("?");
    }

    @Override
    public boolean isClient() {
        return dist == Dist.CLIENT;
    }
}
