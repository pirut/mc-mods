package net.minecraft.client.gui.screens.options;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
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
      Component p_344923_, List<Component> p_344787_, ImmutableList<UnsupportedGraphicsWarningScreen.ButtonOption> p_346327_
   ) {
      super(p_344923_);
      this.message = p_344787_;
      this.narrationMessage = CommonComponents.joinForNarration(p_344923_, ComponentUtils.formatList(p_344787_, CommonComponents.EMPTY));
      this.buttonOptions = p_346327_;
   }

   @Override
   public Component getNarrationMessage() {
      return this.narrationMessage;
   }

   @Override
   public void init() {
      UnmodifiableIterator $$1 = this.buttonOptions.iterator();

      while ($$1.hasNext()) {
         UnsupportedGraphicsWarningScreen.ButtonOption $$0 = (UnsupportedGraphicsWarningScreen.ButtonOption)$$1.next();
         this.buttonWidth = Math.max(this.buttonWidth, 20 + this.font.width($$0.message) + 20);
      }

      int $$1x = 5 + this.buttonWidth + 5;
      int $$2 = $$1x * this.buttonOptions.size();
      this.messageLines = MultiLineLabel.create(this.font, $$2, this.message.toArray(new Component[0]));
      int $$3 = this.messageLines.getLineCount() * 9;
      this.contentTop = (int)(this.height / 2.0 - $$3 / 2.0);
      int $$4 = this.contentTop + $$3 + 9 * 2;
      int $$5 = (int)(this.width / 2.0 - $$2 / 2.0);

      for (UnmodifiableIterator var6 = this.buttonOptions.iterator(); var6.hasNext(); $$5 += $$1x) {
         UnsupportedGraphicsWarningScreen.ButtonOption $$6 = (UnsupportedGraphicsWarningScreen.ButtonOption)var6.next();
         this.addRenderableWidget(Button.builder($$6.message, $$6.onPress).bounds($$5, $$4, this.buttonWidth, 20).build());
      }
   }

   @Override
   public void render(GuiGraphics p_345032_, int p_345965_, int p_345120_, float p_346047_) {
      super.render(p_345032_, p_345965_, p_345120_, p_346047_);
      p_345032_.drawCenteredString(this.font, this.title, this.width / 2, this.contentTop - 9 * 2, -1);
      this.messageLines.renderCentered(p_345032_, this.width / 2, this.contentTop);
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @OnlyIn(Dist.CLIENT)
   public static final class ButtonOption {
      final Component message;
      final Button.OnPress onPress;

      public ButtonOption(Component p_346062_, Button.OnPress p_345795_) {
         this.message = p_346062_;
         this.onPress = p_345795_;
      }
   }
}
