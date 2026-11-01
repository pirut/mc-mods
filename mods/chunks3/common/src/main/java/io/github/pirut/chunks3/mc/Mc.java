package io.github.pirut.chunks3.mc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import com.mojang.serialization.Codec;
//#if MC >= 1.21.5
//$$ import net.minecraft.world.level.chunk.Strategy;
//#endif

import java.util.Set;

/**
 * Minecraft calls that changed between the versions Chunks3 builds for, in one place, so the rest
 * of the code reads the same on every version. See ../../../../../../../../preprocess.gradle.
 */
public final class Mc {
    private Mc() {}

    // --- Heights. 1.21.2 made the top inclusive (getMaxY) where it was exclusive (getMaxBuildHeight).

    /** One above the highest block a level holds: 1.21.1's {@code getMaxBuildHeight()}. */
    public static int maxBuildHeight(LevelHeightAccessor level) {
        //#if MC >= 1.21.2
        //$$ return level.getMaxY() + 1;
        //#else
        return level.getMaxBuildHeight();
        //#endif
    }

    /** One above the highest section: 1.21.1's {@code getMaxSection()}. */
    public static int maxSection(LevelHeightAccessor level) {
        //#if MC >= 1.21.2
        //$$ return level.getMaxSectionY() + 1;
        //#else
        return level.getMaxSection();
        //#endif
    }

    // --- Sections.

    //#if MC >= 1.21.5
    //$$ private static final Strategy<BlockState> STATES = Strategy.createForBlockStates(Block.BLOCK_STATE_REGISTRY);
    //#endif

    /** A 16³ block palette filled with {@code fill}. */
    public static PalettedContainer<BlockState> states(BlockState fill) {
        //#if MC >= 1.21.5
        //$$ return new PalettedContainer<>(fill, STATES);
        //#else
        return new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, fill, PalettedContainer.Strategy.SECTION_STATES);
        //#endif
    }

    /** A 4³ biome palette filled with {@code fill}. */
    public static PalettedContainer<Holder<Biome>> biomes(IdMap<Holder<Biome>> ids, Holder<Biome> fill) {
        //#if MC >= 1.21.5
        //$$ return new PalettedContainer<>(fill, Strategy.createForBiomes(ids));
        //#else
        return new PalettedContainer<>(ids, fill, PalettedContainer.Strategy.SECTION_BIOMES);
        //#endif
    }

    /** An empty section: air, and plains. */
    public static LevelChunkSection emptySection(Registry<Biome> biomes) {
        //#if MC >= 1.21.5
        //$$ return new LevelChunkSection(states(Blocks.AIR.defaultBlockState()), biomes(biomes.asHolderIdMap(), biomes.getOrThrow(Biomes.PLAINS)));
        //#else
        return new LevelChunkSection(biomes);
        //#endif
    }

    /** The disk codec for a section's blocks. */
    public static Codec<PalettedContainer<BlockState>> statesCodec() {
        //#if MC >= 1.21.5
        //$$ return PalettedContainer.codecRW(BlockState.CODEC, STATES, Blocks.AIR.defaultBlockState());
        //#else
        return PalettedContainer.codecRW(Block.BLOCK_STATE_REGISTRY, BlockState.CODEC, PalettedContainer.Strategy.SECTION_STATES, Blocks.AIR.defaultBlockState());
        //#endif
    }

    /** The disk codec for a section's biomes. */
    public static Codec<PalettedContainerRO<Holder<Biome>>> biomesCodec(Registry<Biome> biomes) {
        //#if MC >= 1.21.5
        //$$ return PalettedContainer.codecRO(biomes.holderByNameCodec(), Strategy.createForBiomes(biomes.asHolderIdMap()), biomes.getOrThrow(Biomes.PLAINS));
        //#else
        return PalettedContainer.codecRO(biomes.asHolderIdMap(), biomes.holderByNameCodec(), PalettedContainer.Strategy.SECTION_BIOMES, biomes.getHolderOrThrow(Biomes.PLAINS));
        //#endif
    }

    // --- Registries.

    /** The entry named {@code id}, or null. */
    public static <T> T value(Registry<T> registry, ResourceLocation id) {
        //#if MC >= 1.21.2
        //$$ return registry.getValue(id);
        //#else
        return registry.get(id);
        //#endif
    }

    // --- Entities.

    /** An entity (with its passengers) from its saved tag, or null. */
    public static net.minecraft.world.entity.Entity loadEntity(net.minecraft.nbt.CompoundTag tag, ServerLevel level) {
        //#if MC >= 1.21.2
        //$$ return net.minecraft.world.entity.EntityType.loadEntityRecursive(tag, level, net.minecraft.world.entity.EntitySpawnReason.LOAD, e -> e);
        //#else
        return net.minecraft.world.entity.EntityType.loadEntityRecursive(tag, level, e -> e);
        //#endif
    }

    /** An entity saved as a passenger would be (with its own passengers), or null if it is not saved so. */
    public static net.minecraft.nbt.CompoundTag saveEntity(net.minecraft.world.entity.Entity entity) {
        //#if MC >= 1.21.6
        //$$ var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, entity.registryAccess());
        //$$ return entity.saveAsPassenger(out) ? out.buildResult() : null;
        //#else
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        return entity.saveAsPassenger(tag) ? tag : null;
        //#endif
    }

    // --- Players and the server.

    public static MinecraftServer server(ServerPlayer player) {
        //#if MC >= 1.21.9
        //$$ return player.level().getServer();
        //#else
        return player.server;
        //#endif
    }

    /** Moves a player, to another level too, facing {@code yaw} and {@code pitch}. */
    public static void teleport(ServerPlayer player, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        //#if MC >= 1.21.2
        //$$ player.teleportTo(level, x, y, z, Set.of(), yaw, pitch, true);
        //#else
        player.teleportTo(level, x, y, z, yaw, pitch);
        //#endif
    }

    /** Whether a player may use operator commands (permission level 2). */
    public static boolean isOperator(ServerPlayer player) {
        //#if MC >= 1.21.11
        //$$ return player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
        //#else
        return player.hasPermissions(2);
        //#endif
    }

    /** Whether a player owns a singleplayer (or LAN) world. */
    public static boolean isSingleplayerOwner(MinecraftServer server, ServerPlayer player) {
        //#if MC >= 1.21.9
        //$$ return server.isSingleplayerOwner(player.nameAndId());
        //#else
        return server.isSingleplayerOwner(player.getGameProfile());
        //#endif
    }

    /** The spawnRadius (respawn radius) game rule. */
    public static int spawnRadius(ServerLevel level) {
        //#if MC >= 1.21.11
        //$$ return level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.RESPAWN_RADIUS);
        //#else
        return level.getServer().getSpawnRadius(level);
        //#endif
    }

    /** The randomTickSpeed game rule. */
    public static int randomTickSpeed(ServerLevel level) {
        //#if MC >= 1.21.11
        //$$ return level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED);
        //#else
        return level.getGameRules().getInt(net.minecraft.world.level.GameRules.RULE_RANDOMTICKING);
        //#endif
    }

    /** Whether mobs may spawn in a chunk (it is in range of a player and ticking). */
    public static boolean canSpawnIn(ServerLevel level, net.minecraft.world.level.ChunkPos chunk) {
        //#if MC >= 1.21.11
        //$$ return level.canSpawnEntitiesInChunk(chunk);
        //#else
        return level.isNaturalSpawningAllowed(chunk);
        //#endif
    }

    /** Where the world spawn is. */
    public static BlockPos sharedSpawn(ServerLevel level) {
        //#if MC >= 1.21.9
        //$$ return level.getRespawnData().pos();
        //#else
        return level.getSharedSpawnPos();
        //#endif
    }
}
