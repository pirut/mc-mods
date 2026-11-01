package net.minecraft.client.gui.screens.dialog;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.dialog.input.InputControlHandlers;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.Input;
import net.minecraft.server.dialog.action.Action;

@Environment(EnvType.CLIENT)
public class DialogControlSet {
   public static final Supplier<Optional<ClickEvent>> EMPTY_ACTION = Optional::empty;
   private final DialogScreen<?> screen;
   private final Map<String, Action.ValueGetter> valueGetters = new HashMap<>();

   public DialogControlSet(DialogScreen<?> screen) {
      this.screen = screen;
   }

   public void addInput(Input input, Consumer<LayoutElement> adder) {
      String string = input.key();
      InputControlHandlers.createHandler(input.control(), this.screen, (layoutElement, valueGetter) -> {
         this.valueGetters.put(string, valueGetter);
         adder.accept(layoutElement);
      });
   }

   private static Button.Builder createDialogButton(CommonButtonData buttonData, Button.OnPress onPress) {
      Button.Builder builder = Button.builder(buttonData.label(), onPress);
      builder.width(buttonData.width());
      if (buttonData.tooltip().isPresent()) {
         builder = builder.tooltip(Tooltip.create(buttonData.tooltip().get()));
      }

      return builder;
   }

   public Supplier<Optional<ClickEvent>> bindAction(Optional<Action> action) {
      if (action.isPresent()) {
         Action action2 = action.get();
         return () -> action2.createAction(this.valueGetters);
      } else {
         return EMPTY_ACTION;
      }
   }

   public Button.Builder createActionButton(ActionButton button) {
      Supplier<Optional<ClickEvent>> supplier = this.bindAction(button.action());
      return createDialogButton(button.button(), buttonx -> this.screen.runAction(supplier.get()));
   }
}
