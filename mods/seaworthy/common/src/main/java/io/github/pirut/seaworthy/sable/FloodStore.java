package io.github.pirut.seaworthy.sable;

import io.github.pirut.seaworthy.Seaworthy;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The water in each ship's compartments, saved with the level so a flooded ship is still flooded after a restart.
 *
 * <p>Compartments are keyed by their anchor cell, relative to the ship's bounds, which doesn't change unless the hull
 * does. The plot a ship sits in isn't part of the key.
 */
final class FloodStore extends SavedData {
    private static final String NAME = Seaworthy.MOD_ID + "_floods";
    // The type only matters when a world is upgraded, and nothing in this file looks like what it fixes.
    private static final Factory<FloodStore> FACTORY =
            new Factory<>(FloodStore::new, FloodStore::load, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<UUID, Map<Long, Float>> ships = new HashMap<>();
    /** Every ship we've seen, so what happens once for a new ship doesn't happen again after a restart. */
    private final Set<UUID> seen = new HashSet<>();

    private FloodStore() {
    }

    static FloodStore of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    private static FloodStore load(CompoundTag tag, HolderLookup.Provider registries) {
        FloodStore store = new FloodStore();
        CompoundTag ships = tag.getCompound("ships");
        for (String id : ships.getAllKeys()) {
            CompoundTag ship = ships.getCompound(id);
            Map<Long, Float> volumes = new HashMap<>();
            for (String anchor : ship.getAllKeys()) volumes.put(Long.parseLong(anchor), ship.getFloat(anchor));
            store.ships.put(UUID.fromString(id), volumes);
        }
        for (String id : tag.getCompound("seen").getAllKeys()) store.seen.add(UUID.fromString(id));
        return store;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag ships = new CompoundTag();
        this.ships.forEach((id, volumes) -> {
            CompoundTag ship = new CompoundTag();
            volumes.forEach((anchor, volume) -> ship.putFloat(Long.toString(anchor), volume));
            ships.put(id.toString(), ship);
        });
        tag.put("ships", ships);
        CompoundTag seen = new CompoundTag();
        this.seen.forEach(id -> seen.putBoolean(id.toString(), true));
        tag.put("seen", seen);
        return tag;
    }

    /** True the first time this is asked about a ship, ever. */
    boolean firstSight(UUID ship) {
        if (!seen.add(ship)) return false;
        setDirty();
        return true;
    }

    /** The ship's saved water, by anchor. Empty if it has none. */
    Map<Long, Float> get(UUID ship) {
        return ships.getOrDefault(ship, Map.of());
    }

    /** Replaces the ship's saved water with these volumes; those of no water aren't kept. */
    void put(UUID ship, long[] anchors, float[] volumes) {
        Map<Long, Float> kept = new HashMap<>();
        for (int i = 0; i < anchors.length; i++) {
            if (volumes[i] > 0) kept.put(anchors[i], volumes[i]);
        }
        if (kept.isEmpty() ? ships.remove(ship) != null : !kept.equals(ships.put(ship, kept))) setDirty();
    }

    void forget(UUID ship) {
        if (ships.remove(ship) != null | seen.remove(ship)) setDirty();
    }
}
