-- CC: Agent chat: talk to Claude Code or Codex from a CC computer. The agent runs on the player's
-- machine, signed in as them, and acts in the world through CC: Agent's tools. See "help claude".

local expect = require "cc.expect".expect
local strings = require "cc.strings"

local NAMES = { claude = "Claude Code", codex = "Codex" }

-- What /model, /effort and /mode offer. Anything else can be typed: "/model claude-opus-4-1".
local MODELS = {
    claude = { "default", "opus", "sonnet", "haiku" },
    codex = { "default" },
}
local EFFORTS = {
    claude = { "default", "low", "medium", "high", "xhigh", "max" },
    codex = { "default", "minimal", "low", "medium", "high" },
}
local MODES = {
    claude = {
        { "auto", "use CC tools without asking" },
        { "ask", "ask before anything that changes things" },
        { "plan", "look and plan, ask before doing it" },
    },
    codex = {
        { "auto", "use CC tools without asking" },
    },
}

local HELP = {
    "/model    pick the model (or /model <name>)",
    "/effort   how hard it thinks",
    "/mode     when it asks first (Shift+Tab cycles)",
    "/resume   go back to an earlier conversation",
    "/status   show the settings",
    "/new      start a new conversation",
    "/clear    clear the screen",
    "/exit     leave (or Ctrl+T)",
    "Scroll the history with the mouse wheel or Page Up/Down.",
    "Other /commands (/context, /compact, your own) go to the agent. Tab completes commands. You can type while it works: your message waits its turn.",
}
local COMMANDS = { "model", "effort", "mode", "permissions", "resume", "status", "new", "clear", "exit", "help",
    "context", "compact", "cost", "init", "review" }
local MAX_SESSIONS = 8

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

    -- The chat history: every message, wrapped to the screen, scrollable with the mouse wheel and
    -- Page Up/Down. It's drawn in rows 2 .. log_rows + 1, above the question panel and the input line.
    local MAX_ENTRIES = 1000
    local entries, lines = {}, {}
    local log_rows = h - 2
    local scroll = 0 -- lines scrolled up from the bottom

    local function wrap_entry(e)
        local out = {}
        local first = true
        for part in (e.text .. "\n"):gmatch("(.-)\n") do
            local full = (first and e.prefix or (e.prefix ~= "" and (" "):rep(#e.prefix) or "")) .. part
            local wrapped = full == "" and { "" } or strings.wrap(full, w)
            for i, line in ipairs(wrapped) do
                local fg = colours.toBlit(e.fg):rep(#line)
                if first and i == 1 and e.prefix ~= "" then
                    local n = math.min(#e.prefix, #line)
                    fg = colours.toBlit(c(colours.grey, colours.lightGrey)):rep(n) .. fg:sub(n + 1)
                end
                out[#out + 1] = { line, fg }
            end
            first = false
        end
        return out
    end

    local function draw_log()
        local bottom = #lines - scroll
        for row = 1, log_rows do
            native.setCursorPos(1, row + 1)
            native.setBackgroundColour(colours.black)
            native.clearLine()
            local line = lines[bottom - log_rows + row]
            if line then native.blit(line[1], line[2], ("f"):rep(#line[1])) end
        end
        if scroll > 0 then
            local note = (" %d more below: scroll down or PgDn "):format(scroll)
            native.setCursorPos(math.max(1, w - #note + 1), log_rows + 1)
            native.setBackgroundColour(c(colours.grey, colours.white))
            native.setTextColour(c(colours.white, colours.black))
            native.write(note:sub(1, w))
            native.setBackgroundColour(colours.black)
        end
    end

    local function scroll_by(n)
        scroll = math.max(0, math.min(scroll + n, #lines - log_rows))
        draw_log()
    end

    local function rewrap()
        lines = {}
        for _, e in ipairs(entries) do
            for _, line in ipairs(wrap_entry(e)) do lines[#lines + 1] = line end
        end
        scroll = math.max(0, math.min(scroll, #lines - log_rows))
    end

    local function clear_log()
        entries, lines, scroll = {}, {}, 0
        draw_log()
    end
    local session, run_id, started = nil, nil, nil
    local spinner, spin = { "|", "/", "-", "\\" }, 1
    local timer

    -- Settings live in the computer's settings file, per agent.
    local function setting(name)
        return settings.get("ccagent." .. agent .. "." .. name)
    end
    local function set_setting(name, value)
        if value == nil or value == "default" then settings.unset("ccagent." .. agent .. "." .. name)
        else settings.set("ccagent." .. agent .. "." .. name, value) end
        settings.save()
    end
    local function mode() return setting("mode") or "auto" end
    local function set_mode(value, keep_answers)
        set_setting("mode", value)
        if not keep_answers then ccagent.resetPermissions() end
    end

    -- ---- Drawing ----

    local status_text = nil
    local function header()
        native.setCursorPos(1, 1)
        native.setBackgroundColour(c(colours.orange, colours.white))
        native.setTextColour(colours.black)
        native.clearLine()
        local title = " " .. NAMES[agent] .. " #" .. os.getComputerID()
        native.write(title)
        local right = status_text
        if not right then
            right = (setting("model") or "default") .. " | " .. mode()
            if setting("effort") then right = right .. " | " .. setting("effort") end
        end
        right = right .. " "
        if #title + #right < w then
            native.setCursorPos(w - #right + 1, 1)
            native.write(right)
        end
        native.setBackgroundColour(colours.black)
    end

    local function add(fg, prefix, text)
        local e = { fg = fg, prefix = prefix, text = tostring(text) }
        entries[#entries + 1] = e
        local new = wrap_entry(e)
        for _, line in ipairs(new) do lines[#lines + 1] = line end
        if #entries > MAX_ENTRIES then
            table.remove(entries, 1)
            rewrap()
        end
        -- Reading back up: stay where you are.
        if scroll > 0 then scroll = scroll + #new end
        draw_log()
    end
    local function info(text) add(c(colours.lightGrey, colours.white), "", text) end
    local function warn(text) add(c(colours.red, colours.white), "! ", text) end

    -- The question panel: a question with options to click, drawn over the bottom of the chat.
    local questions = {}     -- waiting questions, oldest first
    local buttons = {}       -- screen row -> option index, for the one shown
    local panel_top = h

    local buffer, cursor = "", 0
    local shift = false
    local history, history_at = {}, nil

    local function draw_input()
        native.setCursorPos(1, h)
        native.setBackgroundColour(colours.black)
        native.clearLine()
        local q = questions[1]
        local prompt = q and "? " or "> "
        native.setTextColour(c(q and colours.lightBlue or colours.yellow, colours.white))
        native.write(prompt)
        native.setTextColour(colours.white)
        local room = w - #prompt
        local shift = math.max(0, cursor - room + 1)
        native.write(buffer:sub(shift + 1, shift + room))
        native.setCursorPos(#prompt + cursor - shift + 1, h)
        native.setCursorBlink(true)
    end

    -- The history shrinks above the panel, keeping its newest lines in view.
    local function fit_log(rows)
        rows = math.max(1, rows)
        if rows ~= log_rows then
            log_rows = rows
            scroll = math.max(0, math.min(scroll, #lines - log_rows))
            draw_log()
        end
    end

    local function draw_panel()
        buttons = {}
        local q = questions[1]
        if not q then
            panel_top = h
            fit_log(h - 2)
            return
        end
        local lines = {}
        for part in (q.question .. "\n"):gmatch("(.-)\n") do
            for _, line in ipairs(strings.wrap(part, w - 1)) do lines[#lines + 1] = { "text", line } end
        end
        for i, option in ipairs(q.options) do
            lines[#lines + 1] = { "option", (" %d  %s"):format(i, option), i }
        end
        local hint = #q.options > 0 and (q.other and "click, press a number, or type an answer" or "click or press a number")
            or "type your answer and press Enter"
        if #questions > 1 then hint = hint .. (" (+%d more)"):format(#questions - 1) end
        lines[#lines + 1] = { "hint", hint }

        local height = math.min(#lines + 1, h - 3)
        panel_top = h - height
        fit_log(panel_top - 2)
        local first = #lines - (height - 1) + 1 -- keep the options when the question is long
        native.setCursorPos(1, panel_top)
        local band = q.kind == "permission" and colours.red or q.kind == "plan" and colours.green or colours.blue
        native.setBackgroundColour(c(band, colours.white))
        native.setTextColour(c(colours.white, colours.black))
        native.clearLine()
        native.write((q.kind == "permission" and " Permission" or q.kind == "plan" and " Plan" or " Question"):sub(1, w))
        for i = math.max(1, first), #lines do
            local y = panel_top + 1 + i - math.max(1, first)
            local line = lines[i]
            native.setCursorPos(1, y)
            if line[1] == "option" then
                native.setBackgroundColour(c(colours.grey, colours.white))
                native.setTextColour(c(colours.white, colours.black))
                buttons[y] = line[3]
            else
                native.setBackgroundColour(c(colours.black, colours.black))
                native.setTextColour(line[1] == "hint" and c(colours.grey, colours.lightGrey) or colours.white)
            end
            native.clearLine()
            native.write(line[2]:sub(1, w))
        end
        native.setBackgroundColour(colours.black)
    end

    local function redraw()
        native.setBackgroundColour(colours.black)
        native.clear()
        header()
        draw_log()
        draw_panel()
        draw_input()
    end

    local function refresh()
        -- The log may have drawn under the panel; put the panel and cursor back.
        if questions[1] then draw_panel() end
        draw_input()
    end

    -- ---- Questions ----

    local function ask_local(question, options, other, pick)
        questions[#questions + 1] = { question = question, options = options, other = other, pick = pick, kind = "question" }
        if #questions == 1 then buffer, cursor = "", 0 end
        redraw()
    end

    local function close_question(answer)
        local q = table.remove(questions, 1)
        if not q then return end
        buffer, cursor = "", 0
        if q.id then
            add(c(colours.lightBlue, colours.white), "  ", answer)
            ccagent.answer(q.id, answer)
            -- Approving a plan picks how the rest goes, like in the terminal.
            if q.kind == "plan" and answer == "Yes, and don't ask again" then set_mode("auto", true) header()
            elseif q.kind == "plan" and answer == "Yes, ask before each change" then set_mode("ask", true) header() end
        elseif q.pick and answer then
            q.pick(answer)
        end
        redraw()
    end

    -- ---- Turns ----

    local function options()
        return { model = setting("model"), effort = setting("effort"), mode = mode() }
    end

    local queued = {}

    -- Recent conversations for /resume, newest first: { id, title }.
    local function sessions() return setting("sessions") or {} end
    local function remember(id, title)
        local list = { { id = id, title = title } }
        for _, s in ipairs(sessions()) do
            if s.id ~= id and #list < MAX_SESSIONS then list[#list + 1] = s end
        end
        set_setting("sessions", list)
    end
    local session_title = nil

    local function send(prompt)
        if not session then session_title = prompt:sub(1, 60) end
        add(c(colours.yellow, colours.white), "> ", prompt)
        local id, err = ccagent.ask(agent, prompt, session, w, options())
        if not id then
            warn(err)
            return
        end
        run_id, started = id, os.epoch("utc")
        timer = os.startTimer(0.25)
    end

    local function finish(text)
        local secs = math.floor((os.epoch("utc") - started) / 1000)
        run_id, status_text = nil, nil
        add(c(colours.grey, colours.lightGrey), "", ("(%ds%s)"):format(secs, text ~= "" and ", " .. text or ""))
        if session then remember(session, session_title or "conversation") end
        header()
        if #queued > 0 then send(table.remove(queued, 1)) end
    end

    -- ---- Commands ----

    local function choose(name, list, labels, other)
        return function(arg)
            if arg and arg ~= "" then
                set_setting(name, arg)
                if name == "mode" then ccagent.resetPermissions() end
                info(name .. ": " .. arg)
                header()
                return
            end
            local shown, values = {}, {}
            for i, item in ipairs(list) do
                local value = type(item) == "table" and item[1] or item
                values[i] = value
                shown[i] = (type(item) == "table" and (item[1] .. " - " .. item[2]) or item)
                    .. (((setting(name) or (name == "mode" and "auto" or "default")) == value) and "  (now)" or "")
            end
            ask_local(labels, shown, other, function(answer)
                local value = answer
                for i, label in ipairs(shown) do
                    if label == answer then value = values[i] end
                end
                set_setting(name, value)
                if name == "mode" then ccagent.resetPermissions() end
                info(name .. ": " .. value)
                header()
            end)
        end
    end

    local commands = {
        model = choose("model", MODELS[agent], "Which model?", true),
        effort = choose("effort", EFFORTS[agent], "How much effort?", false),
        resume = function()
            local list = sessions()
            if #list == 0 then info("No earlier conversations on this computer yet.") return end
            local shown = {}
            for i, s in ipairs(list) do shown[i] = s.title end
            ask_local("Which conversation?", shown, false, function(answer)
                for i, label in ipairs(shown) do
                    if label == answer then
                        session, session_title = list[i].id, list[i].title
                        clear_log()
                        info("Back in: " .. list[i].title)
                    end
                end
            end)
        end,
        mode = choose("mode", MODES[agent], agent == "codex"
            and "When should Codex ask first? (Codex in the chat only runs in auto; codex --tui has all modes)"
            or "When should " .. NAMES[agent] .. " ask before acting?", false),
        status = function()
            info(("model %s, effort %s, mode %s, %s"):format(setting("model") or "default",
                setting("effort") or "default", mode(), session and "conversation going" or "new conversation"))
        end,
        help = function()
            for _, line in ipairs(HELP) do info(line) end
        end,
        new = function()
            session = nil
            ccagent.resetPermissions()
            clear_log()
            info("New conversation.")
        end,
        clear = function()
            clear_log()
        end,
    }
    commands.permissions = commands.mode

    local function submit(line)
        if questions[1] then
            local q = questions[1]
            if line:find("%S") and (q.other or #q.options == 0) then close_question(line) end
            return true
        end
        if not line:find("%S") then return true end
        history[#history + 1] = line
        local name, arg = line:match("^/(%S+)%s*(.-)%s*$")
        if name == "exit" or line == "exit" then return false end
        if name and commands[name] then
            commands[name](arg)
        elseif run_id then
            queued[#queued + 1] = line
            add(c(colours.grey, colours.lightGrey), "> ", line .. "  (queued)")
        else
            send(line) -- including the agent's own /commands
        end
        return true
    end

    -- ---- Main loop ----

    redraw()
    info("Ask for anything on your computers. /help lists commands. Ctrl+T stops a reply, or leaves.")
    local first = table.concat({ ... }, " ")
    if first ~= "" then send(first) end
    refresh()

    while true do
        local e = table.pack(os.pullEventRaw())
        local name = e[1]
        if name == "ccagent_chat" and e[2] == run_id then
            local kind, text = e[3], e[4]
            if kind == "session" then
                session = text
            elseif kind == "text" then
                add(colours.white, "", text)
            elseif kind == "tool" then
                add(c(colours.cyan, colours.lightGrey), "* ", text)
            elseif kind == "plan" then
                add(c(colours.green, colours.white), "", "Plan:")
                add(colours.white, "", text)
            elseif kind == "thinking" then
                add(c(colours.grey, colours.lightGrey), "~ ", text)
            elseif kind == "todos" then
                for line in (text .. "\n"):gmatch("(.-)\n") do
                    local mark = line:sub(2, 2)
                    add(mark == "x" and c(colours.green, colours.lightGrey) or mark == ">" and colours.white
                        or c(colours.lightGrey, colours.white), "  ", line)
                end
            elseif kind == "tool_error" then
                add(c(colours.red, colours.lightGrey), "  ", text)
            elseif kind == "error" then
                warn(text)
            elseif kind == "done" then
                finish(text)
            end
            refresh()
        elseif name == "ccagent_ask" then
            local q = { id = e[2], kind = e[3], question = e[4], options = {}, other = e[6] }
            for i = 1, #(e[5] or {}) do q.options[i] = e[5][i] end
            if q.kind == "permission" then add(c(colours.red, colours.white), "? ", (q.question:gsub("\n.*", "")))
            elseif q.kind ~= "plan" then add(c(colours.lightBlue, colours.white), "? ", q.question) end
            questions[#questions + 1] = q
            if #questions == 1 then buffer, cursor = "", 0 end
            redraw()
        elseif name == "ccagent_ask_closed" then
            for i = #questions, 1, -1 do
                if questions[i].id == e[2] then table.remove(questions, i) end
            end
            redraw()
        elseif name == "timer" and e[2] == timer then
            if run_id then
                spin = spin % #spinner + 1
                status_text = spinner[spin] .. " working " .. math.floor((os.epoch("utc") - started) / 1000) .. "s"
                header()
                draw_input()
                timer = os.startTimer(0.25)
            end
        elseif name == "mouse_scroll" then
            scroll_by(-e[2] * 3)
            refresh()
        elseif name == "mouse_click" then
            local option = buttons[e[4]]
            if option and questions[1] then close_question(questions[1].options[option]) end
        elseif name == "char" then
            local q = questions[1]
            local n = tonumber(e[2])
            if q and buffer == "" and n and q.options[n] then
                close_question(q.options[n])
            else
                buffer = buffer:sub(1, cursor) .. e[2] .. buffer:sub(cursor + 1)
                cursor = cursor + 1
                draw_input()
            end
        elseif name == "paste" then
            buffer = buffer:sub(1, cursor) .. e[2] .. buffer:sub(cursor + 1)
            cursor = cursor + #e[2]
            draw_input()
        elseif name == "key_up" and (e[2] == keys.leftShift or e[2] == keys.rightShift) then
            shift = false
        elseif name == "key" then
            local key = e[2]
            if key == keys.leftShift or key == keys.rightShift then
                shift = true
            elseif key == keys.tab and shift then
                -- Shift+Tab cycles the mode, like in the terminal.
                local list, at = MODES[agent], 1
                for i, m in ipairs(list) do if m[1] == mode() then at = i end end
                local next_mode = list[at % #list + 1][1]
                if next_mode ~= mode() then
                    set_mode(next_mode)
                    info("mode: " .. next_mode .. " - " .. list[at % #list + 1][2])
                end
                header()
                refresh()
            elseif key == keys.tab and buffer:sub(1, 1) == "/" and not buffer:find(" ") then
                local typed = buffer:sub(2)
                local matches = {}
                for _, name in ipairs(COMMANDS) do
                    if name:sub(1, #typed) == typed then matches[#matches + 1] = name end
                end
                if #matches == 1 then
                    buffer = "/" .. matches[1] .. " "
                    cursor = #buffer
                elseif #matches > 1 then
                    info(table.concat(matches, "  "))
                    refresh()
                end
                draw_input()
            elseif key == keys.pageUp or key == keys.pageDown then
                scroll_by((key == keys.pageUp and 1 or -1) * math.max(1, log_rows - 1))
                refresh()
            elseif key == keys.enter or key == keys.numPadEnter then
                if scroll > 0 then scroll = 0 draw_log() end
                local line = buffer
                buffer, cursor, history_at = "", 0, nil
                if not submit(line) then break end
                draw_input()
            elseif key == keys.backspace and cursor > 0 then
                buffer = buffer:sub(1, cursor - 1) .. buffer:sub(cursor + 1)
                cursor = cursor - 1
                draw_input()
            elseif key == keys.delete then
                buffer = buffer:sub(1, cursor) .. buffer:sub(cursor + 2)
                draw_input()
            elseif key == keys.left then
                cursor = math.max(0, cursor - 1)
                draw_input()
            elseif key == keys.right then
                cursor = math.min(#buffer, cursor + 1)
                draw_input()
            elseif key == keys.home then
                cursor = 0
                draw_input()
            elseif key == keys["end"] then
                cursor = #buffer
                draw_input()
            elseif (key == keys.up or key == keys.down) and #history > 0 and not questions[1] then
                history_at = history_at or (#history + 1)
                history_at = math.max(1, math.min(#history + 1, history_at + (key == keys.up and -1 or 1)))
                buffer = history[history_at] or ""
                cursor = #buffer
                draw_input()
            end
        elseif name == "terminate" then
            if run_id then
                ccagent.stop(run_id)
                for i = #questions, 1, -1 do
                    if questions[i].id then
                        ccagent.answer(questions[i].id, "(the player stopped you)")
                        table.remove(questions, i)
                    end
                end
                warn("stopped")
                redraw()
            elseif questions[1] and not questions[1].id then
                close_question(nil)
            else
                break
            end
        elseif name == "term_resize" then
            w, h = native.getSize()
            log_rows = h - 2
            rewrap()
            redraw()
        end
    end

    for _, q in ipairs(questions) do
        if q.id then ccagent.answer(q.id, "(the player left the chat)") end
    end
    if run_id then ccagent.stop(run_id) end
    native.setBackgroundColour(colours.black)
    native.setTextColour(colours.white)
    native.clear()
    native.setCursorPos(1, 1)
end

return { run = run }
