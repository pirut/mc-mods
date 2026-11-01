package net.minecraft.client.gui.screens.dialog;

import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.server.ServerLinks;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.ServerLinksDialog;
import net.minecraft.server.dialog.action.StaticAction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ServerLinksDialogScreen extends ButtonListDialogScreen<ServerLinksDialog> {
   public ServerLinksDialogScreen(@Nullable Screen p_425820_, ServerLinksDialog p_426102_, DialogConnectionAccess p_427488_) {
      super(p_425820_, p_426102_, p_427488_);
   }

   protected Stream<ActionButton> createListActions(ServerLinksDialog p_427267_, DialogConnectionAccess p_427256_) {
      return p_427256_.serverLinks().entries().stream().map(p_428067_ -> createDialogClickAction(p_427267_, p_428067_));
   }

   private static ActionButton createDialogClickAction(ServerLinksDialog p_426181_, ServerLinks.Entry p_425645_) {
      return new ActionButton(
         new CommonButtonData(p_425645_.displayName(), p_426181_.buttonWidth()), Optional.of(new StaticAction(new ClickEvent.OpenUrl(p_425645_.link())))
      );
   }
}
