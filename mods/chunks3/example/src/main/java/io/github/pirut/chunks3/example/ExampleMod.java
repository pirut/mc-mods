package io.github.pirut.chunks3.example;

import io.github.pirut.chunks3.Cubic;
import io.github.pirut.chunks3.gen.HighlandsGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.fml.common.Mod;

/**
 * Makes the dimension {@code chunks3example:highlands} cubic: hills around Y 2400 with snowy ridges
 * above Y 3000, far above vanilla's build limit. The dimension itself is a datapack file
 * ({@code data/chunks3example/dimension/highlands.json}) on a 16-block carrier dimension type.
 * In game, {@code /cubes enter} takes you there.
 */
@Mod(ExampleMod.MOD_ID)
public final class ExampleMod {
    public static final String MOD_ID = "chunks3example";
    public static final ResourceKey<Level> HIGHLANDS =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(MOD_ID, "highlands"));

    public ExampleMod() {
        Cubic.register(HIGHLANDS, (level, folder) ->
                new HighlandsGenerator(level.getSeed(), level.registryAccess().registryOrThrow(Registries.BIOME)));
    }
}
