package io.github.pirut.chunks3.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.pirut.chunks3.CubicEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.PlayerRespawnLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A new world whose overworld is cubic (the Cubic world type) gets its spawn on the cubic ground, found by
 * the engine, where vanilla's search of the empty carrier chunks finds none. Only that search is replaced:
 * NeoForge's CreateSpawnPosition event still comes first, and the bonus chest is still placed at the spawn.
 */
@Mixin(MinecraftServer.class)
abstract class CubicWorldSpawnMixin {
    @WrapOperation(method = "setInitialSpawn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/PlayerRespawnLogic;getSpawnPosInChunk(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/ChunkPos;)Lnet/minecraft/core/BlockPos;"))
    private static BlockPos chunks3$cubicSpawn(ServerLevel level, ChunkPos chunk, Operation<BlockPos> original) {
        BlockPos cubic = CubicEvents.initialSpawn(level);
        return cubic != null ? cubic : original.call(level, chunk);
    }
}
