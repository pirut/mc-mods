package io.github.pirut.ccagent;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

/** Chat click and hover events, for Minecraft before 1.21.5. */
final class McChat {
    private McChat() {
    }

    static ClickEvent copy(String text) {
        return new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text);
    }

    static ClickEvent run(String command) {
        return new ClickEvent(ClickEvent.Action.RUN_COMMAND, command);
    }

    static HoverEvent tooltip(Component text) {
        return new HoverEvent(HoverEvent.Action.SHOW_TEXT, text);
    }
}
