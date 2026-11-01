package com.mojang.realmsclient.gui.screens;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.util.task.GetServerDetailsTask;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.CommonLinks;
import net.minecraft.util.Util;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class RealmsTermsScreen extends RealmsScreen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component TITLE = Component.translatable("mco.terms.title");
   private static final Component TERMS_STATIC_TEXT = Component.translatable("mco.terms.sentence.1");
   private static final Component TERMS_LINK_TEXT = CommonComponents.space()
      .append(Component.translatable("mco.terms.sentence.2").withStyle(Style.EMPTY.withUnderlined(true)));
   private final Screen lastScreen;
   private final RealmsServer realmsServer;
   private boolean onLink;

   public RealmsTermsScreen(Screen lastScreen, RealmsServer realmsServer) {
      super(TITLE);
      this.lastScreen = lastScreen;
      this.realmsServer = realmsServer;
   }

   @Override
   public void init() {
      int i = this.width / 4 - 2;
      this.addRenderableWidget(
         Button.builder(Component.translatable("mco.terms.buttons.agree"), button -> this.agreedToTos()).bounds(this.width / 4, row(12), i, 20).build()
      );
      this.addRenderableWidget(
         Button.builder(Component.translatable("mco.terms.buttons.disagree"), button -> this.minecraft.setScreen(this.lastScreen))
            .bounds(this.width / 2 + 4, row(12), i, 20)
            .build()
      );
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      if (event.key() == 256) {
         this.minecraft.setScreen(this.lastScreen);
         return true;
      } else {
         return super.keyPressed(event);
      }
   }

   private void agreedToTos() {
      RealmsClient realmsClient = RealmsClient.getOrCreate();

      try {
         realmsClient.agreeToTos();
         this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen, new GetServerDetailsTask(this.lastScreen, this.realmsServer)));
      } catch (RealmsServiceException var3) {
         LOGGER.error("Couldn't agree to TOS", var3);
      }
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
      if (this.onLink) {
         this.minecraft.keyboardHandler.setClipboard(CommonLinks.REALMS_TERMS.toString());
         Util.getPlatform().openUri(CommonLinks.REALMS_TERMS);
         return true;
      } else {
         return super.mouseClicked(event, isDoubleClick);
      }
   }

   @Override
   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(super.getNarrationMessage(), TERMS_STATIC_TEXT).append(CommonComponents.SPACE).append(TERMS_LINK_TEXT);
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      super.render(guiGraphics, mouseX, mouseY, partialTick);
      guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 17, -1);
      guiGraphics.drawString(this.font, TERMS_STATIC_TEXT, this.width / 2 - 120, row(5), -1);
      int i = this.font.width(TERMS_STATIC_TEXT);
      int j = this.width / 2 - 121 + i;
      int k = row(5);
      int l = j + this.font.width(TERMS_LINK_TEXT) + 1;
      int m = k + 1 + 9;
      this.onLink = j <= mouseX && mouseX <= l && k <= mouseY && mouseY <= m;
      guiGraphics.drawString(this.font, TERMS_LINK_TEXT, this.width / 2 - 120 + i, row(5), this.onLink ? -9670204 : -13408581);
   }
}
