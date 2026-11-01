package io.bugcourier.client;

import io.bugcourier.core.CrashReports;
import io.bugcourier.core.IssueBody;
import io.bugcourier.core.Report;
import io.bugcourier.client.gui.BaseScreen;
import io.bugcourier.client.gui.Canvas;
import io.bugcourier.client.version.Mc;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Step one: which mod, a title and what happened. Step two is the preview. */
public final class ReportScreen extends BaseScreen {
    private static final int MAX_WIDTH = 320;
    private int formWidth = MAX_WIDTH;
    private int goesToY;
    private boolean compact;
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
        // Laid out from the bottom up: the buttons always keep their row, and the description box takes what is left.
        formWidth = Math.min(MAX_WIDTH, width - 16);
        int left = (width - formWidth) / 2;
        int buttonY = height - 26;
        int bw = Math.min(150, (width - 20) / 2);
        compact = height < 230;
        if (target == null) {
            addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose()).bounds(width / 2 - bw / 2, buttonY, bw, 20).build());
            return;
        }
        int y = compact ? 6 : 24;
        addRenderableWidget(Mc.cycleButton(t -> Component.literal(t.name()), targets, target, left, y, formWidth, 20,
                Component.translatable("bugcourier.screen.mod"), t -> target = t));
        goesToY = y + 24;
        y += 36;
        titleBox = new EditBox(font, left, y, formWidth, 20, Component.translatable("bugcourier.screen.issue_title"));
        titleBox.setMaxLength(120);
        titleBox.setHint(Component.translatable("bugcourier.screen.issue_title.hint"));
        titleBox.setValue(title);
        titleBox.setResponder(s -> {
            title = s;
            update();
        });
        addRenderableWidget(titleBox);
        y += 24;
        int checkboxY = buttonY - 24;
        // The edit box draws its character count under itself, so leave it 10 pixels.
        int boxHeight = Math.max(20, Math.min(110, checkboxY - 12 - y));
        descriptionBox = Mc.multiLineEditBox(font, left, y, formWidth, boxHeight, Component.translatable("bugcourier.screen.description.hint"),
                Component.translatable("bugcourier.screen.description"));
        descriptionBox.setCharacterLimit(6000);
        descriptionBox.setValue(description);
        descriptionBox.setValueListener(s -> {
            description = s;
            update();
        });
        addRenderableWidget(descriptionBox);
        addRenderableWidget(Checkbox.builder(Component.translatable("bugcourier.screen.include_player"), font).pos(left, checkboxY)
                .selected(includePlayer).onValueChange((c, v) -> includePlayer = v).build());

        next = addRenderableWidget(Button.builder(Component.translatable("bugcourier.screen.preview"), b -> preview())
                .bounds(width / 2 - bw - 2, buttonY, bw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose()).bounds(width / 2 + 2, buttonY, bw, 20).build());
        setInitialFocus(titleBox);
        update();
    }

    private void update() {
        if (next == null) return;
        next.active = !title.isBlank() && description.strip().length() >= IssueBody.MIN_DESCRIPTION;
        // Why the button is grey goes in its tooltip, so it needs no line of its own on a small window.
        next.setTooltip(next.active ? null : Tooltip.create(Component.translatable(title.isBlank() ? "bugcourier.screen.need_title"
                : "bugcourier.screen.need_more", IssueBody.MIN_DESCRIPTION)));
    }

    private void preview() {
        Report report = Collectors.build(target, title, description, crashText, includePlayer);
        Mc.setScreen(new PreviewScreen(this, target, report));
    }

    @Override
    protected void draw(Canvas c) {
        if (!compact) c.centered(font, getTitle(), width / 2, 8, Canvas.WHITE);
        if (target == null) {
            c.centered(font, Component.translatable("bugcourier.screen.no_mods"), width / 2, height / 2 - 4, Canvas.LIGHT_GREY);
            return;
        }
        Component goesTo = Component.translatable("bugcourier.screen.goes_to", target.tracker().displayName());
        c.text(font, Component.literal(font.substrByWidth(goesTo, formWidth).getString()), (width - formWidth) / 2, goesToY, Canvas.GREY);
    }

    Screen parentScreen() {
        return parent;
    }

    @Override
    public void onClose() {
        Mc.setScreen(parent);
    }
}
