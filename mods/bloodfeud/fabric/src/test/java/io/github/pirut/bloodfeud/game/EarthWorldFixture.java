package io.github.pirut.bloodfeud.game;

import io.earthplatform.minecraft.cubic.CubeWorld;
import io.earthplatform.minecraft.cubic.CubicLevel;
import io.earthplatform.minecraft.cubic.ServerCubeWorld;
import io.earthplatform.minecraft.cubic.gen.CubeGenerator;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A level that behaves like Earth Platform's cubic one: every carrier chunk is loaded, the build range says 16 blocks,
 * and blocks live in cubes that read as void air and ignore writes until they are loaded. Made without running any
 * constructor, so it needs no server.
 */
public final class EarthWorldFixture {
    public static final ResourceKey<Level> CUBIC =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("earthplatform", "cubic"));
    /** About 12.8 million blocks out, as eastern Australia is. */
    public static final int X = 12_770_013, Z = -3_591_395;

    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @SuppressWarnings("unchecked")
    private static <T> T allocate(Class<T> type) {
        try {
            Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (T) ((sun.misc.Unsafe) f.get(null)).allocateInstance(type);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    public static FakeLevel level(int ground, int minBuild, int maxBuild) {
        FakeLevel level = allocate(FakeLevel.class);
        level.ground = ground;
        level.minBuild = minBuild;
        level.maxBuild = maxBuild;
        level.blocks = new HashMap<>();
        level.blockEntities = new HashMap<>();
        level.unknownHeightReads = new AtomicInteger();
        level.serverThreadMapReads = new AtomicInteger();
        level.key = CUBIC;
        Thread serverThread = Thread.currentThread();
        level.cubes = new ServerCubeWorld(new CubeGenerator() {
            @Override
            public int surfaceY(int x, int z) {
                if (Thread.currentThread() == serverThread) level.serverThreadMapReads.incrementAndGet();
                return ground;
            }
        });
        level.chunks = allocate(FakeChunks.class);
        level.chunks.chunk = allocate(LevelChunk.class);
        level.type = new DimensionType(OptionalLong.empty(), true, false, false, true, 1.0, true, false,
                0, 16, 16, BlockTags.INFINIBURN_OVERWORLD, BuiltinDimensionTypes.OVERWORLD_EFFECTS, 0f,
                new DimensionType.MonsterSettings(false, false, ConstantInt.of(0), 0));
        FakeServer server = allocate(FakeServer.class);
        server.level = level;
        level.fakeServer = server;
        return level;
    }

    public static final class FakeLevel extends ServerLevel implements CubicLevel {
        public ServerCubeWorld cubes;
        public Map<BlockPos, BlockState> blocks;
        public Map<BlockPos, BlockEntity> blockEntities;
        public AtomicInteger unknownHeightReads, serverThreadMapReads;
        public ResourceKey<Level> key;
        int ground, minBuild, maxBuild;
        FakeChunks chunks;
        DimensionType type;
        FakeServer fakeServer;

        private FakeLevel() {
            super(null, null, null, null, null, null, null, false, 0, null, false, null);
        }

        @Override
        public CubeWorld cubes() {
            return cubes;
        }

        /** Loads every cube in a box round a point. */
        public void load(BlockPos at, int side, int down, int up) {
            for (int x = (at.getX() - side) >> 4; x <= (at.getX() + side) >> 4; x++) {
                for (int y = (at.getY() - down) >> 4; y <= (at.getY() + up) >> 4; y++) {
                    for (int z = (at.getZ() - side) >> 4; z <= (at.getZ() + side) >> 4; z++) cubes.load(x << 4, y << 4, z << 4);
                }
            }
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            if (cubes != null && !cubes.isLoaded(pos)) return Blocks.VOID_AIR.defaultBlockState();
            BlockState set = blocks.get(pos);
            if (set != null) return set;
            // Gravel, because block tags (stone, dirt) are empty without data packs.
            return (pos.getY() <= ground ? Blocks.GRAVEL : Blocks.AIR).defaultBlockState();
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return getBlockState(pos).getFluidState();
        }

        @Override
        public boolean setBlock(BlockPos pos, BlockState state, int flags) {
            if (cubes != null && !cubes.isLoaded(pos)) return false;
            BlockPos at = pos.immutable();
            blocks.put(at, state);
            BlockEntity old = blockEntities.get(at);
            if (old != null && !old.getType().isValid(state)) blockEntities.remove(at);
            if (state.getBlock() instanceof net.minecraft.world.level.block.EntityBlock maker && !blockEntities.containsKey(at)) {
                BlockEntity made = maker.newBlockEntity(at, state);
                if (made != null) {
                    made.setLevel(this);
                    blockEntities.put(at, made);
                }
            }
            return true;
        }

        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return blockEntities.get(pos);
        }

        @Override
        public void blockEntityChanged(BlockPos pos) {
        }

        @Override
        public void updateNeighbourForOutputSignal(BlockPos pos, net.minecraft.world.level.block.Block block) {
        }

        @Override
        public int getHeight(Heightmap.Types type, int x, int z) {
            if (cubes != null && cubes.columnTops(x >> 4, z >> 4) == null) unknownHeightReads.incrementAndGet();
            return ground + 1;
        }

        @Override
        public int getMinBuildHeight() {
            return minBuild;
        }

        @Override
        public int getMaxBuildHeight() {
            return maxBuild;
        }

        @Override
        public int getRawBrightness(BlockPos pos, int darken) {
            return 0;
        }

        @Override
        public DimensionType dimensionType() {
            return type;
        }

        @Override
        public ResourceKey<Level> dimension() {
            return key;
        }

        @Override
        public ServerChunkCache getChunkSource() {
            return chunks;
        }

        @Override
        public MinecraftServer getServer() {
            return fakeServer;
        }

        @Override
        public BlockPos getSharedSpawnPos() {
            return BlockPos.ZERO;
        }

        @Override
        public boolean hasChunk(int x, int z) {
            return true;
        }

        @Override
        public LevelChunk getChunk(int x, int z) {
            return chunks.chunk;
        }
    }

    static final class FakeChunks extends ServerChunkCache {
        LevelChunk chunk;

        private FakeChunks() {
            super(null, null, null, null, null, null, 0, 0, false, null, null, null);
        }

        @Override
        public LevelChunk getChunkNow(int x, int z) {
            return chunk;
        }
    }

    static final class FakeServer extends DedicatedServer {
        FakeLevel level;

        private FakeServer() {
            super(null, null, null, null, null, null, null, null);
        }

        @Override
        public ServerLevel getLevel(ResourceKey<Level> dimension) {
            return level;
        }
    }
}
