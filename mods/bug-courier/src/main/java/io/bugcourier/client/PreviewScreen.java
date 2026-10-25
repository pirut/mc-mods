package io.bugcourier.client;

import io.bugcourier.core.Attachment;
import io.bugcourier.core.IssueBody;
import io.bugcourier.core.Report;
import java.time.Duration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Step two: the player sees every byte that will leave the computer, unticks what they don't want, and sends. */
final class PreviewScreen extends Screen {
    private final ReportScreen parent;
    private final ReportTarget target;
    private final Report report;
    private TextView view;
    private Button send;
    private String full;

    PreviewScreen(ReportScreen parent, ReportTarget target, Report report) {
        super(Component.translatable("bugcourier.preview.title"));
        this.parent = parent;
        this.target = target;
        this.report = report;
    }

    @Override
    protected void init() {
        int margin = Math.max(12, (width - 520) / 2);
        int x = margin;
        int y = 28;
        for (Attachment a : report.attachments()) {
            Component label = Component.translatable("bugcourier.preview.attachment", a.label(), kb(a.sizeBytes()));
            int w = font.width(label) + 24;
            if (x + w > width - margin) {
                x = margin;
                y += 22;
            }
            addRenderableWidget(Checkbox.builder(label, font).pos(x, y).selected(a.included()).onValueChange((c, v) -> {
                a.setIncluded(v);
                refresh();
            }).build());
            x += w + 8;
        }
        y += 26;
        view = addRenderableWidget(new TextView(font, margin, y, width - 2 * margin, Math.max(40, height - y - 64)));
        int bw = 120;
        int bx = width / 2 - (3 * bw + 8) / 2;
        send = addRenderableWidget(Button.builder(Component.translatable("bugcourier.preview.send"), b -> send()).bounds(bx, height - 28, bw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("bugcourier.preview.copy"), b -> minecraft.keyboardHandler.setClipboard(full))
                .bounds(bx + bw + 4, height - 28, bw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose()).bounds(bx + 2 * (bw + 4), height - 28, bw, 20).build());
        refresh();
    }

    private void refresh() {
        full = IssueBody.full(report, Sender.libraryVersion());
        view.setText(full);
    }

    private void send() {
        Duration wait = Sender.cooldownLeft(target.modId());
        if (!wait.isZero()) return;
        minecraft.setScreen(Sender.send(this, parent, target, report, full));
    }

    @Override
    public void tick() {
        Duration wait = Sender.cooldownLeft(target.modId());
        send.active = wait.isZero();
        send.setMessage(wait.isZero() ? Component.translatable("bugcourier.preview.send")
                : Component.translatable("bugcourier.preview.wait", wait.toMinutes(), String.format("%02d", wait.toSecondsPart())));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        Component how = target.tracker().prefills()
                ? Component.translatable("bugcourier.preview.how", target.tracker().displayName())
                : Component.translatable("bugcourier.preview.how_paste", target.tracker().displayName());
        MultiLineLabel.create(font, how, width - 40).renderCentered(g, width / 2, height - 52, font.lineHeight, 0xA0A0A0);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private static String kb(int bytes) {
        return bytes < 1024 ? bytes + " B" : (bytes + 512) / 1024 + " KB";
    }
}
