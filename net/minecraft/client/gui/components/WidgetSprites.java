package net.minecraft.client.gui.components;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public record WidgetSprites(Identifier enabled, Identifier disabled, Identifier enabledFocused, Identifier disabledFocused) {
   public WidgetSprites(Identifier sprite) {
      this(sprite, sprite, sprite, sprite);
   }

   public WidgetSprites(Identifier unfocused, Identifier focused) {
      this(unfocused, unfocused, focused, focused);
   }

   public WidgetSprites(Identifier enabled, Identifier disabled, Identifier focused) {
      this(enabled, disabled, focused, disabled);
   }

   public Identifier get(boolean enabled, boolean focused) {
      if (enabled) {
         return focused ? this.enabledFocused : this.enabled;
      } else {
         return focused ? this.disabledFocused : this.disabled;
      }
   }
}
