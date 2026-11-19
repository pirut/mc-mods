package io.github.pirut.chunks3.config;

import io.github.pirut.chunks3.far.Far;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/**
 * The far distance on the pause menu, as Earth Platform has it: a drag takes effect when the mouse lets go, so the
 * far terrain is asked for once and not at every step, and a keyboard step at once. Nothing waits to be applied, so
 * closing the menu right after never loses a change. (The options' own slider applies a moment after the last
 * change, from its next frame drawn; the pause menu can be gone by then.)
 */
final class FarDistanceSlider extends AbstractSliderButton {
    private static final int MIN = 4, MAX = Far.MAX_DETAIL;
    private boolean held;

    FarDistanceSlider(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty(), (Chunks3Config.FAR_DETAIL.get() - MIN) / (double) (MAX - MIN));
        setTooltip(Tooltip.create(Component.translatable("chunks3.configuration.farDetail.tooltip")));
        updateMessage();
    }

    int detail() { return MIN + (int) Math.round(value * (MAX - MIN)); }

    @Override protected void updateMessage() { setMessage(Chunks3ConfigScreen.distanceLabel(detail())); }

    @Override public void onClick(double mouseX, double mouseY) {
        held = true;
        super.onClick(mouseX, mouseY);
    }

    /** Only a release ending a press that began on this slider commits: the game hands any release over it here. */
    @Override public void onRelease(double mouseX, double mouseY) {
        super.onRelease(mouseX, mouseY);
        commit();
    }

    /**
     * Applies a drag in progress, once; nothing when none is. Called on release (the screen sends a drag's release to
     * the focused slider wherever the mouse is) and when the pause screen closes mid-drag ({@link PauseMenu#closing}).
     * Not on focus loss: a click hands focus to the slider after its press, unfocusing it first even when it already
     * had focus, so from the second drag on that applied the value at the press and then at every step.
     */
    void commit() {
        if (!held) return;
        held = false;
        Chunks3Config.setDetail(detail());
    }

    /** Keyboard steps take effect as they come; a drag when it ends. */
    @Override protected void applyValue() { if (!held) Chunks3Config.setDetail(detail()); }
}
