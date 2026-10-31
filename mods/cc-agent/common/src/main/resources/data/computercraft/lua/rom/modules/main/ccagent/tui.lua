-- CC: Agent full screen: Claude Code's or Codex's own terminal UI, running on the player's machine,
-- drawn on a monitor (or this screen) and typed into from this computer. See "help claude".

local expect = require "cc.expect".expect

local NAMES = { claude = "Claude Code", codex = "Codex" }

-- Slots 0 and 1 are always the terminal's background and text colors; the mod hands out the rest
-- to the colors the TUI draws with, and sends them along with the frames.
local BG, FG = 2 ^ 0, 2 ^ 1
local START = { [0] = 0x0c0c0c, [1] = 0xcccccc }

local MIN_COLS, MIN_ROWS = 80, 24

local function reset_palette(t)
    for i = 0, 15 do t.setPaletteColour(2 ^ i, term.nativePaletteColour(2 ^ i)) end
end

-- The largest text that still fits the TUI's minimum size, else the smallest.
local function pick_scale(m)
    local best = 0.5
    for scale = 0.5, 5, 0.5 do
        m.setTextScale(scale)
        local w, h = m.getSize()
        if w >= MIN_COLS and h >= MIN_ROWS then best = scale end
    end
    m.setTextScale(best)
end

-- The biggest colour monitor attached, or the one on the side given.
local function find_monitor(side)
    if side then
        if not peripheral.hasType(side, "monitor") then return nil, "no monitor on " .. side end
        return peripheral.wrap(side), side
    end
    local best, best_name, best_area = nil, nil, 0
    for _, name in ipairs(peripheral.getNames()) do
        if peripheral.hasType(name, "monitor") then
            local m = peripheral.wrap(name)
            if m.isColour() then
                m.setTextScale(0.5)
                local w, h = m.getSize()
                if w * h > best_area then best, best_name, best_area = m, name, w * h end
            end
        end
    end
    return best, best_name
end

local function run(agent, side)
    expect(1, agent, "string")
    expect(2, side, "string", "nil")
    if type(ccagent) ~= "table" or not ccagent.tui then
        printError("CC: Agent isn't installed on this server.")
        return
    end

    local screen = term.current()
    local monitor, monitor_name
    if side ~= "term" then
        monitor, monitor_name = find_monitor(side)
        if side and not monitor then printError(monitor_name) return end
        if monitor and not monitor.isColour() then printError("That monitor isn't an advanced monitor.") return end
    end
    if not monitor and not screen.isColour() then
        printError("The full " .. NAMES[agent] .. " screen needs an advanced computer or an advanced monitor.")
        print("Run \"" .. agent .. "\" without --tui for the chat.")
        return
    end

    local out = monitor or screen
    if monitor then pick_scale(monitor) end
    local cols, rows = out.getSize()

    local id, err = ccagent.tui(agent, cols, rows, monitor_name)
    if not id then printError(err) return end

    -- The whole terminal, kept so this computer's screen can show the part being worked on.
    local lines = {}
    local function blank_line() return { (" "):rep(cols), ("1"):rep(cols), ("0"):rep(cols) } end
    for y = 1, rows do lines[y] = blank_line() end
    local cx, cy, cursor_on = 1, 1, true
    local view_x, view_y = 1, 1

    local mirror = monitor and screen.isColour()
    local sw, sh = screen.getSize()
    local status = monitor and 1 or 0

    local function set_colours(changes)
        for slot, rgb in pairs(changes) do
            out.setPaletteColour(2 ^ slot, rgb)
            if mirror then screen.setPaletteColour(2 ^ slot, rgb) end
        end
    end

    set_colours(START)
    out.setBackgroundColour(BG)
    out.setTextColour(FG)
    out.clear()
    if monitor then
        screen.setBackgroundColour(mirror and BG or colours.black)
        screen.clear()
    end

    local function draw_status()
        if not monitor then return end
        screen.setCursorPos(1, 1)
        screen.setBackgroundColour(mirror and FG or colours.white)
        screen.setTextColour(mirror and BG or colours.black)
        screen.clearLine()
        screen.write((" " .. NAMES[agent] .. " on " .. monitor_name .. "  wheel scrolls, hold Ctrl+T quits"):sub(1, sw))
        screen.setBackgroundColour(mirror and BG or colours.black)
    end

    -- The bottom of what's drawn: TUIs keep their prompt and status lines at the end.
    local function last_used_row()
        for y = rows, 1, -1 do
            if lines[y][1]:find("[^ ]") then return y end
        end
        return 1
    end

    -- This screen shows a window into the terminal: down to the last line drawn, across to the cursor.
    local function draw_view()
        if not monitor then return end
        local vh = sh - status
        local bottom = math.max(cy, last_used_row())
        local ny = math.max(1, math.min(bottom - vh + 1, rows - vh + 1))
        if cy < ny then ny = cy end
        local nx = view_x
        if cx < nx then nx = cx elseif cx >= nx + sw then nx = cx - sw + 1 end
        nx = math.max(1, math.min(nx, cols - sw + 1))
        view_x, view_y = nx, ny
        for y = 1, vh do
            local line = lines[ny + y - 1]
            screen.setCursorPos(1, y + status)
            if line then
                local text = line[1]:sub(nx, nx + sw - 1)
                if mirror then
                    screen.blit(text, line[2]:sub(nx, nx + sw - 1), line[3]:sub(nx, nx + sw - 1))
                else
                    screen.setTextColour(colours.white)
                    screen.write(text)
                end
            else
                screen.clearLine()
            end
        end
        screen.setTextColour(mirror and FG or colours.white)
        screen.setCursorPos(cx - nx + 1, cy - ny + 1 + status)
        screen.setCursorBlink(cursor_on)
    end

    local function frame(changed, palette, x, y, visible)
        set_colours(palette)
        for row, line in pairs(changed) do
            if row <= rows and #line[1] == cols then
                lines[row] = line
                out.setCursorPos(1, row)
                out.blit(line[1], line[2], line[3])
            end
        end
        cx, cy, cursor_on = x, y, visible
        out.setTextColour(FG)
        out.setCursorPos(cx, cy)
        out.setCursorBlink(visible)
        draw_view()
    end

    local function resize()
        local w, h = out.getSize()
        if w == cols and h == rows then return end
        cols, rows = w, h
        lines = {}
        for y = 1, rows do lines[y] = blank_line() end
        out.setBackgroundColour(BG)
        out.clear()
        ccagent.tuiResize(id, cols, rows)
    end

    local code, exit_error
    -- If drawing fails (the monitor was broken or detached), still end the session and tidy up.
    local ok, failure = pcall(function()
        draw_status()
        while true do
            local event = table.pack(os.pullEventRaw())
            local name = event[1]
            if name == "ccagent_tui" and event[2] == id then
                if event[3] == "frame" then
                    frame(event[4], event[5], event[6], event[7], event[8])
                elseif event[3] == "exit" then
                    code, exit_error = event[4], event[5]
                    return
                end
            elseif name == "mouse_scroll" or name == "mouse_click" then
                -- Mouse on this screen: where it lands in the terminal. The wheel scrolls (Claude Code's
                -- own scrolling, or the lines that scrolled off the top); clicks go to the program.
                local tx, ty = event[3], event[4]
                if monitor then tx, ty = view_x + tx - 1, view_y + ty - 1 - status end
                if ty >= 1 then
                    ccagent.tuiInput(id, name == "mouse_scroll" and "scroll" or "click", event[2], tx, ty)
                end
            elseif name == "monitor_touch" and event[2] == monitor_name then
                ccagent.tuiInput(id, "click", 1, event[3], event[4])
            elseif name == "char" or name == "key" or name == "key_up" or name == "paste" then
                ccagent.tuiInput(id, name, event[2])
            elseif name == "terminate" then
                return
            elseif (name == "monitor_resize" and event[2] == monitor_name) or (name == "term_resize" and not monitor) then
                resize()
                ccagent.tuiRedraw(id)
            elseif name == "term_resize" and monitor then
                sw, sh = screen.getSize()
                screen.clear()
                draw_status()
                draw_view()
            end
        end
    end)
    ccagent.tuiStop(id)

    for _, t in ipairs(monitor and { monitor, screen } or { screen }) do
        pcall(function()
            t.setCursorBlink(false)
            reset_palette(t)
            t.setBackgroundColour(colours.black)
            t.setTextColour(colours.white)
            t.clear()
            t.setCursorPos(1, 1)
        end)
    end
    if not ok then printError(failure) end
    if exit_error then printError(exit_error) end
    if code and code ~= 0 then print(NAMES[agent] .. " exited (" .. tostring(code) .. ").") end
end

return { run = run }
