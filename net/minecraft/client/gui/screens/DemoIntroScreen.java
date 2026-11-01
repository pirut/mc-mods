package net.minecraft.client.gui.screens;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Options;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonLinks;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public class DemoIntroScreen extends Screen {
   private static final Identifier DEMO_BACKGROUND_LOCATION = Identifier.withDefaultNamespace("textures/gui/demo_background.png");
   private static final int BACKGROUND_TEXTURE_WIDTH = 256;
   private static final int BACKGROUND_TEXTURE_HEIGHT = 256;
   private static final int TEXT_COLOR = -14737633;
   private MultiLineLabel movementMessage = MultiLineLabel.EMPTY;
   private MultiLineLabel durationMessage = MultiLineLabel.EMPTY;

   public DemoIntroScreen() {
      super(Component.translatable("demo.help.title"));
   }

   @Override
   protected void init() {
      int i = -16;
      this.addRenderableWidget(Button.builder(Component.translatable("demo.help.buy"), button -> {
         button.active = false;
         Util.getPlatform().openUri(CommonLinks.BUY_MINECRAFT_JAVA);
      }).bounds(this.width / 2 - 116, this.height / 2 + 62 + -16, 114, 20).build());
      this.addRenderableWidget(Button.builder(Component.translatable("demo.help.later"), button -> {
         this.minecraft.setScreen(null);
         this.minecraft.mouseHandler.grabMouse();
      }).bounds(this.width / 2 + 2, this.height / 2 + 62 + -16, 114, 20).build());
      Options options = this.minecraft.options;
      this.movementMessage = MultiLineLabel.create(
         this.font,
         this.movementMessage(
            Component.translatable(
               "demo.help.movementShort",
               options.keyUp.getTranslatedKeyMessage(),
               options.keyLeft.getTranslatedKeyMessage(),
               options.keyDown.getTranslatedKeyMessage(),
               options.keyRight.getTranslatedKeyMessage()
            )
         ),
         this.movementMessage(Component.translatable("demo.help.movementMouse")),
         this.movementMessage(Component.translatable("demo.help.jump", options.keyJump.getTranslatedKeyMessage())),
         this.movementMessage(Component.translatable("demo.help.inventory", options.keyInventory.getTranslatedKeyMessage()))
      );
      this.durationMessage = MultiLineLabel.create(this.font, Component.translatable("demo.help.fullWrapped").withoutShadow().withColor(-14737633), 218);
   }

   private Component movementMessage(MutableComponent message) {
      return message.withoutShadow().withColor(-11579569);
   }

   @Override
   public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
      int i = (this.width - 248) / 2;
      int j = (this.height - 166) / 2;
      guiGraphics.blit(RenderPipelines.GUI_TEXTURED, DEMO_BACKGROUND_LOCATION, i, j, 0.0F, 0.0F, 248, 166, 256, 256);
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      super.render(guiGraphics, mouseX, mouseY, partialTick);
      int i = (this.width - 248) / 2 + 10;
      int j = (this.height - 166) / 2 + 8;
      ActiveTextCollector activeTextCollector = guiGraphics.textRenderer();
      guiGraphics.drawString(this.font, this.title, i, j, -14737633, false);
      j = this.movementMessage.visitLines(TextAlignment.LEFT, i, j + 12, 12, activeTextCollector);
      this.durationMessage.visitLines(TextAlignment.LEFT, i, j + 20, 9, activeTextCollector);
   }
}
