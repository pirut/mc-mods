# Multiplayer

Put CC: Agent on the server **and** in each player's game. Then everything works on the server: the
`claude`/`codex` chat, `--tui`, and agents in your own terminal.

![Claude on a server computer](images/multiplayer-claude-on-server.jpg)

How it works: the agent always runs on **your own PC**, signed in as you. When it uses a tool, your game
sends the call to the server, which runs it with the server's limits and sends the result back. The server
never runs an agent or sees your login. For the full screen (`--tui`), your PC runs Claude Code or Codex and
the server's computer shows its screen.

Each player needs Claude Code or Codex installed and signed in on their own PC, the same as in
singleplayer (see [Getting started](getting-started.md)).

## Limits (for server owners)

In the server's `config/ccagent.properties`:

| Key | Default | |
| --- | --- | --- |
| `relay` | `true` | Allow players' agents at all |
| `relayOpsOnly` | `false` | Only operators may use agents |
| `relayRange` | `128` | Blocks from the player their agent can reach (0 = anywhere loaded). Operators have no limit |
| `relayCallsPerMinute` | `240` | Tool calls per player per minute |

Command computers are operators only. Players only see their own jobs. Questions and permission prompts
appear on the computer the player is chatting from.

You don't need `dedicatedServer=true` for any of this: that setting runs an MCP server *on the server
machine itself*, for an agent running there.
