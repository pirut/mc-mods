package io.github.pirut.chunks3.mixin;

import io.github.pirut.chunks3.CubeKey;
import io.github.pirut.chunks3.CubeWorld;
import io.github.pirut.chunks3.CubicLevel;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Biomes come from cubes, and a box is loaded only when every cube it touches is. */
@Mixin(LevelReader.class)
interface CubicLevelReaderMixin {
    @Inject(method = "getNoiseBiome(III)Lnet/minecraft/core/Holder;", at = @At("HEAD"), cancellable = true)
    private void chunks3$cubeBiome(int x, int y, int z, CallbackInfoReturnable<Holder<Biome>> result) {
        if ((Object) this instanceof Level level && CubicLevel.at(level, x << 2, z << 2) instanceof CubeWorld cubes) {
            Holder<Biome> biome = cubes.getNoiseBiome(x, y, z);
            if (biome != null) result.setReturnValue(biome);
        }
    }

    @Inject(method = "hasChunksAt(IIIIII)Z", at = @At("HEAD"), cancellable = true)
    private void chunks3$cubeBox(int fromX, int fromY, int fromZ, int toX, int toY, int toZ, CallbackInfoReturnable<Boolean> result) {
        if (!((Object) this instanceof Level level) || !(CubicLevel.at(level, fromX, fromZ) instanceof CubeWorld cubes)) return;
        for (int cy = fromY >> 4; cy <= toY >> 4; cy++) for (int cz = fromZ >> 4; cz <= toZ >> 4; cz++) for (int cx = fromX >> 4; cx <= toX >> 4; cx++) {
            if (cubes.cube(CubeKey.pack(cx, cy, cz)) == null) { result.setReturnValue(false); return; }
        }
        result.setReturnValue(true);
    }
}
