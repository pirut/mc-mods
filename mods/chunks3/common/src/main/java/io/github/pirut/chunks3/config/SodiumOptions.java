package io.github.pirut.chunks3.config;

import io.github.pirut.chunks3.Cubic;
import io.github.pirut.chunks3.far.Far;
import java.util.Locale;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPointForge;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionGroupBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * A Chunks3 page in Sodium's Video Settings, as Earth Platform has one: the far terrain's distance, sharpness,
 * frame rate to hold and textured levels, and the engine's switches. Sodium finds this class only when it is
 * installed (by the annotation on NeoForge, the {@code sodium:config_api_user} entry point on Fabric), so
 * nothing here loads without it. The values are the same ones the config screen and the pause menu use.
 */
@ConfigEntryPointForge(Cubic.MOD_ID)
public final class SodiumOptions implements ConfigEntryPoint {
    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        var detail = builder.createIntegerOption(id("far_distance"))
                .setName(Component.translatable("chunks3.sodium.distance"))
                .setTooltip(Component.translatable("chunks3.configuration.farDetail.tooltip"))
                .setRange(4, Far.MAX_DETAIL, 1)
                .setValueFormatter(value -> Component.literal(distance(value)))
                .setDefaultValue(Chunks3Config.FAR_DETAIL.defaultValue())
                .setImpact(OptionImpact.HIGH)
                .setBinding(Chunks3Config::setDetail, Chunks3Config.FAR_DETAIL::get)
                .setStorageHandler(Chunks3Config::save);
        var sharpness = builder.createEnumOption(id("far_sharpness"), Chunks3Config.Sharpness.class)
                .setName(Component.translatable("chunks3.sodium.sharpness"))
                .setTooltip(Component.translatable("chunks3.configuration.farSharpness.tooltip"))
                .setElementNameProvider(Chunks3Config.Sharpness::getTranslatedName)
                .setDefaultValue(Chunks3Config.FAR_SHARPNESS.defaultValue())
                .setImpact(OptionImpact.HIGH)
                .setBinding(Chunks3Config::setSharpness, Chunks3Config.FAR_SHARPNESS::get)
                .setStorageHandler(Chunks3Config::save);
        var target = builder.createIntegerOption(id("far_target_fps"))
                .setName(Component.translatable("chunks3.sodium.targetFps"))
                .setTooltip(Component.translatable("chunks3.configuration.farTargetFps.tooltip"))
                .setRange(0, 240, 5)
                .setValueFormatter(value -> value == 0 ? Component.translatable("options.off") : Component.translatable("options.framerate", value))
                .setDefaultValue(0)
                .setImpact(OptionImpact.VARIES)
                .setBinding(Chunks3Config::setTargetFps, () -> Chunks3Config.FAR_TARGET_FPS.get() / 5 * 5)
                .setStorageHandler(Chunks3Config::save);
        var textured = builder.createIntegerOption(id("far_textured_levels"))
                .setName(Component.translatable("chunks3.sodium.texturedLevels"))
                .setTooltip(Component.translatable("chunks3.configuration.farTexturedLevels.tooltip"))
                .setRange(0, Far.MAX_LEVEL, 1)
                .setValueFormatter(value -> Component.literal(Integer.toString(value)))
                .setDefaultValue(Chunks3Config.FAR_TEXTURED_LEVELS.defaultValue())
                .setImpact(OptionImpact.MEDIUM)
                .setFlags(OptionFlag.REQUIRES_GAME_RESTART)
                .setBinding(Chunks3Config::setTexturedLevels, Chunks3Config.FAR_TEXTURED_LEVELS::get)
                .setStorageHandler(Chunks3Config::save);
        var allSettings = builder.createExternalButtonOption(id("all_settings"))
                .setName(Component.translatable("chunks3.sodium.allSettings"))
                .setTooltip(Component.translatable("chunks3.sodium.allSettings.tooltip"))
                .setScreenConsumer(parent -> Minecraft.getInstance().setScreen(new Chunks3ConfigScreen(parent)));

        builder.registerOwnModOptions()
                .setName("Chunks3")
                .addPage(builder.createOptionPage()
                        .setName(Component.translatable("chunks3.sodium.page"))
                        .addOptionGroup(toggles(builder, builder.createOptionGroup()
                                .setName(Component.translatable("chunks3.configuration.group.farTerrain"))
                                .addOption(detail)
                                .addOption(sharpness)
                                .addOption(target)
                                .addOption(textured), EngineToggles.Group.FAR_TERRAIN))
                        .addOptionGroup(toggles(builder, builder.createOptionGroup()
                                .setName(Component.translatable("chunks3.configuration.group.nearbyBlocks")), EngineToggles.Group.NEARBY_BLOCKS))
                        .addOptionGroup(toggles(builder, builder.createOptionGroup()
                                .setName(Component.translatable("chunks3.configuration.group.compatibility")), EngineToggles.Group.COMPATIBILITY)
                                .addOption(allSettings)));
    }

    /** A group's switches, each read when the game starts, so each carries Sodium's restart flag. */
    private static OptionGroupBuilder toggles(ConfigBuilder builder, OptionGroupBuilder group, EngineToggles.Group heading) {
        for (EngineToggles.Toggle toggle : EngineToggles.of(heading)) {
            group.addOption(builder.createBooleanOption(id(toggle.sodiumId()))
                    .setName(Component.translatable(toggle.langKey()))
                    .setTooltip(Component.translatable(toggle.langKey() + ".tooltip"))
                    .setDefaultValue(toggle.defaultValue())
                    .setImpact(OptionImpact.VARIES)
                    .setFlags(OptionFlag.REQUIRES_GAME_RESTART)
                    .setBinding(value -> Chunks3Config.setToggle(toggle, value), () -> EngineToggles.get(toggle))
                    .setStorageHandler(Chunks3Config::save));
        }
        return group;
    }

    /** The far distance as the config screen shows it: kilometres, with miles beside them. */
    static String distance(int detail) {
        int km = Chunks3Config.distanceKm(detail);
        return String.format(Locale.ROOT, "%d km (%d mi)", km, Math.round(km * 0.621371));
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(Cubic.MOD_ID, path); }
}
