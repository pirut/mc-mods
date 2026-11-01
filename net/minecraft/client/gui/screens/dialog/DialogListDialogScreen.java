package net.minecraft.client.gui.screens.dialog;

import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.DialogListDialog;
import net.minecraft.server.dialog.action.StaticAction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DialogListDialogScreen extends ButtonListDialogScreen<DialogListDialog> {
   public DialogListDialogScreen(@Nullable Screen p_425808_, DialogListDialog p_425874_, DialogConnectionAccess p_427482_) {
      super(p_425808_, p_425874_, p_427482_);
   }

   protected Stream<ActionButton> createListActions(DialogListDialog p_425863_, DialogConnectionAccess p_427504_) {
      return p_425863_.dialogs().stream().map(p_428062_ -> createDialogClickAction(p_425863_, (Holder<Dialog>)p_428062_));
   }

   private static ActionButton createDialogClickAction(DialogListDialog p_426162_, Holder<Dialog> p_425952_) {
      return new ActionButton(
         new CommonButtonData(p_425952_.value().common().computeExternalTitle(), p_426162_.buttonWidth()),
         Optional.of(new StaticAction(new ClickEvent.ShowDialog(p_425952_)))
      );
   }
}
