package net.neoforged.neoforge.client.gui.widget;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

public class ExtendedButton extends Button {
   public ExtendedButton(int xPos, int yPos, int width, int height, Component displayString, Button.OnPress handler) {
      this(xPos, yPos, width, height, displayString, handler, DEFAULT_NARRATION);
   }

   public ExtendedButton(int xPos, int yPos, int width, int height, Component displayString, Button.OnPress handler, Button.CreateNarration createNarration) {
      super(xPos, yPos, width, height, displayString, handler, createNarration);
   }

   public ExtendedButton(Button.Builder builder) {
      super(builder);
   }

   @Override
   public void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      Minecraft mc = Minecraft.getInstance();
      guiGraphics.blitSprite(
         RenderPipelines.GUI_TEXTURED, SPRITES.get(this.active, this.isHoveredOrFocused()), this.getX(), this.getY(), this.getWidth(), this.getHeight()
      );
      FormattedText buttonText = mc.font.ellipsize(this.getMessage(), this.width - 6);
      guiGraphics.drawCenteredString(
         mc.font, Language.getInstance().getVisualOrder(buttonText), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, this.getFGColor()
      );
      if (this.isHovered()) {
         guiGraphics.requestCursor(this.isActive() ? CursorTypes.POINTING_HAND : CursorTypes.NOT_ALLOWED);
      }
   }
}
