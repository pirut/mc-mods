package io.github.pirut.chunks3.fabric.mixin;

import io.github.pirut.chunks3.fabric.Chunks3Fabric;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The engine's extra spawn rules (see {@code CubicSpawns}): a mob may spawn where vanilla's rule or ours allows it. */
@Mixin(SpawnPlacements.class)
abstract class SpawnPlacementsMixin {
    @Inject(method = "checkSpawnRules", at = @At("RETURN"), cancellable = true)
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends net.minecraft.world.entity.Entity> void chunks3$orCubicRule(EntityType<T> type, ServerLevelAccessor level, MobSpawnType spawn,
                                                                                               BlockPos pos, RandomSource random, CallbackInfoReturnable<Boolean> result) {
        if (result.getReturnValueZ()) return;
        SpawnPlacements.SpawnPredicate rule = Chunks3Fabric.SPAWN_RULES.get(type);
        if (rule != null && rule.test(type, level, spawn, pos, random)) result.setReturnValue(true);
    }
}
