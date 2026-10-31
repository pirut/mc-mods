-- CC: Agent. Lets an AI coding agent (Claude Code, Codex, any MCP client) run code on this
-- computer through the mod's local MCP server.
--
-- Jobs arrive as "ccagent_exec" events. Whatever program is waiting for an event (usually the
-- shell's prompt) runs the job in place, with its output captured in an invisible window, and the
-- events that arrive meanwhile are handed back to that program afterwards. Jobs run side by side;
-- to the program they are like a blocking call it never made: it pauses until they are all done.
--
-- Set `settings.set("ccagent.enabled", false)` and reboot to keep agents off this computer.

if type(ccagent) ~= "table" or ccagent.__hooked then return end
if settings and settings.get("ccagent.enabled") == false then return end

local agent = ccagent
agent.__hooked = true

-- Captured from the autorun environment: jobs get the same shell and multishell a program would.
local job_shell, job_multishell = shell, multishell

-- Jobs on advanced computers run outside any multishell tab, so the shell's
-- multishell.setTitle(multishell.getCurrent(), ...) passes nil. Let that be a no-op.
if job_multishell then
    local set_title = job_multishell.setTitle
    job_multishell.setTitle = function(n, title)
        if n == nil then return end
        return set_title(n, title)
    end
end

-- Programs get require from the shell; give jobs one too.
local make_require = require and require("cc.require").make

local native_pull = os.pullEventRaw
local backlog = {}  -- events that arrived while a job ran, for the program that was waiting
local active = nil  -- the job being resumed right now

local MAX_LINES = 2000
local MAX_BACKLOG = 256

local function serialise(value, as_json)
    if as_json then
        local ok, result = pcall(textutils.serialiseJSON, value)
        if ok then return result end
    end
    local ok, result = pcall(textutils.serialise, value, { compact = true, allow_repetitions = true })
    if ok then return result end
    return tostring(value)
end

-- An invisible window the size of the screen that keeps every line that scrolls off or is cleared.
local function capture()
    local w, h = term.native().getSize()
    local win = window.create(term.native(), 1, 1, w, h, false)
    local lines, dropped = {}, 0

    local function keep(line)
        lines[#lines + 1] = (line:gsub("%s+$", ""))
        if #lines > MAX_LINES then
            table.remove(lines, 1)
            dropped = dropped + 1
        end
    end

    local scroll, clear = win.scroll, win.clear
    win.scroll = function(n)
        for y = 1, math.min(math.max(n, 0), h) do keep((win.getLine(y))) end
        return scroll(n)
    end
    win.clear = function()
        local last = 0
        for y = 1, h do
            if (win.getLine(y)):find("%S") then last = y end
        end
        for y = 1, last do keep((win.getLine(y))) end
        return clear()
    end

    local function text()
        local out = {}
        if dropped > 0 then out[1] = ("[%d earlier lines dropped]"):format(dropped) end
        for i = 1, #lines do out[#out + 1] = lines[i] end
        for y = 1, h do out[#out + 1] = ((win.getLine(y)):gsub("%s+$", "")) end
        while #out > 0 and out[#out] == "" do out[#out] = nil end
        return table.concat(out, "\n")
    end

    return win, text
end

local function finish(job, ok, values, err)
    agent.reply(job.id, ok, values, job.text(), err)
end

local function start(id, kind, code, as_json)
    local env = setmetatable({ shell = job_shell, multishell = job_multishell }, { __index = _G })
    if make_require then env.require, env.package = make_require(env, "/") end
    local fn, err
    if kind == "shell" then
        fn = function()
            local ok = job_shell.run(code)
            return ok
        end
    else
        -- Try it as an expression first, so "turtle.getFuelLevel()" returns its value.
        fn = load("return " .. code, "=ccagent", "t", env)
        if not fn then fn, err = load(code, "=ccagent", "t", env) end
    end

    local job = { id = id, as_json = as_json }
    job.win, job.text = capture()
    if not fn then
        finish(job, false, nil, err)
        return nil
    end
    job.co = coroutine.create(fn)
    return job
end

-- Resume a job with one event. Returns true once the job is over.
local function step(job, event)
    if event and job.filter ~= nil and event[1] ~= job.filter and event[1] ~= "terminate" then
        return false
    end

    local previous = term.redirect(job.win)
    active = job
    local result
    if event then
        result = table.pack(coroutine.resume(job.co, table.unpack(event, 1, event.n)))
    else
        result = table.pack(coroutine.resume(job.co))
    end
    active = nil
    term.redirect(previous)

    if not result[1] then
        finish(job, false, nil, tostring(result[2]))
        return true
    end
    if coroutine.status(job.co) == "dead" then
        local values
        if job.as_json then
            values = serialise(result[2], true)
        else
            local parts = {}
            for i = 2, result.n do parts[#parts + 1] = serialise(result[i], false) end
            values = table.concat(parts, ", ")
        end
        finish(job, true, values, nil)
        return true
    end
    job.filter = result[2]
    return false
end

-- Run jobs side by side until every one is done. New jobs start as they arrive.
local function run_all(jobs)
    while #jobs > 0 do
        local event = table.pack(native_pull())
        local name = event[1]
        if name == "ccagent_exec" then
            local job = start(event[2], event[3], event[4], event[5])
            if job and not step(job, nil) then jobs[#jobs + 1] = job end
        elseif name == "ccagent_cancel" then
            for i = #jobs, 1, -1 do
                if jobs[i].id == event[2] then
                    finish(jobs[i], false, nil, "cancelled")
                    table.remove(jobs, i)
                end
            end
        else
            for i = #jobs, 1, -1 do
                if step(jobs[i], event) then table.remove(jobs, i) end
            end
            -- A terminate stays with the jobs; everything else goes back to the program afterwards.
            if name ~= "terminate" then
                backlog[#backlog + 1] = event
                if #backlog > MAX_BACKLOG then table.remove(backlog, 1) end
            end
        end
    end
end

local function exec(event)
    local job = start(event[2], event[3], event[4], event[5])
    if job and not step(job, nil) then run_all({ job }) end
end

function os.pullEventRaw(filter)
    -- Calls from inside a job (sleep, turtle moves, read) pass straight through to the job runner.
    if active then return native_pull(filter) end

    while true do
        while #backlog > 0 do
            local event = table.remove(backlog, 1)
            if filter == nil or event[1] == filter or event[1] == "terminate" then
                return table.unpack(event, 1, event.n)
            end
        end

        local event = table.pack(native_pull())
        local name = event[1]
        if name == "ccagent_exec" then
            exec(event)
        elseif name ~= "ccagent_cancel" then
            if filter == nil or name == filter or name == "terminate" then
                return table.unpack(event, 1, event.n)
            end
        end
    end
end

agent.ready()
