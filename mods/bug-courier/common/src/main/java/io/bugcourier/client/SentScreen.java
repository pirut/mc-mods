package io.bugcourier.client;

import io.bugcourier.core.IssueBody;
import java.nio.file.Path;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Step three: tells the player what happened and how to finish the issue in the browser. */
final class SentScreen extends Screen {
    private final Screen parent;
    private final ReportTarget target;
    private final IssueBody.Prefilled prefilled;
    private final Path zip;
    private final String full;
    private MultiLineLabel text;

    SentScreen(Screen parent, ReportTarget target, IssueBody.Prefilled prefilled, Path zip, String full) {
        super(Component.translatable("bugcourier.sent.title"));
        this.parent = parent;
        this.target = target;
        this.prefilled = prefilled;
        this.zip = zip;
        this.full = full;
    }

    @Override
    protected void init() {
        Component body = zip == null
                ? Component.translatable("bugcourier.sent.body_nozip", target.tracker().displayName())
                : Component.translatable("bugcourier.sent.body", target.tracker().displayName(), zip.getFileName().toString());
        text = MultiLineLabel.create(font, body, Math.min(360, width - 24));
        // Buttons sit on the bottom two rows, the text above them, so a narrow window can't push them off screen.
        int bw = Math.min(150, (width - 20) / 2);
        int row2 = height - 26;
        int row1 = row2 - 24;
        addRenderableWidget(Button.builder(Component.translatable("bugcourier.sent.open_page"), b -> Util.getPlatform().openUri(prefilled.uri()))
                .bounds(width / 2 - bw - 2, row1, bw, 20).build());
        Button folder = addRenderableWidget(Button.builder(Component.translatable("bugcourier.sent.open_folder"),
                b -> Util.getPlatform().openFile(zip.getParent().toFile())).bounds(width / 2 + 2, row1, bw, 20).build());
        folder.active = zip != null;
        addRenderableWidget(Button.builder(Component.translatable("bugcourier.preview.copy"), b -> minecraft.keyboardHandler.setClipboard(full))
                .bounds(width / 2 - bw - 2, row2, bw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(width / 2 + 2, row2, bw, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int textHeight = 14 + text.getLineCount() * font.lineHeight;
        int top = Math.max(6, (height - 52 - textHeight) / 2);
        g.drawCenteredString(font, title, width / 2, top, 0xFFFFFF);
        text.renderCentered(g, width / 2, top + 14);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
