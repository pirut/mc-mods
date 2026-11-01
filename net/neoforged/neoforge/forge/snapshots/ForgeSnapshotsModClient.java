package net.neoforged.neoforge.forge.snapshots;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.NeoForgeMod;

public class ForgeSnapshotsModClient {
   public static void renderMainMenuWarning(String neoForgeVersion, GuiGraphics graphics, Font font, int width, int height, int alpha) {
      if (NeoForgeMod.isPRBuild()) {
         graphics.drawCenteredString(font, Component.translatable("loadwarning.neoforge.prbuild"), width / 2, 4 + (9 + 1) / 2, 16777215 | alpha);
      } else if (neoForgeVersion.contains("-beta")) {
         Component line = Component.translatable("neoforge.update.beta.1", ChatFormatting.RED.toString(), ChatFormatting.RESET.toString())
            .withStyle(ChatFormatting.RED);
         graphics.drawCenteredString(font, line, width / 2, 4 + 0 * (9 + 1), 16777215 | alpha);
         line = Component.translatable("neoforge.update.beta.2");
         graphics.drawCenteredString(font, line, width / 2, 4 + 1 * (9 + 1), 16777215 | alpha);
      }
   }
}
