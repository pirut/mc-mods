package io.github.pirut.chunks3.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.pirut.chunks3.Cubic;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The Twilight Forest only lets portals form in its origin dimension (the Overworld by default). In an
 * Earth world the player lives in the Earth dimension, so portals may form there too.
 */
@Pseudo
@Mixin(targets = "twilightforest.events.TFTickHandler", remap = false)
abstract class TwilightPortalCreationMixin {
    @WrapOperation(method = "checkForPortalCreation", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/ResourceLocation;equals(Ljava/lang/Object;)Z", ordinal = 0))
    private static boolean chunks3$earthIsOrigin(ResourceLocation dimension, Object origin, Operation<Boolean> original,
                                                     ServerPlayer player, Level level, float range) {
        return Cubic.isCubic(level.dimension()) || original.call(dimension, origin);
    }
}
