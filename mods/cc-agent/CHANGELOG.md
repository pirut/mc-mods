# Changelog

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
