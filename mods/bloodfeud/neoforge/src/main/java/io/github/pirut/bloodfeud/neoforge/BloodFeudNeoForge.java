package io.github.pirut.bloodfeud.neoforge;

import io.github.pirut.bloodfeud.BloodFeud;
import net.neoforged.fml.common.Mod;

/** Everything else comes through Pirut Lib. */
@Mod(BloodFeud.MOD_ID)
public final class BloodFeudNeoForge {
    public BloodFeudNeoForge() {
        BloodFeud.init();
    }
}
