package net.minecraft.client.gui.screens.dialog;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.dialog.body.DialogBodyHandlers;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.DialogAction;
import net.minecraft.server.dialog.Input;
import net.minecraft.server.dialog.body.DialogBody;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class DialogScreen<T extends Dialog> extends Screen {
   public static final Component DISCONNECT = Component.translatable("menu.custom_screen_info.disconnect");
   private static final int WARNING_BUTTON_SIZE = 20;
   private static final WidgetSprites WARNING_BUTTON_SPRITES = new WidgetSprites(
      Identifier.withDefaultNamespace("dialog/warning_button"),
      Identifier.withDefaultNamespace("dialog/warning_button_disabled"),
      Identifier.withDefaultNamespace("dialog/warning_button_highlighted")
   );
   private final T dialog;
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   @Nullable
   private final Screen previousScreen;
   @Nullable
   private ScrollableLayout bodyScroll;
   private Button warningButton;
   private final DialogConnectionAccess connectionAccess;
   private Supplier<Optional<ClickEvent>> onClose = DialogControlSet.EMPTY_ACTION;

   public DialogScreen(@Nullable Screen p_425924_, T p_426171_, DialogConnectionAccess p_427372_) {
      super(p_426171_.common().title());
      this.dialog = p_426171_;
      this.previousScreen = p_425924_;
      this.connectionAccess = p_427372_;
   }

   @Override
   protected final void init() {
      super.init();
      this.warningButton = this.createWarningButton();
      this.warningButton.setTabOrderGroup(-10);
      DialogControlSet dialogcontrolset = new DialogControlSet(this);
      LinearLayout linearlayout = LinearLayout.vertical().spacing(10);
      linearlayout.defaultCellSetting().alignHorizontallyCenter();
      this.layout.addToHeader(this.createTitleWithWarningButton());

      for (DialogBody dialogbody : this.dialog.common().body()) {
         LayoutElement layoutelement = DialogBodyHandlers.createBodyElement(this, dialogbody);
         if (layoutelement != null) {
            linearlayout.addChild(layoutelement);
         }
      }

      for (Input input : this.dialog.common().inputs()) {
         dialogcontrolset.addInput(input, linearlayout::addChild);
      }

      this.populateBodyElements(linearlayout, dialogcontrolset, this.dialog, this.connectionAccess);
      this.bodyScroll = new ScrollableLayout(this.minecraft, linearlayout, this.layout.getContentHeight());
      this.layout.addToContents(this.bodyScroll);
      this.updateHeaderAndFooter(this.layout, dialogcontrolset, this.dialog, this.connectionAccess);
      this.onClose = dialogcontrolset.bindAction(this.dialog.onCancel());
      this.layout.visitWidgets(p_425641_ -> {
         if (p_425641_ != this.warningButton) {
            this.addRenderableWidget(p_425641_);
         }
      });
      this.addRenderableWidget(this.warningButton);
      this.repositionElements();
   }

   protected void populateBodyElements(LinearLayout p_425540_, DialogControlSet p_428409_, T p_426230_, DialogConnectionAccess p_427345_) {
   }

   protected void updateHeaderAndFooter(HeaderAndFooterLayout p_425628_, DialogControlSet p_428522_, T p_425752_, DialogConnectionAccess p_427506_) {
   }

   @Override
   protected void repositionElements() {
      this.bodyScroll.setMaxHeight(this.layout.getContentHeight());
      this.layout.arrangeElements();
      this.makeSureWarningButtonIsInBounds();
   }

   protected LayoutElement createTitleWithWarningButton() {
      LinearLayout linearlayout = LinearLayout.horizontal().spacing(10);
      linearlayout.defaultCellSetting().alignHorizontallyCenter().alignVerticallyMiddle();
      linearlayout.addChild(new StringWidget(this.title, this.font));
      linearlayout.addChild(this.warningButton);
      return linearlayout;
   }

   protected void makeSureWarningButtonIsInBounds() {
      int i = this.warningButton.getX();
      int j = this.warningButton.getY();
      if (i < 0 || j < 0 || i > this.width - 20 || j > this.height - 20) {
         this.warningButton.setX(Math.max(0, this.width - 40));
         this.warningButton.setY(Math.min(5, this.height));
      }
   }

   private Button createWarningButton() {
      ImageButton imagebutton = new ImageButton(
         0,
         0,
         20,
         20,
         WARNING_BUTTON_SPRITES,
         p_450696_ -> this.minecraft.setScreen(DialogScreen.WarningScreen.create(this.minecraft, this.connectionAccess, this)),
         Component.translatable("menu.custom_screen_info.button_narration")
      );
      imagebutton.setTooltip(Tooltip.create(Component.translatable("menu.custom_screen_info.tooltip")));
      return imagebutton;
   }

   @Override
   public boolean isPauseScreen() {
      return this.dialog.common().pause();
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return this.dialog.common().canCloseWithEscape();
   }

   @Override
   public void onClose() {
      this.runAction(this.onClose.get(), DialogAction.CLOSE);
   }

   public void runAction(Optional<ClickEvent> p_428264_) {
      this.runAction(p_428264_, this.dialog.common().afterAction());
   }

   public void runAction(Optional<ClickEvent> p_428216_, DialogAction p_428273_) {
      Screen screen = (Screen)(switch (p_428273_) {
         case NONE -> this;
         case CLOSE -> this.previousScreen;
         case WAIT_FOR_RESPONSE -> new WaitingForResponseScreen(this.previousScreen);
      });
      if (p_428216_.isPresent()) {
         this.handleDialogClickEvent(p_428216_.get(), screen);
      } else {
         this.minecraft.setScreen(screen);
      }
   }

   private void handleDialogClickEvent(ClickEvent p_427388_, @Nullable Screen p_428550_) {
      switch (p_427388_) {
         case ClickEvent.RunCommand(String var10):
            this.connectionAccess.runCommand(Commands.trimOptionalPrefix(var10), p_428550_);
            break;
         case ClickEvent.ShowDialog clickevent$showdialog:
            this.connectionAccess.openDialog(clickevent$showdialog.dialog(), p_428550_);
            break;
         case ClickEvent.Custom clickevent$custom:
            this.connectionAccess.sendCustomAction(clickevent$custom.id(), clickevent$custom.payload());
            this.minecraft.setScreen(p_428550_);
            break;
         default:
            defaultHandleClickEvent(p_427388_, this.minecraft, p_428550_);
      }
   }

   @Nullable
   public Screen previousScreen() {
      return this.previousScreen;
   }

   protected static LayoutElement packControlsIntoColumns(List<? extends LayoutElement> p_425683_, int p_425923_) {
      GridLayout gridlayout = new GridLayout();
      gridlayout.defaultCellSetting().alignHorizontallyCenter();
      gridlayout.columnSpacing(2).rowSpacing(2);
      int i = p_425683_.size();
      int j = i / p_425923_;
      int k = j * p_425923_;

      for (int l = 0; l < k; l++) {
         gridlayout.addChild(p_425683_.get(l), l / p_425923_, l % p_425923_);
      }

      if (i != k) {
         LinearLayout linearlayout = LinearLayout.horizontal().spacing(2);
         linearlayout.defaultCellSetting().alignHorizontallyCenter();

         for (int i1 = k; i1 < i; i1++) {
            linearlayout.addChild(p_425683_.get(i1));
         }

         gridlayout.addChild(linearlayout, j, 0, 1, p_425923_);
      }

      return gridlayout;
   }

   @OnlyIn(Dist.CLIENT)
   public static class WarningScreen extends ConfirmScreen {
      private final MutableObject<Screen> returnScreen;

      public static Screen create(Minecraft p_427253_, DialogConnectionAccess p_451163_, Screen p_428764_) {
         return new DialogScreen.WarningScreen(p_427253_, p_451163_, new MutableObject(p_428764_));
      }

      private WarningScreen(Minecraft p_427476_, DialogConnectionAccess p_450983_, MutableObject<Screen> p_427235_) {
         super(
            p_460240_ -> {
               if (p_460240_) {
                  p_450983_.disconnect(DialogScreen.DISCONNECT);
               } else {
                  p_427476_.setScreen((Screen)p_427235_.get());
               }
            },
            Component.translatable("menu.custom_screen_info.title"),
            Component.translatable("menu.custom_screen_info.contents"),
            CommonComponents.disconnectButtonLabel(p_427476_.isLocalServer()),
            CommonComponents.GUI_BACK
         );
         this.returnScreen = p_427235_;
      }

      @Nullable
      public Screen returnScreen() {
         return (Screen)this.returnScreen.get();
      }

      public void updateReturnScreen(@Nullable Screen p_428767_) {
         this.returnScreen.setValue(p_428767_);
      }
   }
}
