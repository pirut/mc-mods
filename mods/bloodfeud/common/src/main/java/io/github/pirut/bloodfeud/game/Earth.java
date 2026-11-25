package io.github.pirut.bloodfeud.game;

import io.github.pirut.bloodfeud.BloodFeud;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Everything Blood Feud knows about Earth Platform, which is optional and only reached by reflection.
 * <p>
 * The Earth keeps its blocks in 16-block cubes behind empty carrier chunks, so a loaded chunk says nothing about its
 * blocks: an unloaded cube reads as void air and ignores writes. Cubes that no player watches are dropped every 40
 * ticks and can't be held, so callers look again each tick and only act on ticks where what they touch is loaded.
 * Anything that reads map data may download it, so that only ever runs off the server thread.
 * <p>
 * If any of this is missing or has changed, it logs once and nothing in the Earth counts as loaded, so nothing is
 * built there. In every other dimension every answer is the vanilla one.
 */
public final class Earth {
    private static final ResourceLocation CUBIC = ResourceLocation.fromNamespaceAndPath("earthplatform", "cubic");
    private static final String PACKAGE = "io.earthplatform.minecraft.cubic.";
    /** How long a column's map data is trusted to still be in the generator's memory. */
    private static final long WARM_NANOS = TimeUnit.SECONDS.toNanos(30);
    /** How long to leave a column whose map data couldn't be had. */
    private static final long WARM_RETRY_NANOS = TimeUnit.SECONDS.toNanos(5);
    private static final int MAX_WARM_COLUMNS = 4096;

    private static Api api;
    private static boolean failed;
    private static final Map<Long, Warm> warmColumns = new HashMap<>();
    private static Object warmWorld;

    private record Warm(CompletableFuture<Void> data, long since) {
    }

    /** The parts of Earth Platform this uses. */
    private record Api(Method cubesOf, Method cubesAt, Method isLoaded, Method cubeAt, Method columnTops,
                       Method blockEntities, Method readyAround, Method generator, Method surfaceY, Method waterY,
                       Method skyTopY, Method arrival, Field session, Field sessionServer, Field sessionLanding,
                       int minY, int maxY, Method keepAround, Method release, int maxWatchRadius, int verticalRadius) {
        static Api find() throws ReflectiveOperationException {
            Class<?> cubicLevel = type("CubicLevel");
            Class<?> world = type("CubeWorld");
            Class<?> serverWorld = type("ServerCubeWorld");
            Class<?> generator = type("gen.CubeGenerator");
            Field session = type("play.EarthWorlds").getDeclaredField("session");
            Field server = session.getType().getDeclaredField("server");
            Field landing = session.getType().getDeclaredField("landing");
            session.setAccessible(true);
            server.setAccessible(true);
            landing.setAccessible(true);
            return new Api(
                    cubicLevel.getMethod("of", Level.class),
                    cubicLevel.getMethod("at", Level.class, int.class, int.class),
                    world.getMethod("isLoaded", BlockPos.class),
                    world.getMethod("cubeAtBlock", int.class, int.class, int.class),
                    world.getMethod("columnTops", int.class, int.class),
                    type("Cube").getMethod("blockEntities"),
                    serverWorld.getMethod("readyAround", BlockPos.class),
                    serverWorld.getMethod("generator"),
                    generator.getMethod("surfaceY", int.class, int.class),
                    generator.getMethod("waterY", int.class, int.class),
                    generator.getMethod("skyTopY", int.class, int.class),
                    generator.getMethod("arrival"),
                    session, server, landing,
                    world.getField("MIN_Y").getInt(null),
                    world.getField("MAX_Y").getInt(null),
                    optional(serverWorld, "keepAround", Object.class, BlockPos.class),
                    optional(serverWorld, "release", Object.class),
                    constant("Cubic", "MAX_HORIZONTAL_RADIUS", 12),
                    constant("Cubic", "VERTICAL_RADIUS", 6));
        }

        /** A method newer builds have (build 78 keeps cubes for a holder), or null. */
        private static Method optional(Class<?> owner, String name, Class<?>... args) {
            try {
                return owner.getMethod(name, args);
            } catch (NoSuchMethodException e) {
                return null;
            }
        }

        private static int constant(String owner, String name, int fallback) {
            try {
                return type(owner).getField(name).getInt(null);
            } catch (ReflectiveOperationException | RuntimeException e) {
                return fallback;
            }
        }

        private static Class<?> type(String name) throws ClassNotFoundException {
            return Class.forName(PACKAGE + name, false, Earth.class.getClassLoader());
        }
    }

    private Earth() {
    }

    /** Whether this is the Earth's dimension, whether or not Blood Feud can work with it. */
    public static boolean is(Level level) {
        return is(level.dimension());
    }

    public static boolean is(ResourceKey<Level> dimension) {
        return dimension != null && CUBIC.equals(dimension.location());
    }

    static boolean is(String dimension) {
        return CUBIC.toString().equals(dimension);
    }

    private static Api api() {
        if (api == null && !failed) {
            try {
                api = Api.find();
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                fail(e);
            }
        }
        return failed ? null : api;
    }

    private static void fail(Throwable why) {
        if (failed) return;
        failed = true;
        BloodFeud.LOG.warn("Blood Feud can't work with this version of Earth Platform, so it builds nothing on the Earth", why);
    }

    /** Whether settlements may be built in this level at all. */
    public static boolean canBuildIn(Level level) {
        return !is(level) || api() != null;
    }

    /** The lowest Y a block can be at. */
    public static int bottom(Level level) {
        Api a = is(level) ? api() : null;
        return a == null ? level.getMinBuildHeight() : a.minY();
    }

    /** One above the highest Y a block can be at. */
    public static int top(Level level) {
        Api a = is(level) ? api() : null;
        return a == null ? level.getMaxBuildHeight() : a.maxY() + 1;
    }

    /**
     * Whether the block at {@code pos} can be read and written. Outside the Earth this is always true: callers check
     * the chunk themselves.
     */
    public static boolean isLoaded(Level level, BlockPos pos) {
        if (!is(level)) return true;
        Api a = api();
        if (a == null || pos.getY() < a.minY() || pos.getY() > a.maxY()) return false;
        try {
            Object cubes = a.cubesAt().invoke(null, level, pos.getX(), pos.getZ());
            return cubes != null && (boolean) a.isLoaded().invoke(cubes, pos);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            fail(e);
            return false;
        }
    }

    /**
     * Whether every cube in the box is loaded on this tick, without asking for any. Only cubes a player keeps loaded
     * pass, and they hold real blocks: an unloaded cube's void air never counts as ground here.
     */
    public static boolean loaded(ServerLevel level, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (!is(level)) return true;
        Api a = api();
        if (a == null || minY < a.minY() || maxY > a.maxY()) return false;
        try {
            Object cubes = a.cubesAt().invoke(null, level, minX, minZ);
            if (cubes == null || a.cubesAt().invoke(null, level, maxX, maxZ) != cubes) return false;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
                for (int cy = minY >> 4; cy <= maxY >> 4; cy++) {
                    for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                        if (!(boolean) a.isLoaded().invoke(cubes, pos.set(cx << 4, cy << 4, cz << 4))) return false;
                    }
                }
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            fail(e);
            return false;
        }
    }

    /**
     * Whether every cube in the box is loaded on this tick. If not, asks the Earth's workers for them without waiting,
     * once the map data their columns need has been read off the server thread: adopting a cube reads its column's
     * height on the server thread, which would otherwise wait for a download.
     */
    public static boolean ready(ServerLevel level, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (!is(level)) return true;
        Api a = api();
        if (a == null || minY < a.minY() || maxY > a.maxY()) return false;
        try {
            Object cubes = a.cubesAt().invoke(null, level, minX, minZ);
            if (cubes == null || a.cubesAt().invoke(null, level, maxX, maxZ) != cubes) return false;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            boolean loaded = true;
            for (int cx = minX >> 4; cx <= maxX >> 4 && loaded; cx++) {
                for (int cy = minY >> 4; cy <= maxY >> 4 && loaded; cy++) {
                    for (int cz = minZ >> 4; cz <= maxZ >> 4 && loaded; cz++) {
                        loaded = (boolean) a.isLoaded().invoke(cubes, pos.set(cx << 4, cy << 4, cz << 4));
                    }
                }
            }
            if (loaded) return true;

            List<Integer> xs = requestCentres(minX >> 4, maxX >> 4);
            List<Integer> ys = requestCentres(minY >> 4, maxY >> 4);
            List<Integer> zs = requestCentres(minZ >> 4, maxZ >> 4);
            // Lighting and random ticks read one column past the cubes asked for, hence 2 around each centre.
            if (!warm(a, cubes, xs.get(0) - 2, zs.get(0) - 2, xs.get(xs.size() - 1) + 2, zs.get(zs.size() - 1) + 2)) {
                return false;
            }
            for (int cx : xs) {
                for (int cy : ys) {
                    for (int cz : zs) a.readyAround().invoke(cubes, new BlockPos(cx << 4, cy << 4, cz << 4));
                }
            }
            return false;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            fail(e);
            return false;
        }
    }

    /** Centres of the 3-wide requests that cover cubes {@code min..max}, with no overhang once the range is 3 wide. */
    private static List<Integer> requestCentres(int min, int max) {
        List<Integer> centres = new ArrayList<>();
        for (int c = min + 1; ; c += 3) {
            if (c + 1 >= max) {
                centres.add(Math.max(min + 1, max - 1));
                return centres;
            }
            centres.add(c);
        }
    }

    /** True once the height of every block in these columns is in the generator's memory. */
    private static boolean warm(Api a, Object cubes, int minCx, int minCz, int maxCx, int maxCz)
            throws ReflectiveOperationException {
        if (warmWorld != cubes) {
            warmColumns.clear();
            warmWorld = cubes;
        }
        long now = System.nanoTime();
        boolean warm = true;
        List<int[]> cold = new ArrayList<>();
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                Warm w = warmColumns.get(column(cx, cz));
                if (w == null) {
                    warm = false;
                    cold.add(new int[]{cx, cz});
                } else if (!w.data().isDone()) {
                    warm = false;
                } else {
                    boolean bad = w.data().isCompletedExceptionally();
                    if (bad) warm = false;
                    if (now - w.since() > (bad ? WARM_RETRY_NANOS : WARM_NANOS)) {
                        warm = false;
                        cold.add(new int[]{cx, cz});
                    }
                }
            }
        }
        if (cold.isEmpty()) return warm;
        if (warmColumns.size() > MAX_WARM_COLUMNS) {
            warmColumns.values().removeIf(w -> w.data().isDone() && now - w.since() > WARM_RETRY_NANOS);
        }
        Object generator = a.generator().invoke(cubes);
        CompletableFuture<Void> data = CompletableFuture.runAsync(() -> {
            for (int[] c : cold) {
                for (int i = 0; i < 256; i++) mapData(a.skyTopY(), generator, (c[0] << 4) + (i & 15), (c[1] << 4) + (i >> 4));
            }
        }, Util.ioPool());
        Warm w = new Warm(data, now);
        for (int[] c : cold) warmColumns.put(column(c[0], c[1]), w);
        return warm;
    }

    private static long column(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    /** Off the server thread only: may download. Map data that can't be had yet throws, and is tried again later. */
    private static int mapData(Method method, Object generator, int x, int z) {
        try {
            return (int) method.invoke(generator, x, z);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("No Earth map data at " + x + ", " + z, e);
        }
    }

    /**
     * Reads the generated height of each {@code {x, z}} column off the server thread, as {@code {ground, water}}.
     * Null if the Earth can't be read.
     */
    public static CompletableFuture<int[][]> survey(ServerLevel level, List<int[]> columns) {
        return survey(level, columns, () -> false);
    }

    /**
     * As {@link #survey(ServerLevel, List)}, stopping its map reads as soon as {@code cancelled} says so: the column
     * after that is never read, and the future completes with a cancellation.
     */
    public static CompletableFuture<int[][]> survey(ServerLevel level, List<int[]> columns, java.util.function.BooleanSupplier cancelled) {
        Api a = api();
        if (a == null) return null;
        try {
            Object cubes = a.cubesOf().invoke(null, level);
            if (cubes == null) return null;
            Object generator = a.generator().invoke(cubes);
            return CompletableFuture.supplyAsync(() -> {
                int[][] out = new int[columns.size()][];
                for (int i = 0; i < out.length; i++) {
                    if (cancelled.getAsBoolean()) throw new java.util.concurrent.CancellationException("survey cancelled");
                    int[] c = columns.get(i);
                    out[i] = new int[]{mapData(a.surfaceY(), generator, c[0], c[1]), mapData(a.waterY(), generator, c[0], c[1])};
                }
                return out;
            }, Util.ioPool());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            fail(e);
            return null;
        }
    }

    /**
     * Whether {@code getHeight} can answer for this column from memory. For any other column on the Earth it reads map
     * data on the calling thread, which can hang the server on a download.
     */
    public static boolean knowsHeight(Level level, int x, int z) {
        if (!is(level)) return true;
        Api a = api();
        if (a == null) return false;
        try {
            Object cubes = a.cubesAt().invoke(null, level, x, z);
            return cubes != null && a.columnTops().invoke(cubes, x >> 4, z >> 4) != null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            fail(e);
            return false;
        }
    }

    /**
     * The column new players arrive around: the level's shared spawn means nothing on the Earth. Null while Earth
     * Platform is still working it out, or if it can't be read.
     */
    public static BlockPos arrival(ServerLevel level) {
        Api a = api();
        if (a == null) return null;
        try {
            int[] at;
            Object session = a.session().get(null);
            if (session != null && a.sessionServer().get(session) == level.getServer()) {
                at = (int[]) a.sessionLanding().get(session);
            } else {
                // Not an Earth world, only its dimension: /cubes enter lands around the generator's own start.
                Object cubes = a.cubesOf().invoke(null, level);
                at = cubes == null ? null : (int[]) a.arrival().invoke(a.generator().invoke(cubes));
            }
            return at == null ? null : new BlockPos(at[0], 0, at[1]);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            fail(e);
            return null;
        }
    }

    /**
     * Cubes kept for each owner: the world and release method they were kept with, and how many holders (one per
     * tile) it took. Releasing uses these, so a hold is let go even after Earth Platform stopped answering.
     */
    private static final Map<Object, Kept> kept = new HashMap<>();

    private record Kept(Object cubes, Method release, int tiles) {
    }

    /** One tile of an owner's kept box: Earth Platform keeps 3x3 columns, one cube below to three above, per holder. */
    private record Hold(Object owner, int tile) {
    }

    /** Whether this Earth Platform can keep unwatched cubes loaded (build 78 and later). */
    public static boolean canKeep(Level level) {
        Api a = is(level) ? api() : null;
        return a != null && a.keepAround() != null && a.release() != null;
    }

    /**
     * Keeps every cube of the box loaded for {@code owner} though no player is near, until {@link #release}d. It
     * doesn't load them: {@link #ready} asks for them. False where this Earth Platform can't keep cubes. If keeping
     * fails part way, what was kept is let go before this returns.
     */
    public static boolean keep(ServerLevel level, Object owner, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (!canKeep(level)) return false;
        Api a = api();
        Object cubes;
        try {
            cubes = a.cubesOf().invoke(null, level);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            fail(e);
            return false;
        }
        if (cubes == null) return false;
        release(owner); // in place of whatever it kept before
        int tile = 0;
        boolean done = false;
        try {
            // Each holder keeps cubes cx-1..cx+1, cy-1..cy+3, cz-1..cz+1 round the cube of the block it is given.
            for (int cx = (minX >> 4) + 1; ; cx += 3) {
                for (int cy = (minY >> 4) + 1; ; cy += 5) {
                    for (int cz = (minZ >> 4) + 1; ; cz += 3) {
                        kept.put(owner, new Kept(cubes, a.release(), tile + 1)); // counted before it is taken
                        a.keepAround().invoke(cubes, new Hold(owner, tile), new BlockPos(cx << 4, cy << 4, cz << 4));
                        tile++;
                        if (cz + 1 >= maxZ >> 4) break;
                    }
                    if (cy + 3 >= maxY >> 4) break;
                }
                if (cx + 1 >= maxX >> 4) break;
            }
            done = true;
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            fail(e);
            return false;
        } finally {
            if (!done) release(owner);
        }
    }

    /**
     * Lets go of every cube {@link #keep} kept for {@code owner}; the Earth unloads them once no player is near. Works
     * whatever else failed, with the handles they were kept with.
     */
    public static void release(Object owner) {
        Kept k = kept.remove(owner);
        if (k == null) return;
        for (int i = 0; i < k.tiles(); i++) {
            try {
                k.release().invoke(k.cubes(), new Hold(owner, i));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                BloodFeud.LOG.warn("Blood Feud couldn't let go of a kept Earth cube; it goes when the world closes", e);
            }
        }
    }

    /** How many owners have cubes kept. */
    public static int keptOwners() {
        return kept.size();
    }

    /** Whether {@code owner} has cubes kept. */
    public static boolean keeps(Object owner) {
        return kept.containsKey(owner);
    }

    /** How far (in cubes, across) the Earth sends cubes round a player at this view distance; and how far up and down. */
    public static int watchRadius(Level level, int viewDistance) {
        Api a = is(level) ? api() : null;
        return Math.max(2, Math.min(a == null ? 12 : a.maxWatchRadius(), viewDistance));
    }

    public static int watchVertical(Level level) {
        Api a = is(level) ? api() : null;
        return a == null ? 6 : a.verticalRadius();
    }

    /** Block entities of the loaded cubes around {@code at}. The carrier chunk holds none. */
    static List<BlockEntity> blockEntitiesAround(ServerLevel level, BlockPos at) {
        Api a = api();
        if (a == null) return List.of();
        List<BlockEntity> found = new ArrayList<>();
        try {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int x = at.getX() + dx * 16, z = at.getZ() + dz * 16;
                    Object cubes = a.cubesAt().invoke(null, level, x, z);
                    if (cubes == null) continue;
                    for (int dy = -2; dy <= 2; dy++) {
                        Object cube = a.cubeAt().invoke(cubes, x, at.getY() + dy * 16, z);
                        if (cube == null) continue;
                        for (Object be : ((Map<?, ?>) a.blockEntities().invoke(cube)).values()) found.add((BlockEntity) be);
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            fail(e);
        }
        return found;
    }

    /** For tests: forget a failure and look Earth Platform up again. */
    static void reset() {
        api = null;
        failed = false;
        for (Object owner : new ArrayList<>(kept.keySet())) release(owner);
        warmColumns.clear();
        warmWorld = null;
    }

    /** The server stopped: let go of its world. */
    static void stopped() {
        warmColumns.clear();
        warmWorld = null;
        for (Object owner : new ArrayList<>(kept.keySet())) release(owner);
    }
}
