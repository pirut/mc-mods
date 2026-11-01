package net.minecraft.client.gui.screens.dialog;

import java.util.stream.Stream;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.MultiActionDialog;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class MultiButtonDialogScreen extends ButtonListDialogScreen<MultiActionDialog> {
   public MultiButtonDialogScreen(@Nullable Screen p_426046_, MultiActionDialog p_425677_, DialogConnectionAccess p_427467_) {
      super(p_426046_, p_425677_, p_427467_);
   }

   protected Stream<ActionButton> createListActions(MultiActionDialog p_425688_, DialogConnectionAccess p_427456_) {
      return p_425688_.actions().stream();
   }
}
