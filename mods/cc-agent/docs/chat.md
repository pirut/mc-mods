# Using the chat

Type `claude` or `codex` on any CC computer (advanced computers get colors and clicking). `claude <request>`
starts with that request. The agent runs on the PC of the player at the computer, signed in as them.

![The chat](images/chat-question.jpg)

## Commands

| Command | What it does |
| --- | --- |
| `/model` | Pick the model from a list, or `/model <name>` (Claude: `opus`, `sonnet`, `haiku` or a full model name) |
| `/effort` | How hard it thinks: low to max (Claude), minimal to high (Codex) |
| `/mode` | When it asks before acting (below). Shift+Tab cycles through the modes |
| `/resume` | Go back to one of the last 8 conversations on this computer |
| `/new` | Start a new conversation |
| `/status` | Show the model, effort and mode |
| `/clear` | Clear the screen (the conversation goes on) |
| `/help` | List the commands |
| `/exit` | Leave (or Ctrl+T when it isn't working) |
| anything else starting with `/` | Goes to the agent, like in its terminal: `/context`, `/compact`, your own custom commands |

Tab completes a command. Up and down go through what you typed before. Settings are kept per computer.

**Scrolling:** the mouse wheel or Page Up / Page Down scroll back through the conversation (the last 1000
messages). A "more below" marker shows while you're scrolled up; new messages don't pull you down, and
sending a message jumps back to the bottom.

## Modes

| Mode | What happens |
| --- | --- |
| **auto** (default) | It uses the CC tools without asking. |
| **ask** | It looks around freely but asks before anything that changes things: writing files, running code, moving turtles. |
| **plan** | It looks around and writes a plan, then asks "Go ahead with this plan?" |

![A permission prompt](images/chat-permission.jpg)

On a permission prompt, **Allow** runs that one step, **Allow for this chat** allows that tool until you
start a new conversation or change mode, **Deny** says no. You can also type a reason ("no, use the other
monitor") and it'll take that into account.

![Approving a plan](images/chat-plan.jpg)

Approving a plan offers **Yes, and don't ask again** (switches to auto) or **Yes, ask before each change**
(switches to ask), like the terminal version.

Codex in the chat always runs in auto (Codex's own approvals only work in its full screen:
`codex --tui`).

## Questions

The agent can ask you things, with answers to click. Click one, press its number, or type your own answer
and press Enter. If you leave the chat, it's told you left.

## While it works

- The header shows a spinner and the time.
- `* write_file #0 /hello.lua` lines are its steps; red lines are steps that failed (it usually fixes them).
- `~` lines are its thinking, `[ ]` / `[>]` / `[x]` lines are its todo list.
- You can type your next message: it's queued and sent when the reply is done.
- **Ctrl+T** (hold it) stops the reply.
- After each reply: how long it took, the model, and how full its context is.

## Full screen instead

`claude --tui` / `codex --tui` run the agent's real terminal screen on a monitor, with every feature of the
terminal version. See [Getting started](getting-started.md#4-the-full-screen-on-a-monitor-wall).

## Watching

`agentlog` on another computer (or `agentlog right` for a monitor) shows every step any agent takes, live.
