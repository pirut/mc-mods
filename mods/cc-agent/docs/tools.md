# Tools

These are the tools the agent gets. In the chat it also gets web search (for mod docs).

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
| `describe_peripheral` | The block or turtle upgrade behind a peripheral and the mod that added it (name, version, links), so the agent can look up that mod's docs online |
| `turtle` | A list of turtle actions (`forward`, `dig`, `select 2`, `refuel`...) with each result and the new position |
| `turtle_inventory` | All 16 slots, fuel, facing, upgrades |
| `inspect_block`, `scan_area` | A block (with chest contents and NBT) or every block within 8 blocks of a computer |
| `watch_events` | Record the events a computer receives for a few seconds |
| `cc_help` | CC: Tweaked's own help pages |
| `ask_player` | Ask the player a question with answers to click, on their CC computer |

Peripherals from addon mods (Advanced Peripherals, Create, Mekanism and the rest) work with no extra
setup, since everything goes through CC: Tweaked's own peripheral system. CC only reports method names, so
agents are told to call `describe_peripheral` and read that mod's docs online before guessing arguments.

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

