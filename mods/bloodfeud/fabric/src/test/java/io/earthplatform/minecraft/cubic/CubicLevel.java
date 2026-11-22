package io.earthplatform.minecraft.cubic;

import net.minecraft.world.level.Level;

/**
 * Test stand-ins for the parts of Earth Platform (build 76) that Blood Feud reaches by reflection, with the same
 * names and signatures. Tests only: none of this is in the mod.
 */
public interface CubicLevel {
    CubeWorld cubes();

    static CubeWorld of(Level level) {
        return ((CubicLevel) level).cubes();
    }

    static CubeWorld at(Level level, int blockX, int blockZ) {
        return of(level);
    }
}
