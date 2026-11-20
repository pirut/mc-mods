package io.github.pirut.chunks3;

import io.github.pirut.chunks3.gen.CubeGenerator;
import java.nio.file.Path;
import java.util.function.IntSupplier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * A dimension registered with {@link Cubic#register}, or adopted because its dimension type is the Cubic
 * world type's ({@link Cubic#CUBIC_TYPE}): where its cubes come from, and its options.
 */
public final class CubicDimension {
    /** Opens a level's generator. {@code folder} is the dimension's own save folder, for the generator to pin its settings in. */
    @FunctionalInterface
    public interface Generators {
        CubeGenerator open(ServerLevel level, Path folder);
    }

    private final ResourceKey<Level> key;
    private final Generators generators;
    private final boolean adopted;
    private volatile IntSupplier cloudHeight = () -> 0;

    CubicDimension(ResourceKey<Level> key, Generators generators, boolean adopted) {
        this.key = key;
        this.generators = generators;
        this.adopted = adopted;
    }

    public ResourceKey<Level> key() { return key; }
    public Generators generators() { return generators; }
    /** Whether the dimension is cubic for its dimension type alone, for the world being played, rather than registered by a mod. */
    public boolean adopted() { return adopted; }

    /**
     * Where shader packs' clouds float, as a Y read each frame: 0 (the default) leaves the pack's
     * own height. Client side only.
     */
    public CubicDimension cloudHeight(IntSupplier height) { this.cloudHeight = height; return this; }
    public int cloudHeight() { return cloudHeight.getAsInt(); }
}
