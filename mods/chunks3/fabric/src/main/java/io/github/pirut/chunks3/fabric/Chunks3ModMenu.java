package io.github.pirut.chunks3.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.pirut.chunks3.config.Chunks3ConfigScreen;

/** Chunks3's settings behind Mod Menu's config button; Mod Menu calls this only when it is installed. */
public final class Chunks3ModMenu implements ModMenuApi {
    @Override public ConfigScreenFactory<?> getModConfigScreenFactory() { return Chunks3ConfigScreen::new; }
}
