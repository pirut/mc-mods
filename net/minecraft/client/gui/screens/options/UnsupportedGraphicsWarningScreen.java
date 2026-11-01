package net.minecraft.client.gui.screens.options;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;

@Environment(EnvType.CLIENT)
public class UnsupportedGraphicsWarningScreen extends Screen {
   private static final int BUTTON_PADDING = 20;
   private static final int BUTTON_MARGIN = 5;
   private static final int BUTTON_HEIGHT = 20;
   private final Component narrationMessage;
   private final List<Component> message;
   private final ImmutableList<UnsupportedGraphicsWarningScreen.ButtonOption> buttonOptions;
   private MultiLineLabel messageLines = MultiLineLabel.EMPTY;
   private int contentTop;
   private int buttonWidth;

   protected UnsupportedGraphicsWarningScreen(
      Component title, List<Component> message, ImmutableList<UnsupportedGraphicsWarningScreen.ButtonOption> buttonOptions
   ) {
      super(title);
      this.message = message;
      this.narrationMessage = CommonComponents.joinForNarration(title, ComponentUtils.formatList(message, CommonComponents.EMPTY));
      this.buttonOptions = buttonOptions;
   }

   @Override
   public Component getNarrationMessage() {
      return this.narrationMessage;
   }

   @Override
   public void init() {
      UnmodifiableIterator i = this.buttonOptions.iterator();

      while (i.hasNext()) {
         UnsupportedGraphicsWarningScreen.ButtonOption buttonOption = (UnsupportedGraphicsWarningScreen.ButtonOption)i.next();
         this.buttonWidth = Math.max(this.buttonWidth, 20 + this.font.width(buttonOption.message) + 20);
      }

      int ix = 5 + this.buttonWidth + 5;
      int j = ix * this.buttonOptions.size();
      this.messageLines = MultiLineLabel.create(this.font, j, this.message.toArray(new Component[0]));
      int k = this.messageLines.getLineCount() * 9;
      this.contentTop = (int)(this.height / 2.0 - k / 2.0);
      int l = this.contentTop + k + 9 * 2;
      int m = (int)(this.width / 2.0 - j / 2.0);

      for (UnmodifiableIterator var6 = this.buttonOptions.iterator(); var6.hasNext(); m += ix) {
         UnsupportedGraphicsWarningScreen.ButtonOption buttonOption2 = (UnsupportedGraphicsWarningScreen.ButtonOption)var6.next();
         this.addRenderableWidget(Button.builder(buttonOption2.message, buttonOption2.onPress).bounds(m, l, this.buttonWidth, 20).build());
      }
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      super.render(guiGraphics, mouseX, mouseY, partialTick);
      ActiveTextCollector activeTextCollector = guiGraphics.textRenderer();
      guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.contentTop - 9 * 2, -1);
      this.messageLines.visitLines(TextAlignment.CENTER, this.width / 2, this.contentTop, 9, activeTextCollector);
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Environment(EnvType.CLIENT)
   public static final class ButtonOption {
      final Component message;
      final Button.OnPress onPress;

      public ButtonOption(Component message, Button.OnPress onPress) {
         this.message = message;
         this.onPress = onPress;
      }
   }
}
