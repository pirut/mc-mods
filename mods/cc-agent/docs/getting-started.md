# Getting started

This takes about 15 minutes the first time. You need Minecraft Java Edition and a Claude (Pro or Max) or
ChatGPT (Plus or better) account. No API keys.

## 1. Install the agent on your PC

Pick one or both. Each only needs to be signed in once.

**Claude Code** ([install guide](https://code.claude.com/docs/en/setup))

- macOS / Linux: `curl -fsSL https://claude.ai/install.sh | bash`
- Windows (PowerShell): `irm https://claude.ai/install.ps1 | iex`
- Or with Node.js: `npm install -g @anthropic-ai/claude-code`

Then open a terminal, run `claude`, and sign in when it asks. Type `/exit` once you see the prompt.

**Codex** ([install guide](https://github.com/openai/codex))

- `npm install -g @openai/codex` (needs [Node.js](https://nodejs.org))

Then run `codex` in a terminal and choose **Sign in with ChatGPT**.

> Check it works: in a new terminal, `claude --version` or `codex --version` should print a version.

## 2. Set up Minecraft

Any launcher works. [Prism Launcher](https://prismlauncher.org) or the
[Modrinth app](https://modrinth.com/app) are the easiest:

1. Create a new instance: **Minecraft 1.21.1** with **NeoForge** (or **Fabric**).
2. Add these mods:
   - [CC: Tweaked](https://modrinth.com/mod/cc-tweaked) for 1.21.1 (1.120.0 or newer)
   - on Fabric only: [Fabric API](https://modrinth.com/mod/fabric-api)
   - CC: Agent: download `ccagent-neoforge-1.21.1-<version>.jar` or `ccagent-fabric-1.21.1-<version>.jar`
     from the [latest release](https://github.com/pirut/mc-mods/releases?q=cc-agent) and drop it in the
     instance's `mods` folder.
3. Start the game and create a **Creative** world (easier for testing).

## 3. Your first program

1. Get an **Advanced Computer** from the creative menu (search "computer") and place it.
2. Right-click it. You get a CraftOS prompt.
3. Type `claude` and press Enter (or `codex`).
4. Ask for something, for example:

   > make a program called hello that prints hello world in rainbow colors, then run it

5. Watch it work. Each step shows as a line (`* write_file #0 /hello.lua`, `* run_shell #0 hello`). When
   it's done, type `/exit`, then `hello` at the shell to run the program yourself.

![The chat](images/chat-question.jpg)

Things to try next:

- *"Put a monitor on top of this computer and show the time on it in big letters"* (place an advanced
  monitor on the computer first)
- *"Make the turtle dig a 3 by 3 tunnel 10 blocks long"* (place a turtle with fuel in it)
- *"Ask me what color I like, then make the monitor that color"*: it asks with buttons you click
- `/mode` then **ask**: now it asks before each change. `/mode` then **plan**: it plans first.

## 4. The full screen on a monitor wall

`claude --tui` shows the real Claude Code screen. It needs at least 80x24 characters, so build a wall of
**advanced monitors**:

1. Place advanced monitors in a rectangle touching the computer (on top of it, or next to it). Place them by
   hand: they join into one big screen as you place them. 8 wide by 6 tall gives 82x40 characters at a
   comfortable text size; 4 wide by 3 tall works with small text.
2. Right-click the computer and type `claude --tui`.
3. Stand back. Type on the computer screen; it shows the part around the prompt, the wall shows everything.
4. Scroll back with the mouse wheel on the computer screen (Claude Code's own scrolling; Ctrl+End jumps
   back down). Clicking on the computer screen, or touching the monitor, clicks in Claude Code.
5. Hold **Ctrl+T** for a second to quit, or type `/exit` in Claude Code.

![Claude Code on a monitor wall](images/tui-monitor-wall.jpg)

The first time, Claude Code asks you to pick a theme (press Enter) and whether to trust its folder: press
the down arrow to **Yes, I trust this folder**, then Enter. Use
`claude --tui left` to pick a monitor by side or name, and `claude --tui term` to use the computer's own
screen.

## 5. Optional: drive it from your own terminal

You can also connect the Claude Code or Codex you run in a normal terminal to the game, for bigger jobs. See
[Your own terminal](terminal.md).

## Help

Something not working? See [Troubleshooting](troubleshooting.md). In game, `help claude` shows the program's
own help.
