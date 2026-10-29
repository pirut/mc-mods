# Testing checklist

Thanks for testing! Go down the list, tick what works, and note anything odd. Screenshots help a lot (F2 in
game saves one to `screenshots/`).

Setup: [Getting started](getting-started.md). Note your **loader** (NeoForge or Fabric), **OS**, and
**CC: Agent version** (`ccagent.version()` in the Lua prompt: type `lua`, then that).

## Chat (`claude` on an advanced computer)

- [ ] `claude` opens the chat; a simple request works ("make a program that prints hello")
- [ ] The program it wrote runs from the shell afterwards
- [ ] Follow-up messages remember the conversation
- [ ] `/model`: pick one by clicking; the header shows it; the next reply uses it
- [ ] `/effort`: pick one
- [ ] `/mode` → **ask**: it asks before writing files; **Allow**, **Allow for this chat** and **Deny** all
      do what they say; typing a reason instead works too
- [ ] `/mode` → **plan**: it shows a plan and asks to go ahead; both "Yes" answers work
- [ ] Shift+Tab cycles auto → ask → plan
- [ ] "Ask me which color I want, then ..." shows a question you can click, answer by number, or type
- [ ] Typing while it works queues your message
- [ ] Ctrl+T (hold) stops a reply; Ctrl+T again leaves
- [ ] `/context` shows the agent's context usage; `/compact` works
- [ ] `/resume` lists earlier conversations and goes back to one
- [ ] `/new`, `/clear`, `/status`, `/help`, Tab completion
- [ ] The same with `codex` (if you have Codex)

## Full screen (`claude --tui`)

- [ ] On a monitor wall: the real Claude Code screen appears, readable
- [ ] Typing on the computer works, including Enter, arrows, backspace, Ctrl+C
- [ ] The computer's screen follows the prompt
- [ ] Slash commands and permission prompts work inside it
- [ ] Hold Ctrl+T quits and the monitor is cleared
- [ ] `claude --tui term` on a computer without a monitor
- [ ] `codex --tui`

## Things it can build (be creative)

- [ ] Something on a monitor (a clock, a dashboard)
- [ ] A turtle job (dig, build, farm)
- [ ] Redstone (doors, lamps)
- [ ] A peripheral from another mod, if you have one (Advanced Peripherals, Create, ...)
- [ ] `agentlog` on a second computer while an agent works

## Multiplayer (if you have a server)

- [ ] With the mod on the server and in your game, `claude` works on a server computer
- [ ] `claude --tui` works on a server computer
- [ ] A friend without the mod gets a clear message

## Reporting

For each problem: what you typed, what you expected, what happened, a screenshot, and `logs/latest.log`.
Open an issue at https://github.com/pirut/cc-agent/issues or send them to JR.
