package io.github.pirut.ccagent;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

/** Chat click and hover events, for Minecraft 1.21.5 and later. */
final class McChat {
    private McChat() {
    }

    static ClickEvent copy(String text) {
        return new ClickEvent.CopyToClipboard(text);
    }

    static ClickEvent run(String command) {
        return new ClickEvent.RunCommand(command);
    }

    static HoverEvent tooltip(Component text) {
        return new HoverEvent.ShowText(text);
    }
}
