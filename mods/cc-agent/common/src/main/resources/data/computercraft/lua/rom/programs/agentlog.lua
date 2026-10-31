-- CC: Agent activity feed: every tool call an agent makes, live. "agentlog" or "agentlog <monitor>".

if type(ccagent) ~= "table" or not ccagent.activity then
    printError("CC: Agent isn't installed on this server.")
    return
end

local args = { ... }
local target = term.current()
if args[1] then
    local monitor = peripheral.wrap(args[1])
    if not monitor or not peripheral.hasType(args[1], "monitor") then
        printError("No monitor called " .. args[1])
        return
    end
    monitor.setTextScale(tonumber(args[2]) or 0.5)
    target = monitor
    print("Showing agent activity on " .. args[1] .. ". Ctrl+T stops.")
end

local colour = target.isColour()
local w, h = target.getSize()
target.setBackgroundColour(colours.black)
target.clear()
target.setCursorPos(1, 1)
target.setBackgroundColour(colour and colours.orange or colours.white)
target.setTextColour(colours.black)
target.clearLine()
target.write(" Agent activity")
target.setBackgroundColour(colours.black)
local body = window.create(target, 1, 2, w, h - 1)

local last = 0
-- Start from the last screenful so it isn't empty.
local backlog = ccagent.activity(0)
if #backlog > h then last = backlog[#backlog - h].id end

while true do
    for _, entry in ipairs(ccagent.activity(last)) do
        last = entry.id
        local previous = term.redirect(body)
        term.setTextColour(colours.grey)
        write(entry.time .. " ")
        term.setTextColour(entry.ok and (colour and colours.cyan or colours.white) or (colour and colours.red or colours.white))
        write(entry.tool)
        if entry.computer then
            term.setTextColour(colour and colours.yellow or colours.white)
            write(" #" .. entry.computer)
        end
        term.setTextColour(colours.white)
        print(entry.text ~= "" and (" " .. entry.text) or "")
        term.redirect(previous)
    end
    sleep(0.5)
end
