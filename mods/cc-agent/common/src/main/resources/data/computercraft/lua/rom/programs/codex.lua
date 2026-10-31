local args = { ... }
if args[1] == "--tui" or args[1] == "-t" then
    require("ccagent.tui").run("codex", args[2])
else
    require("ccagent.chat").run("codex", ...)
end
