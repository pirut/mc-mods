package net.neoforged.neoforge.client.extensions;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.ClientHooks;

public interface IMinecraftExtension {
   private Minecraft self() {
      return (Minecraft)this;
   }

   default void pushGuiLayer(Screen screen) {
      ClientHooks.pushGuiLayer(this.self(), screen);
   }

   default void popGuiLayer() {
      ClientHooks.popGuiLayer(this.self());
   }

   default Locale getLocale() {
      return this.self().getLanguageManager().getJavaLocale();
   }
}
