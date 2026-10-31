# CC: Agent

**Claude Code and Codex inside your [CC: Tweaked](https://modrinth.com/mod/cc-tweaked) computers.** Walk up to
a computer, type `claude`, and ask for what you want: a program that shows your base's power on a monitor, a
turtle that digs a tunnel, a door that opens for your friends. The agent writes the program on that computer,
runs it, reads the errors and fixes them, while you watch.

![Claude Code running on a wall of CC monitors](docs/images/tui-monitor-wall.jpg)

*The real Claude Code, on an 8x6 wall of advanced monitors, after reading and explaining a program.*

Minecraft 1.21.1 · NeoForge and Fabric · CC: Tweaked 1.120.0 or newer · singleplayer and servers

**[Download the latest release](https://github.com/pirut/cc-agent/releases/latest)** ·
[Getting started](docs/getting-started.md) · [Using the chat](docs/chat.md) ·
[Troubleshooting](docs/troubleshooting.md) · [Testing checklist](docs/testing.md)

## What it does

The agent runs on **your own PC, signed in to your own Claude or ChatGPT account** (no API keys). The mod
gives it a set of tools for the CC world: list computers, read and write their files, run programs and Lua,
read the screen, use peripherals, drive turtles, look at nearby blocks. It can only do what a CC computer
could do.

There are three ways to use it:

| | How | Best for |
| --- | --- | --- |
| **Chat** | Type `claude` or `codex` on any computer | Asking for things while you play |
| **Full screen** | Type `claude --tui` with an advanced monitor attached | The real Claude Code / Codex screen, everything included |
| **Your terminal** | Connect Claude Code or Codex on your PC to the game once | Bigger jobs from your desk |

### The chat

![The chat on a CC computer: Claude asks a question with buttons](docs/images/chat-question.jpg)

Talk to the agent right on the CC screen. Each step it takes shows as a line. It can ask you questions,
and you click the answer (or press its number, or type your own).

| | |
| --- | --- |
| ![Permission prompt: Allow, Allow for this chat, Deny](docs/images/chat-permission.jpg) | ![Plan mode: go ahead with the plan?](docs/images/chat-plan.jpg) |
| **Permissions.** In *ask* mode it asks before anything that changes your world. | **Plan mode.** It looks around, writes a plan, and waits for your go. |

It has the same controls as the terminal version: `/model`, `/effort`, `/mode` (or Shift+Tab), `/resume`,
`/new`, the agent's own commands like `/context` and `/compact`, a todo list while it works, and you can type
your next message while it's busy, and the mouse wheel scrolls back through the conversation. See
[Using the chat](docs/chat.md).

![Picking a model with /model](docs/images/chat-model-picker.jpg)

### The full screen

`claude --tui` and `codex --tui` run the agent's own terminal program on your PC and draw it on the biggest
advanced monitor attached to the computer. Everything the terminal version has works: slash commands,
permission prompts, plan mode, the lot. You type on the computer; its screen shows the part around the
prompt. Hold Ctrl+T to quit.

| | |
| --- | --- |
| ![Claude Code full screen on a server computer](docs/images/tui-on-a-server.jpg) | ![Codex full screen](docs/images/codex-tui.jpg) |
| Without a monitor (`claude --tui term`) it uses the computer's own screen. | Codex works too. |

### Things it has built

| | |
| --- | --- |
| ![A monitor dashboard Claude Code wrote](docs/images/dashboard-written-by-claude-code.jpg) | ![Claude using Advanced Peripherals](docs/images/claude-uses-advanced-peripherals.jpg) |
| A dashboard with the time and a chest's contents, written from one sentence. | Addon peripherals work too: here Advanced Peripherals' environment detector. |
| ![agentlog showing every tool call](docs/images/agentlog.jpg) | ![Claude on a multiplayer server](docs/images/multiplayer-claude-on-server.jpg) |
| `agentlog` shows every step an agent takes, live. | On servers, each player's agent runs on their own PC. |

## Quick start

1. Install [Claude Code](https://code.claude.com/docs/en/setup) or [Codex](https://github.com/openai/codex)
   on your PC and sign in once by running `claude` (or `codex`) in a terminal.
2. Add CC: Tweaked and the CC: Agent jar for your loader to your mods folder (Fabric also needs Fabric API).
3. Start a world, place an **advanced computer**, right-click it and type `claude`.

The [getting started guide](docs/getting-started.md) walks through every step, including the monitor wall.

## Docs

- [Getting started](docs/getting-started.md): installing everything, first program, building a monitor wall
- [Using the chat](docs/chat.md): commands, modes, questions, permissions, keys
- [Your own terminal](docs/terminal.md): connecting Claude Code, Codex or any MCP client
- [Multiplayer](docs/multiplayer.md): servers, limits for server owners
- [Tools](docs/tools.md): every tool the agent gets, and how it runs code on a computer
- [Configuration](docs/configuration.md): every setting in `config/ccagent.properties`
- [Troubleshooting](docs/troubleshooting.md)
- [Testing checklist](docs/testing.md): what to try and how to report a bug
- [Changelog](CHANGELOG.md)

## Safety

- The agent only gets CC: Agent's tools and web search: it can't touch your PC's files or run commands on
  your PC from the chat. It acts through the same CC APIs a program on the computer could use.
- The mod's server listens only on `127.0.0.1` and needs a token; browsers can't reach it.
- On servers, the agent runs on the player's PC; the server only runs the tool calls, with limits (range,
  rate, operators only for command computers).
- `set ccagent.enabled false` on a computer (then reboot) keeps agents off it.

## Building

```sh
./gradlew build
```

Jars land in `neoforge/build/libs` and `fabric/build/libs`. `common/` holds the shared code; each loader
folder adds only its entry point. `./gradlew :neoforge:runClient` or `:fabric:runClient` starts a dev game.

## License

LGPL-3.0-only. See `COPYING` and `COPYING.LESSER`. The jars bundle [pty4j](https://github.com/JetBrains/pty4j)
(EPL-1.0) and the Kotlin standard library (Apache-2.0) for the terminal behind `--tui`. CC: Tweaked is by
SquidDev and contributors; this mod is not affiliated with it, with Anthropic or with OpenAI.
