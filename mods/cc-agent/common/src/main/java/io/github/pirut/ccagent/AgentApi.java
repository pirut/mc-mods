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
            return new Object[]{AgentRunner.start(system, agent, prompt, session, width)};
        } catch (ToolException e) {
            return new Object[]{null, e.getMessage()};
        }
    }

    @LuaFunction
    public final void stop(int run) {
        AgentRunner.stop(run);
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
