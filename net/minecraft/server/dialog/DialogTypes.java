package net.minecraft.server.dialog;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;

public class DialogTypes {
   public static MapCodec<? extends Dialog> bootstrap(Registry<MapCodec<? extends Dialog>> p_425643_) {
      Registry.register(p_425643_, "notice", NoticeDialog.MAP_CODEC);
      Registry.register(p_425643_, "server_links", ServerLinksDialog.MAP_CODEC);
      Registry.register(p_425643_, "dialog_list", DialogListDialog.MAP_CODEC);
      Registry.register(p_425643_, "multi_action", MultiActionDialog.MAP_CODEC);
      return Registry.register(p_425643_, "confirmation", ConfirmationDialog.MAP_CODEC);
   }
}
