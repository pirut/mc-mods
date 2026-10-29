# Changelog

## 0.2.0

- **Talk to Claude Code or Codex from any CC computer.** Type `claude` or `codex` at the shell (or
  `claude <request>`). The agent runs on your PC, signed in as you, and the conversation streams onto the
  CC screen with a line for each step it takes. Follow-ups continue the conversation; `/new` starts over,
  Ctrl+T stops a reply. Singleplayer only, and the agent only gets CC: Agent's tools plus web search.
  `launchAgents`, `claudeCommand`, `codexCommand`, `claudeArgs` and `codexArgs` in the config.
- **`agentlog`** shows every tool call agents make, live, on the computer or a monitor (`agentlog right`).
- **`describe_peripheral`** tool: a peripheral's methods, the block or turtle upgrade behind it, and the
  mod that added it with its version and links, so agents can look up that mod's docs online. Works with
  any addon (tested with Advanced Peripherals).

## 0.1.0

First release. Minecraft 1.21.1, NeoForge and Fabric, CC: Tweaked 1.120.0 or newer.

- A local MCP server (127.0.0.1, token protected) runs while a world is open. `/ccagent claude` and
  `/ccagent codex` copy the one-time setup for Claude Code and Codex.
- 26 tools: list computers and turtles (including placed ones never started), power, run Lua and shell
  commands with their output, read the screen, type and press keys, queue events, read/write/edit/move/delete
  files, list and call peripherals, drive turtles, turtle inventory, inspect blocks and chests, scan nearby
  blocks, watch events, and CC's built-in help.
- Jobs run inside whatever program is waiting for events, so the real screen isn't touched. Turn agents off
  for one computer with `set ccagent.enabled false`.
- Off on dedicated servers unless `dedicatedServer=true`.
