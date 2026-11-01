package com.mojang.realmsclient.util;

import com.mojang.authlib.yggdrasil.ProfileResult;
import java.util.Date;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsUtil {
   private static final Component RIGHT_NOW = Component.translatable("mco.util.time.now");
   private static final int MINUTES = 60;
   private static final int HOURS = 3600;
   private static final int DAYS = 86400;

   public static Component convertToAgePresentation(long p_287679_) {
      if (p_287679_ < 0L) {
         return RIGHT_NOW;
      } else {
         long $$1 = p_287679_ / 1000L;
         if ($$1 < 60L) {
            return Component.translatable("mco.time.secondsAgo", $$1);
         } else if ($$1 < 3600L) {
            long $$2 = $$1 / 60L;
            return Component.translatable("mco.time.minutesAgo", $$2);
         } else if ($$1 < 86400L) {
            long $$3 = $$1 / 3600L;
            return Component.translatable("mco.time.hoursAgo", $$3);
         } else {
            long $$4 = $$1 / 86400L;
            return Component.translatable("mco.time.daysAgo", $$4);
         }
      }
   }

   public static Component convertToAgePresentationFromInstant(Date p_287698_) {
      return convertToAgePresentation(System.currentTimeMillis() - p_287698_.getTime());
   }

   public static void renderPlayerFace(GuiGraphics p_281255_, int p_281818_, int p_281791_, int p_282088_, UUID p_294599_) {
      Minecraft $$5 = Minecraft.getInstance();
      ProfileResult $$6 = $$5.getMinecraftSessionService().fetchProfile(p_294599_, false);
      PlayerSkin $$7 = $$6 != null ? $$5.getSkinManager().getInsecureSkin($$6.profile()) : DefaultPlayerSkin.get(p_294599_);
      PlayerFaceRenderer.draw(p_281255_, $$7.texture(), p_281818_, p_281791_, p_282088_);
   }
}
