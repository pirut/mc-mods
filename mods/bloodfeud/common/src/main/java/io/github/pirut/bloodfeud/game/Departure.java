package io.github.pirut.bloodfeud.game;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * A Brute that just rose is departing, and while it is it can't fight. Every tick ({@link #suppress}) its moving,
 * looking and targeting goals are switched off (vanilla switches MOVE and LOOK back on every few ticks) and its target
 * is cleared, in its goals and in its brain (piglin brutes). It walks only where it is sent, and any hit it lands
 * does nothing ({@link Effects#immune}). When a departure ends without the mob leaving the world, exactly the flags
 * this switched off are switched back on ({@link #release}).
 */
final class Departure {
    static final String TAG = "bloodfeud.departing";
    private static final Goal.Flag[] FLAGS = {Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.TARGET};
    @SuppressWarnings("rawtypes")
    private static final List<MemoryModuleType> ANGER = List.of(MemoryModuleType.ATTACK_TARGET, MemoryModuleType.ANGRY_AT,
            MemoryModuleType.UNIVERSAL_ANGER, MemoryModuleType.HURT_BY, MemoryModuleType.HURT_BY_ENTITY,
            MemoryModuleType.NEAREST_ATTACKABLE);
    private static List<Field> selectors;
    private static Field disabledFlags;

    /** Per goal selector, the flags a hold switched off that weren't off before. */
    record Held(List<EnumSet<Goal.Flag>> ours) {
    }

    private Departure() {
    }

    static boolean departing(Entity e) {
        return e != null && e.getTags().contains(TAG);
    }

    /** It starts departing: notes which flags are its to switch off, and switches them off. */
    static Held hold(Mob mob) {
        mob.addTag(TAG);
        List<EnumSet<Goal.Flag>> ours = new ArrayList<>();
        for (GoalSelector g : selectors(mob)) {
            EnumSet<Goal.Flag> off = disabled(g);
            EnumSet<Goal.Flag> mine = EnumSet.noneOf(Goal.Flag.class);
            for (Goal.Flag f : FLAGS) if (off == null || !off.contains(f)) mine.add(f);
            ours.add(mine);
        }
        Held held = new Held(ours);
        suppress(mob, held);
        return held;
    }

    /**
     * Every tick while it departs: no moving, looking or targeting of its own (all three switched off, whoever had
     * switched them back on), and no target.
     */
    static void suppress(Mob mob, Held held) {
        for (GoalSelector g : selectors(mob)) for (Goal.Flag f : FLAGS) g.disableControlFlag(f);
        calm(mob);
    }

    /** It stops departing but stays in the world: exactly what the hold switched off comes back on. */
    static void release(Mob mob, Held held) {
        if (!mob.removeTag(TAG) || held == null) return;
        List<GoalSelector> gs = selectors(mob);
        for (int i = 0; i < gs.size() && i < held.ours().size(); i++) {
            for (Goal.Flag f : held.ours().get(i)) gs.get(i).enableControlFlag(f);
        }
    }

    /** No target, in the goal system or in its brain's memory. */
    @SuppressWarnings("unchecked")
    static void calm(Mob mob) {
        mob.setTarget(null);
        for (MemoryModuleType<?> m : ANGER) mob.getBrain().eraseMemory(m);
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

    /** The flags a selector has switched off now (its one EnumSet field), or null if it can't be read. */
    @SuppressWarnings("unchecked")
    static EnumSet<Goal.Flag> disabled(GoalSelector g) {
        try {
            if (disabledFlags == null) {
                for (Field f : GoalSelector.class.getDeclaredFields()) {
                    if (f.getType() == EnumSet.class && f.trySetAccessible()) disabledFlags = f;
                }
            }
            return disabledFlags == null ? null : EnumSet.copyOf((EnumSet<Goal.Flag>) disabledFlags.get(g));
        } catch (IllegalAccessException | IllegalArgumentException e) {
            return null;
        }
    }
}
