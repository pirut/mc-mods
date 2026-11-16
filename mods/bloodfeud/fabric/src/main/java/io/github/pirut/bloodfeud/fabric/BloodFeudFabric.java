package io.github.pirut.bloodfeud.fabric;

import io.github.pirut.bloodfeud.BloodFeud;
import net.fabricmc.api.ModInitializer;

/** Everything else comes through Pirut Lib. */
public final class BloodFeudFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        BloodFeud.init();
    }
}
