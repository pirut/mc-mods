package io.github.pirut.lib;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/** Data saved with the world, in the overworld's data folder ({@code data/<name>.dat}). */
public final class Stores {
    private Stores() {
    }

    /**
     * The world's copy of a {@link SavedData}, read from disk the first time.
     *
     * @param name   the file name, usually your mod id
     * @param create makes an empty one for a new world
     * @param load   reads one from its saved tag
     */
    public static <T extends SavedData> T get(MinecraftServer server, String name, Supplier<T> create,
                                              BiFunction<CompoundTag, HolderLookup.Provider, T> load) {
        // The data fix type must not be null on Fabric and plain vanilla (it is looked up on load); it only matters
        // for files from older game versions.
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(create, load, DataFixTypes.LEVEL), name);
    }
}
