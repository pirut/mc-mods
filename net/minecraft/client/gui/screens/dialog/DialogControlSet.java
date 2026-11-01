package net.minecraft.client.gui.screens.dialog;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.dialog.input.InputControlHandlers;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.Input;
import net.minecraft.server.dialog.action.Action;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DialogControlSet {
   public static final Supplier<Optional<ClickEvent>> EMPTY_ACTION = Optional::empty;
   private final DialogScreen<?> screen;
   private final Map<String, Action.ValueGetter> valueGetters = new HashMap<>();

   public DialogControlSet(DialogScreen<?> p_428431_) {
      this.screen = p_428431_;
   }

   public void addInput(Input p_428434_, Consumer<LayoutElement> p_428339_) {
      String s = p_428434_.key();
      InputControlHandlers.createHandler(p_428434_.control(), this.screen, (p_428413_, p_428530_) -> {
         this.valueGetters.put(s, p_428530_);
         p_428339_.accept(p_428413_);
      });
   }

   private static Button.Builder createDialogButton(CommonButtonData p_428495_, Button.OnPress p_428202_) {
      Button.Builder button$builder = Button.builder(p_428495_.label(), p_428202_);
      button$builder.width(p_428495_.width());
      if (p_428495_.tooltip().isPresent()) {
         button$builder = button$builder.tooltip(Tooltip.create(p_428495_.tooltip().get()));
      }

      return button$builder;
   }

   public Supplier<Optional<ClickEvent>> bindAction(Optional<Action> p_428480_) {
      if (p_428480_.isPresent()) {
         Action action = p_428480_.get();
         return () -> action.createAction(this.valueGetters);
      } else {
         return EMPTY_ACTION;
      }
   }

   public Button.Builder createActionButton(ActionButton p_428203_) {
      Supplier<Optional<ClickEvent>> supplier = this.bindAction(p_428203_.action());
      return createDialogButton(p_428203_.button(), p_428410_ -> this.screen.runAction(supplier.get()));
   }
}
