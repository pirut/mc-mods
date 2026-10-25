package io.bugcourier.client;

import io.bugcourier.core.CrashReports;
import io.bugcourier.core.IssueBody;
import io.bugcourier.core.Report;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Step one: which mod, a title and what happened. Step two is the preview. */
public final class ReportScreen extends Screen {
    private static final int WIDTH = 320;
    private final Screen parent;
    private final String crashText;
    private final List<ReportTarget> targets;
    private ReportTarget target;
    private String title = "";
    private String description = "";
    private boolean includePlayer;
    private EditBox titleBox;
    private MultiLineEditBox descriptionBox;
    private Button next;

    public ReportScreen(Screen parent, String modId, String crashText) {
        super(Component.translatable("bugcourier.screen.title"));
        this.parent = parent;
        this.crashText = crashText;
        this.targets = ReportTargets.all();
        this.target = pick(modId, crashText);
    }

    private ReportTarget pick(String modId, String crash) {
        if (modId != null) for (ReportTarget t : targets) if (t.modId().equals(modId)) return t;
        if (crash != null) {
            for (String suspect : CrashReports.suspectedMods(crash))
                for (ReportTarget t : targets) if (t.modId().equals(suspect)) return t;
        }
        return targets.isEmpty() ? null : targets.get(0);
    }

    @Override
    protected void init() {
        int left = (width - WIDTH) / 2;
        int y = 34;
        if (target == null) {
            addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose()).bounds(width / 2 - 75, height - 28, 150, 20).build());
            return;
        }
        addRenderableWidget(CycleButton.<ReportTarget>builder(t -> Component.literal(t.name()))
                .withValues(targets).withInitialValue(target)
                .create(left, y, WIDTH, 20, Component.translatable("bugcourier.screen.mod"), (b, t) -> target = t));
        y += 36;
        titleBox = new EditBox(font, left, y, WIDTH, 20, Component.translatable("bugcourier.screen.issue_title"));
        titleBox.setMaxLength(120);
        titleBox.setHint(Component.translatable("bugcourier.screen.issue_title.hint"));
        titleBox.setValue(title);
        titleBox.setResponder(s -> {
            title = s;
            update();
        });
        addRenderableWidget(titleBox);
        y += 24;
        int boxHeight = Math.max(44, Math.min(110, height - y - 92));
        descriptionBox = new MultiLineEditBox(font, left, y, WIDTH, boxHeight, Component.translatable("bugcourier.screen.description.hint"),
                Component.translatable("bugcourier.screen.description"));
        descriptionBox.setCharacterLimit(6000);
        descriptionBox.setValue(description);
        descriptionBox.setValueListener(s -> {
            description = s;
            update();
        });
        addRenderableWidget(descriptionBox);
        y += boxHeight + 6;
        addRenderableWidget(Checkbox.builder(Component.translatable("bugcourier.screen.include_player"), font).pos(left, y)
                .selected(includePlayer).onValueChange((c, v) -> includePlayer = v).build());

        next = addRenderableWidget(Button.builder(Component.translatable("bugcourier.screen.preview"), b -> preview())
                .bounds(width / 2 - 154, height - 28, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose()).bounds(width / 2 + 4, height - 28, 150, 20).build());
        setInitialFocus(titleBox);
        update();
    }

    private void update() {
        if (next != null) next.active = !title.isBlank() && description.strip().length() >= IssueBody.MIN_DESCRIPTION;
    }

    private void preview() {
        Report report = Collectors.build(target, title, description, crashText, includePlayer);
        minecraft.setScreen(new PreviewScreen(this, target, report));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, getTitle(), width / 2, 12, 0xFFFFFF);
        if (target == null) {
            g.drawCenteredString(font, Component.translatable("bugcourier.screen.no_mods"), width / 2, height / 2 - 4, 0xAAAAAA);
            return;
        }
        int left = (width - WIDTH) / 2;
        g.drawString(font, Component.translatable("bugcourier.screen.goes_to", target.tracker().displayName()), left, 58, 0xA0A0A0);
        if (next != null && !next.active && !title.isBlank()) {
            g.drawCenteredString(font, Component.translatable("bugcourier.screen.need_more", IssueBody.MIN_DESCRIPTION), width / 2, height - 40, 0xA0A0A0);
        }
    }

    Screen parentScreen() {
        return parent;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
