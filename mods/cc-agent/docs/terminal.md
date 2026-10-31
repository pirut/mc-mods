# Your own terminal

Besides the in-game chat, you can connect the Claude Code or Codex you run in a normal terminal to the game.
Then you work from your desk and the agent works in the world. You do this once per PC: the address and
token stay the same across worlds and restarts.

1. Open a singleplayer world and run `/ccagent`. It shows the address and two clickable links.
2. Click the one for your agent; it copies the setup to your clipboard.

## Claude Code

`/ccagent claude` copies a command like this. Run it in a terminal:

```sh
claude mcp add --scope user --transport http cc-agent http://127.0.0.1:25599/mcp \
  --header "Authorization: Bearer YOUR_TOKEN"
```

Then start `claude` anywhere and ask, for example: *"On computer 0, write a program that shows the time on
the monitor on its left and run it."* `claude mcp list` checks the connection (the game must be open).

## Codex

`/ccagent codex` copies a block for `~/.codex/config.toml`:

```toml
[mcp_servers.cc-agent]
url = "http://127.0.0.1:25599/mcp"
http_headers = { "Authorization" = "Bearer YOUR_TOKEN" }
default_tools_approval_mode = "approve"
```

The last line lets Codex use CC: Agent's tools without asking each time; leave it out to approve each call.

Or keep the token out of the file: `export CC_AGENT_TOKEN=YOUR_TOKEN`, then
`codex mcp add cc-agent --url http://127.0.0.1:25599/mcp --bearer-token-env-var CC_AGENT_TOKEN`.

## Other MCP clients

Any client that speaks Streamable HTTP works: URL `http://127.0.0.1:25599/mcp`, header
`Authorization: Bearer YOUR_TOKEN`. The token is in `config/ccagent.properties`.

## On a server

The same setup works when you're playing on a server that has CC: Agent: your game passes the calls on. See
[Multiplayer](multiplayer.md).

Tip: run `agentlog` on a computer near you to watch what your terminal agent does in the world.
