package io.github.pirut.chunks3.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.Lifecycle;
import io.github.pirut.chunks3.Cubic;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Opening a world with a cubic dimension skips the experimental settings warning its data pack
 * dimension would bring up every time, as creating one does. NeoForge asks once per world and remembers the answer.
 */
@Mixin(WorldOpenFlows.class)
abstract class CubicOpenWorldMixin {
    @WrapOperation(method = "openWorldCheckWorldStemCompatibility", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/storage/WorldData;worldGenSettingsLifecycle()Lcom/mojang/serialization/Lifecycle;"))
    private Lifecycle chunks3$trustCubic(WorldData data, Operation<Lifecycle> original,
                                               LevelStorageSource.LevelStorageAccess storage, WorldStem stem, PackRepository packs, Runnable onFail) {
        var stems = stem.registries().compositeAccess().registryOrThrow(Registries.LEVEL_STEM);
        boolean cubic = Cubic.dimensions().stream().anyMatch(key -> stems.containsKey(key.location()));
        return cubic ? Lifecycle.stable() : original.call(data);
    }
}
