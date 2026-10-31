# Configuration

`config/ccagent.properties` in your game folder (made the first time a world opens). Changes apply the next
time a world opens.

| Key | Default | |
| --- | --- | --- |
| `enabled` | `true` | Run the MCP server while a world is open |
| `dedicatedServer` | `false` | Also run it on a dedicated server, for an agent running on the server machine |
| `host` | `127.0.0.1` | Address to listen on. Keep it unless you know what you're opening |
| `port` | `25599` | Port |
| `token` | random | The token agents send. Delete the line to make a new one |
| `allowShellAndLua` | `true` | Let agents run Lua and shell commands |
| `allowFileWrites` | `true` | Let agents write, move and delete files |
| `launchAgents` | `true` | Let the in-game `claude`/`codex` programs start the CLIs on this PC |
| `claudeCommand`, `codexCommand` | `claude`, `codex` | How to start them. A full path works if the game can't find them |
| `claudeArgs`, `codexArgs` | empty | Extra arguments for every chat turn |
| `relay` | `true` | Server: accept tool calls from players' own agents |
| `relayOpsOnly` | `false` | Server: only operators may |
| `relayRange` | `128` | Server: how far from the player their agent can reach (0 = anywhere loaded; ops unlimited) |
| `relayCallsPerMinute` | `240` | Server: per-player rate limit |

Per computer (CC settings, kept in the computer's `.settings` file):

| Setting | |
| --- | --- |
| `ccagent.enabled` | `false` keeps agents off this computer (reboot after changing) |
| `ccagent.claude.model`, `.effort`, `.mode` | What `/model`, `/effort`, `/mode` chose (same for `ccagent.codex.*`) |

## Where things live on your PC

- `<game folder>/ccagent/workspace`: the chat's scratch folder (and its MCP config files)
- `<game folder>/ccagent/tui`: the full screen's scratch folder, with a `CLAUDE.md`/`AGENTS.md` telling the
  agent it's inside Minecraft
