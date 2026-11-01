package net.minecraft.client.gui.screens;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.net.URI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ConfirmLinkScreen extends ConfirmScreen {
   private static final Component WARNING_TEXT = Component.translatable("chat.link.warning").withColor(-13108);
   private static final int BUTTON_WIDTH = 100;
   private final String url;
   private final boolean showWarning;

   public ConfirmLinkScreen(BooleanConsumer p_95631_, String p_95632_, boolean p_95633_) {
      this(
         p_95631_, confirmMessage(p_95633_), Component.literal(p_95632_), p_95632_, p_95633_ ? CommonComponents.GUI_CANCEL : CommonComponents.GUI_NO, p_95633_
      );
   }

   public ConfirmLinkScreen(BooleanConsumer p_238329_, Component p_238330_, String p_238331_, boolean p_238332_) {
      this(p_238329_, p_238330_, confirmMessage(p_238332_, p_238331_), p_238331_, p_238332_ ? CommonComponents.GUI_CANCEL : CommonComponents.GUI_NO, p_238332_);
   }

   public ConfirmLinkScreen(BooleanConsumer p_352448_, Component p_352313_, URI p_352270_, boolean p_352104_) {
      this(p_352448_, p_352313_, p_352270_.toString(), p_352104_);
   }

   public ConfirmLinkScreen(BooleanConsumer p_352145_, Component p_352090_, Component p_352169_, URI p_352197_, Component p_352365_, boolean p_352117_) {
      this(p_352145_, p_352090_, p_352169_, p_352197_.toString(), p_352365_, true);
   }

   public ConfirmLinkScreen(BooleanConsumer p_240191_, Component p_240192_, Component p_240193_, String p_240194_, Component p_240195_, boolean p_240196_) {
      super(p_240191_, p_240192_, p_240193_);
      this.yesButtonComponent = p_240196_ ? CommonComponents.GUI_OPEN_IN_BROWSER : CommonComponents.GUI_YES;
      this.noButtonComponent = p_240195_;
      this.showWarning = !p_240196_;
      this.url = p_240194_;
   }

   protected static MutableComponent confirmMessage(boolean p_239180_, String p_239181_) {
      return confirmMessage(p_239180_).append(CommonComponents.SPACE).append(Component.literal(p_239181_));
   }

   protected static MutableComponent confirmMessage(boolean p_240014_) {
      return Component.translatable(p_240014_ ? "chat.link.confirmTrusted" : "chat.link.confirm");
   }

   @Override
   protected void addAdditionalText() {
      if (this.showWarning) {
         this.layout.addChild(new StringWidget(WARNING_TEXT, this.font));
      }
   }

   @Override
   protected void addButtons(LinearLayout p_428302_) {
      this.yesButton = p_428302_.addChild(Button.builder(this.yesButtonComponent, p_169249_ -> this.callback.accept(true)).width(100).build());
      p_428302_.addChild(Button.builder(CommonComponents.GUI_COPY_TO_CLIPBOARD, p_169247_ -> {
         this.copyToClipboard();
         this.callback.accept(false);
      }).width(100).build());
      this.noButton = p_428302_.addChild(Button.builder(this.noButtonComponent, p_169245_ -> this.callback.accept(false)).width(100).build());
   }

   public void copyToClipboard() {
      this.minecraft.keyboardHandler.setClipboard(this.url);
   }

   public static void confirmLinkNow(Screen p_350478_, String p_350522_, boolean p_350852_) {
      Minecraft minecraft = Minecraft.getInstance();
      minecraft.setScreen(new ConfirmLinkScreen(p_465495_ -> {
         if (p_465495_) {
            Util.getPlatform().openUri(p_350522_);
         }

         minecraft.setScreen(p_350478_);
      }, p_350522_, p_350852_));
   }

   public static void confirmLinkNow(Screen p_352415_, URI p_352168_, boolean p_352122_) {
      Minecraft minecraft = Minecraft.getInstance();
      minecraft.setScreen(new ConfirmLinkScreen(p_465499_ -> {
         if (p_465499_) {
            Util.getPlatform().openUri(p_352168_);
         }

         minecraft.setScreen(p_352415_);
      }, p_352168_.toString(), p_352122_));
   }

   public static void confirmLinkNow(Screen p_352190_, URI p_352392_) {
      confirmLinkNow(p_352190_, p_352392_, true);
   }

   public static void confirmLinkNow(Screen p_275593_, String p_275417_) {
      confirmLinkNow(p_275593_, p_275417_, true);
   }

   public static Button.OnPress confirmLink(Screen p_350304_, String p_350370_, boolean p_350962_) {
      return p_349796_ -> confirmLinkNow(p_350304_, p_350370_, p_350962_);
   }

   public static Button.OnPress confirmLink(Screen p_352068_, URI p_352436_, boolean p_352216_) {
      return p_351646_ -> confirmLinkNow(p_352068_, p_352436_, p_352216_);
   }

   public static Button.OnPress confirmLink(Screen p_275326_, String p_275241_) {
      return confirmLink(p_275326_, p_275241_, true);
   }

   public static Button.OnPress confirmLink(Screen p_352385_, URI p_352416_) {
      return confirmLink(p_352385_, p_352416_, true);
   }
}
