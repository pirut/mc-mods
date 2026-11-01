package net.minecraft.client.gui.screens.inventory.tooltip;

import com.mojang.authlib.yggdrasil.ProfileResult;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientActivePlayersTooltip implements ClientTooltipComponent {
   private static final int SKIN_SIZE = 10;
   private static final int PADDING = 2;
   private final List<ProfileResult> activePlayers;

   public ClientActivePlayersTooltip(ClientActivePlayersTooltip.ActivePlayersTooltip p_350898_) {
      this.activePlayers = p_350898_.profiles();
   }

   @Override
   public int getHeight() {
      return this.activePlayers.size() * 12 + 2;
   }

   @Override
   public int getWidth(Font p_351017_) {
      int $$1 = 0;

      for (ProfileResult $$2 : this.activePlayers) {
         int $$3 = p_351017_.width($$2.profile().getName());
         if ($$3 > $$1) {
            $$1 = $$3;
         }
      }

      return $$1 + 10 + 6;
   }

   @Override
   public void renderImage(Font p_350808_, int p_350702_, int p_350999_, GuiGraphics p_350342_) {
      for (int $$4 = 0; $$4 < this.activePlayers.size(); $$4++) {
         ProfileResult $$5 = this.activePlayers.get($$4);
         int $$6 = p_350999_ + 2 + $$4 * 12;
         PlayerFaceRenderer.draw(p_350342_, Minecraft.getInstance().getSkinManager().getInsecureSkin($$5.profile()), p_350702_ + 2, $$6, 10);
         p_350342_.drawString(p_350808_, $$5.profile().getName(), p_350702_ + 10 + 4, $$6 + 2, -1);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record ActivePlayersTooltip(List<ProfileResult> profiles) implements TooltipComponent {
   }
}
