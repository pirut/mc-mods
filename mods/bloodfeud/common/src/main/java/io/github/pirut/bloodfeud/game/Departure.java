package io.github.pirut.bloodfeud.game;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * A Brute that just rose is departing: while it is, it can't fight. Its moving, looking and targeting goals are
 * switched off (it walks only where it is sent), its target is cleared, and its hits do nothing (see
 * {@link Effects#immune}). Anything that ends a departure without removing the mob turns them back on.
 */
final class Departure {
    static final String TAG = "bloodfeud.departing";
    private static final Goal.Flag[] FLAGS = {Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.TARGET};
    private static List<Field> selectors;

    private Departure() {
    }

    static boolean departing(Entity e) {
        return e != null && e.getTags().contains(TAG);
    }

    /** It starts departing: no fighting from now on. */
    static void hold(Mob mob) {
        mob.addTag(TAG);
        for (GoalSelector g : selectors(mob)) for (Goal.Flag f : FLAGS) g.disableControlFlag(f);
        calm(mob);
    }

    /** It stops departing but stays in the world: it can fight again. */
    static void release(Mob mob) {
        if (!mob.removeTag(TAG)) return;
        for (GoalSelector g : selectors(mob)) for (Goal.Flag f : FLAGS) g.enableControlFlag(f);
    }

    /** No target, in the goal system or (piglin brutes) in its brain's memory. */
    static void calm(Mob mob) {
        mob.setTarget(null);
        mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        mob.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
    }

    /**
     * A mob's goal selectors. They are protected, and named differently on each loader at runtime, so they are found
     * by their type.
     */
    static List<GoalSelector> selectors(Mob mob) {
        if (selectors == null) {
            List<Field> found = new ArrayList<>();
            for (Field f : Mob.class.getDeclaredFields()) {
                if (f.getType() == GoalSelector.class && f.trySetAccessible()) found.add(f);
            }
            selectors = found;
        }
        List<GoalSelector> out = new ArrayList<>();
        for (Field f : selectors) {
            try {
                if (f.get(mob) instanceof GoalSelector g) out.add(g);
            } catch (IllegalAccessException ignored) {
            }
        }
        return out;
    }
}
