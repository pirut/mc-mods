package net.minecraft.client.gui.spectator;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public interface SpectatorMenuItem {
   void selectItem(SpectatorMenu menu);

   Component getName();

   void renderIcon(GuiGraphics guiGraphics, float brightness, float alpha);

   boolean isEnabled();
}
