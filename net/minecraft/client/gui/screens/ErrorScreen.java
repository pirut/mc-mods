package net.minecraft.client.gui.screens;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class ErrorScreen extends Screen {
   private final Component message;

   public ErrorScreen(Component title, Component message) {
      super(title);
      this.message = message;
   }

   @Override
   protected void init() {
      super.init();
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_CANCEL, button -> this.minecraft.setScreen(null)).bounds(this.width / 2 - 100, 140, 200, 20).build()
      );
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      super.render(guiGraphics, mouseX, mouseY, partialTick);
      guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 90, -1);
      guiGraphics.drawCenteredString(this.font, this.message, this.width / 2, 110, -1);
   }

   @Override
   public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      guiGraphics.fillGradient(0, 0, this.width, this.height, -12574688, -11530224);
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }
}
