# Changelog

## 0.5.0

- **The chat now works like the terminal version.**
  - `/model`, `/effort` and `/mode` open pickers you click (or press a number). Settings are kept per
    computer.
  - Modes: **auto** (uses CC tools without asking), **ask** (asks before anything that changes things) and
    **plan** (plans first, then asks to go ahead). Shift+Tab cycles them.
  - Permission prompts with **Allow**, **Allow for this chat** and **Deny**. You can also type a reason.
  - `/resume` goes back to an earlier conversation.
  - Other slash commands (`/context`, `/compact`, your own) go to the agent.
  - Tab completes commands. You can type ahead while it works; the message waits its turn.
  - It shows the agent's todo list and thinking, and after each reply the model and how full the context is.
- **Agents can ask you questions** with answers to click: the new `ask_player` tool. It works in the chat and
  from your own terminal (it asks on the computer you name).
- New docs with screenshots: getting started, the chat, troubleshooting and a testing checklist.

## 0.4.0

- **The real Claude Code and Codex screens in CC.** `claude --tui` (or `codex --tui`) runs the agent's
  own full-screen terminal UI on your PC and draws it on the biggest advanced monitor attached to the
  computer, at the largest text size that still fits 80x24. Type on the computer: its screen shows the
  part you're working in, following the prompt. `claude --tui left` picks a monitor, `claude --tui term`
  uses the computer's own screen. Hold Ctrl+T to quit, or use the agent's own exit.
- Colors are matched per session, so Claude Code's orange stays orange; box drawing and blocks use CC's
  teletext characters.
- Works in multiplayer the same way as the chat: the TUI runs on the player's PC and the screen is relayed
  to the server's computer.
- Bundles pty4j (EPL-1.0) and the Kotlin standard library (Apache-2.0) for the terminal; the jars are
  about 4.5 MB now.

## 0.3.0

- **Multiplayer.** With CC: Agent on both the server and your game, everything works on servers: the
  in-game `claude`/`codex` programs and agents you drive from your own terminal. The agent always runs on
  your own PC with your own login; your game forwards its tool calls to the server, which runs them with
  limits. Same setup as singleplayer (same address and token).
- Server owners: `relay`, `relayOpsOnly`, `relayRange` (default 128 blocks from the player; operators have
  no limit) and `relayCallsPerMinute` (default 240) in `config/ccagent.properties`. Command computers are
  operators only. Players only see their own jobs.
- Fixed: Codex refused every CC: Agent tool ("requires approval, but approval policy is never"). The in-game
  codex program now approves CC: Agent's tools, and `/ccagent codex` adds
  `default_tools_approval_mode = "approve"` for terminal use.
- Fixed: "couldn't find codex/claude" when the game doesn't get your terminal's PATH. It now also looks in
  winget, scoop, nvm, Volta, pnpm and the Codex app's folders, and asks your login shell on macOS/Linux.

## 0.2.0

- **Talk to Claude Code or Codex from any CC computer.** Type `claude` or `codex` at the shell (or
  `claude <request>`). The agent runs on your PC, signed in as you, and the conversation streams onto the
  CC screen with a line for each step it takes. Follow-ups continue the conversation; `/new` starts over,
  Ctrl+T stops a reply. The agent only gets CC: Agent's tools plus web search.
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
