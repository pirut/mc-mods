package io.github.pirut.chunks3.fabric.mixin;

import io.github.pirut.chunks3.portal.CubicTravel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
//#if MC >= 1.21.2
//$$ import net.minecraft.world.level.portal.TeleportTransition;
//#else
import net.minecraft.world.level.portal.DimensionTransition;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Where a player respawns, as NeoForge's PlayerRespawnPositionEvent: right after vanilla finds it. */
@Mixin(PlayerList.class)
abstract class PlayerListRespawnMixin {
    // The inner-class name sits after '$', which the word remap does not cross, so the descriptor is written out.
    @ModifyVariable(method = "respawn", at = @At(value = "INVOKE_ASSIGN",
//#if MC >= 1.21.2
//$$            target = "Lnet/minecraft/server/level/ServerPlayer;findRespawnPositionAndUseSpawnBlock(ZLnet/minecraft/world/level/portal/TeleportTransition$PostTeleportTransition;)Lnet/minecraft/world/level/portal/TeleportTransition;"))
//$$    private TeleportTransition chunks3$respawnAt(TeleportTransition found, ServerPlayer player) {
//#else
            target = "Lnet/minecraft/server/level/ServerPlayer;findRespawnPositionAndUseSpawnBlock(ZLnet/minecraft/world/level/portal/DimensionTransition$PostDimensionTransition;)Lnet/minecraft/world/level/portal/DimensionTransition;"))
    private DimensionTransition chunks3$respawnAt(DimensionTransition found, ServerPlayer player) {
//#endif
        return CubicTravel.home(player, found);
    }
}
