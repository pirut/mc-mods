-- CC: Agent chat: talk to Claude Code or Codex from a CC computer. The agent runs on the player's
-- machine, signed in as them, and acts in the world through CC: Agent's tools. See "help claude".

local expect = require "cc.expect".expect

local NAMES = { claude = "Claude Code", codex = "Codex" }

local function run(agent, ...)
    expect(1, agent, "string")
    if type(ccagent) ~= "table" or not ccagent.ask then
        printError("CC: Agent isn't installed on this server.")
        return
    end

    local native = term.current()
    local w, h = native.getSize()
    local colour = native.isColour()
    local function c(colourful, plain) return colour and colourful or plain end

    local log = window.create(native, 1, 2, w, h - 2)
    -- read() prints a newline when done; in its own one-line window that can't scroll the chat away.
    local input = window.create(native, 1, h, w, 1)
    local session = nil
    local busy = false

    local function header(status)
        native.setCursorPos(1, 1)
        native.setBackgroundColour(c(colours.orange, colours.white))
        native.setTextColour(c(colours.black, colours.black))
        native.clearLine()
        local title = " " .. NAMES[agent] .. " on #" .. os.getComputerID()
        native.write(title)
        if status then
            native.setCursorPos(math.max(#title + 2, w - #status), 1)
            native.write(status)
        end
        native.setBackgroundColour(colours.black)
    end

    local function add(fg, prefix, text)
        local previous = term.redirect(log)
        term.setTextColour(prefix == "" and fg or c(colours.grey, colours.lightGrey))
        if prefix ~= "" then write(prefix) end
        term.setTextColour(fg)
        print(text)
        term.redirect(previous)
    end

    local function footer(text)
        input.setCursorPos(1, 1)
        input.setBackgroundColour(colours.black)
        input.clearLine()
        input.setTextColour(c(colours.grey, colours.lightGrey))
        input.write(text:sub(1, w))
    end

    native.setBackgroundColour(colours.black)
    native.clear()
    header()
    add(c(colours.lightGrey, colours.white), "", "Ask for anything on your computers. /new starts over, /exit or Ctrl+T leaves. Ctrl+T while it works stops it.")

    local spinner = { "|", "/", "-", "\\" }

    local function turn(prompt)
        add(c(colours.yellow, colours.white), "> ", prompt)
        local id, err = ccagent.ask(agent, prompt, session, w)
        if not id then
            add(c(colours.red, colours.white), "! ", err)
            return
        end
        busy = true
        local frame, started = 1, os.epoch("utc")
        local timer = os.startTimer(0.25)
        while true do
            local e = table.pack(os.pullEventRaw())
            if e[1] == "ccagent_chat" and e[2] == id then
                local kind, text = e[3], e[4]
                if kind == "session" then
                    session = text
                elseif kind == "text" then
                    add(colours.white, "", text)
                elseif kind == "tool" then
                    add(c(colours.cyan, colours.lightGrey), "* ", text)
                elseif kind == "tool_error" then
                    add(c(colours.red, colours.lightGrey), "  ", text)
                elseif kind == "error" then
                    add(c(colours.red, colours.white), "! ", text)
                elseif kind == "done" then
                    local secs = math.floor((os.epoch("utc") - started) / 1000)
                    header(text ~= "" and (text .. " ") or nil)
                    add(c(colours.grey, colours.lightGrey), "", ("(%ds)"):format(secs))
                    break
                end
            elseif e[1] == "timer" and e[2] == timer then
                frame = frame % #spinner + 1
                local secs = math.floor((os.epoch("utc") - started) / 1000)
                footer(spinner[frame] .. " working " .. secs .. "s  (Ctrl+T stops)")
                timer = os.startTimer(0.25)
            elseif e[1] == "terminate" then
                ccagent.stop(id)
                add(c(colours.red, colours.white), "! ", "stopped")
            elseif e[1] == "term_resize" then
                w, h = native.getSize()
                log.reposition(1, 2, w, h - 2)
                input.reposition(1, h, w, 1)
                header()
            end
        end
        busy = false
    end

    local first = table.concat({ ... }, " ")
    if first ~= "" then turn(first) end

    local history = {}
    while true do
        footer("")
        input.setCursorPos(1, 1)
        input.setTextColour(c(colours.yellow, colours.white))
        input.write("> ")
        local previous = term.redirect(input)
        local ok, line = pcall(read, nil, history)
        term.redirect(previous)
        if not ok then break end -- Ctrl+T at the prompt
        if line == "/exit" or line == "exit" then break end
        if line == "/new" then
            session = nil
            log.clear()
            log.setCursorPos(1, 1)
            add(c(colours.lightGrey, colours.white), "", "New conversation.")
        elseif line:find("%S") then
            history[#history + 1] = line
            turn(line)
        end
    end

    native.setBackgroundColour(colours.black)
    native.setTextColour(colours.white)
    native.clear()
    native.setCursorPos(1, 1)
end

return { run = run }
