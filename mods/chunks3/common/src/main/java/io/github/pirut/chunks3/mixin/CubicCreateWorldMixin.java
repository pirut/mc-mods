package io.github.pirut.chunks3.mixin;

import com.mojang.serialization.Lifecycle;
import io.github.pirut.chunks3.Cubic;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Creating a Cubic world skips the experimental settings warning that vanilla raises for any overworld of a
 * dimension type of its own, when that is all that is experimental (no experimental features or other data).
 * The world's lifecycle is left as it is, so NeoForge still notes the warning as seen for the next opening.
 */
@Mixin(CreateWorldScreen.class)
abstract class CubicCreateWorldMixin {
    @ModifyArg(method = "onCreate", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/worldselection/WorldOpenFlows;confirmWorldCreation(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;Lcom/mojang/serialization/Lifecycle;Ljava/lang/Runnable;Z)V"),
            index = 4)
    private boolean chunks3$trustCubic(boolean skipWarnings) {
        if (skipWarnings) return true;
        WorldCreationContext settings = ((CreateWorldScreen) (Object) this).getUiState().getSettings();
        if (!Cubic.cubicWorld(settings.selectedDimensions().dimensions().entrySet())) return false;
        if (FeatureFlags.isExperimental(settings.dataConfiguration().enabledFeatures())) return false;
        return settings.worldgenRegistries().compositeAccess().registries()
                .allMatch(entry -> entry.key().equals(Registries.LEVEL_STEM) || entry.value().registryLifecycle() == Lifecycle.stable());
    }
}
