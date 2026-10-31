# Troubleshooting

**"couldn't find claude" / "couldn't find codex"**
The game didn't get your terminal's PATH (common when the launcher starts from the Dock or Start menu). In
a terminal run `which claude` (macOS/Linux) or `where claude` (Windows), and put that full path in
`claudeCommand=` in `config/ccagent.properties` (use forward slashes on Windows, e.g.
`C:/Users/you/AppData/Roaming/npm/claude.cmd`). Reopen the world.

**"exited with code 1 ... sign in"**
Run `claude` (or `codex`) once in a normal terminal and sign in.

**"CC: Agent's MCP server isn't running"**
Something else uses port 25599 (a second Minecraft open?). Close it, or change `port=` in the config.
`/ccagent` in game shows the status.

**Nothing happens on a computer / jobs time out**
The computer's program has to wait for events, like the shell prompt does. A program stuck in a loop without
`sleep` or `os.pullEvent` blocks it: hold Ctrl+T or Ctrl+R on the computer. `set ccagent.enabled false`
turns agents off for a computer, so check it's not set.

**The full screen says it needs an advanced computer or monitor**
`--tui` needs colors: use an advanced computer, or attach an advanced (gold) monitor.

**The monitor wall shows as separate screens**
Monitors join when placed by hand. Blocks set with `/fill` or `/setblock` don't join: break one
and place it again.

**The full screen is tiny or cut off**
Claude Code and Codex need at least 80x24. Make the wall bigger (8x6 is great), then run it again.

**Codex says a tool "requires approval, but approval policy is never"**
That was fixed in 0.3.0; update CC: Agent. For Codex in your own terminal, add
`default_tools_approval_mode = "approve"` to the `[mcp_servers.cc-agent]` block.

**On a server: "install CC: Agent in your own game too"**
The agent runs on your PC, so your game needs the mod as well as the server.

**Something else**
Open an issue at https://github.com/pirut/mc-mods/issues with what you did, what you saw, and your
`logs/latest.log` (in the game folder). See the [testing checklist](testing.md).
