package io.github.pirut.chunks3.mixin;

import io.github.pirut.chunks3.CubicEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A new world whose overworld is cubic (the Cubic world type) gets its spawn on the cubic ground, found by
 * the engine, rather than where vanilla's search of the empty carrier chunks would leave it, deep in the rock.
 */
@Mixin(MinecraftServer.class)
abstract class CubicWorldSpawnMixin {
    @Inject(method = "setInitialSpawn", at = @At("HEAD"), cancellable = true)
    private static void chunks3$cubicSpawn(ServerLevel level, ServerLevelData data, boolean bonusChest, boolean debug, CallbackInfo info) {
        if (!debug && CubicEvents.initialSpawn(level, data)) info.cancel();
    }
}
