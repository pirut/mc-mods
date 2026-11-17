package io.github.pirut.lib;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Server-side events, the same on NeoForge and Fabric. Register listeners from your mod's init; Pirut Lib's own
 * loader glue fires them. Everything here runs on the server thread.
 */
public final class ServerEvents {
    private ServerEvents() {
    }

    public interface Tick {
        void tick(MinecraftServer server);
    }

    public interface Started {
        void started(MinecraftServer server);
    }

    public interface Stopped {
        void stopped(MinecraftServer server);
    }

    /** An entity is being added to a server level (spawned or loaded from disk). Return false to drop it. */
    public interface EntityLoad {
        boolean allow(Entity entity, ServerLevel level);
    }

    /** A living entity died, players included. */
    public interface Death {
        void died(LivingEntity entity, DamageSource source);
    }

    /** Return true to make a hit do nothing at all (as if the entity were immune). */
    public interface Ignore {
        boolean ignore(LivingEntity target, DamageSource source, float amount);
    }

    /** Returns the damage a hit should do, after this mod's changes. */
    public interface Damage {
        float damage(LivingEntity target, DamageSource source, float amount);
    }

    public interface Register {
        void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context, Commands.CommandSelection selection);
    }

    private static final List<Tick> TICK = new CopyOnWriteArrayList<>();
    private static final List<Started> STARTED = new CopyOnWriteArrayList<>();
    private static final List<Stopped> STOPPED = new CopyOnWriteArrayList<>();
    private static final List<EntityLoad> ENTITY_LOAD = new CopyOnWriteArrayList<>();
    private static final List<Death> DEATH = new CopyOnWriteArrayList<>();
    private static final List<Ignore> IGNORE = new CopyOnWriteArrayList<>();
    private static final List<Damage> DAMAGE = new CopyOnWriteArrayList<>();
    private static final List<Register> COMMANDS = new CopyOnWriteArrayList<>();

    /** After every server tick. */
    public static void onTick(Tick l) {
        TICK.add(l);
    }

    public static void onStarted(Started l) {
        STARTED.add(l);
    }

    public static void onStopped(Stopped l) {
        STOPPED.add(l);
    }

    public static void onEntityLoad(EntityLoad l) {
        ENTITY_LOAD.add(l);
    }

    public static void onDeath(Death l) {
        DEATH.add(l);
    }

    /** Called at the start of every hit on a living entity, on the server, before armor. */
    public static void onIgnoreDamage(Ignore l) {
        IGNORE.add(l);
    }

    /** Called at the start of every hit on a living entity, on the server, before armor. */
    public static void onDamage(Damage l) {
        DAMAGE.add(l);
    }

    public static void onRegisterCommands(Register l) {
        COMMANDS.add(l);
    }

    /** Firing the events: for Pirut Lib's loader glue only. */
    public static final class Fire {
        private Fire() {
        }

        public static void tick(MinecraftServer server) {
            for (Tick l : TICK) {
                try {
                    l.tick(server);
                } catch (RuntimeException e) {
                    PirutLib.LOG.error("A server tick listener failed", e);
                }
            }
        }

        public static void started(MinecraftServer server) {
            for (Started l : STARTED) l.started(server);
        }

        public static void stopped(MinecraftServer server) {
            for (Stopped l : STOPPED) l.stopped(server);
        }

        public static boolean entityLoad(Entity entity, ServerLevel level) {
            boolean allow = true;
            for (EntityLoad l : ENTITY_LOAD) allow &= l.allow(entity, level);
            return allow;
        }

        public static void death(LivingEntity entity, DamageSource source) {
            for (Death l : DEATH) l.died(entity, source);
        }

        public static boolean ignore(LivingEntity target, DamageSource source, float amount) {
            for (Ignore l : IGNORE) if (l.ignore(target, source, amount)) return true;
            return false;
        }

        public static float damage(LivingEntity target, DamageSource source, float amount) {
            float out = amount;
            for (Damage l : DAMAGE) out = l.damage(target, source, out);
            return out;
        }

        public static void commands(CommandDispatcher<CommandSourceStack> d, CommandBuildContext c, Commands.CommandSelection s) {
            for (Register l : COMMANDS) l.register(d, c, s);
        }
    }
}
