package net.neoforged.neoforge.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.RegisterPresetEditorsEvent;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

public final class PresetEditorManager {
   private static Map<ResourceKey<WorldPreset>, PresetEditor> editors = Map.of();

   private PresetEditorManager() {
   }

   @Internal
   static void init() {
      Map<ResourceKey<WorldPreset>, PresetEditor> gatheredEditors = new HashMap<>();
      PresetEditor.EDITORS.forEach((k, v) -> k.ifPresent(key -> gatheredEditors.put((ResourceKey<WorldPreset>)key, v)));
      RegisterPresetEditorsEvent event = new RegisterPresetEditorsEvent(gatheredEditors);
      ModLoader.postEventWrapContainerInModOrder(event);
      editors = gatheredEditors;
   }

   @Nullable
   public static PresetEditor get(ResourceKey<WorldPreset> key) {
      return editors.get(key);
   }
}
