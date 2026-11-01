package net.neoforged.neoforge.client.event;

import java.util.Map;
import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.event.IModBusEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterPresetEditorsEvent extends Event implements IModBusEvent {
   private static final Logger LOGGER = LogManager.getLogger();
   private final Map<ResourceKey<WorldPreset>, PresetEditor> editors;

   @Internal
   public RegisterPresetEditorsEvent(Map<ResourceKey<WorldPreset>, PresetEditor> editors) {
      this.editors = editors;
   }

   public void register(ResourceKey<WorldPreset> key, PresetEditor editor) {
      PresetEditor old = this.editors.put(key, editor);
      if (old != null) {
         LOGGER.debug("PresetEditor {} overridden by mod {}", key.location(), ModLoadingContext.get().getActiveNamespace());
      }
   }
}
