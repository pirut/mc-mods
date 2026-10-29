# CC: Agent

Let Claude Code, Codex, or any MCP client write programs for and drive your
[CC: Tweaked](https://modrinth.com/mod/cc-tweaked) computers and turtles.

While a world is open, the mod runs a small [MCP](https://modelcontextprotocol.io) server on your own
machine. Point your coding agent at it and it can list your computers, write and edit their files, run
programs and Lua and read what they print, look at the screen, type and press keys, call peripherals,
drive turtles, read chests and nearby blocks, and watch the events a computer receives. You ask for a
program in plain words; the agent writes it on the computer, runs it, reads the errors, and fixes it.

Minecraft 1.21.1, NeoForge and Fabric. Needs CC: Tweaked 1.120.0 or newer (built and tested on 1.120.2).

## Setup

1. Put the CC: Agent jar for your loader in `mods/` next to CC: Tweaked (and Fabric API on Fabric).
2. Open a singleplayer world and run `/ccagent`. It shows the server address and two links:
   - **`/ccagent claude`** gives you a Claude Code command to copy.
   - **`/ccagent codex`** gives you a Codex config block to copy.
3. Connect your agent once (below). The token stays the same across worlds and restarts, so you only do
   this once per machine.

### Claude Code

Run the command `/ccagent claude` copied for you. It looks like this:

```sh
claude mcp add --scope user --transport http cc-agent http://127.0.0.1:25599/mcp \
  --header "Authorization: Bearer YOUR_TOKEN"
```

Then start `claude` anywhere and ask, for example: *"On computer 0, write a program that shows the time
on the monitor on its left and run it."* Check the connection with `claude mcp list`.

### Codex

Paste the block `/ccagent codex` copied into `~/.codex/config.toml`:

```toml
[mcp_servers.cc-agent]
url = "http://127.0.0.1:25599/mcp"
http_headers = { "Authorization" = "Bearer YOUR_TOKEN" }
```

Or keep the token out of the file: `export CC_AGENT_TOKEN=YOUR_TOKEN`, then
`codex mcp add cc-agent --url http://127.0.0.1:25599/mcp --bearer-token-env-var CC_AGENT_TOKEN`.

### Other MCP clients

Any client that speaks Streamable HTTP works: URL `http://127.0.0.1:25599/mcp`, header
`Authorization: Bearer YOUR_TOKEN`. The token is in `config/ccagent.properties`.

## Tools

| Tool | What it does |
| --- | --- |
| `list_computers` | Every loaded computer, turtle and pocket computer, plus placed ones never started |
| `computer_info` | Kind, on/off, position, screen size, nearest player, turtle fuel and facing |
| `power` | Turn on, shut down, reboot |
| `run_lua` | Run Lua and get the returned values and everything it printed |
| `run_shell` | Run a shell command line (`myprogram args`, `ls`, `pastebin get ...`) and capture output |
| `get_job`, `cancel_job` | Follow up on long-running Lua or shell jobs |
| `read_screen` | The terminal as text (optionally with blit colors) and the cursor |
| `type_text`, `press_keys` | Type into the real screen and press keys (`enter`, `up`, `ctrl+s`, `terminate`...) |
| `queue_event` | Queue any event, for programs waiting on `os.pullEvent` |
| `list_files`, `read_file`, `write_file`, `edit_file`, `delete_file`, `move_file`, `make_dir` | The computer's own drive, straight from the save, even while it's off |
| `list_peripherals`, `call_peripheral` | Every attached or networked peripheral, its types and methods, and calling them |
| `turtle` | A list of turtle actions (`forward`, `dig`, `select 2`, `refuel`...) with each result and the new position |
| `turtle_inventory` | All 16 slots, fuel, facing, upgrades |
| `inspect_block`, `scan_area` | A block (with chest contents and NBT) or every block within 8 blocks of a computer |
| `watch_events` | Record the events a computer receives for a few seconds |
| `cc_help` | CC: Tweaked's own help pages |

Computers placed but never clicked haven't been started by CC: Tweaked yet. `list_computers` shows them
by position, and any tool takes `"x,y,z"` as the computer to start one.

## How it runs code

CC: Tweaked runs one Lua program per computer, so the mod adds a small ROM autorun script
(`rom/autorun/ccagent.lua`) to every computer. It wraps `os.pullEventRaw`: when an agent sends a job,
whatever program is waiting for an event (normally the shell prompt) runs the job in place, with its
output going to an invisible window that is sent back to the agent, and the events that arrived meanwhile
are handed back to the program afterwards. The real screen isn't touched. Jobs run side by side.

So a job can't run on a computer whose program never waits for events, or that replaced
`os.pullEventRaw` itself. `read_screen`, `type_text` and `press_keys` still work there, and `power`
with `reboot` starts fresh. To keep agents away from one computer, run
`set ccagent.enabled false` on it and reboot.

## Safety

- The server only listens on `127.0.0.1` and every request needs the token. Browsers' cross-site requests
  are refused.
- It's off on dedicated servers unless you set `dedicatedServer=true`. Anyone with the token controls every
  loaded computer on the server, so only do that on a server you run, and keep `host=127.0.0.1` unless you
  know what you're opening.
- `/ccagent` (which shows the token) works for the singleplayer host and level 4 operators only.
- `allowShellAndLua=false` or `allowFileWrites=false` in `config/ccagent.properties` turn off running code
  or changing files. Delete the `token=` line to make a new token.

Agents act through the same CC: Tweaked APIs a program on the computer could use. They can't do anything
in the world a computer couldn't.

## Config

`config/ccagent.properties` (made the first time a world opens):

| Key | Default | |
| --- | --- | --- |
| `enabled` | `true` | Run the MCP server while a world is open |
| `dedicatedServer` | `false` | Also run it on dedicated servers |
| `host` | `127.0.0.1` | Address to listen on |
| `port` | `25599` | Port |
| `token` | random | Bearer token agents send |
| `allowShellAndLua` | `true` | Let agents run Lua and shell commands |
| `allowFileWrites` | `true` | Let agents write, move and delete files |

## Building

```sh
./gradlew build
```

Jars land in `neoforge/build/libs` and `fabric/build/libs`. `common/` holds the shared code; each loader
folder adds only its entry point. `./gradlew :neoforge:runClient` or `:fabric:runClient` starts a dev game.

## License

LGPL-3.0-only. See `COPYING` and `COPYING.LESSER`. CC: Tweaked is by SquidDev and contributors (MPL-2.0 and
others); this mod is not affiliated with it.
