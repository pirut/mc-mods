package io.github.pirut.chunks3.config;

import com.mojang.serialization.Codec;
import io.github.pirut.chunks3.far.Far;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Chunks3's settings screen: Mods > Chunks3 > Config on NeoForge, Mod Menu on Fabric, and the button at the
 * bottom of Sodium's Chunks3 page. The far distance and sharpness apply at once, also while playing; the
 * sliders apply a moment after the last change, so the far terrain is asked for once and not at every step of a
 * drag; Done, Escape and closing the screen apply what is still waiting (OptionsSubScreen#onClose).
 */
public final class Chunks3ConfigScreen extends OptionsSubScreen {
    public Chunks3ConfigScreen(Screen parent) {
        super(parent, Minecraft.getInstance().options, Component.translatable("chunks3.configuration.title"));
    }

    @Override protected void addOptions() {
        list.addBig(distance());
        list.addSmall(sharpness(), smooth());
        list.addSmall(targetFps(), texturedLevels());
        List<OptionInstance<?>> toggles = new ArrayList<>();
        for (EngineToggles.Toggle toggle : EngineToggles.ALL) toggles.add(toggle(toggle));
        list.addSmall(toggles.toArray(OptionInstance<?>[]::new));
    }

    /** The far distance, in kilometres and miles, from 4 (about 37 km) to {@link Far#MAX_DETAIL}; past {@link Far#NORMAL_DETAIL} marked extreme. */
    static OptionInstance<Integer> distance() {
        return new OptionInstance<>("chunks3.configuration.farDetail", value -> Tooltip.create(Component.translatable("chunks3.configuration.farDetail.tooltip")),
                (caption, detail) -> distanceLabel(detail),
                new OptionInstance.IntRange(4, Far.MAX_DETAIL, false), Codec.INT, Chunks3Config.FAR_DETAIL.get(), Chunks3Config::setDetail);
    }

    /** "Far terrain distance: 94 km / 58 mi", and "(extreme)" past {@link Far#NORMAL_DETAIL}. */
    static Component distanceLabel(int detail) {
        int km = Chunks3Config.distanceKm(detail);
        return Component.translatable(detail > Far.NORMAL_DETAIL ? "chunks3.configuration.farDetail.extreme" : "chunks3.configuration.farDetail.value",
                Component.translatable("chunks3.configuration.farDetail"), km, Math.round(km * 0.621371));
    }

    static OptionInstance<Chunks3Config.Sharpness> sharpness() {
        return new OptionInstance<>("chunks3.configuration.farSharpness", value -> Tooltip.create(Component.translatable("chunks3.configuration.farSharpness.tooltip")),
                (caption, value) -> CommonComponents.optionNameValue(caption, value.getTranslatedName()),
                new OptionInstance.Enum<>(Arrays.asList(Chunks3Config.Sharpness.values()), Codec.STRING.xmap(Chunks3Config.Sharpness::valueOf, Enum::name)),
                Chunks3Config.FAR_SHARPNESS.get(), Chunks3Config::setSharpness);
    }

    /** Smooth far ground: Cubic worlds alone (the default), every cubic dimension, or none. */
    static OptionInstance<Chunks3Config.FarSmooth> smooth() {
        return new OptionInstance<>("chunks3.configuration.farSmooth", value -> Tooltip.create(Component.translatable("chunks3.configuration.farSmooth.tooltip")),
                (caption, value) -> CommonComponents.optionNameValue(caption, value.getTranslatedName()),
                new OptionInstance.Enum<>(Arrays.asList(Chunks3Config.FarSmooth.values()), Codec.STRING.xmap(Chunks3Config.FarSmooth::valueOf, Enum::name)),
                Chunks3Config.FAR_SMOOTH.get(), Chunks3Config::setSmooth);
    }

    /** The frame rate to hold, in steps of five: 0 is off. */
    static OptionInstance<Integer> targetFps() {
        return new OptionInstance<>("chunks3.configuration.farTargetFps", value -> Tooltip.create(Component.translatable("chunks3.configuration.farTargetFps.tooltip")),
                (caption, steps) -> CommonComponents.optionNameValue(caption, steps == 0 ? CommonComponents.OPTION_OFF : Component.translatable("options.framerate", steps * 5)),
                new OptionInstance.IntRange(0, 48, false), Codec.INT, Chunks3Config.FAR_TARGET_FPS.get() / 5, steps -> Chunks3Config.setTargetFps(steps * 5));
    }

    static OptionInstance<Integer> texturedLevels() {
        return new OptionInstance<>("chunks3.configuration.farTexturedLevels", value -> Tooltip.create(restart("chunks3.configuration.farTexturedLevels.tooltip")),
                (caption, levels) -> CommonComponents.optionNameValue(caption, Component.literal(Integer.toString(levels))),
                new OptionInstance.IntRange(0, Far.MAX_LEVEL, false), Codec.INT, Chunks3Config.FAR_TEXTURED_LEVELS.get(), Chunks3Config::setTexturedLevels);
    }

    static OptionInstance<Boolean> toggle(EngineToggles.Toggle toggle) {
        Component tooltip = Component.translatable(toggle.langKey() + ".tooltip");
        if (toggle.experimental()) tooltip = Component.translatable("chunks3.configuration.experimental").append("\n").append(tooltip);
        Component tip = tooltip;
        return OptionInstance.createBoolean(toggle.langKey(), value -> Tooltip.create(tip), EngineToggles.get(toggle),
                value -> Chunks3Config.setToggle(toggle, value));
    }

    /** A description with the line that a change applies at the next start. */
    private static Component restart(String key) {
        return Component.translatable(key).append("\n").append(Component.translatable("chunks3.configuration.restart"));
    }
}
