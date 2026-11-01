package io.github.pirut.chunks3.portal;

import io.github.pirut.chunks3.Cubic;
import io.github.pirut.chunks3.CubicEvents;
import io.github.pirut.chunks3.CubicLevel;
import io.github.pirut.chunks3.ServerCubeWorld;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Remembers which overworld each player lives in, so trips to the Nether and the End, and deaths
 * there, bring them back to the cubic Earth when that is where they came from.
 */
public final class CubicTravel {
    private CubicTravel() {}

    public static void changedDimension(ServerPlayer player, ResourceKey<Level> from, ResourceKey<Level> to) {
        if (to == Level.OVERWORLD) player.removeTag(CubicPortals.HOME_TAG);
        else if (Cubic.isCubic(to) || Cubic.isCubic(from)) player.addTag(CubicPortals.HOME_TAG);
    }

    /**
     * Where a player respawns, and a respawn-style trip for one: dying anywhere without a bed or
     * anchor brings a cubic player back to the Earth's arrival point. A trip for a player: unchanged when they don't live in the cubic world or
     * when their bed or anchor still stands, else to the cubic world's arrival point.
     */
    public static DimensionTransition home(ServerPlayer player, DimensionTransition target) {
        if (!CubicPortals.livesInCubic(player) || Cubic.isCubic(target.newLevel().dimension())) return target;
        //#if MC >= 1.21.5
        //$$ if (player.getRespawnConfig() != null && !target.missingRespawnBlock()) return target;
        //#else
        if (player.getRespawnPosition() != null && !target.missingRespawnBlock()) return target;
        //#endif
        ServerLevel cubic = CubicPortals.cubicLevel(player.serverLevel());
        if (cubic == null || !(CubicLevel.of(cubic) instanceof ServerCubeWorld cubes)) return target;
        //#if MC >= 1.21.2
        //$$ return new TeleportTransition(cubic, CubicEvents.arrival(cubes), Vec3.ZERO, target.yRot(), 0.0F,
                //$$ target.missingRespawnBlock(), false, java.util.Set.of(), target.postTeleportTransition());
        //#else
        return new DimensionTransition(cubic, CubicEvents.arrival(cubes), Vec3.ZERO, target.yRot(), 0.0F,
                target.missingRespawnBlock(), target.postDimensionTransition());
        //#endif
    }
}
