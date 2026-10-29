package io.github.pirut.ccagent;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.IComputerSystem;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

/**
 * The {@code ccagent} global on every computer. Only the ROM autorun hook is meant to call it: it
 * says when the hook is listening and hands back job results.
 */
public final class AgentApi implements ILuaAPI {
    private final IComputerSystem system;

    public AgentApi(IComputerSystem system) {
        this.system = system;
    }

    @Override
    public String[] getNames() {
        return new String[]{"ccagent"};
    }

    @Override
    public void startup() {
        Jobs.computerStarted(system.getID());
    }

    @Override
    public void shutdown() {
        Jobs.computerStopped(system.getID());
        try {
            TuiSession.stopComputer(system.getID());
            Relay.stopTuis(system.getID());
        } catch (RuntimeException e) {
            CCAgent.LOG.warn("Couldn't stop computer #{}'s TUI", system.getID(), e);
        }
    }

    @LuaFunction
    public final void ready() {
        Jobs.markReady(system.getID());
    }

    @LuaFunction
    public final void reply(IArguments args) throws LuaException {
        var id = args.getInt(0);
        var ok = args.getBoolean(1);
        var values = LuaStrings.fromLua(args.optString(2, null));
        var output = LuaStrings.fromLua(args.optString(3, ""));
        var error = LuaStrings.fromLua(args.optString(4, null));
        var job = Jobs.get(id);
        long millis = job == null ? 0 : System.currentTimeMillis() - job.started;
        Jobs.complete(id, new Jobs.Result(ok, values, output, error, millis));
    }

    /** Start Claude Code or Codex for the chat program: (agent, prompt, session?, width) -> run id, or nil and an error. */
    @LuaFunction
    public final Object[] ask(IArguments args) throws LuaException {
        var agent = args.getString(0);
        var prompt = LuaStrings.fromLua(args.getString(1));
        var session = args.optString(2, null);
        int width = args.optInt(3, 51);
        try {
            return new Object[]{startAgent(agent, prompt, session, width)};
        } catch (ToolException e) {
            return new Object[]{null, e.getMessage()};
        }
    }

    private int startAgent(String agent, String prompt, String session, int width) {
        var server = CCAgent.server();
        var config = CCAgent.config();
        if (server == null || config == null) throw new ToolException("no world is open");
        var player = Relay.interactingPlayer(system.getID());
        // Someone else's game: their own PC runs the agent.
        if (player != null && !server.isSingleplayerOwner(player.getGameProfile())) {
            return Relay.launch(player, system, agent, prompt, session, width);
        }
        if (server.isDedicatedServer()) {
            throw new ToolException("open this computer's screen in the game to use " + agent
                + " (it runs on your PC, with CC: Agent installed there too)");
        }
        if (!CCAgent.running()) throw new ToolException("CC: Agent's MCP server isn't running (see /ccagent)");
        AgentRunner.Sink sink = (run, kind, text) -> system.queueEvent("ccagent_chat", run, kind, LuaStrings.toLua(text));
        return AgentRunner.start(sink, system.getID(), system.getLabel(), agent, prompt, session, width, config, config.url());
    }

    @LuaFunction
    public final void stop(int run) {
        if (Relay.isRelayChat(run)) Relay.stopChat(run);
        else AgentRunner.stop(run);
    }

    /**
     * Start Claude Code's or Codex's own TUI in a terminal cols x rows: (agent, cols, rows, monitorName?) -> session id,
     * or nil and an error. Frames arrive as ccagent_tui events.
     */
    @LuaFunction
    public final Object[] tui(String agent, int cols, int rows, java.util.Optional<String> monitor) {
        try {
            var server = CCAgent.server();
            var config = CCAgent.config();
            if (server == null || config == null) throw new ToolException("no world is open");
            cols = Math.max(20, cols);
            rows = Math.max(5, rows);
            var player = Relay.interactingPlayer(system.getID());
            // Someone else's game: their own PC runs the agent.
            if (player != null && !server.isSingleplayerOwner(player.getGameProfile())) {
                return new Object[]{Relay.launchTui(player, system, agent, cols, rows, monitor.orElse(null))};
            }
            if (server.isDedicatedServer()) {
                throw new ToolException("open this computer's screen in the game to use " + agent
                    + " (it runs on your PC, with CC: Agent installed there too)");
            }
            if (!CCAgent.running()) throw new ToolException("CC: Agent's MCP server isn't running (see /ccagent)");
            TuiSession.Sink sink = new TuiSession.Sink() {
                @Override
                public void frame(int id, java.util.Map<Integer, String[]> frameRows, java.util.Map<Integer, Integer> palette,
                                  int x, int y, boolean visible) {
                    system.queueEvent("ccagent_tui", id, "frame", TuiSession.luaRows(frameRows), palette, x + 1, y + 1, visible);
                }

                @Override
                public void exit(int id, int code) {
                    system.queueEvent("ccagent_tui", id, "exit", code);
                }
            };
            return new Object[]{TuiSession.start(sink, system.getID(), system.getLabel(), agent, cols, rows,
                monitor.orElse(null), config, config.url())};
        } catch (ToolException e) {
            return new Object[]{null, e.getMessage()};
        }
    }

    /** Keyboard input for a TUI session: (id, "char"|"key"|"key_up"|"paste", value). */
    @LuaFunction
    public final void tuiInput(IArguments args) throws LuaException {
        int id = args.getInt(0);
        var kind = args.getString(1);
        Object value = switch (kind) {
            // CC's char and paste events carry Latin-1 text, which is what Lua strings map to.
            case "char", "paste" -> args.getString(2);
            default -> args.getInt(2);
        };
        if (Relay.isRelayTui(id, system.getID())) {
            Relay.tuiControl(id, Json.obj("action", "input", "kind", kind, "value", value));
            return;
        }
        var session = session(id);
        try {
            session.input(kind, value);
        } catch (ToolException e) {
            throw new LuaException(e.getMessage());
        }
    }

    @LuaFunction
    public final void tuiResize(int id, int cols, int rows) throws LuaException {
        if (Relay.isRelayTui(id, system.getID())) Relay.tuiControl(id, Json.obj("action", "resize", "cols", cols, "rows", rows));
        else session(id).resize(cols, rows);
    }

    @LuaFunction
    public final void tuiRedraw(int id) throws LuaException {
        if (Relay.isRelayTui(id, system.getID())) Relay.tuiControl(id, Json.obj("action", "redraw"));
        else session(id).redraw();
    }

    @LuaFunction
    public final void tuiStop(int id) {
        if (Relay.isRelayTui(id, system.getID())) {
            Relay.tuiControl(id, Json.obj("action", "stop"));
            return;
        }
        var session = TuiSession.get(id);
        if (session != null && session.computerId == system.getID()) TuiSession.stop(id);
    }

    private TuiSession session(int id) throws LuaException {
        var session = TuiSession.get(id);
        if (session == null || session.computerId != system.getID()) throw new LuaException("no such session");
        return session;
    }

    /** Agent tool calls after the given entry id, oldest first. */
    @LuaFunction
    public final java.util.List<java.util.Map<String, Object>> activity(java.util.Optional<Integer> after) {
        return ActivityLog.after(after.orElse(0));
    }

    @LuaFunction
    public final String version() {
        return CCAgent.VERSION;
    }
}
