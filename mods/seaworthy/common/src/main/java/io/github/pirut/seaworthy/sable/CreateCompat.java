package io.github.pirut.seaworthy.sable;

import io.github.pirut.seaworthy.Seaworthy;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Method;

/** What we need from Create, looked up at runtime so it stays optional. */
final class CreateCompat {
    private static final ResourceLocation PUMP = ResourceLocation.fromNamespaceAndPath("create", "mechanical_pump");

    private static Block pump;
    private static Method getSpeed;
    private static boolean broken;

    private CreateCompat() {
    }

    static boolean isPump(BlockState state) {
        if (pump == null) pump = BuiltInRegistries.BLOCK.get(PUMP);
        return pump != Blocks.AIR && state.getBlock() == pump;
    }

    /** How fast a Create machine is turning, in RPM, or 0 if it isn't one. */
    static float speed(BlockEntity entity) {
        if (entity == null || broken) return 0;
        try {
            if (getSpeed == null || !getSpeed.getDeclaringClass().isInstance(entity)) {
                getSpeed = entity.getClass().getMethod("getSpeed");
            }
            return getSpeed.invoke(entity) instanceof Float speed ? speed : 0;
        } catch (ReflectiveOperationException | RuntimeException e) {
            Seaworthy.LOG.warn("Couldn't read how fast a Create pump is turning, so pumps won't work: {}", e.toString());
            broken = true;
            return 0;
        }
    }
}
