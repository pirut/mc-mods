package io.github.pirut.chunks3.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.pirut.chunks3.CubicWeather;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Falling rain, its splashes and its sound match the server's rain at real mountain heights (see
 * CubicWeather). Optional, so a mod that draws its own weather still loads.
 */
//#if MC >= 1.21.11
//$$ @Mixin(net.minecraft.client.renderer.WeatherEffectRenderer.class)
//$$ abstract class CubicRainRenderMixin {
//$$     @WrapOperation(method = "getPrecipitationAt", require = 0, at = @At(value = "INVOKE",
//$$             target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;I)Lnet/minecraft/world/level/biome/Biome$Precipitation;"))
//$$     private Biome.Precipitation chunks3$rain(Biome biome, BlockPos pos, int seaLevel, Operation<Biome.Precipitation> original,
//$$                                              @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) net.minecraft.world.level.Level level) {
//$$         return CubicWeather.precipitation(level, biome, original.call(biome, pos, seaLevel));
//$$     }
//$$ }
//#else
@Mixin(LevelRenderer.class)
abstract class CubicRainRenderMixin {
    @Shadow private ClientLevel level;

    @WrapOperation(method = {"renderSnowAndRain", "tickRain"}, require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"))
    private Biome.Precipitation chunks3$rain(Biome biome, BlockPos pos, Operation<Biome.Precipitation> original) {
        return CubicWeather.precipitation(level, biome, original.call(biome, pos));
    }
}
//#endif
