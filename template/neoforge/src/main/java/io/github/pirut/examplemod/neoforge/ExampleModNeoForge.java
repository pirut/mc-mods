package io.github.pirut.examplemod.neoforge;

import io.github.pirut.examplemod.ExampleMod;
import net.neoforged.fml.common.Mod;

@Mod(ExampleMod.MOD_ID)
public final class ExampleModNeoForge {
    public ExampleModNeoForge() {
        ExampleMod.init("neoforge");
    }
}
