package io.github.pirut.ccagent;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dan200.computercraft.shared.computer.blocks.AbstractComputerBlockEntity;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.core.ServerContext;
import dan200.computercraft.shared.pocket.core.PocketServerComputer;
import dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** Finding CC: Tweaked computers and talking to them. */
public final class Computers {
    private final MinecraftServer server;

    public Computers(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer server() {
        return server;
    }

    /** Run on the server thread and wait for the answer. */
    public <T> T onServer(Supplier<T> task) {
        if (server.isSameThread()) return task.get();
        try {
            return CompletableFuture.supplyAsync(task, server).get(20, TimeUnit.SECONDS);
        } catch (java.util.concurrent.ExecutionException e) {
            var cause = e.getCause();
            if (cause instanceof RuntimeException r) throw r;
            throw new ToolException(cause.toString());
        } catch (java.util.concurrent.TimeoutException e) {
            throw new ToolException("the server didn't answer within 20 s (is the game paused or frozen?)");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ToolException("interrupted");
        }
    }

    public List<ServerComputer> all() {
        return onServer(() -> {
            var list = new ArrayList<>(ServerContext.get(server).registry().getComputers());
            list.sort(Comparator.comparingInt(ServerComputer::getID));
            return list;
        });
    }

    /** A placed computer CC: Tweaked hasn't started yet (nobody has used it since it was placed or loaded). */
    public record Dormant(ServerLevel level, BlockPos pos, AbstractComputerBlockEntity block) {
    }

    /** Dormant computers in loaded chunks near players and the world spawn. */
    public List<Dormant> dormant() {
        return onServer(() -> {
            var found = new ArrayList<Dormant>();
            var seen = new java.util.HashSet<String>();
            for (var level : server.getAllLevels()) {
                var centers = new ArrayList<BlockPos>();
                for (var player : level.players()) centers.add(player.blockPosition());
                if (level == server.overworld()) centers.add(level.getSharedSpawnPos());
                for (var center : centers) {
                    int cx = center.getX() >> 4, cz = center.getZ() >> 4, r = 8;
                    for (int x = cx - r; x <= cx + r; x++) {
                        for (int z = cz - r; z <= cz + r; z++) {
                            if (!seen.add(level.dimension().location() + "/" + x + "/" + z)) continue;
                            var chunk = level.getChunkSource().getChunkNow(x, z);
                            if (chunk == null) continue;
                            for (var be : chunk.getBlockEntities().values()) {
                                if (be instanceof AbstractComputerBlockEntity computer && computer.getServerComputer() == null) {
                                    found.add(new Dormant(level, be.getBlockPos(), computer));
                                }
                            }
                        }
                    }
                }
            }
            return found;
        });
    }

    /** A computer by id (number), label, or block position "x,y,z" (which also wakes a dormant one). */
    public ServerComputer find(JsonElement selector) {
        if (selector == null || selector.isJsonNull()) throw new ToolException("say which computer: its id, label or x,y,z");
        String text = selector.getAsString().trim();
        var parts = text.split("[,\\s]+");
        if (parts.length == 3) {
            try {
                var pos = new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                return at(pos);
            } catch (NumberFormatException ignored) {
            }
        }
        var computers = all();
        Integer id = null;
        try {
            id = Integer.parseInt(text.startsWith("#") ? text.substring(1) : text);
        } catch (NumberFormatException ignored) {
        }
        ServerComputer best = null;
        for (var computer : computers) {
            boolean match = id != null ? computer.getID() == id : text.equalsIgnoreCase(computer.getLabel());
            if (match && (best == null || (!best.isOn() && computer.isOn()))) best = computer;
        }
        if (best == null) {
            var known = new StringBuilder();
            for (var computer : computers) {
                if (!known.isEmpty()) known.append(", ");
                known.append('#').append(computer.getID());
                if (computer.getLabel() != null) known.append(" \"").append(computer.getLabel()).append('"');
            }
            throw new ToolException("no loaded computer matches " + text + ". Loaded: "
                + (known.isEmpty() ? "none (computers only exist while their chunk is loaded)" : known));
        }
        return best;
    }

    private ServerComputer at(BlockPos pos) {
        return onServer(() -> {
            for (var level : server.getAllLevels()) {
                if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof AbstractComputerBlockEntity computer) {
                    return computer.createServerComputer();
                }
            }
            throw new ToolException("no computer block at " + pos.toShortString() + " in a loaded chunk");
        });
    }

    public String kind(ServerComputer computer) {
        if (computer instanceof PocketServerComputer) return "pocket";
        if (turtle(computer) != null) return "turtle";
        return switch (computer.getFamily()) {
            case COMMAND -> "command_computer";
            case ADVANCED -> "advanced_computer";
            default -> "computer";
        };
    }

    public TurtleBlockEntity turtle(ServerComputer computer) {
        return onServer(() -> {
            var level = computer.getLevel();
            var pos = computer.getPosition();
            if (!level.isLoaded(pos)) return null;
            return level.getBlockEntity(pos) instanceof TurtleBlockEntity turtle ? turtle : null;
        });
    }

    public JsonObject describe(ServerComputer computer, boolean detail) {
        return onServer(() -> {
            var out = new JsonObject();
            out.addProperty("id", computer.getID());
            if (computer.getLabel() != null) out.addProperty("label", computer.getLabel());
            out.addProperty("kind", kind(computer));
            out.addProperty("family", computer.getFamily().name().toLowerCase());
            out.addProperty("on", computer.isOn());
            out.addProperty("agent_ready", computer.isOn() && Jobs.isReady(computer.getID()));
            out.addProperty("dimension", computer.getLevel().dimension().location().toString());
            var pos = computer.getPosition();
            out.addProperty("x", pos.getX());
            out.addProperty("y", pos.getY());
            out.addProperty("z", pos.getZ());
            var turtle = turtle(computer);
            if (turtle != null) {
                var access = turtle.getAccess();
                out.addProperty("facing", access.getDirection().getName());
                out.addProperty("fuel", access.isFuelNeeded() ? Integer.toString(access.getFuelLevel()) : "unlimited");
                out.addProperty("selected_slot", access.getSelectedSlot() + 1);
            }
            if (detail) {
                var nearest = nearestPlayer(computer);
                if (nearest != null) {
                    out.addProperty("nearest_player", nearest.getGameProfile().getName());
                    out.addProperty("nearest_player_distance", Math.round(Math.sqrt(nearest.blockPosition().distSqr(pos))));
                }
                var state = computer.getTerminalState().create();
                out.addProperty("screen_width", state.getWidth());
                out.addProperty("screen_height", state.getHeight());
                out.addProperty("color", state.isColour());
            }
            return out;
        });
    }

    private ServerPlayer nearestPlayer(ServerComputer computer) {
        ServerPlayer best = null;
        double bestDist = Double.MAX_VALUE;
        for (var player : computer.getLevel().players()) {
            var d = player.blockPosition().distSqr(computer.getPosition());
            if (d < bestDist) {
                bestDist = d;
                best = player;
            }
        }
        return best;
    }

    public JsonObject screen(ServerComputer computer, boolean colors) {
        var terminal = computer.getTerminalState().create();
        var out = new JsonObject();
        out.addProperty("width", terminal.getWidth());
        out.addProperty("height", terminal.getHeight());
        out.addProperty("cursor_x", terminal.getCursorX() + 1);
        out.addProperty("cursor_y", terminal.getCursorY() + 1);
        out.addProperty("cursor_blink", terminal.getCursorBlink());
        var text = new StringBuilder();
        var fg = new JsonArray();
        var bg = new JsonArray();
        for (int y = 0; y < terminal.getHeight(); y++) {
            if (y > 0) text.append('\n');
            text.append(LuaStrings.fromLua(terminal.getLine(y).toString()));
            if (colors) {
                fg.add(terminal.getTextColourLine(y).toString());
                bg.add(terminal.getBackgroundColourLine(y).toString());
            }
        }
        out.addProperty("text", text.toString());
        if (colors) {
            out.add("text_colors", fg);
            out.add("background_colors", bg);
        }
        return out;
    }

    /** Turn the computer on if needed and wait for the agent hook to start listening. */
    public void awaitAgent(ServerComputer computer, boolean turnOn) {
        if (computer.isOn() && Jobs.isReady(computer.getID())) return;
        if (!computer.isOn()) {
            if (!turnOn) throw new ToolException("computer #" + computer.getID() + " is off (use power to turn it on)");
            onServer(() -> {
                computer.turnOn();
                return null;
            });
        }
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            if (computer.isOn() && Jobs.isReady(computer.getID())) return;
            sleep(50);
        }
        throw new ToolException("computer #" + computer.getID() + " is on but the CC: Agent hook isn't listening. "
            + "Its startup may run a program that never waits for events, may have replaced os.pullEventRaw, "
            + "or set ccagent.enabled=false. Try power with action=reboot, or press_keys/read_screen to use it by hand.");
    }

    public Jobs.Job submit(ServerComputer computer, String kind, String code, boolean json) {
        if (!CCAgent.config().allowShellAndLua) {
            throw new ToolException("running Lua and shell commands is turned off (allowShellAndLua=false in ccagent.properties)");
        }
        awaitAgent(computer, true);
        var job = Jobs.create(computer.getID(), kind);
        computer.queueEvent("ccagent_exec", new Object[]{job.id, kind, LuaStrings.toLua(code), json});
        return job;
    }

    /** Run Lua that returns one value and give back that value as JSON. */
    public JsonElement lua(ServerComputer computer, String code, int timeoutSeconds) {
        var job = submit(computer, "lua", code, true);
        var result = await(job, timeoutSeconds);
        if (result == null) throw new ToolException("timed out after " + timeoutSeconds + " s (job " + job.id + " is still running)");
        if (!result.ok()) throw new ToolException(result.error() == null ? "failed" : result.error());
        try {
            return com.google.gson.JsonParser.parseString(result.values() == null ? "null" : result.values());
        } catch (RuntimeException e) {
            var raw = new JsonObject();
            raw.addProperty("value", result.values());
            return raw;
        }
    }

    public static Jobs.Result await(Jobs.Job job, int timeoutSeconds) {
        try {
            return job.future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (java.util.concurrent.ExecutionException e) {
            throw new ToolException(e.getCause().toString());
        }
    }

    static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ToolException("interrupted");
        }
    }
}
