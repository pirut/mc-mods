package io.github.pirut.seaworthy.sable;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import io.github.pirut.seaworthy.net.FloodSyncPayload;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/** Keeps a {@link ShipHull} for every ship in a level, on the server and on each player's game. */
public final class ShipTracker implements SubLevelObserver {
    private static final Map<Level, ShipTracker> TRACKERS = Collections.synchronizedMap(new WeakHashMap<>());

    private final Level level;
    private final Map<UUID, ShipHull> hulls = new ConcurrentHashMap<>();
    /** Flood levels that arrived before we had looked at the ship ourselves. */
    private final Map<UUID, FloodSyncPayload> early = new ConcurrentHashMap<>();
    private long ticks;

    private ShipTracker(Level level) {
        this.level = level;
    }

    public static ShipTracker create(Level level) {
        ShipTracker tracker = new ShipTracker(level);
        TRACKERS.put(level, tracker);
        return tracker;
    }

    public static @Nullable ShipTracker of(Level level) {
        return TRACKERS.get(level);
    }

    /** Called when a block in a ship changes. */
    public static void blockChanged(SubLevel subLevel) {
        ShipTracker tracker = of(subLevel.getLevel());
        if (tracker == null) return;
        ShipHull hull = tracker.hulls.get(subLevel.getUniqueId());
        if (hull != null) hull.markDirty();
    }

    @Nullable ShipHull hull(UUID id) {
        return hulls.get(id);
    }

    public void receive(FloodSyncPayload payload) {
        ShipHull hull = hulls.get(payload.ship());
        if (hull == null || !hull.receive(payload)) early.put(payload.ship(), payload);
    }

    @Override
    public void tick(SubLevelContainer container) {
        ticks++;
        Set<UUID> seen = new HashSet<>();
        for (SubLevel subLevel : container.getAllSubLevels()) {
            if (subLevel.isRemoved()) continue;
            UUID id = subLevel.getUniqueId();
            seen.add(id);
            ShipHull hull = hulls.get(id);
            if (hull == null || hull.subLevel() != subLevel) {
                if (hull != null) hull.dispose();
                hull = new ShipHull(subLevel);
                hulls.put(id, hull);
            }
            try {
                hull.tick(ticks);
            } catch (RuntimeException e) {
                hull.fail(e);
            }
            FloodSyncPayload waiting = early.get(id);
            if (waiting != null && hull.receive(waiting)) early.remove(id);
        }
        hulls.entrySet().removeIf(e -> {
            if (seen.contains(e.getKey())) return false;
            e.getValue().dispose();
            return true;
        });
        early.keySet().retainAll(seen);
    }

    @Override
    public void onSubLevelRemoved(SubLevel subLevel, SubLevelRemovalReason reason) {
        ShipHull hull = hulls.remove(subLevel.getUniqueId());
        if (hull != null) hull.dispose();
    }

    Level level() {
        return level;
    }
}
