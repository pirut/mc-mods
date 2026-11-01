package io.github.pirut.chunks3.fabric.mixin;

import io.github.pirut.chunks3.CubicEvents;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** NeoForge's level save event, where NeoForge fires it. */
@Mixin(ServerLevel.class)
abstract class ServerLevelHooksMixin {
    /** After the chunks and entities are written, as NeoForge's LevelEvent.Save. */
    @Inject(method = "save", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;autoSave()V", shift = At.Shift.AFTER))
    private void chunks3$savedAuto(CallbackInfo info) { CubicEvents.save((ServerLevel) (Object) this); }

    @Inject(method = "save", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;saveAll()V", shift = At.Shift.AFTER))
    private void chunks3$savedAll(CallbackInfo info) { CubicEvents.save((ServerLevel) (Object) this); }

}
