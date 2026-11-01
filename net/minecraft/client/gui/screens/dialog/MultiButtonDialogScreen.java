package net.minecraft.client.gui.screens.dialog;

import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.MultiActionDialog;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class MultiButtonDialogScreen extends ButtonListDialogScreen<MultiActionDialog> {
   public MultiButtonDialogScreen(@Nullable Screen previousScreen, MultiActionDialog dialog, DialogConnectionAccess connectionAccess) {
      super(previousScreen, dialog, connectionAccess);
   }

   protected Stream<ActionButton> createListActions(MultiActionDialog multiActionDialog, DialogConnectionAccess dialogConnectionAccess) {
      return multiActionDialog.actions().stream();
   }
}
