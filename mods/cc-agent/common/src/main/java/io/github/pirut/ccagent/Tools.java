package io.github.pirut.ccagent;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dan200.computercraft.shared.computer.core.ServerComputer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static io.github.pirut.ccagent.Json.obj;
import static io.github.pirut.ccagent.Json.prop;
import static io.github.pirut.ccagent.Json.schema;

/** Every MCP tool the mod offers. */
public final class Tools implements McpServer.Backend {
    public interface Handler {
        Object call(JsonObject args) throws Exception;
    }

    public record Tool(String name, String title, String description, JsonObject inputSchema, boolean readOnly, Handler handler) {
        JsonObject listing() {
            var annotations = obj("title", title, "readOnlyHint", readOnly);
            return obj("name", name, "title", title, "description", description, "inputSchema", inputSchema,
                "annotations", annotations);
        }
    }

    private static final String COMPUTER = "The computer: its id number (from list_computers), its label, or its block position as \"x,y,z\".";
    private static final int DEFAULT_TIMEOUT = 30;
    private static final int MAX_TIMEOUT = 600;

    private final Computers computers;
    private final FileAccess files;
    private final WorldAccess world;
    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public Tools(MinecraftServer server) {
        computers = new Computers(server);
        files = new FileAccess(server);
        world = new WorldAccess(computers);
        register();
    }

    @Override
    public JsonArray tools() {
        var list = new JsonArray();
        for (var tool : tools.values()) list.add(tool.listing());
        return list;
    }

    @Override
    public JsonObject call(String name, JsonObject args) {
        var tool = tools.get(name);
        if (tool == null) return McpServer.toolResult("unknown tool " + name, true);
        var result = run(tool, args);
        var who = Access.current() == null ? "" : Access.current().player().getGameProfile().getName() + ": ";
        ActivityLog.add(name, Json.str(args, "computer", null), who + summary(name, args), !result.get("isError").getAsBoolean());
        return result;
    }

    private static JsonObject run(Tool tool, JsonObject args) {
        try {
            var value = tool.handler().call(args);
            if (value instanceof ToolResult r) return McpServer.toolResult(r.text(), r.error());
            if (value instanceof JsonElement json) return McpServer.toolResult(Json.PRETTY.toJson(json), false);
            return McpServer.toolResult(String.valueOf(value), false);
        } catch (ToolException e) {
            return McpServer.toolResult(e.getMessage(), true);
        } catch (Exception e) {
            CCAgent.LOG.warn("Tool {} failed", tool.name(), e);
            return McpServer.toolResult(tool.name() + " failed: " + e, true);
        }
    }

    public List<Tool> all() {
        return new ArrayList<>(tools.values());
    }

    public Tool get(String name) {
        return tools.get(name);
    }

    private void add(String name, String title, boolean readOnly, String description, JsonObject schema, Handler handler) {
        tools.put(name, new Tool(name, title, description, schema, readOnly, handler));
    }

    private static JsonObject computerOnly() {
        return schema(obj("computer", prop("string", COMPUTER)), "computer");
    }

    private ServerComputer computer(JsonObject args) {
        return computers.find(args.get("computer"));
    }

    private static int timeout(JsonObject args) {
        return Math.max(1, Math.min(Json.integer(args, "timeout_seconds", DEFAULT_TIMEOUT), MAX_TIMEOUT));
    }

    /** The computer named in args, else the one whose chat this request comes from. */
    private ServerComputer chatComputer(JsonObject args) {
        if (args.has("computer")) return computer(args);
        Integer chat = Questions.CHAT.get();
        if (chat == null && args.has("__chat")) chat = Json.integer(args, "__chat", -1);
        if (chat == null) throw new ToolException("say which computer to ask on (computer)");
        return computers.find(new com.google.gson.JsonPrimitive(String.valueOf(chat)));
    }

    /** Tools the player said yes to for the rest of a chat, per computer. */
    private final Map<Integer, java.util.Set<String>> alwaysAllowed = new java.util.concurrent.ConcurrentHashMap<>();

    void forgetAllowed(int computerId) {
        alwaysAllowed.remove(computerId);
    }

    private String permissionPrompt(JsonObject args) {
        var c = chatComputer(args);
        var tool = Json.need(args, "tool_name");
        var input = args.has("input") && args.get("input").isJsonObject() ? args.getAsJsonObject("input") : new JsonObject();
        var allowed = alwaysAllowed.computeIfAbsent(c.getID(), k -> java.util.concurrent.ConcurrentHashMap.newKeySet());
        if (allowed.contains(tool) || allowed.contains("*") && tool.startsWith("mcp__cc-agent__")) return allow(input);
        // Plan mode's own plan file.
        if (tool.equals("Write") && Json.str(input, "file_path", "").replace('\\', '/').contains("/.claude/plans/")) return allow(input);

        String answer;
        if (tool.equals("ExitPlanMode")) {
            answer = Questions.ask(c, "plan", "Go ahead with the plan above?",
                List.of(PLAN_AUTO, PLAN_ASK, "No, keep planning"), true, 600);
            if (answer.equals(PLAN_AUTO)) {
                allowed.add("*");
                return allow(input);
            }
            if (answer.equals(PLAN_ASK)) return allow(input);
        } else {
            var shortName = tool.startsWith("mcp__cc-agent__") ? tool.substring("mcp__cc-agent__".length()) : tool;
            var detail = summary(shortName, input);
            var where = Json.str(input, "computer", null);
            var question = "Allow " + shortName + (where == null ? "" : " on computer " + where) + "?"
                + (detail == null || detail.isEmpty() ? "" : "\n" + shorten(detail, 300));
            answer = Questions.ask(c, "permission", question, List.of("Allow", "Allow for this chat", "Deny"), true, 600);
            if (answer.equals("Allow")) return allow(input);
            if (answer.equals("Allow for this chat")) {
                allowed.add(tool);
                return allow(input);
            }
        }
        var message = answer.equals("Deny") || answer.startsWith("No,") ? "The player said no."
            : "The player said no: " + answer;
        return Json.COMPACT.toJson(obj("behavior", "deny", "message", message));
    }

    // The chat switches its own mode to match when the player picks one of these.
    static final String PLAN_AUTO = "Yes, and don't ask again";
    static final String PLAN_ASK = "Yes, ask before each change";

    private static String allow(JsonObject input) {
        return Json.COMPACT.toJson(obj("behavior", "allow", "updatedInput", input));
    }

    private static String shorten(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }

    private void register() {
        add("list_computers", "List computers", true, """
                List every loaded CC: Tweaked computer, turtle and pocket computer: id, label, kind, on/off, \
                whether the agent hook is ready, dimension and position, plus placed computers near players that were \
                never started. Computers only exist while their chunk is loaded.""",
            schema(new JsonObject()), args -> {
                var out = new JsonArray();
                for (var c : computers.all()) out.add(computers.describe(c, false));
                var asleep = new JsonArray();
                for (var d : computers.dormant()) {
                    asleep.add(obj("computer", d.pos().getX() + "," + d.pos().getY() + "," + d.pos().getZ(),
                        "block", net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(d.block().getBlockState().getBlock()).toString(),
                        "dimension", d.level().dimension().location().toString()));
                }
                var result = obj("computers", out, "count", out.size());
                if (!asleep.isEmpty()) {
                    result.add("not_started", asleep);
                    result.addProperty("not_started_note", "Placed but never started. Pass the x,y,z string as `computer` "
                        + "to any tool to start it (it gets an id then).");
                }
                return result;
            });

        add("computer_info", "Computer info", true, """
                Details for one computer: kind, on/off, position, screen size and color support, the nearest player, \
                and for turtles facing, fuel and selected slot.""",
            computerOnly(), args -> computers.describe(computer(args), true));

        add("power", "Turn on, shut down or reboot", false, "Turn a computer on, shut it down, or reboot it.",
            schema(obj("computer", prop("string", COMPUTER),
                "action", obj("type", "string", "enum", Json.arr("on", "shutdown", "reboot"))), "computer", "action"),
            args -> {
                var c = computer(args);
                var action = Json.need(args, "action");
                computers.onServer(() -> {
                    switch (action) {
                        case "on" -> c.turnOn();
                        case "shutdown" -> c.shutdown();
                        case "reboot" -> c.reboot();
                        default -> throw new ToolException("action must be on, shutdown or reboot");
                    }
                    return null;
                });
                if (!action.equals("shutdown")) {
                    long deadline = System.currentTimeMillis() + 5000;
                    Computers.sleep(200);
                    while (System.currentTimeMillis() < deadline && !(c.isOn() && Jobs.isReady(c.getID()))) Computers.sleep(50);
                }
                return computers.describe(c, false);
            });

        add("read_screen", "Read the screen", true, """
                The computer's terminal as text, with the cursor position. Set colors to also get each line's text and \
                background colors as CC blit strings (0-f, one character per cell).""",
            schema(obj("computer", prop("string", COMPUTER), "colors", prop("boolean", "Include blit color lines.")), "computer"),
            args -> computers.screen(computer(args), Json.bool(args, "colors", false)));

        add("run_lua", "Run Lua", false, """
                Run Lua on the computer and wait for it. An expression's value is returned (e.g. "turtle.getFuelLevel()"), \
                or use return in a chunk. print/write output is captured (it doesn't touch the real screen). \
                `shell` and `multishell` are available. The code runs inside whatever program is waiting for events \
                (usually the shell), which pauses until it's done. Long jobs keep running after the timeout: \
                check them with get_job. Lua is CC: Tweaked's (Cobalt, Lua 5.2 with some 5.3 parts).""",
            schema(obj("computer", prop("string", COMPUTER), "code", prop("string", "Lua code or an expression."),
                "timeout_seconds", prop("integer", "How long to wait, 1-600 (default 30).")), "computer", "code"),
            args -> runJob(computer(args), "lua", Json.need(args, "code"), timeout(args)));

        add("run_shell", "Run a shell command", false, """
                Run a CraftOS shell command line (e.g. "ls", "edit" won't work headless, "myprogram arg1", \
                "pastebin get CODE file") and capture what it prints. Same rules as run_lua.""",
            schema(obj("computer", prop("string", COMPUTER), "command", prop("string", "The command line."),
                "timeout_seconds", prop("integer", "How long to wait, 1-600 (default 30).")), "computer", "command"),
            args -> runJob(computer(args), "shell", Json.need(args, "command"), timeout(args)));

        add("get_job", "Check a job", true, "Wait for a run_lua/run_shell job that outlived its timeout and get its result.",
            schema(obj("job_id", prop("integer", "The job id."),
                "wait_seconds", prop("integer", "How long to wait for it, 0-600 (default 30).")), "job_id"),
            args -> {
                var job = Jobs.get(Json.integer(args, "job_id", -1));
                if (job == null || !Jobs.visible(job)) throw new ToolException("no such job (only the last 200 are kept)");
                int wait = Math.max(0, Math.min(Json.integer(args, "wait_seconds", DEFAULT_TIMEOUT), MAX_TIMEOUT));
                return formatJob(job, Computers.await(job, wait), wait);
            });

        add("cancel_job", "Cancel a job", false, "Stop a running run_lua/run_shell job.",
            schema(obj("job_id", prop("integer", "The job id.")), "job_id"),
            args -> {
                var job = Jobs.get(Json.integer(args, "job_id", -1));
                if (job == null || !Jobs.visible(job)) throw new ToolException("no such job");
                if (job.future.isDone()) return "job " + job.id + " already finished";
                for (var c : computers.all()) {
                    if (c.getID() == job.computerId) c.queueEvent("ccagent_cancel", new Object[]{job.id});
                }
                var result = Computers.await(job, 5);
                return result == null ? "cancel sent; the job hasn't stopped yet (it may be busy without yielding)" : "cancelled job " + job.id;
            });

        add("type_text", "Type text", false, """
                Type into the computer like a player: a paste of the text, then Enter unless enter=false. Use it to \
                answer a program's read() prompt or drive the real shell visibly. Then read_screen to see the result.""",
            schema(obj("computer", prop("string", COMPUTER), "text", prop("string", "Text to type."),
                "enter", prop("boolean", "Press Enter afterwards (default true).")), "computer", "text"),
            args -> {
                var c = computer(args);
                var text = Json.need(args, "text");
                if (!text.isEmpty()) c.queueEvent("paste", new Object[]{LuaStrings.toLua(text)});
                if (Json.bool(args, "enter", true)) Keys.press(c, "enter");
                Computers.sleep(300);
                return computers.screen(c, false);
            });

        add("press_keys", "Press keys", false, """
                Press keys in order, each a name like "enter", "up", "backspace", "tab", "f1", "a", "space", or a combo like \
                "ctrl+s". Special: "terminate" (like holding Ctrl+T), "reboot", "shutdown". Returns the screen afterwards.""",
            schema(obj("computer", prop("string", COMPUTER), "keys", obj("type", "array", "items", obj("type", "string"),
                "description", "Keys to press.")), "computer", "keys"),
            args -> {
                var c = computer(args);
                for (var key : Json.array(args, "keys")) {
                    Keys.press(c, key.getAsString());
                    Computers.sleep(60);
                }
                Computers.sleep(300);
                return computers.screen(c, false);
            });

        add("queue_event", "Queue an event", false, """
                Queue any event on the computer (os.queueEvent from outside), e.g. name "redstone" or a custom one your \
                program waits for with os.pullEvent.""",
            schema(obj("computer", prop("string", COMPUTER), "name", prop("string", "Event name."),
                "args", obj("type", "array", "description", "Event arguments (JSON values).")), "computer", "name"),
            args -> {
                var c = computer(args);
                var values = new ArrayList<>();
                for (var item : Json.array(args, "args")) values.add(Json.java(item));
                c.queueEvent(LuaStrings.toLua(Json.need(args, "name")), values.toArray());
                return "queued";
            });

        add("list_files", "List files", true, """
                List files on the computer's own drive (from the world save, so it works while the computer is off). \
                /rom and /disk mounts aren't on the drive: use run_lua with fs.list for those.""",
            schema(obj("computer", prop("string", COMPUTER), "path", prop("string", "Directory (default /)."),
                "recursive", prop("boolean", "Include subdirectories.")), "computer"),
            args -> files.list(computer(args).getID(), Json.str(args, "path", "/"), Json.bool(args, "recursive", false)));

        add("read_file", "Read a file", true, "Read a file from the computer's drive as text.",
            schema(obj("computer", prop("string", COMPUTER), "path", prop("string", "File path, e.g. /startup.lua.")), "computer", "path"),
            args -> files.read(computer(args).getID(), Json.need(args, "path")));

        add("write_file", "Write a file", false, """
                Write a file on the computer's drive, making directories as needed. Programs usually end in .lua; \
                /startup.lua runs at boot. Computers have about 1 MB of space by default.""",
            schema(obj("computer", prop("string", COMPUTER), "path", prop("string", "File path."),
                "content", prop("string", "The whole file."), "append", prop("boolean", "Append instead of replacing.")),
                "computer", "path", "content"),
            args -> {
                var c = computer(args);
                var path = Json.need(args, "path");
                long size = files.write(c.getID(), path, Json.need(args, "content"), Json.bool(args, "append", false));
                return "wrote " + path + " (" + size + " bytes)";
            });

        add("edit_file", "Edit a file", false, "Replace exact text in a file on the computer's drive.",
            schema(obj("computer", prop("string", COMPUTER), "path", prop("string", "File path."),
                "old_text", prop("string", "Exact text to find."), "new_text", prop("string", "Replacement."),
                "replace_all", prop("boolean", "Replace every match (default: exactly one must match).")),
                "computer", "path", "old_text", "new_text"),
            args -> {
                var c = computer(args);
                int n = files.edit(c.getID(), Json.need(args, "path"), Json.need(args, "old_text"), Json.need(args, "new_text"),
                    Json.bool(args, "replace_all", false));
                return "replaced " + n + (n == 1 ? " match" : " matches");
            });

        add("delete_file", "Delete a file", false, "Delete a file or directory (with its contents) on the computer's drive.",
            schema(obj("computer", prop("string", COMPUTER), "path", prop("string", "Path.")), "computer", "path"),
            args -> {
                files.delete(computer(args).getID(), Json.need(args, "path"));
                return "deleted";
            });

        add("move_file", "Move a file", false, "Move or rename a file or directory on the computer's drive.",
            schema(obj("computer", prop("string", COMPUTER), "from", prop("string", "Path."), "to", prop("string", "New path.")),
                "computer", "from", "to"),
            args -> {
                files.move(computer(args).getID(), Json.need(args, "from"), Json.need(args, "to"));
                return "moved";
            });

        add("make_dir", "Make a directory", false, "Make a directory (and parents) on the computer's drive.",
            schema(obj("computer", prop("string", COMPUTER), "path", prop("string", "Path.")), "computer", "path"),
            args -> {
                files.mkdir(computer(args).getID(), Json.need(args, "path"));
                return "made";
            });

        add("list_peripherals", "List peripherals", true, """
                Peripherals the computer can see (sides and wired-modem names) with their types and every method name. \
                Includes CC's generic peripherals, e.g. any chest shows up as type "inventory".""",
            computerOnly(), args -> computers.lua(computer(args), Lua.PERIPHERALS, DEFAULT_TIMEOUT));

        add("call_peripheral", "Call a peripheral", false, """
                Call a method on a peripheral, e.g. peripheral "left", method "list" on a chest, or "getEnergy". \
                Returns every value the method returned.""",
            schema(obj("computer", prop("string", COMPUTER), "peripheral", prop("string", "Side or network name."),
                "method", prop("string", "Method name."), "args", obj("type", "array", "description", "Arguments (JSON values)."),
                "timeout_seconds", prop("integer", "How long to wait (default 30).")), "computer", "peripheral", "method"),
            args -> {
                var call = new StringBuilder("return peripheral.call(").append(LuaStrings.quote(Json.need(args, "peripheral")))
                    .append(", ").append(LuaStrings.quote(Json.need(args, "method")));
                for (var item : Json.array(args, "args")) call.append(", ").append(Json.lua(item));
                call.append(")");
                return computers.lua(computer(args), Lua.packed(call.toString()), timeout(args));
            });

        add("ask_player", "Ask the player", true, """
                Ask the player a question on their CC computer and wait for the answer. Give 2-6 short options when \
                there's a choice (they click one); they can also type their own answer unless allow_other is false. \
                Defaults to the computer the player is chatting from. Waits up to 10 minutes.""",
            schema(obj("question", prop("string", "The question, short and plain."),
                "options", obj("type", "array", "items", obj("type", "string"), "description", "Answers to click."),
                "allow_other", prop("boolean", "Let them type an answer of their own (default true)."),
                "computer", prop("string", COMPUTER + " Defaults to the player's chat computer.")), "question"),
            args -> {
                var options = new ArrayList<String>();
                for (var item : Json.array(args, "options")) options.add(item.getAsString());
                if (options.size() > 9) throw new ToolException("at most 9 options");
                var answer = Questions.ask(chatComputer(args), "question", Json.need(args, "question"), options,
                    Json.bool(args, "allow_other", true) || options.isEmpty(), 600);
                return "The player answered: " + answer;
            });

        add("permission_prompt", "Permission prompt", true, """
                Used by CC: Agent's in-game chat to ask the player before a tool runs. Don't call it yourself.""",
            schema(obj("tool_name", prop("string", "Tool."), "input", obj("type", "object"),
                "computer", prop("string", COMPUTER)), "tool_name"),
            args -> permissionPrompt(args));

        add("describe_peripheral", "Describe a peripheral", true, """
                One peripheral in depth: its types and methods, the block (or turtle upgrade) behind it, and the mod that \
                added it with that mod's version and links. CC only reports method names, so for anything you don't \
                already know, search the web for that mod's ComputerCraft docs for this exact version before calling it.""",
            schema(obj("computer", prop("string", COMPUTER), "peripheral", prop("string", "Side or network name.")),
                "computer", "peripheral"),
            args -> describePeripheral(computer(args), Json.need(args, "peripheral")));

        add("turtle", "Drive a turtle", false, """
                Run turtle actions in order, each "function arg ..." from CC's turtle API, e.g. "forward", "turnLeft", \
                "dig", "digUp", "place", "select 2", "refuel 1", "drop 16", "inspect", "getItemDetail 3", "transferTo 5 10", \
                "equipLeft", "craft". Stops at the first action that returns false (set stop_on_failure=false to go on). \
                Returns each result plus the turtle's new position, facing and fuel. Moves need fuel.""",
            schema(obj("computer", prop("string", COMPUTER), "actions", obj("type", "array", "items", obj("type", "string"),
                "description", "Turtle actions in order."), "stop_on_failure", prop("boolean", "Default true."),
                "timeout_seconds", prop("integer", "How long to wait (default 30).")), "computer", "actions"),
            args -> {
                var c = computer(args);
                if (computers.turtle(c) == null) throw new ToolException("computer #" + c.getID() + " isn't a turtle");
                var code = Lua.turtle(Json.array(args, "actions"), Json.bool(args, "stop_on_failure", true));
                var steps = computers.lua(c, code, timeout(args));
                var out = obj("steps", steps);
                var after = computers.describe(c, false);
                for (var key : new String[]{"x", "y", "z", "facing", "fuel", "selected_slot"}) {
                    if (after.has(key)) out.add(key, after.get(key));
                }
                return out;
            });

        add("turtle_inventory", "Turtle inventory", true, """
                A turtle's 16 slots, selected slot, fuel, facing and equipped upgrades (read from the world, works while off).""",
            computerOnly(), args -> world.turtleInventory(computer(args)));

        add("inspect_block", "Inspect a block", true, """
                One block: its state, block entity, items if it's a container (chest, furnace, barrel...), and NBT. Give \
                x/y/z (+ dimension), or a computer plus a side relative to it: "up", "down", or for turtles "front", \
                "back", "left", "right"; plain computers also take "north"/"south"/"east"/"west".""",
            schema(obj("computer", prop("string", COMPUTER), "side", prop("string", "Side relative to the computer."),
                "x", prop("integer", "X"), "y", prop("integer", "Y"), "z", prop("integer", "Z"),
                "dimension", prop("string", "e.g. minecraft:overworld (default: the computer's, else overworld)."))),
            args -> {
                var where = place(args);
                return world.inspect(where.level, where.pos);
            });

        add("scan_area", "Scan nearby blocks", true, """
                Every non-air block in a cube around a computer (or x/y/z), radius 0-8 (default 3), as offsets with block \
                states, plus counts per block type.""",
            schema(obj("computer", prop("string", COMPUTER), "radius", prop("integer", "0-8, default 3."),
                "include_air", prop("boolean", "Also list air."), "x", prop("integer", "X"), "y", prop("integer", "Y"),
                "z", prop("integer", "Z"), "dimension", prop("string", "Dimension for x/y/z."))),
            args -> {
                var where = place(args);
                return world.scan(where.level, where.pos, Json.integer(args, "radius", 3), Json.bool(args, "include_air", false));
            });

        add("watch_events", "Watch events", true, """
                Record the events the computer receives for a while (redstone, modem_message, rednet_message, \
                monitor_touch, peripheral, turtle_inventory, timers, keys...). Good for seeing what a program would get.""",
            schema(obj("computer", prop("string", COMPUTER), "seconds", prop("number", "How long, 0.1-120 (default 5)."),
                "filter", obj("type", "array", "items", obj("type", "string"), "description", "Only these event names."),
                "max_events", prop("integer", "Stop after this many (default 100).")), "computer"),
            args -> {
                double seconds = Math.max(0.1, Math.min(args.has("seconds") ? args.get("seconds").getAsDouble() : 5, 120));
                var code = Lua.watch(seconds, Json.array(args, "filter"), Math.max(1, Json.integer(args, "max_events", 100)));
                return computers.lua(computer(args), code, (int) Math.ceil(seconds) + 15);
            });

        add("cc_help", "CC help", true, """
                Read CC: Tweaked's built-in help (the same pages as the in-game help program) on a topic like "turtle", \
                "peripheral", "rednet", "gps", "monitor", "redstone", "fs", "http", "textutils", "settings". Without a \
                topic, lists every topic.""",
            schema(obj("computer", prop("string", COMPUTER), "topic", prop("string", "Help topic.")), "computer"),
            args -> {
                var topic = Json.str(args, "topic", null);
                var result = computers.lua(computer(args), Lua.help(topic), DEFAULT_TIMEOUT);
                return result.isJsonPrimitive() ? result.getAsString() : result;
            });
    }

    private static final String[] SIDES = {"bottom", "top", "back", "front", "right", "left"};

    private JsonObject describePeripheral(ServerComputer computer, String name) {
        var info = computers.lua(computer, "local n = " + LuaStrings.quote(name) + "\n" + """
            if not peripheral.isPresent(n) then error("no peripheral called " .. n .. " (see list_peripherals)", 0) end
            local methods = peripheral.getMethods(n) or {}
            table.sort(methods)
            return { types = { peripheral.getType(n) }, methods = #methods > 0 and methods or textutils.empty_json_array }
            """, DEFAULT_TIMEOUT).getAsJsonObject();
        var out = obj("peripheral", name);
        out.add("types", info.get("types"));
        out.add("methods", info.get("methods"));

        String blockId = null;
        var side = java.util.Arrays.asList(SIDES).indexOf(name.toLowerCase(Locale.ROOT));
        if (side >= 0) {
            var found = computers.onServer(() -> sideSource(computer, dan200.computercraft.core.computer.ComputerSide.valueOf(side)));
            if (found != null) {
                for (var e : found.entrySet()) out.add(e.getKey(), e.getValue());
                if (found.has("block")) blockId = found.get("block").getAsString();
                else if (found.has("upgrade_item")) blockId = found.get("upgrade_item").getAsString();
            }
        } else {
            // Wired-modem names are "<block id>_<n>" for most blocks.
            var m = java.util.regex.Pattern.compile("^([a-z0-9_.-]+:[a-z0-9_./-]+)_\\d+$").matcher(name);
            if (m.matches()) {
                blockId = m.group(1);
                out.addProperty("block", blockId);
                out.addProperty("connection", "wired modem network");
            } else {
                out.addProperty("connection", "wired modem network (the name doesn't say which block it is)");
            }
        }

        var modId = blockId != null && blockId.contains(":") ? blockId.substring(0, blockId.indexOf(':')) : null;
        if (modId != null) {
            var meta = CCAgent.mod(modId);
            if (meta != null) {
                var mod = obj("id", meta.id(), "name", meta.name(), "version", meta.version(),
                    "homepage", meta.homepage(), "sources", meta.sources(), "issues", meta.issues());
                out.add("mod", mod);
            } else {
                out.addProperty("mod_id", modId);
            }
        }
        var type = info.get("types").getAsJsonArray().size() > 0 ? info.get("types").getAsJsonArray().get(0).getAsString() : name;
        var modName = out.has("mod") ? out.getAsJsonObject("mod").get("name").getAsString() + " "
            + out.getAsJsonObject("mod").get("version").getAsString() : modId != null ? modId : "";
        out.addProperty("docs_search", (modName + " ComputerCraft peripheral \"" + type + "\" methods").trim());
        out.addProperty("note", "Method names only come from CC. Look up the arguments in the mod's docs (search above, "
            + "or the mod's homepage/sources) for this version, or test carefully with call_peripheral.");
        return out;
    }

    /** What sits on one side of a computer or turtle: a neighbouring block, or a turtle upgrade. */
    private JsonObject sideSource(ServerComputer computer, dan200.computercraft.core.computer.ComputerSide side) {
        var level = computer.getLevel();
        var pos = computer.getPosition();
        if (!level.isLoaded(pos)) return null;
        var be = level.getBlockEntity(pos);
        Direction front;
        boolean swapSides = false;
        if (be instanceof dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity turtle) {
            var access = turtle.getAccess();
            front = access.getDirection();
            var turtleSide = side == dan200.computercraft.core.computer.ComputerSide.LEFT ? dan200.computercraft.api.turtle.TurtleSide.LEFT
                : side == dan200.computercraft.core.computer.ComputerSide.RIGHT ? dan200.computercraft.api.turtle.TurtleSide.RIGHT : null;
            if (turtleSide != null && access.getUpgrade(turtleSide) != null) {
                var upgrade = access.getUpgrade(turtleSide);
                var item = upgrade.getCraftingItem();
                var out = obj("connection", "turtle upgrade", "upgrade", upgrade.getAdjective().getString());
                if (!item.isEmpty()) out.addProperty("upgrade_item", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem()).toString());
                return out;
            }
        } else if (be instanceof dan200.computercraft.shared.computer.blocks.ComputerBlockEntity block) {
            front = block.getDirection();
            // CC computers swap left and right: their front faces the player, a turtle's faces away.
            swapSides = true;
        } else {
            return obj("connection", "attached to a " + kind(computer) + " (no fixed position)");
        }
        var want = !swapSides ? side : side == dan200.computercraft.core.computer.ComputerSide.LEFT
            ? dan200.computercraft.core.computer.ComputerSide.RIGHT
            : side == dan200.computercraft.core.computer.ComputerSide.RIGHT ? dan200.computercraft.core.computer.ComputerSide.LEFT : side;
        for (var dir : Direction.values()) {
            if (dan200.computercraft.shared.util.DirectionUtil.toLocal(front, dir) != want) continue;
            var at = pos.relative(dir);
            var state = level.getBlockState(at);
            return obj("connection", "adjacent block", "block",
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(),
                "x", at.getX(), "y", at.getY(), "z", at.getZ());
        }
        return null;
    }

    private String kind(ServerComputer computer) {
        return computers.kind(computer).replace('_', ' ');
    }

    /** One line about a tool call, for the activity log. */
    public static String summary(String tool, JsonObject args) {
        String text = switch (tool) {
            case "run_lua" -> Json.str(args, "code", "");
            case "run_shell" -> Json.str(args, "command", "");
            case "write_file", "read_file", "edit_file", "delete_file", "make_dir", "list_files" -> Json.str(args, "path", "/");
            case "move_file" -> Json.str(args, "from", "") + " -> " + Json.str(args, "to", "");
            case "call_peripheral" -> Json.str(args, "peripheral", "") + "." + Json.str(args, "method", "") + "()";
            case "describe_peripheral" -> Json.str(args, "peripheral", "");
            case "turtle" -> String.join(", ", java.util.stream.StreamSupport.stream(Json.array(args, "actions").spliterator(), false)
                .map(JsonElement::getAsString).toList());
            case "type_text" -> Json.str(args, "text", "");
            case "press_keys" -> String.join(" ", java.util.stream.StreamSupport.stream(Json.array(args, "keys").spliterator(), false)
                .map(JsonElement::getAsString).toList());
            case "power" -> Json.str(args, "action", "");
            case "queue_event" -> Json.str(args, "name", "");
            case "cc_help" -> Json.str(args, "topic", "topics");
            case "get_job", "cancel_job" -> "job " + Json.str(args, "job_id", "?");
            default -> "";
        };
        text = text.replaceAll("\\s+", " ").trim();
        return text.length() > 120 ? text.substring(0, 117) + "..." : text;
    }

    private record Place(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
    }

    private Place place(JsonObject args) {
        ServerComputer c = args.has("computer") && !args.get("computer").isJsonNull() ? computer(args) : null;
        if (args.has("x") && args.has("y") && args.has("z")) {
            var dim = Json.str(args, "dimension", null);
            var level = dim != null ? world.level(dim) : c != null ? c.getLevel() : world.level(null);
            var at = new BlockPos(Json.integer(args, "x", 0), Json.integer(args, "y", 0), Json.integer(args, "z", 0));
            Access.check(level, at);
            return new Place(level, at);
        }
        if (c == null) throw new ToolException("give x, y and z, or a computer");
        var pos = c.getPosition();
        var side = Json.str(args, "side", null);
        if (side == null) return new Place(c.getLevel(), pos);
        var turtle = computers.turtle(c);
        Direction facing = turtle != null ? computers.onServer(() -> turtle.getAccess().getDirection()) : null;
        Direction dir = switch (side.toLowerCase(Locale.ROOT)) {
            case "up", "top" -> Direction.UP;
            case "down", "bottom" -> Direction.DOWN;
            case "front" -> need(facing, side);
            case "back" -> need(facing, side).getOpposite();
            case "left" -> need(facing, side).getCounterClockWise();
            case "right" -> need(facing, side).getClockWise();
            default -> {
                var named = Direction.byName(side.toLowerCase(Locale.ROOT));
                if (named == null) throw new ToolException("unknown side " + side);
                yield named;
            }
        };
        return new Place(c.getLevel(), pos.relative(dir));
    }

    private static Direction need(Direction facing, String side) {
        if (facing == null) throw new ToolException("\"" + side + "\" only works for turtles; use north/south/east/west/up/down");
        return facing;
    }

    private Object runJob(ServerComputer computer, String kind, String code, int timeout) {
        var job = computers.submit(computer, kind, code, false);
        return formatJob(job, Computers.await(job, timeout), timeout);
    }

    private static Object formatJob(Jobs.Job job, Jobs.Result result, int waited) {
        if (result == null) {
            return new ToolResult("job " + job.id + " is still running after " + waited + " s. Check it with get_job, "
                + "stop it with cancel_job.", false);
        }
        var text = new StringBuilder();
        text.append(result.ok() ? "ok" : "error").append(" (job ").append(job.id).append(", ")
            .append(String.format(Locale.ROOT, "%.2f", result.millis() / 1000.0)).append(" s)\n");
        if (result.ok()) {
            if (result.values() != null && !result.values().isEmpty()) {
                text.append("returned: ").append(result.values()).append('\n');
            }
        } else if (result.error() != null) {
            text.append("error: ").append(result.error()).append('\n');
        }
        var output = result.output() == null ? "" : result.output();
        text.append(output.isEmpty() ? "(no output)" : "output:\n" + output);
        return new ToolResult(text.toString(), !result.ok());
    }

    public record ToolResult(String text, boolean error) {
    }

    /** Lua snippets behind the higher-level tools. Each returns one value, sent back as JSON. */
    static final class Lua {
        static final String PERIPHERALS = """
            local out = {}
            for _, name in ipairs(peripheral.getNames()) do
                local methods = peripheral.getMethods(name) or {}
                table.sort(methods)
                out[#out + 1] = { name = name, types = { peripheral.getType(name) }, methods = methods }
            end
            if #out == 0 then return textutils.empty_json_array end
            return out
            """;

        static String packed(String call) {
            return "local r = table.pack((function() " + call + " end)())\n"
                + "local out = {}\n"
                + "for i = 1, r.n do out[i] = r[i] == nil and textutils.json_null or r[i] end\n"
                + "if r.n == 0 then return textutils.empty_json_array end\n"
                + "return out";
        }

        static String turtle(JsonArray actions, boolean stopOnFailure) {
            var steps = new StringBuilder("{");
            for (JsonElement action : actions) {
                var parts = action.getAsString().trim().split("\\s+");
                if (parts.length == 0 || parts[0].isEmpty()) continue;
                steps.append("table.pack(").append(LuaStrings.quote(parts[0]));
                for (int i = 1; i < parts.length; i++) {
                    var p = parts[i];
                    steps.append(", ");
                    if (p.matches("-?\\d+(\\.\\d+)?") || p.equals("true") || p.equals("false")) steps.append(p);
                    else steps.append(LuaStrings.quote(p));
                }
                steps.append("), ");
            }
            steps.append("}");
            return "local steps = " + steps + "\nlocal stop = " + stopOnFailure + "\n" + """
                if not turtle then error("this computer isn't a turtle", 0) end
                local results = {}
                for i, s in ipairs(steps) do
                    local entry = { step = i, action = table.concat({ table.unpack(s, 1, s.n) }, " ") }
                    results[#results + 1] = entry
                    local fn = turtle[s[1]]
                    if type(fn) ~= "function" then
                        entry.ok = false
                        entry.error = "turtle." .. tostring(s[1]) .. " doesn't exist"
                        break
                    end
                    local r = table.pack(pcall(fn, table.unpack(s, 2, s.n)))
                    if not r[1] then
                        entry.ok = false
                        entry.error = tostring(r[2])
                        break
                    end
                    local values = {}
                    for j = 2, r.n do values[j - 1] = r[j] == nil and textutils.json_null or r[j] end
                    entry.result = r.n > 1 and values or textutils.empty_json_array
                    entry.ok = r[2] ~= false
                    if r[2] == false and type(r[3]) == "string" then entry.error = r[3] end
                    if r[2] == false and stop then break end
                end
                return results
                """;
        }

        static String watch(double seconds, JsonArray filter, int max) {
            var names = new StringBuilder("{");
            for (var name : filter) names.append('[').append(LuaStrings.quote(name.getAsString())).append("] = true, ");
            names.append('}');
            return "local only = " + names + "\nlocal has_filter = next(only) ~= nil\nlocal max = " + max + "\n"
                + "local timer = os.startTimer(" + seconds + ")\n" + """
                local start = os.epoch("utc")
                local events = {}
                while #events < max do
                    local e = table.pack(os.pullEventRaw())
                    if e[1] == "timer" and e[2] == timer then break end
                    if not has_filter or only[e[1]] then
                        local args = {}
                        for i = 2, e.n do
                            local v = e[i]
                            if v == nil then v = textutils.json_null
                            elseif type(v) == "table" then v = textutils.serialise(v, { compact = true })
                            elseif type(v) ~= "string" and type(v) ~= "number" and type(v) ~= "boolean" then v = tostring(v) end
                            args[#args + 1] = v
                        end
                        events[#events + 1] = { name = e[1], args = #args > 0 and args or textutils.empty_json_array,
                            ms = os.epoch("utc") - start }
                    end
                end
                if #events == 0 then return textutils.empty_json_array end
                return events
                """;
        }

        static String help(String topic) {
            if (topic == null || topic.isBlank()) {
                return "local t = help.topics() table.sort(t) return table.concat(t, \" \")";
            }
            return "local path = help.lookup(" + LuaStrings.quote(topic.trim()) + ")\n" + """
                if not path then
                    return "No help page for that topic. Call cc_help without a topic to list them."
                end
                local f = fs.open(path, "r")
                local text = f.readAll()
                f.close()
                return text
                """;
        }
    }

    /** CC key names to GLFW key codes (what CC: Tweaked's key events carry). */
    static final class Keys {
        private static final Map<String, Integer> CODES = new LinkedHashMap<>();

        static {
            CODES.put("space", 32);
            CODES.put("apostrophe", 39);
            CODES.put("comma", 44);
            CODES.put("minus", 45);
            CODES.put("period", 46);
            CODES.put("slash", 47);
            CODES.put("semicolon", 59);
            CODES.put("equals", 61);
            CODES.put("leftbracket", 91);
            CODES.put("backslash", 92);
            CODES.put("rightbracket", 93);
            CODES.put("grave", 96);
            for (char c = '0'; c <= '9'; c++) CODES.put(String.valueOf(c), (int) c);
            for (char c = 'a'; c <= 'z'; c++) CODES.put(String.valueOf(c), (int) Character.toUpperCase(c));
            CODES.put("escape", 256);
            CODES.put("enter", 257);
            CODES.put("return", 257);
            CODES.put("tab", 258);
            CODES.put("backspace", 259);
            CODES.put("insert", 260);
            CODES.put("delete", 261);
            CODES.put("right", 262);
            CODES.put("left", 263);
            CODES.put("down", 264);
            CODES.put("up", 265);
            CODES.put("pageup", 266);
            CODES.put("pagedown", 267);
            CODES.put("home", 268);
            CODES.put("end", 269);
            for (int i = 1; i <= 12; i++) CODES.put("f" + i, 289 + i);
            CODES.put("leftshift", 340);
            CODES.put("shift", 340);
            CODES.put("leftctrl", 341);
            CODES.put("ctrl", 341);
            CODES.put("leftalt", 342);
            CODES.put("alt", 342);
            CODES.put("rightshift", 344);
            CODES.put("rightctrl", 345);
            CODES.put("rightalt", 346);
        }

        static void press(ServerComputer computer, String name) {
            var key = name.trim().toLowerCase(Locale.ROOT);
            switch (key) {
                case "terminate", "ctrl+t" -> {
                    computer.queueEvent("terminate", null);
                    return;
                }
                case "reboot", "ctrl+r" -> {
                    computer.reboot();
                    return;
                }
                case "shutdown" -> {
                    computer.shutdown();
                    return;
                }
                default -> {
                }
            }
            var parts = key.equals("+") ? new String[]{"+"} : key.split("\\+");
            var codes = new ArrayList<Integer>();
            for (var part : parts) {
                var code = CODES.get(part);
                if (code == null) throw new ToolException("unknown key \"" + part + "\". Known: " + String.join(", ", CODES.keySet()));
                codes.add(code);
            }
            boolean modified = codes.size() > 1;
            for (int i = 0; i < codes.size(); i++) {
                computer.queueEvent("key", new Object[]{codes.get(i), false});
                // A plain printable key also types its character, like the real keyboard.
                var part = parts[i];
                if (!modified && (part.length() == 1 || part.equals("space"))) {
                    computer.queueEvent("char", new Object[]{part.equals("space") ? " " : part});
                }
            }
            for (int i = codes.size() - 1; i >= 0; i--) computer.queueEvent("key_up", new Object[]{codes.get(i)});
        }
    }
}
