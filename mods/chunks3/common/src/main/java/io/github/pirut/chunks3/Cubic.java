package io.github.pirut.chunks3;

import com.mojang.logging.LogUtils;
import io.github.pirut.chunks3.gen.CubeGenerator;
import io.github.pirut.chunks3.gen.OverworldGenerator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import org.slf4j.Logger;

/**
 * The cubic engine's identity and its registry of cubic dimensions. A mod makes a dimension cubic by
 * registering it here (from its mod constructor or common setup, before any level loads) with the
 * generator its cubes come from. A world made with the Cubic world type has dimensions of type
 * {@link #CUBIC_TYPE}; those are {@linkplain #adopt adopted} for as long as that world is played, with
 * {@link OverworldGenerator}. Every other dimension keeps vanilla's chunks.
 */
public final class Cubic {
    /** The namespace of the engine's network messages and the mod id its event subscribers register under. */
    public static final String MOD_ID = "chunks3";
    public static final Logger LOGGER = LogUtils.getLogger();
    /** Vertical cube radius kept loaded and sent around each player. */
    public static final int VERTICAL_RADIUS = 6;
    /** Horizontal cube radius cap, applied on top of the server view distance. */
    public static final int MAX_HORIZONTAL_RADIUS = 12;

    /**
     * Height above which no generated terrain stands, so an entity {@linkplain #inTransit in transit}
     * up there is sent only the cubes players built (Earth Platform's jet stream starts here).
     */
    public static final int CLEAR_SKY = Integer.getInteger("chunks3.clearSky", 9000);

    /** -Dchunks3.cubic.trace=true logs cube traffic, for engine debugging. */
    public static final boolean TRACE = Boolean.getBoolean("chunks3.cubic.trace");

    private static final Map<ResourceKey<Level>, CubicDimension> DIMENSIONS = new ConcurrentHashMap<>();
    private static final List<ResourceKey<Level>> ORDER = new CopyOnWriteArrayList<>();
    /** Dimensions a client adopted itself from a dedicated server's levels, dropped when it leaves. */
    private static final java.util.Set<ResourceKey<Level>> CLIENT_ADOPTED = ConcurrentHashMap.newKeySet();
    private static final List<Predicate<Entity>> TRANSIT = new CopyOnWriteArrayList<>();
    private static final List<Consumer<ServerPlayer>> ARRIVALS = new CopyOnWriteArrayList<>();

    private Cubic() {}

    /**
     * Makes a dimension cubic. Its dimension type must be a 16-block carrier (min Y 0, height 16, as in
     * the engine's docs); its blocks then live in cubes from {@link CubeWorld#MIN_Y} to {@link CubeWorld#MAX_Y}.
     * The first dimension registered is the {@linkplain #home() home} that portals from vanilla's
     * Nether and End lead back to.
     */
    public static synchronized CubicDimension register(ResourceKey<Level> key, CubicDimension.Generators generators) {
        CubicDimension existing = DIMENSIONS.get(key);
        if (existing != null && !existing.adopted()) throw new IllegalStateException("Cubic dimension " + key.location() + " is registered twice");
        if (existing != null) ORDER.remove(key);
        CubicDimension dimension = new CubicDimension(key, generators, false);
        DIMENSIONS.put(key, dimension);
        // Registered dimensions come before adopted ones, so the first registered stays home.
        int at = 0;
        while (at < ORDER.size() && !DIMENSIONS.get(ORDER.get(at)).adopted()) at++;
        ORDER.add(at, key);
        return dimension;
    }

    /** The dimension type of the Cubic world type: a 16-block carrier whose levels are cubic without any mod registering them. */
    public static final ResourceKey<DimensionType> CUBIC_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, ResourceLocation.fromNamespaceAndPath(MOD_ID, "cubic"));

    /** How an adopted dimension's cubes are made: vanilla's overworld, with the ground going on down to a deep bedrock floor. */
    public static final CubicDimension.Generators WORLD_TYPE = (level, folder) -> new OverworldGenerator(level.getSeed(), level.registryAccess());

    /**
     * Makes exactly these dimensions the adopted ones: each that no mod registered becomes cubic with
     * {@link #WORLD_TYPE}, and adopted dimensions not among them stop being cubic, so a vanilla world opened
     * after a Cubic one in the same game stays vanilla. Registered dimensions are never touched.
     */
    public static synchronized void adopt(Collection<ResourceKey<Level>> keys) {
        // The server's choice now: nothing stays the client's own to drop when it leaves.
        CLIENT_ADOPTED.clear();
        for (ResourceKey<Level> key : new ArrayList<>(ORDER)) {
            CubicDimension dimension = DIMENSIONS.get(key);
            if (dimension.adopted() && !keys.contains(key)) { DIMENSIONS.remove(key); ORDER.remove(key); }
        }
        for (ResourceKey<Level> key : keys) {
            if (DIMENSIONS.containsKey(key)) continue;
            DIMENSIONS.put(key, new CubicDimension(key, WORLD_TYPE, true));
            ORDER.add(key);
            LOGGER.info("Cubic world: {} is cubic", key.location());
        }
    }

    /** Adopts a world's dimensions of the {@linkplain #CUBIC_TYPE Cubic type}, from its level stems, before its levels load. */
    public static void adopt(Registry<LevelStem> stems) {
        List<ResourceKey<Level>> cubic = new ArrayList<>();
        for (var entry : stems.entrySet())
            if (entry.getValue().type().is(CUBIC_TYPE)) cubic.add(ResourceKey.create(Registries.DIMENSION, entry.getKey().location()));
        adopt(cubic);
    }

    /**
     * Keeps one level's adoption in step with its dimension type, for a client that only sees the levels it
     * is in (a dedicated server's statics never reach it): adopted when it is of the Cubic type, dropped when
     * it was adopted and no longer is.
     */
    public static void adoptLevel(ResourceKey<Level> key, boolean cubicType) {
        CubicDimension known = DIMENSIONS.get(key);
        if (cubicType ? known != null : known == null || !known.adopted()) return;
        synchronized (Cubic.class) {
            CubicDimension dimension = DIMENSIONS.get(key);
            if (cubicType && dimension == null) {
                DIMENSIONS.put(key, new CubicDimension(key, WORLD_TYPE, true));
                ORDER.add(key);
                CLIENT_ADOPTED.add(key);
            } else if (!cubicType && dimension != null && dimension.adopted()) {
                DIMENSIONS.remove(key);
                ORDER.remove(key);
                CLIENT_ADOPTED.remove(key);
            }
        }
    }

    /**
     * Drops what a client {@linkplain #adoptLevel adopted itself} when it leaves a server, so a dedicated Cubic
     * server's overworld is not cubic for the next world. An integrated server's adoptions are its own, and go
     * when it stops.
     */
    public static synchronized void dropClientAdopted() {
        for (ResourceKey<Level> key : new ArrayList<>(CLIENT_ADOPTED)) {
            CubicDimension dimension = DIMENSIONS.get(key);
            if (dimension != null && dimension.adopted()) { DIMENSIONS.remove(key); ORDER.remove(key); }
        }
        CLIENT_ADOPTED.clear();
    }

    /**
     * Whether a world's dimensions (its level stems, by key) hold a cubic one: a stem of the {@linkplain
     * #CUBIC_TYPE Cubic type}, or one a mod {@linkplain #register registered}. Dimensions adopted for the
     * world played before count for nothing.
     */
    public static boolean cubicWorld(Collection<? extends java.util.Map.Entry<ResourceKey<LevelStem>, LevelStem>> stems) {
        for (var entry : stems) {
            if (entry.getValue().type().is(CUBIC_TYPE)) return true;
            CubicDimension dimension = DIMENSIONS.get(ResourceKey.create(Registries.DIMENSION, entry.getKey().location()));
            if (dimension != null && !dimension.adopted()) return true;
        }
        return false;
    }

    public static boolean isCubic(@Nullable ResourceKey<Level> key) { return key != null && DIMENSIONS.containsKey(key); }

    /** How a registered cubic dimension is made, or null for any other dimension. */
    public static @Nullable CubicDimension dimension(ResourceKey<Level> key) { return DIMENSIONS.get(key); }

    /** The first cubic dimension registered (else the first adopted), which the Nether's and End's portals return to; null when there is none. */
    public static @Nullable ResourceKey<Level> home() { return ORDER.isEmpty() ? null : ORDER.get(0); }

    /** Every registered cubic dimension, in the order registered. */
    public static List<ResourceKey<Level>> dimensions() { return new ArrayList<>(ORDER); }

    /**
     * Adds a test for players (or other entities) passing far above the terrain, such as on a jet
     * stream: no cubes are kept loaded or sent about them, and they don't wait for cubes to arrive.
     */
    public static void addTransit(Predicate<Entity> transit) { TRANSIT.add(transit); }

    /** Whether any {@linkplain #addTransit transit test} says an entity is passing above the terrain. */
    public static boolean inTransit(Entity entity) {
        for (Predicate<Entity> transit : TRANSIT) if (transit.test(entity)) return true;
        return false;
    }

    /**
     * Adds a listener told, on the server thread, when a player queued with
     * {@link CubicEvents#arriveWhenReady} has landed in a cubic level. The same on NeoForge and Fabric.
     */
    public static void onArrival(Consumer<ServerPlayer> listener) { ARRIVALS.add(listener); }

    static void arrived(ServerPlayer player) {
        for (Consumer<ServerPlayer> listener : ARRIVALS) listener.accept(player);
    }

    /** Opens the generator for a cubic level, which the engine calls once as the level loads. */
    static CubeGenerator open(net.minecraft.server.level.ServerLevel level, java.nio.file.Path folder) {
        CubicDimension dimension = DIMENSIONS.get(level.dimension());
        if (dimension == null) throw new IllegalStateException(level.dimension().location() + " is not a cubic dimension");
        return dimension.generators().open(level, folder);
    }
}
